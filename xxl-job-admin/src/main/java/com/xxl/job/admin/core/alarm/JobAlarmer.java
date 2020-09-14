package com.xxl.job.admin.core.alarm;

import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class JobAlarmer implements ApplicationContextAware, InitializingBean, EnvironmentAware {

    private static Logger logger = LoggerFactory.getLogger(JobAlarmer.class);

    private ApplicationContext applicationContext;
    private List<JobAlarm> jobAlarmList;

    private String ENV = "";

    @Override
    public void setEnvironment(Environment environment) {
        ENV = "[" + environment.getActiveProfiles()[0] + "]";
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, JobAlarm> serviceBeanMap = applicationContext.getBeansOfType(JobAlarm.class);
        if (serviceBeanMap.size() > 0) {
            jobAlarmList = new ArrayList<>(serviceBeanMap.values());
        }
    }

    /**
     * job告警
     *
     * @param info   job信息
     * @param jobLog job执行日记
     * @return
     */
    public boolean alarm(XxlJobInfo info, XxlJobLog jobLog, AlarmLevelAdjudicator.AlarmLevel level) {
        try {
            AlarmerContent.setEnv(ENV);
            boolean result = false;
            if (jobAlarmList != null && jobAlarmList.size() > 0) {
                result = true;
                // 遍历所有告警器
                for (JobAlarm alarm : jobAlarmList) {
                    // 判断这个JobAlarm是否支持处理当前告警级别
                    boolean handle = false;
                    AlarmLevelAdjudicator.AlarmLevel[] handleLevels = alarm.handleLevel();
                    for (AlarmLevelAdjudicator.AlarmLevel alarmLevel : handleLevels) {
                        if (alarmLevel == level) {
                            handle = true;
                            break;
                        }
                    }
                    if (!handle) {
                        continue;
                    }
                    boolean resultItem = false;
                    try {
                        // 调用告警器的告警方法
                        resultItem = alarm.doAlarm(info, jobLog, level);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                    // 有一个处理失败都算失败
                    if (!resultItem) {
                        result = false;
                    }
                }
            }
            return result;
        } finally {
            AlarmerContent.remove();
        }
    }

}
