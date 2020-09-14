package com.xxl.job.core.handler.impl;

import com.xxl.job.core.biz.model.*;
import com.xxl.job.core.checkpoint.CheckpointContext;
import com.xxl.job.core.checkpoint.CheckpointManager;
import com.xxl.job.core.checkpoint.Counter;
import com.xxl.job.core.checkpoint.concurrent.SavepointThreadPoolExecutor;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.handler.OnionJobHandler;
import com.xxl.job.core.handler.OnionShardingJobHandler;
import com.xxl.job.core.handler.OnionBeanJobProxy;
import com.xxl.job.core.thread.OnionTriggerCallbackThread;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Onion分片的JobHandler映射器
 * 使用线程池执行任务
 *
 * @author wujiuye 2020/04/16 jiuye-wu@msyc.cc
 */
public class OnionShardingJobHandlerMapping {

    /**
     * 线程池大小
     */
    private final static int MAX_EXECUTOR_THREADS = Integer.parseInt(System.getProperty("onion_bean_threads", "200"));

    private ConcurrentMap<String, OnionBeanJobProxy<?>> jobHandlerMap;
    private SavepointThreadPoolExecutor executorService;
    private AtomicInteger threadCount;
    private ConcurrentMap<String, Future<ReturnT<String>>> futureConcurrentMap;
    private ConcurrentMap<Integer, String> jobIdAndFutureMap;

    public OnionShardingJobHandlerMapping() {
        jobHandlerMap = new ConcurrentHashMap<>();
        futureConcurrentMap = new ConcurrentHashMap<>();
        jobIdAndFutureMap = new ConcurrentHashMap<>();
        threadCount = new AtomicInteger(0);
        executorService = new SavepointThreadPoolExecutor(0, MAX_EXECUTOR_THREADS,
                60, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                r -> new Thread(r, "OnionJobHandlerThread-" + threadCount.getAndIncrement()),
                (r, executor) -> {
                    throw new RuntimeException("OnionJobHandler线程池已满，丢弃任务!");
                });
    }

    /**
     * 注册JobHandler
     *
     * @param beanName   bean的名称，也是任务的名称
     * @param jobHandler OnionShardingJobHandler or OnionJobHandler
     */
    public void putJobHandler(String beanName, Object jobHandler) {
        if (this.jobHandlerMap.containsKey(beanName)) {
            throw new RuntimeException("JobHandler:" + beanName + " already exist.");
        }
        if (jobHandler instanceof OnionJobHandler) {
            this.jobHandlerMap.put(beanName, new OnionBeanJobProxy((OnionJobHandler) jobHandler));
        } else if (jobHandler instanceof OnionShardingJobHandler) {
            this.jobHandlerMap.put(beanName, new OnionBeanJobProxy((OnionShardingJobHandler) jobHandler));
        } else {
            throw new UnsupportedOperationException("不支持的JobHandler类型，分片任务请继承OnionShardingJobHandler，非分片任务请继承OnionJobHandler");
        }
    }

