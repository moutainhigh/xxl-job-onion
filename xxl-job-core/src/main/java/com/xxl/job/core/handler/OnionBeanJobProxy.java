package com.xxl.job.core.handler;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.checkpoint.CheckpointContext;
import com.xxl.job.core.checkpoint.CheckpointManager;
import com.xxl.job.core.log.XxlJobFileAppender;
import com.xxl.job.core.log.XxlJobLogger;
import com.xxl.job.core.util.JobParamParser;
import com.xxl.job.core.util.ThrowableUtil;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Date;

/**
 * 代理 OnionShardingJobHandler
 *
 * @author wujiuye 2020/04/16 jiuye-wu@msyc.cc
 */
public class OnionBeanJobProxy<T> implements CheckpointManager {

    private Class<T> jobParamClass;
    private OnionJobHandler<T> onionJobHandler;
    private OnionShardingJobHandler<T> onionShardingJobHandler;

    public OnionBeanJobProxy(OnionJobHandler<T> onionJobHandler) {
        this.onionJobHandler = onionJobHandler;
        this.jobParamClass = (Class<T>) initJobParamType(this.onionJobHandler.getClass());
    }

    public OnionBeanJobProxy(OnionShardingJobHandler<T> onionShardingJobHandler) {
        this.onionShardingJobHandler = onionShardingJobHandler;
        this.jobParamClass = (Class<T>) initJobParamType(this.onionShardingJobHandler.getClass());
    }

    private static <T> Class<T> initJobParamType(Class<T> jobHandlerClass) {
        Type[] implInterfaces = jobHandlerClass.getGenericInterfaces();
        // 解决cglib动态代理问题
        if (jobHandlerClass.getName().contains("CGLIB")) {
            implInterfaces = jobHandlerClass.getSuperclass().getGenericInterfaces();
        }
        // 解决接口继承
        Type jobType = null;
        for (Type type : implInterfaces) {
            if (type.getTypeName().startsWith(OnionShardingJobHandler.class.getName())
                    || type.getTypeName().startsWith(OnionJobHandler.class.getName())) {
                jobType = type;
                break;
            } else {
                try {
                    int last = type.getTypeName().indexOf("<");
                    last = last > 0 ? last : type.getTypeName().length();
                    Class<?> las = Class.forName(type.getTypeName().substring(0, last));
                    if (OnionShardingJobHandler.class.isAssignableFrom(las)
                            || OnionJobHandler.class.isAssignableFrom(las)) {
                        jobType = type;
                        break;
                    }
                } catch (ClassNotFoundException e) {
                    //
                }
            }
        }
        if (jobType == null) {
            throw new RuntimeException("Missing type JobHandler by " + jobHandlerClass + ".");
        }
        // 获取泛型接口的泛型参数
        Type type = ((ParameterizedType) jobType).getActualTypeArguments()[0];
        if (!(type instanceof Class)) {
            throw new RuntimeException("Missing type parameter.");
        }
        return (Class<T>) type;
    }

    public ReturnT<String> doExecute(TriggerParam triggerParam) {
        try {
            CheckpointContext.putJob((onionJobHandler == null ? onionShardingJobHandler : onionJobHandler).getId(),
                    triggerParam.getJobId());
            String logFileName = XxlJobFileAppender.makeLogFileName(new Date(triggerParam.getLogDateTime()), triggerParam.getLogId());
            XxlJobFileAppender.contextHolder.set(logFileName);
            XxlJobLogger.log("<br>----------- xxl-job job execute start -----------<br>----------- Param:" + triggerParam.getExecutorParams());

            // OnionShardingJobHandler执行
            if (onionShardingJobHandler != null && triggerParam.isSharding()) {
                this.onionShardingJobHandler.doExecute(triggerParam.getBroadcastTotal(), triggerParam.getBroadcastIndex(),
                        JobParamParser.parseObject(triggerParam.getExecutorParams(), jobParamClass));
            }
            // OnionJobHandler执行
            else if (onionJobHandler != null && !triggerParam.isSharding()) {
                this.onionJobHandler.doExecute(JobParamParser.parseObject(triggerParam.getExecutorParams(), jobParamClass));
            }
            // 不支持
            else {
                throw new UnsupportedOperationException("JobHandler类型为：" +
                        (onionShardingJobHandler != null ? "OnionShardingJobHandler"
                                : (onionJobHandler != null ? "OnionJobHandler" : null))
                        + "不支持分片路由策略配置：" + triggerParam.isSharding());
            }

            XxlJobLogger.log("<br>----------- xxl-job job execute end(finish) -----------<br>----------- ");
            return ReturnT.SUCCESS;
        } catch (Throwable e) {
            String errorMsg = ThrowableUtil.toString(e);
            XxlJobLogger.log("<br>----------- JobThread Exception:" + errorMsg + "<br>----------- xxl-job job execute end(error) -----------");
            return new ReturnT<>(ReturnT.FAIL_CODE, errorMsg);
        } finally {
            XxlJobFileAppender.contextHolder.remove();
            CheckpointContext.removeJob((onionJobHandler == null ? onionShardingJobHandler : onionJobHandler).getId(),
                    triggerParam.getJobId());
        }
    }

}
