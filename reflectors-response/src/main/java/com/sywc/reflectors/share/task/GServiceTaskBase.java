package com.sywc.reflectors.share.task;

import com.sywc.reflectors.share.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class GServiceTaskBase {
    private static final Logger log = LoggerFactory.getLogger(GServiceTaskBase.class);
    private static final int MSG_QUEUE_LENGTH = 100000000;
    protected static BlockingQueue<GMsg> msgQueue = new LinkedBlockingQueue<GMsg>(MSG_QUEUE_LENGTH);
    protected String taskName;

    protected GServiceTaskBase(String taskName, int iPriority) {
        this.taskName = taskName;
        int priority = iPriority;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    GServiceTaskBase.this.taskRun();
                } catch (Throwable t) {
                    log.error("unexpected exception: ", t);
                }
            }
        });

        thread.start();
    }

    private void taskRun() {
        Thread.currentThread().setName(taskName);

        GMsg msg;
        while (true) {
            try {
                msg = msgQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }

            if (null == msg) {
                continue;
            }

            try {
                if (Constants.MSG_ID_SYS_KILL == msg.msgId || Constants.MSG_ID_SYS_QUICLY_KILL == msg.msgId) {
                    log.error("recv task kill msg( msgId == {}), task exit!!", msg.msgId);
                    break;
                }

                handlerMsg(msg.msgId, msg.objContext);
            } catch (Throwable t) {
                log.error("unexpected exception: ", t);
            }
        }
    }

    public int addMsg(int msgId, Object data) {
        //log.debug("add msg to " + taskName + " when msgId=" + msgId);

        GMsg msg = new GMsg(msgId, data);
        if (!msgQueue.offer(msg)) {
            return Constants.RET_ERROR;
        }

        return Constants.RET_OK;
    }

    public int addMsg(GMsg msg) {
        log.debug("add msg to {} when msgId = {}", taskName, msg.msgId);

        if (!msgQueue.offer(msg)) {
            return Constants.RET_ERROR;
        }

        return Constants.RET_OK;
    }

    public abstract boolean startTask();

    public abstract boolean closeTask();

    protected abstract void handlerMsg(int msgId, Object objContext);
}
