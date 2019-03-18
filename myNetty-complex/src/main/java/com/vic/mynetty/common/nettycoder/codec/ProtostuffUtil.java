package com.vic.mynetty.common.nettycoder.codec;

import com.vic.mynetty.common.message.Message;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import lombok.ToString;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProtostuffUtil {
	private static Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<Class<?>, Schema<?>>();

    private static <T> Schema<T> getSchema(Class<T> clazz) {
        @SuppressWarnings("unchecked")
        Schema<T> schema = (Schema<T>) cachedSchema.get(clazz);
        if (schema == null) {
            schema = RuntimeSchema.getSchema(clazz);
            if (schema != null) {
                cachedSchema.put(clazz, schema);
            }
        }
        return schema;
    }

    public static <T> byte[] serializer(T obj) {
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) obj.getClass();
        LinkedBuffer buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);
        try {
            Schema<T> schema = getSchema(clazz);
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            buffer.clear();
        }
    }
    
    
    public static <T> T deserializer(byte[] data, Class<T> clazz) {
        try {
            T obj = clazz.newInstance();
            Schema<T> schema = getSchema(clazz);
            ProtostuffIOUtil.mergeFrom(data, obj, schema);
            return obj;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public static void main(String[] args){
        Data msg = new Data();
        msg.setA(260);
        msg.setB(260);
        byte[] data = ProtostuffUtil.serializer(msg);
//        byte[] datas = new byte[4];
//        for(int i=0;i<4;i++){
//            datas[i] = data[i];
//        }

//        System.out.println(bytes2Int(datas));
        System.out.println(ProtostuffUtil.deserializer((byte[]) data, Data.class));
    }

    public static int bytes2Int(byte[] bytes){
        int value=0;
        value = ((bytes[3] & 0xff)<<24)|
                ((bytes[2] & 0xff)<<16)|
                ((bytes[1] & 0xff)<<8)|
                (bytes[0] & 0xff);
        return value;
    }

    @ToString
    static class Data{
        private int a;
        private long b;

        public int getA() {
            return a;
        }

        public void setA(int a) {
            this.a = a;
        }

        public long getB() {
            return b;
        }

        public void setB(long b) {
            this.b = b;
        }
    }
}
