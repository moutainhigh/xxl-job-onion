package com.xxl.job.core.checkpoint.concurrent;

import com.xxl.job.core.checkpoint.CheckpointManager;

import java.lang.reflect.Proxy;
import java.util.concurrent.*;

/**
 * 异步线程池
 *
 * @author wujiuye 2020/09/11
 */
public class SavepointThreadPoolExecutor extends ThreadPoolExecutor {

    public SavepointThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                       TimeUnit unit, BlockingQueue<Runnable> workQueue,
                                       ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    /**
     * 提交Job执行
     *
     * @param jobId             Job的ID
     * @param checkpointManager 任务对应的安全检查点管理器
     * @param task              Job
     * @param <T>
     * @return
     */
    public <T> Future<T> submit(int jobId, CheckpointManager checkpointManager, Callable<T> task) {
        Future<T> future = super.submit(task);
        return (Future<T>) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[]{Future.class},
                new SavepointFutureInvocationHandler(jobId, checkpointManager, future));
    }

}