    /**
     * 执行任务
     *
     * @param triggerParam
     * @return
     */
    public ReturnT<String> execute(TriggerParam triggerParam) {
        OnionBeanJobProxy<?> onionJobHandler = jobHandlerMap.get(triggerParam.getExecutorHandler());
        if (onionJobHandler == null) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "not found OnionShardingJobHandler by name:{}" + triggerParam.getExecutorHandler());
        }
        Future<ReturnT<String>> shardingJobResultFuture = futureConcurrentMap.get(triggerParam.getExecutorHandler());
        if (shardingJobResultFuture != null) {
            if (shardingJobResultFuture.isDone() || shardingJobResultFuture.isCancelled()) {
                futureConcurrentMap.remove(triggerParam.getExecutorHandler());
            } else {
                // 实现阻塞处理策略
                ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum.match(triggerParam.getExecutorBlockStrategy(), null);
                switch (blockStrategy) {
                    case DISCARD_LATER:
                        return new ReturnT<>(ReturnT.FAIL_CODE,
                                "JobHandler:" + triggerParam.getExecutorHandler()
                                        + " 上周期的任务还未执行完成，阻塞处理策略为丢弃当前任务执行！");
                    case COVER_EARLY:
                        KillParam killParam = new KillParam();
                        killParam.setJobId(triggerParam.getJobId());
                        try {
                            cancelJob(killParam);
                            futureConcurrentMap.remove(triggerParam.getExecutorHandler());
                        } catch (Exception e) {
                            return new ReturnT<>(ReturnT.FAIL_CODE, "JobHandler:" + triggerParam.getExecutorHandler()
                                    + " 阻塞处理策略为中断之前的任务，但尝试中断失败：" + e.getMessage());
                        }
                        break;
                    case SERIAL_EXECUTION:
                    default:
                        return new ReturnT<>(ReturnT.FAIL_CODE, "不支持阻塞策略为单机串行执行！JobHandler:"
                                + triggerParam.getExecutorHandler() + " 上周期的任务还未执行完成！");
                }
            }
        }
        try {
            shardingJobResultFuture = executorService.submit(triggerParam.getJobId(), onionJobHandler,
                    () -> onionJobHandler.doExecute(triggerParam));
            futureConcurrentMap.put(triggerParam.getExecutorHandler(), shardingJobResultFuture);
            jobIdAndFutureMap.put(triggerParam.getJobId(), triggerParam.getExecutorHandler());
            OnionTriggerCallbackThread.pushCallBack(new OnionHandleCallbackParam(shardingJobResultFuture,
                    triggerParam.getExecutorTimeout(),
                    // 构建执行结果
                    new HandleCallbackParam(triggerParam.getLogId(), triggerParam.getLogDateTime(), null)));
            return ReturnT.SUCCESS;
        } catch (RuntimeException e) {
            ReturnT<String> returnT = ReturnT.FAIL;
            returnT.setMsg(e.getMessage());
            return returnT;
        }
    }

    /**
     * 根据任务id获取Future
     *
     * @param jobId
     * @return
     */
    private Future<?> getFutureByJobId(int jobId) {
        String jobHandlerName = jobIdAndFutureMap.get(jobId);
        if (jobHandlerName == null) {
            return null;
        }
        return futureConcurrentMap.get(jobHandlerName);
    }

    /**
     * 中断job
     *
     * @param killParam kill信息
     * @return false: 任务不存在 true: 任务已经完成｜任务已经中断｜任务中断成功
     * @throws Exception 中断异常
     */
    public boolean cancelJob(KillParam killParam) throws Exception {
        Future<?> future = getFutureByJobId(killParam.getJobId());
        if (future == null) {
            return false;
        }
        if (future.isDone() || future.isCancelled()) {
            return true;
        } else {
            String jobHandlerName = jobIdAndFutureMap.get(killParam.getJobId());
            CheckpointManager checkpointManager = jobHandlerMap.get(jobHandlerName);
            // 设置安全检查点标志
            checkpointManager.setBlockLable(killParam.getJobId());
            // 检查是否进入安全检查点
            if (!checkpointManager.checkpoint(killParam.getJobId(), 0, TimeUnit.SECONDS)) {
                // 获取进度
                Optional<Counter> counter = CheckpointContext.getCurCounter(killParam.getJobId());
                if (counter.isPresent()) {
                    throw new UnsupportedOperationException("等待任务进入安全检查点，当前剩余：" + counter.get().get() + "，请稍后重试！");
                }
            }
            if (future.cancel(true)) {
                return true;
            }
        }
        throw new UnsupportedOperationException("任务中断失败，请重试！");
    }

    /**
     * 判断某个job是否还在执行
     *
     * @param idleBeatParam
     * @return
     */
    public boolean jobIsRuning(IdleBeatParam idleBeatParam) {
        Future<?> future = getFutureByJobId(idleBeatParam.getJobId());
        if (future == null) {
            return false;
        }
        return !future.isDone() && !future.isCancelled();
    }

}
