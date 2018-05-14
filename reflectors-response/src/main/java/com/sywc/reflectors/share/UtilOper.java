package com.sywc.reflectors.share;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * 工具类
 *
 * @author huangzhen
 */
public class UtilOper {
    private static final Logger LOG = LoggerFactory.getLogger(UtilOper.class);

    public static void sleep(int millSecond) {
        try {
            Thread.sleep(millSecond);
        } catch (InterruptedException e) {
            LOG.warn("", e);
        }
    }

    public static void accurateSleep(int millSecond) {
        try {
            Thread.sleep(millSecond);
        } catch (InterruptedException e) {
            LOG.warn("", e);
        }
    }

    public static void nanoSleep(long nanoSecond) {
        long setMillSecond = nanoSecond / 1000000;
        long setNanoSecond = nanoSecond % 1000000;
        try {
            Thread.sleep((int) setMillSecond, (int) setNanoSecond);
        } catch (InterruptedException e) {
            LOG.warn("", e);
        }
    }

    public static Map<String, String> getPropertiesToMap(String fileName) {
        Properties properties = new Properties();
        File file = new File(fileName);
        if (!file.exists() || file.isDirectory()) {
            return null;
        }

        InputStream inputFile = null;
        try {
            inputFile = new FileInputStream(file);
            properties.load(inputFile);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputFile != null) {
                try {
                    inputFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        Map<String, String> propertyMap = new HashMap<String, String>();
        Iterator<Entry<Object, Object>> it = properties.entrySet().iterator();
        while (it.hasNext()) {
            Entry<Object, Object> entry = it.next();
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            propertyMap.put(key, value);
        }

        return propertyMap;
    }

    public static int getIntValue(String fileName, String key, int defaultVal) {
        int re = defaultVal;
        String value = getPropertiesToMap(fileName).get(key);
        if (value != null) {
            re = Integer.parseInt(value);
        }
        return re;
    }

    public static long getLongValue(String fileName, String key, long defaultVal) {
        long re = defaultVal;
        String value = getPropertiesToMap(fileName).get(key);
        if (value != null) {
            re = Long.parseLong(value);
        }
        return re;
    }

    public static double getDoubleValue(String fileName, String key, double defaultVal) {
        double re = defaultVal;
        String value = getPropertiesToMap(fileName).get(key);
        if (value != null) {
            re = Double.parseDouble(value);
        }
        return re;
    }

    public static String getStringValue(String fileName, String key, String defaultVal) {
        String re = defaultVal;
        String value = getPropertiesToMap(fileName).get(key);
        if (value != null) {
            re = value;
        }
        return re;
    }

    public static boolean getBooleanValue(String fileName, String key, boolean defaultVal) {
        boolean re = defaultVal;
        String value = getPropertiesToMap(fileName).get(key);
        if (value != null) {
            re = Boolean.parseBoolean(value);
        }
        return re;
    }
}
