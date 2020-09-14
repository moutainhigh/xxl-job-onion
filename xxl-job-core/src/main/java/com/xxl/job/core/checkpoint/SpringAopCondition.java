package com.xxl.job.core.checkpoint;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * 需要依赖aspectj框架
 *
 * @author wujiuye 2020/09/10
 */
public class SpringAopCondition implements Condition {

    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        try {
            // spring-aop
            Class.forName("org.springframework.aop.support.AbstractPointcutAdvisor");
            // aspectj
            Class.forName("org.aspectj.lang.annotation.Pointcut");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
