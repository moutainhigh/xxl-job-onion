package com.xxl.job.core.thread;

import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.RegistryConfig;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.log.XxlJobFileAppender;
import com.xxl.job.core.log.XxlJobLogger;
import com.xxl.job.core.util.FileUtil;
import com.xxl.job.core.util.serialize.JsonSerializeTool;
import com.xxl.job.core.util.serialize.SerializeStategay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 回调结果给admin（支持重试）
 *
 * @author wujiuye 2020/09/09
 */
public abstract class LogCallbackSupporFailRetryThread {

    protected static Logger logger = LoggerFactory.getLogger(OnionTriggerCallbackThread.class);

    private final static SerializeStategay serializeStategay = new JsonSerializeTool();

    private static String failCallbackFilePath = XxlJobFileAppender.getLogPath().concat(File.separator)
            .concat("callbacklog").concat(File.separator);

    private static String failCallbackFileName = failCallbackFilePath.concat("xxl-job-callback-{x}").concat(".log");

    private static Thread triggerRetryCallbackThread;

    static {
        // 失败重试线程
        triggerRetryCallbackThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    try {
                        retryFailCallbackFile();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                    try {
                        TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                    } catch (InterruptedException e) {
                        //
                    }
                }
                logger.info(">>>>>>>>>>> xxl-job, executor retry callback thread destory.");
            }
        });
        triggerRetryCallbackThread.setDaemon(true);
        triggerRetryCallbackThread.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (triggerRetryCallbackThread != null) {
                    triggerRetryCallbackThread.interrupt();
                }
            }
        });
    }

    /**
     * do callback, will retry if error
     *
     * @param callbackParamList
     */
    protected static void doCallback(List<HandleCallbackParam> callbackParamList) {
        boolean callbackRet = false;
        // callback, will retry if error
        for (AdminBiz adminBiz : XxlJobExecutor.getAdminBizList()) {
            try {
                // 回传任务执行结果
                ReturnT<String> callbackResult = adminBiz.callback(callbackParamList);
                // 主要用于admin部署集群的情况下，只将结果回传给一个admin
                if (callbackResult != null && ReturnT.SUCCESS_CODE == callbackResult.getCode()) {
                    callbackLog(callbackParamList, "<br>----------- xxl-job job callback finish.");
                    callbackRet = true;
                    break;
                } else {
                    callbackLog(callbackParamList, "<br>----------- xxl-job job callback fail, callbackResult:" + callbackResult);
                }
            } catch (Exception e) {
                callbackLog(callbackParamList, "<br>----------- xxl-job job callback error, errorMsg:" + e.getMessage());
            }
        }
        if (!callbackRet) {
            appendFailCallbackFile(callbackParamList);
        }
    }

    /**
     * callback log
     */
    protected static void callbackLog(List<HandleCallbackParam> callbackParamList, String logContent) {
        for (HandleCallbackParam callbackParam : callbackParamList) {
            String logFileName = XxlJobFileAppender.makeLogFileName(new Date(callbackParam.getLogDateTim()), callbackParam.getLogId());
            XxlJobFileAppender.contextHolder.set(logFileName);
            XxlJobLogger.log(logContent);
        }
    }

    protected static void appendFailCallbackFile(List<HandleCallbackParam> callbackParamList) {
        // valid
        if (callbackParamList == null || callbackParamList.size() == 0) {
            return;
        }
        // append file
        File callbackLogFile = new File(failCallbackFileName.replace("{x}",
                String.valueOf(System.currentTimeMillis())));
        if (callbackLogFile.exists()) {
            for (int i = 0; i < 100; i++) {
                callbackLogFile = new File(failCallbackFileName.replace("{x}",
                        String.valueOf(System.currentTimeMillis()).concat("-").concat(String.valueOf(i))));
                if (!callbackLogFile.exists()) {
                    break;
                }
            }
        }
        byte[] callbackParamList_bytes = serializeStategay.serializeArray(callbackParamList);
        FileUtil.writeFileContent(callbackLogFile, callbackParamList_bytes);
    }

    protected static void retryFailCallbackFile() {
        // valid
        File callbackLogPath = new File(failCallbackFilePath);
        if (!callbackLogPath.exists()) {
            return;
        }
        if (callbackLogPath.isFile()) {
            if (!callbackLogPath.delete()) {
                return;
            }
        }
        if (!(callbackLogPath.isDirectory() && callbackLogPath.list() != null)) {
            return;
        }
        File[] files = callbackLogPath.listFiles();
        if (files == null || files.length == 0) {
            return;
        }
        // load and clear file, retry
        for (File callbaclLogFile : files) {
            byte[] callbackParamList_bytes = FileUtil.readFileContent(callbaclLogFile);
            List<HandleCallbackParam> callbackParamList = serializeStategay.deserializeArray(callbackParamList_bytes, HandleCallbackParam.class);
            if (callbaclLogFile.delete()) {
                doCallback(callbackParamList);
            }
        }
    }

}
