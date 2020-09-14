package com.xxl.job.core.checkpoint;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 安全检查点管理员
 *
 * @author wujiuye 2020/09/10
 */
public interface CheckpointManager {

    /**
     * 设置检查点标志
     *
     * @param jobId 当前JobID （解决同一个定时任务可能同时被调度执行的情况）
     */
    default void setBlockLable(int jobId) {
        CheckpointContext.putSavepointLable(jobId);
    }

    /**
     * 移除当前设置的检查点标志
     *
     * @param jobId 当前JobID （解决同一个定时任务可能同时被调度执行的情况）
     */
    default void removeBlockLable(int jobId) {
        CheckpointContext.removeSavepointLable(jobId);
    }

    /**
     * 检查当前任务是否已经进入安全检查点，是否可中断当前任务
     *
     * @param jobId    当前任务ID
     * @param time     阻塞等待超时时间
     * @param timeUnit 阻塞等待超时时间单位
     */
    default boolean checkpoint(int jobId, long time, TimeUnit timeUnit) {
        Optional<Counter> counter = CheckpointContext.getCurCounter(jobId);
        if (!counter.isPresent()) {
            return true;
        }
        try {
            counter.get().awit(time, timeUnit);
            return true;
        } catch (TimeoutException | InterruptedException e) {
            return false;
        }
    }

}
