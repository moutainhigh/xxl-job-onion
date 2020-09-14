package com.xxl.job.core.handler;

import com.xxl.job.core.checkpoint.SavepointSuppor;

/**
 * ONION_BEAN模式
 *
 * @author wujiuye 2020/09/09
 */
@FunctionalInterface
public interface OnionJobHandler<T> extends SavepointSuppor {

    /**
     * 开始执行任务
     *
     * @param param 参数，前端以json格式配置
     * @return
     * @throws Exception 程序执行异常需要往外抛出异常，以便重新调用执行
     */
    void doExecute(T param) throws Exception;

}