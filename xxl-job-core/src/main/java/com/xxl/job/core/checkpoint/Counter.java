package com.xxl.job.core.checkpoint;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 计数器
 *
 * @author wujiuye 2020/09/10
 */
public class Counter {

    private int jobId;
    private AtomicInteger aleryCount;
    private AtomicInteger count;
    private volatile boolean notify;

    public Counter(int jobId) {
        this.jobId = jobId;
        this.count = new AtomicInteger(0);
        this.aleryCount = new AtomicInteger(0);
        this.notify = false;
    }

    public int getJobId() {
        return jobId;
    }

    public void incr() {
        if (this.count.incrementAndGet() > 0) {
            notify = false;
        }
        this.aleryCount.incrementAndGet();
    }

    public void decr() {
        if (this.count.decrementAndGet() == 0) {
            notify = true;
        }
    }

    public int get() {
        return this.count.get();
    }

    public int getAlertCount() {
        return this.aleryCount.get();
    }

    public void awit(long time, TimeUnit timeUnit) throws TimeoutException, InterruptedException {
        long ms = time == 0 ? 0 : timeUnit.toMillis(time);
        if (ms == 0) {
            if (notify) {
                return;
            } else {
                throw new TimeoutException();
            }
        }
        long curWaitMs = 0;
        for (; !notify; ) {
            if (curWaitMs > ms) {
                throw new TimeoutException();
            }
            TimeUnit.MILLISECONDS.sleep(200);
            curWaitMs += 200;
        }
    }

}
