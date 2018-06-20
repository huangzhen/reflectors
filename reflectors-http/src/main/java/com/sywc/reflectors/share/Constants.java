package com.sywc.reflectors.share;

public class Constants {
    public final static int RET_OK = 0;
    public final static int RET_ERROR = -1;


    public final static int HTTP_REQ_TYPE_GET = 0;
    public final static int HTTP_REQ_TYPE_POST = 1;
    public final static int HTTP_REQ_TYPE_OTHER = 2;
    public final static int HTTP_REQ_TYPE_HEAD = 3;
    public final static int HTTP_REQ_TYPE_OPTIONS = 4;

    /* system msg ID */
    public final static int MSG_ID_SYS_QUICLY_KILL = -2;
    public final static int MSG_ID_SYS_KILL = -1;
    public final static int MSG_ID_SYS_INIT = 1;
    public final static int MSG_ID_SYS_WORK = 2;
    public final static int MSG_ID_SYS_QUIT = 3;
    public final static int MSG_ID_SYS_TIMER = 4;

    public final static int MSG_ID_SERVICE_START = 1000;

    public final static int MSG_ID_MONITOR_START = 2000;

    public final static int PLAT_RSP_CODE_OK = 0;
}
