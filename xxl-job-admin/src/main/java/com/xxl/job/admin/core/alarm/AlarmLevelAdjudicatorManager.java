package com.xxl.job.admin.core.alarm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 日记级别审批员管理者
 *
 * @author wujiuye 2020/04/17
 */
@Component
public class AlarmLevelAdjudicatorManager {

    @Autowired(required = false)
    private List<AlarmLevelAdjudicator> alarmLevelAdjudicators;

    /**
     * 获取当前告警级别
     *
     * @param jobId
     * @return
     */
    public AlarmLevelAdjudicator.AlarmLevel getAlarmLevel(int jobId) {
        for (AlarmLevelAdjudicator adjudicator : alarmLevelAdjudicators) {
            AlarmLevelAdjudicator.AlarmLevel alarmLevel = adjudicator.getAlarmLevel(jobId);
            if (alarmLevel != null) {
                return alarmLevel;
            }
        }
        return AlarmLevelAdjudicator.AlarmLevel.OTHER;
    }

}
