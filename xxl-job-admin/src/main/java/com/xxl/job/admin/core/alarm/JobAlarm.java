package com.xxl.job.admin.core.alarm;

import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;

/**
 * @author xuxueli 2020-01-19
 */
public interface JobAlarm {

    /**
     * 处理哪些级别的日记
     *
     * @return
     */
    AlarmLevelAdjudicator.AlarmLevel[] handleLevel();

    /**
     * job alarm
     *
     * @param info
     * @param jobLog
     * @param level  告警级别
     * @return
     */
    boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog, AlarmLevelAdjudicator.AlarmLevel level);

}
