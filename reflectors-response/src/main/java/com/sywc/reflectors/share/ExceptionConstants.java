package com.sywc.reflectors.share;

/**
 * 异常常量类
 *
 * @author zhenhuang
 * @version 0.0.1
 * @date 2018-01-23 16:44
 */
public final class ExceptionConstants {
    private final static String UPPLAT_NOT_EXISTS = "目录[%s]下没有平台[%s]文件";
    private final static String PARAM_NAME_NOT_EXISTS = "请求参数里 name 不存在";
    private final static String PARAM_NAME_IS_EMPTY = "请求参数里 name 值为空";
    private final static String PLAT_CONF_NOT_EXISTS = "目录[%s]下没有平台[%s] 的配置文件";
    private final static String PLAT_CONF_IS_EMPTY = "目录[%s]下平台[%s] 的配置文件内容为空";

    public final static String upplatNotExists(String filePath, String fileName) {
        return String.format(UPPLAT_NOT_EXISTS, filePath, fileName);
    }

    public final static String pramNameNotExists() {
        return PARAM_NAME_NOT_EXISTS;
    }

    public final static String paramNameIsEmpty() {
        return PARAM_NAME_IS_EMPTY;
    }

    public final static String platConfNotExists(String filePath, String fileName) {
        return String.format(PLAT_CONF_NOT_EXISTS, filePath, fileName);
    }

    public final static String platConfIsEmpty(String filePath, String fileName) {
        return String.format(PLAT_CONF_IS_EMPTY, filePath, fileName);
    }
}
