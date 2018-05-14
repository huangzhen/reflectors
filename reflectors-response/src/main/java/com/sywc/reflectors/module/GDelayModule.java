package com.sywc.reflectors.module;

import com.sywc.reflectors.SparrowSystem;
import com.sywc.reflectors.share.DelayMsg;
import com.sywc.reflectors.share.GSessionInfo;
import com.iflytek.sparrow.share.GThreadFactory;
import com.sywc.reflectors.share.SparrowConstants;
import com.sywc.reflectors.share.UtilOper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public class GDelayModule {
    private static final Logger logger = LoggerFactory.getLogger(GDelayModule.class);

    private static final String configFile = Thread.currentThread().getContextClassLoader()
            .getResource("reflectors.conf").getPath();
    private static final int DISPATCHER_SIZE = UtilOper.getIntValue(configFile, "delay_dispatcher_size", 2);

    private static final List<GDelayedTaskConsumer> delayedTaskList = new ArrayList<>(DISPATCHER_SIZE);

    private static AtomicLong delayTotalNum = new AtomicLong(0);

    private static final ExecutorService dispatcher = Executors.newFixedThreadPool(DISPATCHER_SIZE,
            new GThreadFactory("GDelayModule-Dispatcher"));

    public static boolean addMsg(DelayMsg delayMsg) {
        if (delayMsg != null && delayMsg.getObjContext() != null) {
            int index = (int) (delayTotalNum.getAndIncrement() % DISPATCHER_SIZE);
            /**
             * 当 delayTotalNum 不断 ++ 达到最大值后，会变成负数，为了防止数组越界 故当 index 小于 0 时重置
             */
            if (index < 0) {
                index = 0;
                delayTotalNum.set(0L);
            }
            delayedTaskList.get(index).addDelayQueue(delayMsg);
            logger.debug("Add Message,delay time is {} ms ", delayMsg.getDelayTime());
            return true;
        }
        return false;
    }

    public static void start() {
        for (int i = 0; i < DISPATCHER_SIZE; i++) {
            GDelayedTaskConsumer delayedTaskConsumer = new GDelayedTaskConsumer("delayTask--" + i);
            delayedTaskList.add(delayedTaskConsumer);
            dispatcher.execute(delayedTaskConsumer);
        }
    }

    public static void shutdown() {
        if (!dispatcher.isShutdown()) {
            dispatcher.shutdown();
        }
    }
}

/**
 * 延迟队列的任务类
 */
class GDelayedTaskConsumer implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(GDelayedTaskConsumer.class);
    private DelayQueue<DelayMsg> msgDelayeQueue;
    private String delayTaskName;

    public GDelayedTaskConsumer(String name) {
        this.msgDelayeQueue = new DelayQueue<DelayMsg>();
        this.delayTaskName = name;
    }

    public void addDelayQueue(DelayMsg delayMsg) {
        msgDelayeQueue.offer(delayMsg);
        logger.debug("addDelayQueue {}:{}:{}", getDelayTaskName(), msgDelayeQueue.size(), delayMsg.getDelayTime());
    }

    public String getDelayTaskName() {
        return delayTaskName;
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (msgDelayeQueue.size() == 0) {
                    continue;
                }
                DelayMsg delayMsg;
                try {
                    delayMsg = msgDelayeQueue.take();
                } catch (InterruptedException e) {
                    logger.warn("msgDelayeQueue.take() failed: ", e.getMessage());
                    continue;
                }
                if (delayMsg == null || delayMsg.getObjContext() == null) {
                    continue;
                }
                if (delayMsg.getObjContext() instanceof GSessionInfo) {
                    SparrowSystem.srvModule().addMsg(SparrowConstants.MSG_ID_SERVICE_ADX_AD_RSP, delayMsg.getObjContext());
                } else {
                    logger.warn("msg.objContext is not instanceof GSessionInfo");
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("exception out of anticipation: {}", e.getMessage());
            }
        }
    }
}

