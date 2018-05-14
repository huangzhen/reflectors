package com.sywc.reflectors.share.task;

import com.iflytek.sparrow.share.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public abstract class GTaskBase {
    private static final Logger log = LoggerFactory.getLogger(GTaskBase.class);
    private static final int MSG_QUEUE_LENGTH = 10000;

    protected String taskName;
    private int priority;
    private Thread thread;

    protected BlockingQueue<GMsg> msgQueue = null;

    protected volatile boolean running = false;

    protected GTaskBase(String taskName, int iPriority) {
        this.taskName = taskName;
        this.priority = iPriority;

        msgQueue = new ArrayBlockingQueue<>(MSG_QUEUE_LENGTH);

        this.running = true;

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    taskRun();
                } catch (Exception e) {
                    log.error("GTaskBase.this.taskRun() failed: ", e);
                }
            }
        });

        thread.start();

    }

    private void taskRun() {
        Thread.currentThread().setName(taskName);

        GMsg msg = null;
        while (true) {
            try {
                msg = msgQueue.take();
            } catch (InterruptedException e) {
                continue;
            }

            if (null == msg) {
                continue;
            }

            try {
                if (Constants.MSG_ID_SYS_KILL == msg.msgId || Constants.MSG_ID_SYS_QUICLY_KILL == msg.msgId) {
                    log.info("{} received a kill-msg({}), and the task will exit.", taskName, msg.msgId);
                    break;
                }

                handlerMsg(msg.msgId, msg.objContext);
            } catch (Exception e) {
                log.error("Handle message failed, ID: {},", msg.msgId, e);
            }
        }
    }

    public int addMsg(int msgId, Object data) {
        if (Constants.MSG_ID_SYS_KILL == msgId || Constants.MSG_ID_SYS_QUICLY_KILL == msgId) {
            running = false;
        }

        GMsg msg = new GMsg(msgId, data);
        if (true != msgQueue.offer(msg)) {
            return Constants.RET_ERROR;
        }

        return Constants.RET_OK;
    }

    public int addMsg(GMsg msg) {
        if (msg != null && (Constants.MSG_ID_SYS_KILL == msg.msgId || Constants.MSG_ID_SYS_QUICLY_KILL == msg.msgId)) {
            running = false;
        }

        if (true != msgQueue.offer(msg)) {
            return Constants.RET_ERROR;
        }

        return Constants.RET_OK;
    }

    public abstract boolean startTask();

    public abstract boolean closeTask();

    protected abstract void handlerMsg(int msgId, Object objContext);
}
