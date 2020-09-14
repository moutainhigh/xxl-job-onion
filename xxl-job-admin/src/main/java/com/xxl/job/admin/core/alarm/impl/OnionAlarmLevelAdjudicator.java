package com.xxl.job.admin.core.alarm.impl;

import com.xxl.job.admin.core.alarm.AlarmLevelAdjudicator;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.cron.CronUtils;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.dao.XxlJobLogDao;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * onion的日记级别定义
 * -- 告警升级过程
 * -- 如果周期为秒钟，则判断是否连续失败60次及以上，是则升级为二级告警，无一级告警；
 * -- 如果周期为分钟，则判断是否连续失败3次及以上，是则升级为二级告警，连续失败5次及以上升级为一级告警；
 * -- 如果周期为小时，则判断是否连续失败1次及以上，是则升级为二级告警，连续失败3次及以上升级为一级告警；
 * -- 如果周期为一天或以上，直接升级为一级告警。
 *
 * @author wujiuye 2020/04/17
 */
@Component
public class OnionAlarmLevelAdjudicator implements AlarmLevelAdjudicator {

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

    @Override
    public AlarmLevel getAlarmLevel(int jobId) {
        XxlJobInfo xxlJobInfo = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().loadById(jobId);
        try {
            CronUtils.CronCycl cronCycl = CronUtils.getCronCycl(xxlJobInfo.getJobCron());
            int jobErrorCnt;
            switch (cronCycl) {
                case SECOND:
                    if (getErrorCntByJob(jobId) >= 60) {
                        return AlarmLevel.SCOND;
                    } else {
                        return AlarmLevel.OTHER;
                    }
                case MINUTE:
                    jobErrorCnt = getErrorCntByJob(jobId);
                    if (jobErrorCnt >= 5) {
                        return AlarmLevel.FIRST;
                    } else if (jobErrorCnt >= 3) {
                        return AlarmLevel.SCOND;
                    } else {
                        return AlarmLevel.OTHER;
                    }
                case HOUR:
                    jobErrorCnt = getErrorCntByJob(jobId);
                    if (jobErrorCnt >= 3) {
                        return AlarmLevel.FIRST;
                    } else {
                        return AlarmLevel.SCOND;
                    }
                case DAY:
                case MONTH:
                default:
                    return AlarmLevel.FIRST;
            }
        } catch (Exception e) {
            // 复杂的表达式直接使用一级告警咯
            return AlarmLevel.FIRST;
        }
    }

    /**
     * 获取job的连续失败次数
     *
     * @param jobId jobInfo表的id
     * @return
     */
    private int getErrorCntByJob(int jobId) {
        int result = 0;
        XxlJobLogDao xxlJobLogDao = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao();
        List<XxlJobLog> logs = xxlJobLogDao.selectJobLogByRange(jobId, 20);
        if (!CollectionUtils.isEmpty(logs)) {
            for (XxlJobLog log : logs) {
                if (log.getHandleCode() != 200 || log.getTriggerCode() != 200) {
                    result++;
                } else {
                    break;
                }
            }
        }
        return result;
    }

}
