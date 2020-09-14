package com.xxl.job.admin.core.alarm;

import java.util.HashMap;
import java.util.Map;

/**
 * 告警功能上下文
 *
 * @author wujiuye 2020/06/15
 */
public final class AlarmerContent {

    private final static ThreadLocal<Map<String, Object>> content;

    static {
        content = new ThreadLocal<>();
    }

    private static Object get(String key) {
        Map<String, Object> map = content.get();
        if (map == null) {
            map = new HashMap<>();
            content.set(map);
        }
        return map.get(key);
    }

    private static void set(String key, String value) {
        Map<String, Object> map = content.get();
        if (map == null) {
            map = new HashMap<>();
            content.set(map);
        }
        map.put(key, value);
    }

    public static void setEnv(String env) {
        set("ENV", env);
    }

    public static String getEnv() {
        return (String) get("ENV");
    }

    public static void remove() {
        content.remove();
    }

}
