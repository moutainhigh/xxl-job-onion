package com.xxl.job.core.checkpoint.concurrent;

/**
 * 中断异常
 *
 * @author wujiuye 2020/09/11
 */
public class SavepointInterruptedException extends InterruptedException {

    private String message;

    public SavepointInterruptedException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getLocalizedMessage() {
        return this.getMessage();
    }

}
