package com.xxl.job.admin.core.alarm;

import org.springframework.core.Ordered;

/**
 * 日记级别审批员
 *
 * @author wujiuye 2020/04/17
 */
public interface AlarmLevelAdjudicator extends Ordered {

    enum AlarmLevel {
        /**
         * 一级告警
         */
        FIRST,
        /**
         * 二级告警
         */
        SCOND,
        /**
         * 普通告警
         */
        OTHER
    }

    AlarmLevel getAlarmLevel(int jobId);

}
