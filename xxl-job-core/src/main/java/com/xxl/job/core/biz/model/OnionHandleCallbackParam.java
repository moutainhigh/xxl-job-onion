package com.xxl.job.core.biz.model;

import java.util.concurrent.Future;

/**
 * 包装回掉参数
 * Created by wujiuye on 2020/04/16
 */
public class OnionHandleCallbackParam {

    private Future<ReturnT<String>> future;
    /**
     * 超时时间
     */
    private int timeout;
    /**
     * 回传结果
     */
    private HandleCallbackParam callbackParam;

    public OnionHandleCallbackParam(Future<ReturnT<String>> future, int timeout, HandleCallbackParam callbackParam) {
        this.future = future;
        this.timeout = timeout;
        this.callbackParam = callbackParam;
    }

    public Future<ReturnT<String>> getFuture() {
        return future;
    }

    public HandleCallbackParam getCallbackParam() {
        return callbackParam;
    }

    public int getTimeout() {
        return timeout;
    }

}
