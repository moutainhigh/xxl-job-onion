package com.xxl.job.executor.service.jobhandler;

import com.xxl.job.core.handler.OnionShardingJobHandler;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 使用OnionShardingJobHandler
 *
 * @author wujiuye 2020/04/16
 */
@Component
public class OnionShardingXxlJob implements OnionShardingJobHandler<JobParam> {

    @Override
    public void doExecute(int shardingTotal, int currentShardingIndex, JobParam param) throws Exception {
        System.out.println("shardingTotal:" + shardingTotal + ", currentShardingIndex: "
                + currentShardingIndex + ", param:" + param);
        // 实际从数据库查询
        List<Integer> orderIds = Arrays.asList(10000, 11111);
        // 这里使用并行流模拟将每个订单放入线程池处理
        orderIds.parallelStream().forEach(this::savepointTest);
    }

    // 编程式使用
    public void savepointTest(int orderId) {
        // 存在Savepoint标志，不能进入
        if (existSavepointLable()) {
            return;
        }
        // 计数器+1
        incr();
        try {
            try {
                TimeUnit.MINUTES.sleep(1);
            } catch (InterruptedException ignored) {
            }
        } finally {
            // 计数器-1
            decr();
        }
    }

}
