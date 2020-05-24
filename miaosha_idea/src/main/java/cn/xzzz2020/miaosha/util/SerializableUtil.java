package cn.xzzz2020.miaosha.util;

import com.alibaba.fastjson.JSON;


/**
 * 序列化和反序列的工具
 */
public class SerializableUtil {

    private SerializableUtil(){}


    /**
     * 使用JSON序列化bean
     */
    public static <T> String beanToString(T value) {
        if (value == null) {
            return null;
        }

        Class<?> aClass = value.getClass();

        if (aClass == int.class || aClass == Integer.class) {
            return String.valueOf(value);
        } else if (aClass == String.class) {
            return (String) value;
        } else if (aClass == long.class || aClass == Long.class) {
            return String.valueOf(value);
        } else {
            return JSON.toJSONString(value);
        }
    }


    /**
     * 反序列化
     *
     * @param value
     * @param aClass
     * @param <T>
     * @return
     */
    public static  <T> T stringToBean(String value, Class<T> aClass) {
        if (value == null || value.length() <= 0 || aClass == null) {
            return null;
        }
        if (aClass == int.class || aClass == Integer.class) {
            return (T) Integer.valueOf(value);
        } else if (aClass == String.class) {
            return (T) value;
        } else if (aClass == long.class || aClass == Long.class) {
            return (T) Long.valueOf(value);
        } else {
            return JSON.toJavaObject(JSON.parseObject(value), aClass);
        }
    }

}
