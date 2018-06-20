package com.sywc.reflectors.share;

import com.sywc.reflectors.share.dto.PlatConfigDTO;
import org.httpkit.server.RespCallback;

import java.util.UUID;

/**
 * 请求的会话类
 *
 * @author huangzhen
 */
public class GSessionInfo {
    /**
     * 会话id, 唯一标识一次会话
     */
    public String sid;

    /**
     * 通信模块接收到请求的纳秒跳数, 精确时间戳用于在后面计算超时时间
     */
    public long nanoRecvReq;
    /**
     * 加入到GTask的时间
     */
    public long nanoAddToGServerTaskTime;
    /**
     * GTask处理MSG_ID_SERVICE_ADX_AD_REQ请求的时间
     */
    public long nanoGSTaskHdlReqMsgTime;
    /**
     * GTask处理MSG_ID_SERVICE_ADX_AD_REQ开始处理请求的时间
     */
    public long nanoStartHandlerReq;
    /**
     * GTask处理MSG_ID_SERVICE_ADX_AD_REQ 结束处理请求的时间
     */
    public long nanoOverHandlerReq;

    /**
     * 通信模块接收到请求的毫秒时间戳
     */
    public long millRecvReq;
    /**
     * 平台名称
     */
    public String platName;
    /**
     * 原始请求信息
     */
    public OriginRequest originReq = new OriginRequest();

    public RespCallback callback = null;

    private PlatConfigDTO platConfigDTO;

    public static GSessionInfo getNewSession(OriginRequest originReq) {
        GSessionInfo session = new GSessionInfo();
        session.millRecvReq = System.currentTimeMillis();
        session.nanoRecvReq = System.nanoTime();

        session.sid = UUID.randomUUID().toString() + "-" + session.millRecvReq;
        session.originReq = originReq;
        return session;
    }

    public PlatConfigDTO getPlatConfigDTO() {
        return platConfigDTO;
    }

    public void setPlatConfigDTO(PlatConfigDTO platConfigDTO) {
        this.platConfigDTO = platConfigDTO;
    }
}