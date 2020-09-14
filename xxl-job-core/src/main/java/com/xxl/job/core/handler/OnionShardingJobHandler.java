package com.xxl.job.core.handler;

import com.xxl.job.core.checkpoint.SavepointSuppor;

/**
 * 强制考虑分片的JobHandler接口
 *
 * @author wujiuye 2020/04/16 jiuye-wu@msyc.cc
 */
@FunctionalInterface
public interface OnionShardingJobHandler<T> extends SavepointSuppor {

    /**
     * 开始执行任务
     *
     * @param shardingTotal        分片总数
     * @param currentShardingIndex 当前分片索引
     * @param param                参数，前端以json格式配置
     * @return
     * @throws Exception 程序执行异常需要往外抛出异常，以便重新调用当前分片执行
     */
    void doExecute(int shardingTotal, int currentShardingIndex, T param) throws Exception;

}
