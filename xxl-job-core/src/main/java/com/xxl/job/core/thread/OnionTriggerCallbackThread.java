package com.xxl.job.core.thread;

import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.OnionHandleCallbackParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.checkpoint.concurrent.SavepointInterruptedException;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.util.ThrowableUtil;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 异步回调
 *
 * @author wujiuye 2020/04/16 jiuye-wu@msyc.cc
 */
public class OnionTriggerCallbackThread extends LogCallbackSupporFailRetryThread {

    private static OnionTriggerCallbackThread instance = new OnionTriggerCallbackThread();

    public static OnionTriggerCallbackThread getInstance() {
        return instance;
    }

    /**
     * job results callback queue
     */
    private LinkedBlockingQueue<OnionHandleCallbackParam> callBackQueue = new LinkedBlockingQueue<>();

    public static void pushCallBack(OnionHandleCallbackParam callback) {
        getInstance().callBackQueue.add(callback);
        logger.debug(">>>>>>>>>>> xxl-job, push callback request, logId:{}", callback.getCallbackParam().getLogId());
    }

    /**
     * callback thread
     */
    private Thread triggerCallbackThread;
    private volatile boolean toStop = false;

    public void start() {

        // valid
        if (XxlJobExecutor.getAdminBizList() == null) {
            logger.warn(">>>>>>>>>>> xxl-job, executor callback config fail, adminAddresses is null.");
            return;
        }

        // callback
        triggerCallbackThread = new Thread(new Runnable() {

            @Override
            public void run() {
                // normal callback
                while (!toStop) {
                    try {
                        OnionHandleCallbackParam callback = getInstance().callBackQueue.take();
                        int timeout = callback.getTimeout();
                        ReturnT<String> result;
                        try {
                            if (timeout > 0) {
                                result = callback.getFuture().get(timeout, TimeUnit.SECONDS);
                            } else {
                                result = callback.getFuture().get();
                            }
                        } catch (TimeoutException e) {
                            if (callback.getFuture().cancel(true)) {
                                result = new ReturnT<>(ReturnT.FAIL_CODE, "超时中断，超时时间设置为：" + callback.getTimeout() + "秒");
                            } else {
                                getInstance().callBackQueue.put(callback);
                                continue;
                            }
                        } catch (SavepointInterruptedException e) {
                            result = new ReturnT<>(ReturnT.FAIL_CODE, e.getMessage());
                        } catch (InterruptedException | UndeclaredThrowableException e) {
                            result = new ReturnT<>(ReturnT.FAIL_CODE, "被强制中断了（可能重启导致、或人工终止）");
                        } catch (Exception e) {
                            result = new ReturnT<>(ReturnT.FAIL_CODE, ThrowableUtil.toString(e));
                        }
                        // 回写执行结果
                        callback.getCallbackParam().setExecuteResult(result);
                        List<HandleCallbackParam> callbackParamList = new ArrayList<>();
                        callbackParamList.add(callback.getCallbackParam());
                        doCallback(callbackParamList);
                    } catch (Exception e) {
                        if (!toStop) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                }
                logger.info(">>>>>>>>>>> xxl-job, executor callback thread destory.");
            }
        });

        triggerCallbackThread.setDaemon(true);
        triggerCallbackThread.setName("xxl-job, executor OnionTriggerCallbackThread");
        triggerCallbackThread.start();
    }

    public void toStop() {
        toStop = true;
        // stop callback, interrupt and wait
        if (triggerCallbackThread != null) {    // support empty admin address
            triggerCallbackThread.interrupt();
            try {
                triggerCallbackThread.join();
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

}
