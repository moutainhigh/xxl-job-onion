package com.xxl.job.core.util.serialize;

import com.xxl.job.core.util.GsonTool;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author wujiuye 2020/09/09
 */
public class JsonSerializeTool implements SerializeStategay {

    @Override
    public byte[] serialize(Object object) {
        return GsonTool.toJson(object).getBytes();
    }

    @Override
    public <T> byte[] serializeArray(List<T> objs) {
        return GsonTool.toJson(objs).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        String json = new String(bytes);
        return GsonTool.fromJson(json, clazz);
    }

    @Override
    public <T> List<T> deserializeArray(byte[] bytes, Class<T> clazz) {
        String json = new String(bytes, StandardCharsets.UTF_8);
        return GsonTool.fromJson(json, List.class, clazz);
    }

}
