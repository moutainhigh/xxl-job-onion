package com.xxl.job.core.util.serialize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author wujiuye 2020/09/09
 */
public interface SerializeStategay {

    Logger logger = LoggerFactory.getLogger(JdkSerializeTool.class);

    <T> byte[] serialize(T obj);

    <T> byte[] serializeArray(List<T> objs);

    <T> T deserialize(byte[] bytes, Class<T> clazz);

    <T> List<T> deserializeArray(byte[] bytes, Class<T> clazz);

}
