package com.xxl.job.admin.core.cron;

/**
 * cron相关工具类
 *
 * @author wujiuye 2020/04/17
 **/
public class CronUtils {

    /**
     * 定时任务执行的周期单位
     */
    public enum CronCycl {
        /**
         * 秒
         */
        SECOND,
        MINUTE,
        HOUR,
        DAY,
        MONTH
    }

    /**
     * 获取cron的周期
     *
     * @param cron 只支持简单的cron表达式，复杂的会抛出异常
     * @return
     */
    public static CronCycl getCronCycl(String cron) {
        cron = cron.trim();
        if (!cron.endsWith("?")) {
            throw new RuntimeException("cron表达式格式不正确！");
        }
        String[] items = cron.replace(" ?", "").split(" ");
        if (items.length != 5) {
            throw new RuntimeException("cron表达式格式不正确！");
        }
        for (int index = 0; index < items.length; index++) {
            if ("*".equals(items[index])) {
                switch (index - 1) {
                    case 0:
                        return CronCycl.SECOND;
                    case 1:
                        return CronCycl.MINUTE;
                    case 2:
                        return CronCycl.HOUR;
                    case 3:
                        return CronCycl.DAY;
                    default:
                }
            }
        }
        return CronCycl.MONTH;
    }

}
