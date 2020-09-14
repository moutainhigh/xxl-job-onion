package com.xxl.job.admin.core.alarm.impl;

import com.xxl.job.admin.core.alarm.AlarmLevelAdjudicator;
import com.xxl.job.admin.core.alarm.JobAlarm;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 忽略告警
 *
 * @author wujiuye 2020/04/17
 */
@Component
public class IgnoreJobAlarm implements JobAlarm {

    private Logger ignoreJobAlarmLogger = LoggerFactory.getLogger(IgnoreJobAlarm.class);

    @Override
    public AlarmLevelAdjudicator.AlarmLevel[] handleLevel() {
        return new AlarmLevelAdjudicator.AlarmLevel[]{AlarmLevelAdjudicator.AlarmLevel.OTHER};
    }

    @Override
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog, AlarmLevelAdjudicator.AlarmLevel level) {
        ignoreJobAlarmLogger.info("----- IgnoreJobAlarm----- <br/>" + jobLog.getTriggerMsg());
        return level == AlarmLevelAdjudicator.AlarmLevel.OTHER;
    }

}
