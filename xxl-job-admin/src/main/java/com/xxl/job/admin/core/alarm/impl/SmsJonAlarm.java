package com.xxl.job.admin.core.alarm.impl;

import com.xxl.job.admin.core.alarm.AlarmLevelAdjudicator;
import com.xxl.job.admin.core.alarm.AlarmerContent;
import com.xxl.job.admin.core.alarm.JobAlarm;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * job alarm by sms
 *
 * @author wujiuye 2020-04-17
 */
@Component
@ConditionalOnBean(SmsJonAlarm.SmsSender.class)
public class SmsJonAlarm implements JobAlarm {

    private static Logger logger = LoggerFactory.getLogger(SmsJonAlarm.class);
    /**
     * 多少个任务对应多少条记录
     */
    private static final ConcurrentMap<Integer, LocalDate> JOB_SEND_SMS_MAP = new ConcurrentHashMap<>();

    /**
     * 短信发送接口
     */
    public interface SmsSender {

        class SmsException extends RuntimeException {
            public SmsException(String msg) {
                super(msg);
            }
        }

        /**
         * 短信息发送接口，相同内容群发
         *
         * @param receiverPhone 接收人(一个或多个)
         * @param msgContent    消息内容
         * @throws
         */
        void sendSms(String msgContent, String... receiverPhone) throws SmsException;

    }

    @Resource
    private SmsSender smsSender;

    @Override
    public AlarmLevelAdjudicator.AlarmLevel[] handleLevel() {
        return AlarmLevelAdjudicator.AlarmLevel.values();
    }

    @Override
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog, AlarmLevelAdjudicator.AlarmLevel level) {
        // 其它级别用于更新短信发送成功映射的缓存
        if (level != AlarmLevelAdjudicator.AlarmLevel.FIRST) {
            // 新的升级过程移除旧的发送记录
            JOB_SEND_SMS_MAP.remove(info.getId());
            return true;
        }
        // 不填就不发，不发就设置为成功
        if (StringUtils.isEmpty(info.getAlarmPhone())) {
            return true;
        }
        // 未发送过｜|或上次发送已经是一天前
        if (!JOB_SEND_SMS_MAP.containsKey(info.getId())
                || !LocalDate.now().equals(JOB_SEND_SMS_MAP.get(info.getId()))) {
            try {
                String message = AlarmerContent.getEnv() + " 任务：" + info.getExecutorHandler() + " 一级告警，详细信息请查看邮件！";
                smsSender.sendSms(message, info.getAlarmPhone().split(","));
                JOB_SEND_SMS_MAP.put(info.getId(), LocalDate.now());
                return true;
            } catch (SmsSender.SmsException e) {
                logger.error(e.getMessage(), e);
                return false;
            }
        }
        return true;
    }

}
