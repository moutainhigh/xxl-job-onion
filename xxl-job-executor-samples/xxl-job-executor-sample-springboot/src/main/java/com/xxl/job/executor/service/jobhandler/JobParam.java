package com.xxl.job.executor.service.jobhandler;

import java.util.Date;

/**
 * Job执行参数
 *
 * @author wujiuye 2020/04/16
 */
public class JobParam {

    private Date startDate;
    private Date endDate;
    private String other;
    private Integer index;

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getOther() {
        return other;
    }

    public void setOther(String other) {
        this.other = other;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "JobParam{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", other='" + other + '\'' +
                ", index=" + index +
                '}';
    }

}
