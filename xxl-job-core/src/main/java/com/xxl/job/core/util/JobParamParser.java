package com.xxl.job.core.util;

import com.xxl.job.core.log.XxlJobLogger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * 定时任务传参解析器
 * 支持两个格式传参：
 * 1、url参数格式：param1=1&param2=222
 * 2、json格式传参：{
 * "param1":1,
 * "param2":222
 * }
 *
 * @author wujiuye 2020/04/28 jiuye-wu@msyc.cc
 */
public class JobParamParser {

    /**
     * 将参数解析为对象
     *
     * @param param  参数
     * @param tClass 对象类型
     * @param <T>
     * @return
     */
    public static <T> T parseObject(String param, Class<T> tClass) throws Exception {
        if (tClass == null || tClass == Void.class) {
            return null;
        }
        if (tClass == String.class) {
            return (T) param;
        }
        if (param.startsWith("{") & param.endsWith("}")) {
            return GsonTool.fromJson(param, tClass);
        } else {
            return parseObjectByUrlParam(param, tClass);
        }
    }

    /**
     * URL参数格式的解析
     *
     * @param param  参数
     * @param tClass 类型
     * @param <T>
     * @return
     * @throws Exception
     */
    private static <T> T parseObjectByUrlParam(String param, Class<T> tClass) throws Exception {
        if (param.startsWith("?")) {
            param = param.substring(1);
        }
        String[] params = param.split("&");
        T obj = null;
        for (String s : params) {
            int first = s.indexOf("=");
            if (first <= 0) {
                continue;
            }
            String key = s.substring(0, first);
            String value = s.substring(first + 1);
            if (obj == null) {
                obj = tClass.newInstance();
            }
            try {
                Field field = tClass.getDeclaredField(key);
                if ((field.getModifiers() & Modifier.FINAL) == Modifier.FINAL) {
                    continue;
                }
                if ((field.getModifiers() & Modifier.STATIC) == Modifier.STATIC) {
                    continue;
                }
                ReflectUtils.applyValueBy(obj, field, value);
            } catch (Exception e) {
                XxlJobLogger.log("未找到字段：" + key + " , java bean class:" + tClass.getName());
            }
        }
        return obj;
    }

}
