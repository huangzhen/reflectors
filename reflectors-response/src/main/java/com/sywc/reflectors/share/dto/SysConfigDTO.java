package com.sywc.reflectors.share.dto;

import com.sywc.reflectors.share.UtilOper;

/**
 * 系统配置DTO
 *
 * @author zhenhuang
 * @version 0.0.1
 * @date 2018-01-22 15:42
 */
public final class SysConfigDTO {
    public String configFile;
    private String srvIp;
    private int srvPort;
    private int srvTaskNum;
    private int serverIoThreads;
    private int sysDealTime;
    public SysConfigDTO(String configFile) {
        this.configFile = configFile;
    }

    public boolean initConfig() {
        srvIp = UtilOper.getStringValue(configFile, "service_server_ip", "0.0.0.0");
        srvPort = UtilOper.getIntValue(configFile, "service_server_port", 9966);
        srvTaskNum = UtilOper.getIntValue(configFile, "service_task_num", 32);
        serverIoThreads = UtilOper.getIntValue(configFile, "server_io_threads", 16);
        sysDealTime = UtilOper.getIntValue(configFile, "sys_deal_time", 10);
        return true;
    }

    public String getSrvIp() {
        return srvIp;
    }

    public int getSrvPort() {
        return srvPort;
    }

    public int getSrvTaskNum() {
        return srvTaskNum;
    }

    public int getServerIoThreads() {
        return serverIoThreads;
    }

    public int getSysDealTime() {
        return sysDealTime;
    }

    @Override
    public String toString() {
        return "SysConfigDTO{" +
                "configFile='" + configFile + '\'' +
                ", srvIp='" + srvIp + '\'' +
                ", srvPort=" + srvPort +
                ", srvTaskNum=" + srvTaskNum +
                ", serverIoThreads=" + serverIoThreads +
                '}';
    }
}
