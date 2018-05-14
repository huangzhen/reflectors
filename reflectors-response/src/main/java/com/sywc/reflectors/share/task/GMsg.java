package com.sywc.reflectors.share.task;

/**
 * @author huangzhen
 * @version 1.0.0
 */
public class GMsg {
    /**
     * 消息事件id
     */
    public int msgId;
    /**
     * 消息内容
     */
    public Object objContext;

    public GMsg(int msgId, Object objContext) {
        this.msgId = msgId;
        this.objContext = objContext;
    }
}
