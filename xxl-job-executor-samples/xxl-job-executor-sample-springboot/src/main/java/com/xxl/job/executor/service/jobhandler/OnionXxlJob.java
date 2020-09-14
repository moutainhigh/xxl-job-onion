package com.xxl.job.executor.service.jobhandler;

import com.xxl.job.core.checkpoint.Savepoint;
import com.xxl.job.core.handler.OnionJobHandler;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 使用OnionJobHandler
 *
 * @author wujiuye 2020/04/16
 */
@Component
public class OnionXxlJob implements OnionJobHandler<JobParam> {

    @Resource
    @Lazy
    private OnionXxlJob onionXxlJob;

    @Override
    public void doExecute(JobParam param) throws Exception {
        System.out.println("param:" + param);
        // 实际从数据库查询
        List<Integer> orderIds = Arrays.asList(10000, 11111);
        // 这里使用并行流模拟将每个订单放入线程池处理
        orderIds.parallelStream().forEach(onionXxlJob::savepointTest);
    }

    // 注解方式使用
    @Savepoint
    public void savepointTest(Integer orderId) {
        System.out.println("订单号:" + orderId);
        try {
            TimeUnit.MINUTES.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
