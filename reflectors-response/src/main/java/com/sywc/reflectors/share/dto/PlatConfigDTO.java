package com.sywc.reflectors.share.dto;

import com.alibaba.fastjson.JSONObject;

/**
 * 平台配置的 DTO
 *
 * @author zhenhuang
 * @version 0.0.1
 * @date 2018-01-22 21:04
 */
public class PlatConfigDTO {
    /**
     * 延迟时间 ，默认值为 100 ms
     */
    private int delayTime;
    /**
     * 下发率 ，默认值 100%
     */
    private int ratio;
    /**
     * 有填充响应码，默认值 200
     */
    private int fillHttpCode;
    /**
     * 无填充响应码，默认值 204
     */
    private int noFillHttpCode;

    public int getDelayTime() {
        return delayTime;
    }

    public void setDelayTime(int delayTime) {
        this.delayTime = delayTime;
    }

    public int getRatio() {
        return ratio;
    }

    public void setRatio(int ratio) {
        this.ratio = ratio;
    }

    public int getFillHttpCode() {
        return fillHttpCode;
    }

    public void setFillHttpCode(int fillHttpCode) {
        this.fillHttpCode = fillHttpCode;
    }

    public int getNoFillHttpCode() {
        return noFillHttpCode;
    }

    public void setNoFillHttpCode(int noFillHttpCode) {
        this.noFillHttpCode = noFillHttpCode;
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }
}
