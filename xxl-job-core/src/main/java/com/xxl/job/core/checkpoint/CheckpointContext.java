package com.xxl.job.core.checkpoint;

import java.util.*;

/**
 * 安全检查点实现的上下文
 *
 * @author wujiuye 2020/09/10
 */
public class CheckpointContext {

    /**
     * JobID与计数器的映射
     */
    private final static Map<Integer, Counter> JOB_COUNTER_MAP = new HashMap<>();
    /**
     * JobHandler当前执行的JOBID
     */
    private final static Map<Object, Integer> JOB_HANDLER_CUR_JOBID_MAP = new HashMap<>();
    /**
     * 任务ID->安全检查点标志
     */
    private final static Set<Integer> JOB_SAVEPOINT_LABLE = new HashSet<>();

    // ==================  jobHandler =======================================

    public static synchronized void putJob(Object jobHandler, Integer jobId) {
        if (JOB_COUNTER_MAP.containsKey(jobId)) {
            return;
        }
        JOB_COUNTER_MAP.put(jobId, new Counter(jobId));
        JOB_HANDLER_CUR_JOBID_MAP.put(jobHandler, jobId);
    }

    public static synchronized void removeJob(Object jobHandler, Integer jodId) {
        JOB_COUNTER_MAP.remove(jodId);
        JOB_HANDLER_CUR_JOBID_MAP.remove(jobHandler);
    }

    public static synchronized Optional<Integer> getCurJob(Object jobHandler) {
        Integer job = JOB_HANDLER_CUR_JOBID_MAP.get(jobHandler);
        return Optional.ofNullable(job);
    }

    // ==================  end jobHandler =======================================

    // ==================  计数器 =======================================

    public static synchronized Optional<Counter> getCurCounter(Integer jobId) {
        return Optional.ofNullable(JOB_COUNTER_MAP.get(jobId));
    }

    public static synchronized void putSavepointLable(Integer jobId) {
        JOB_SAVEPOINT_LABLE.add(jobId);
    }

    public static synchronized void removeSavepointLable(Integer jobId) {
        JOB_SAVEPOINT_LABLE.remove(jobId);
    }

    public static synchronized boolean existSavepointLable(Integer jobId) {
        return JOB_SAVEPOINT_LABLE.contains(jobId);
    }

    // ==================  end 计数器 =======================================

}
