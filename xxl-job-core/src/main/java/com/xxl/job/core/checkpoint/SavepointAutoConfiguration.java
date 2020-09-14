package com.xxl.job.core.checkpoint;

import org.springframework.context.annotation.*;

/**
 * 安全检查点自动配置
 *
 * @author wujiuye 2020/09/10
 */
@Configuration
@Conditional(SpringAopCondition.class)
public class SavepointAutoConfiguration {

    @Bean("XxlJobSavepointSpringAopAdapter")
    public SpringAopAdapter pointcutAdvisor() {
        return new SpringAopAdapter();
    }

}

