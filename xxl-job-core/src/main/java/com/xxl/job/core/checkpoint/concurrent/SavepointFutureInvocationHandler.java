package com.xxl.job.core.checkpoint.concurrent;

import com.xxl.job.core.checkpoint.CheckpointContext;
import com.xxl.job.core.checkpoint.CheckpointManager;
import com.xxl.job.core.checkpoint.Counter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.Future;

/**
 * 通过代理模式让Future支持Savepoint
 *
 * @author wujiuye 2020/09/11
 */
public class SavepointFutureInvocationHandler implements InvocationHandler {

    private Future<?> future;
    private int jobId;
    private CheckpointManager checkpointManager;

    public SavepointFutureInvocationHandler(int jobId, CheckpointManager checkpointManager, Future<?> future) {
        this.future = future;
        this.jobId = jobId;
        this.checkpointManager = checkpointManager;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            Object result = method.invoke(future, args);
            if ("get".equalsIgnoreCase(method.getName())) {
                if (CheckpointContext.existSavepointLable(jobId)) {
                    Optional<Counter> counter = CheckpointContext.getCurCounter(jobId);
                    int alertCount = counter.map(Counter::getAlertCount).orElse(0);
                    throw new SavepointInterruptedException("任务存在安全检查点，属于中断结束，实际已执行方法总次数：" + alertCount);
                }
            }
            return result;
        } finally {
            if ("isCancelled".equalsIgnoreCase(method.getName()) || "isDone".equalsIgnoreCase(method.getName())
                    || "cancel".equalsIgnoreCase(method.getName()) || "get".equalsIgnoreCase(method.getName())) {
                if (future.isCancelled() || future.isDone()) {
                    // 移除安全检查点标志
                    checkpointManager.removeBlockLable(jobId);
                }
            }
        }
    }

}
