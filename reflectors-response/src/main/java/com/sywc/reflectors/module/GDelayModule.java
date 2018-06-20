package com.sywc.reflectors.module;

import com.sywc.reflectors.ReflectorsSystem;
import com.sywc.reflectors.share.DelayMsg;
import com.sywc.reflectors.share.GSessionInfo;
import com.sywc.reflectors.share.GThreadFactory;
import com.sywc.reflectors.share.ReflectorsConstants;
import com.sywc.reflectors.share.UtilOper;
import com.sywc.reflectors.share.task.GMsg;
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
    private static final int CONF_DELAY_DISPATCHER_SIZE = UtilOper.getIntValue(configFile, "delay_dispatcher_size", 1);
    /**
     * 避免随意配置，出现 SIZE 小于0 的情况
     */
    private static final int DELAY_DISPATCHER_SIZE = CONF_DELAY_DISPATCHER_SIZE <= 1 ? 1 : CONF_DELAY_DISPATCHER_SIZE;

    private static final List<GDelayedTaskConsumer> delayedTaskList = new ArrayList<>(DELAY_DISPATCHER_SIZE);
    private static final ExecutorService dispatcher = Executors.newFixedThreadPool(DELAY_DISPATCHER_SIZE,
            new GThreadFactory("GDelayModule-Dispatcher"));
    private static AtomicLong delayTotalNum = new AtomicLong(0);
    /**
     *
     *
     */
    private static boolean DELAY_DISPATCHER_LESS_TWO = false;

    public static boolean addMsg(GMsg msg) {
        if (msg != null && msg.objContext != null) {
            GSessionInfo sessInfo;
            if (!(msg.objContext instanceof GSessionInfo)) {
                return false;
            }
            sessInfo = (GSessionInfo) msg.objContext;

            final long delayTime = realDelayTime(sessInfo);
            if (delayTime <= 0L) {
                ReflectorsSystem.srvModule().addMsg(ReflectorsConstants.MSG_ID_SERVICE_ADX_AD_RSP, sessInfo);
                return true;
            }
            if (DELAY_DISPATCHER_LESS_TWO) {
                delayedTaskList.get(0).addDelayQueue(new DelayMsg(msg.objContext, delayTime));
            } else {
                int index = (int) (delayTotalNum.getAndIncrement() % DELAY_DISPATCHER_SIZE);
                /**
                 * 当 delayTotalNum 不断 ++ 达到最大值后，会变成负数，为了防止数组越界 故当 index 小于 0 时重置
                 */
                if (index < 0) {
                    index = 0;
                    delayTotalNum.set(0L);
                }
                delayedTaskList.get(index).addDelayQueue(new DelayMsg(msg.objContext, delayTime));
            }
            logger.debug("Add Message,delay time is {} ms ", delayTime);
            return true;
        }
        return false;
    }

    public static void start() {
        for (int i = 0; i < DELAY_DISPATCHER_SIZE; i++) {
            GDelayedTaskConsumer delayedTaskConsumer = new GDelayedTaskConsumer("delayTask--" + i);
            delayedTaskList.add(delayedTaskConsumer);
            dispatcher.execute(delayedTaskConsumer);
        }
        if (DELAY_DISPATCHER_SIZE < 2) {
            DELAY_DISPATCHER_LESS_TWO = true;
        }
    }

    public static void shutdown() {
        if (!dispatcher.isShutdown()) {
            dispatcher.shutdown();
        }
    }

    /**
     * 实际休眠时间
     * <p>
     * 设置的休眠时间 - 系统消耗时间 - 系统处理时间（预计）
     *
     * @param sessionInfo
     * @return
     */
    private static long realDelayTime(GSessionInfo sessionInfo) {
        long costTime = System.currentTimeMillis() - sessionInfo.millRecvReq;
        int expectedTime = ReflectorsSystem.sysMgrModule().getSysConfigDTO().getSysDealTime();
        long realTime = sessionInfo.getPlatConfigDTO().getDelayTime() - costTime - expectedTime;

        logger.debug("req has cost time {} ms,expected deal time {} ms,real sleep time {} ms,sid={}",
                costTime, expectedTime, realTime, sessionInfo.sid);

        return realTime;
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
                    ReflectorsSystem.srvModule().addMsg(ReflectorsConstants.MSG_ID_SERVICE_ADX_AD_RSP, delayMsg.getObjContext());
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

