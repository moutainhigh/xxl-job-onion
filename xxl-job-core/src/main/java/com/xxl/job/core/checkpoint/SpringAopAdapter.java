package com.xxl.job.core.checkpoint;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;

import java.lang.reflect.Method;

/**
 * Spring动态AOP
 *
 * @author wujiuye 2020/09/10
 */
public final class SpringAopAdapter extends AbstractPointcutAdvisor {

    @Override
    public Pointcut getPointcut() {
        return new SavepointDynamicMethodMatcherPointcut();
    }

    @Override
    public Advice getAdvice() {
        return new SavepointMethodInterceptor();
    }

    public static class SavepointDynamicMethodMatcherPointcut extends StaticMethodMatcherPointcut {

        @Override
        public ClassFilter getClassFilter() {
            // 编程式只能在JobHandler中，因为方法拦截器需要拿到SavepointSuppor
            return SavepointSuppor.class::isAssignableFrom;
        }

        /**
         * 静态方法检查
         *
         * @param method
         * @param targetClass
         * @return
         */
        @Override
        public boolean matches(Method method, Class<?> targetClass) {
            return method.getAnnotation(Savepoint.class) != null;
        }

    }

    public static class SavepointMethodInterceptor implements MethodInterceptor {

        @Override
        public Object invoke(MethodInvocation methodInvocation) throws Throwable {
            SavepointSuppor savepointSuppor = (SavepointSuppor) methodInvocation.getThis();
            if (savepointSuppor.existSavepointLable()) {
                // 存在保存点，放弃执行
                return null;
            }
            try {
                savepointSuppor.incr();
                return methodInvocation.proceed();
            } finally {
                savepointSuppor.decr();
            }
        }
    }

}
