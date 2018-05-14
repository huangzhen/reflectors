package com.sywc.reflectors.share.dto;

import com.alibaba.fastjson.JSONObject;

import java.io.Serializable;

/**
 * 异常 DTO
 * <p>
 * 主要是文件平台不存在，文件不存在等异常，Json 转换失败
 *
 * @author zhenhuang
 * @version 0.0.1
 * @date 2018-01-22 21:11
 */

public class ExceptionDTO implements Serializable {
    private static final long serialVersionUID = -260679625665088448L;
    private String reqId;
    private String errrMsg;

    public ExceptionDTO() {

    }

    public ExceptionDTO(String reqId, String errrMsg) {
        this.reqId = reqId;
        this.errrMsg = errrMsg;
    }

    public String getReqId() {
        return reqId;
    }

    public void setReqId(String reqId) {
        this.reqId = reqId;
    }

    public String getErrrMsg() {
        return errrMsg;
    }

    public void setErrrMsg(String errrMsg) {
        this.errrMsg = errrMsg;
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }
}
