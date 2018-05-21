package com.sywc.reflectors.share;

public final class ReflectorsConstants {

    public final static int SERVICE_STATUS_INIT = 1;
    public final static int SERVICE_STATUS_WORK = 2;
    public final static int SERVICE_STATUS_QUIT = 3;
    public final static int SERVICE_STATUS_PERIOD = 4;

    public final static int MSG_ID_SERVICE_ADX_AD_REQ = Constants.MSG_ID_SERVICE_START;
    public final static int MSG_ID_SERVICE_ADX_AD_RSP = Constants.MSG_ID_SERVICE_START + 1;
    public final static int MSG_ID_SERVICE_ADX_404 = Constants.MSG_ID_SERVICE_START + 2;
    public final static int MSG_ID_SERVICE_ADX_UNSUPPORT_AD_REQUEST_METHOD = Constants.MSG_ID_SERVICE_START + 3;
    public final static int MSG_ID_SERVICE_ADX_PREFLIGHT_FOR_AD_MONITOR = Constants.MSG_ID_SERVICE_START + 4;
    public final static int MSG_ID_SERVICE_ADX_PREFLIGHT_FOR_AD_REQUEST = Constants.MSG_ID_SERVICE_START + 5;
    public final static int MSG_ID_SERVICE_ADX_AD_STATIC = Constants.MSG_ID_SERVICE_START + 6;

    public final static String MUST_PARAM_NAME_NAME = "platName";
    public final static String UPPLAT_CONF_DIR_NAME = "conf";
    public final static String UPPLAT_RES_DIR_NAME = "response";

}
