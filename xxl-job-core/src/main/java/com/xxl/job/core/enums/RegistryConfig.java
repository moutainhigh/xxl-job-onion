package com.xxl.job.core.enums;

/**
 * Created by xuxueli on 17/5/10.
 */
public class RegistryConfig {

    public static final int BEAT_TIMEOUT = 30;
    public static final int DEAD_TIMEOUT = BEAT_TIMEOUT * 3;

    public enum RegistType {
        /**
         * 执行器自动注册
         */
        EXECUTOR,
        /**
         * 后台手动注册
         */
        ADMIN
    }

}
