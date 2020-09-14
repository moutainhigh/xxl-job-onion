## xxl-job-onion

XXL-JOB-Onion now means [XXL-JOB](https://github.com/xuxueli/xxl-job) eXtensions. :)

XXL-JOB-Onion 是基于 [XXL-JOB 2.2.0](https://github.com/xuxueli/xxl-job) 的二次开发，加入一些定制化功能。

## 主要贡献者

* wujiuye 洋葱集团（`297、广州市洋葱omall电子商务`）

## 当前的主要功能

* 添加`ONION_BEAN`运行模式，`ONION_BEAN`模式使用线程池执行任务，支持分片策略和非分片策略，阻塞处理策略不支持单机串行策略；
* 告警模块实现告警等级区分，一级告警短信发送、二级告警邮件发送；
* 1.2.2-ONION版本支持安全模式中止任务；

## 关于ONION_BEAN模式的疑问

* 为什么添加`ONION_BEAN`模式？`ONION_BEAN`运行模式与其它运行模式有什么不同？

![](https://user-gold-cdn.xitu.io/2020/4/20/17197efda00abdd3?w=1800&h=1092&f=png&s=187777)

使用`BEAN`运行模式可通过在方法上添加`@XxlJob`将任务注册到`admin`，但这种方式对开发人员的约束不够强力，给开发人员自由可能就会给项目后期水平扩展节点实现任务分片执行带来更多的难题。因此，我们放弃了`XXL-JOB`提供的`BEAN`模式，添加新的`ONION_BEAN`运行模式。

为了实现让团队成员开发定时任务时，必须通过实现接口来开发`JobHandler`。除了在接口参数上强制 开发者考虑任务分片执行外，还有一个目的就是限制一个类只能写一个`Job`。我们旧的定时任务项目很多 类都是几千行代码的，一堆的任务在一个类中，失去了代码的可读性。因此使用实现接口的方式还能强制一个类只能编写一个定时任务，在框架层实现代码可读性。当然，缺点就是类增多。


![](https://user-gold-cdn.xitu.io/2020/4/20/17197f07271a7d4b?w=2208&h=712&f=png&s=154004)

`ONION_BEAN`模式强制考虑任务分片是出于让定时任务项目支持水平扩展的考虑，也支持将一个重的定时任务项目按业务拆分。随着集团内部业务的发展，后期定时器会越来越多，并且任务处理的数据量也会越来越大。任务分片执行为项目带来扩展性的同时，分片执行可让多台机器分担一台机器的工作，提升任务的完成速度，从而避免一个任务的执行卡到下一个周期，导致原本多个周期的任务堆积在一个周期下执行的情况。比如`12:30`、`12:35`、`12:40`三个周期的任务由于第一个周期没执行完，后面的任务都在等待，这对一些实时性要求高的任务是个痛点。

`ONION_BEAN`模式改变`XXL-JOB`原有运行模式为每个任务创建一个线程的做法，改为使用非固定线程池执行`JobHandler`。我们都知道，在有限资源，如`CPU`、内存的情况下，线程数会有一个临界点，超过这个临界点时，再添加线程适得其反。因此使用线程池，可在线程池满后拒绝任务的提交，让该分片任务路由到其它空闲节点上执行，遗憾的是`XXL-JOB`的分片模式现在还不支持这点，我们计划在`XXL-JOB-ONION`的后续版本支持。

* `ONION_BEAN`运行模式选择分片执行策略

`ONION_BEAN`运行模式把需要分片执行的任务与不需要分片执行的任务的考虑权重互换了，通过约定开发`JobHandler`时必须通过实现`OnionShardingJobHandler`接口的方式，在接口的方法上添加分片参数，强调团队开发人员在开发定时任务`JobHandler`时，需优先考虑让任务支持分片执行。

* `ONION_BEAN`运行模式选择非分片执行策略

由于不是所有任务都需要分片执行，如果不需要分片执行的情况下，还使用分片策略路由，那么会导致所有的不需要分片执行的任务都被分配到同一台机器上执行，导致某个执行器所在的机器资源消耗过大，因此，我们为不需要分片执行的任务提供`OnionJobHandler`接口，当任务实现`OnionJobHandler`接口时，可选择非分片执行策略之外的策略。

![](https://user-gold-cdn.xitu.io/2020/4/20/17197f0b2a094da6?w=1754&h=774&f=png&s=149676)

* 告警模块做了哪些修改？

告警模块实现等级区分，一级告警发送短信通知，二级告警发送邮件通知，其它告警当前处理策略为忽略，后续可能将多个三级告警合并为一个告警发送邮件通知。同一个任务如果升级到一级告警，且后续还连续失败，也只会触发一次短信发送。

当前是如何区分告警等级的？每个任务都会有一个告警等级的升级过程：
* 如果周期为秒钟，则判断是否连续失败60次及以上，是则升级为二级告警，无一级告警；
* 如果周期为分钟，则判断是否连续失败3次及以上，是则升级为二级告警，连续失败5次及以上升级为一级告警；
* 如果周期为小时，则判断是否连续失败1次及以上，是则升级为二级告警，连续失败3次及以上升级为一级告警；
* 如果周期为一天或以上，直接升级为一级告警。

告警等级的评判由评判器实现，并使用责任链模式（或者说是过滤器）支持多个评判器共存。评判器支持排序，目前仅提供`OnionAlarmLevelAdjudicator`告警等级批评器，如果想要覆盖`OnionAlarmLevelAdjudicator`评判器，可自行添加告警等级评判器，将排序值设置比`OnionAlarmLevelAdjudicator`评判器的排序值小即可，`OnionAlarmLevelAdjudicator`的排序值默认为整型的最大值。

## XXL-JOB分片策略的实现

`XXL-JOB`的分片调度策略实现非常简单，分片总数就是当前注册的集群节点数量，通过`for`循环调用所有节点执行任务。

当一个任务执行完成时，会异步回传任务的执行结果，`XXL-JOB`通过回传的结果判断一个任务是否执行成功。当任务回传结果为失败时，判断是否配置失败重试次数，决定是否需要重新触发任务执行。在分片模式下，会为每个分片添加一条`XxlJobLog`记录，当某个分片执行失败时，重试策略只重试失败的分片。

# 版本更新

## 1.2.2-ONION
* 支持安全模式中止任务；
注解方式使用
```java
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
```

编程方式使用
```java
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
```
