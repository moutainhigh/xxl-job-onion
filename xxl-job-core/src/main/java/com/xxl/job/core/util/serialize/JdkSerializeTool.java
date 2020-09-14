package com.xxl.job.core.util.serialize;

import java.io.*;
import java.util.List;

/**
 * @author xuxueli 2020-04-12 0:14:00
 */
public class JdkSerializeTool implements SerializeStategay {

    @Override
    public byte[] serialize(Object object) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            return baos.toByteArray();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public <T> byte[] serializeArray(List<T> objs) {
        return serialize(objs);
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (T) ois.readObject();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public <T> List<T> deserializeArray(byte[] bytes, Class<T> clazz) {
        return (List<T>) deserialize(bytes, clazz);
    }

}
