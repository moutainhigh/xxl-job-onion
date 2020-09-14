package com.xxl.job.core.checkpoint;

import java.util.Optional;

/**
 * 安全检查点支持
 *
 * @author wujiuye 2020/09/10
 */
public interface SavepointSuppor {

    /**
     * 解决代理问题
     *
     * @return
     */
    default Object getId() {
        return this.getClass();
    }

    /**
     * 是否存在检查点标志，存在则不应用让当前线程进入检查点方法
     *
     * @return
     */
    default boolean existSavepointLable() {
        Optional<Integer> job = CheckpointContext.getCurJob(getId());
        return job.filter(CheckpointContext::existSavepointLable).isPresent();
    }

    /**
     * 计数器自增
     */
    default void incr() {
        Optional<Integer> job = CheckpointContext.getCurJob(getId());
        job.flatMap(CheckpointContext::getCurCounter).ifPresent(Counter::incr);
    }

    /**
     * 计数器自减
     */
    default void decr() {
        Optional<Integer> job = CheckpointContext.getCurJob(getId());
        job.flatMap(CheckpointContext::getCurCounter).ifPresent(Counter::decr);
    }

    /**
     * 获取进度，还剩多少个线程在等待离开安全点（方法执行完成）
     *
     * @return
     */
    default int getProgress() {
        Optional<Integer> job = CheckpointContext.getCurJob(getId());
        if (!job.isPresent()) {
            return 0;
        }
        Optional<Counter> counter = CheckpointContext.getCurCounter(job.get());
        return counter.map(Counter::get).orElse(0);
    }

}
