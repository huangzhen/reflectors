package com.sywc.reflectors.module;

import com.sywc.reflectors.SparrowSystem;
import com.sywc.reflectors.share.DelayMsg;
import com.sywc.reflectors.share.GSessionInfo;
import com.iflytek.sparrow.share.GThreadFactory;
import com.sywc.reflectors.share.SparrowConstants;
import com.sywc.reflectors.share.UtilOper;
import com.sywc.reflectors.share.task.GMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 线程休眠模块
 *
 * @author zhenhuang
 * @version 1.0.0
 * @date 2018-01-25 10:43
 */
public class GSleepModule {
    private static final Logger logger = LoggerFactory.getLogger(GSleepModule.class);
    private static final String configFile = Thread.currentThread().getContextClassLoader().getResource("reflectors.conf").getPath();

    private static final int DISPATCHER_SIZE = UtilOper.getIntValue(configFile, "sleep_dispatcher_size", 16);

    private static final BlockingDeque<GMsg> reqToBeAssembled = new LinkedBlockingDeque<>();

    private static final ExecutorService dispatcher = Executors.newFixedThreadPool(DISPATCHER_SIZE,
            new GThreadFactory("GSleepFactoryModule-Dispatcher"));

    public static boolean addMsg(GMsg msg) {
        if (msg != null && msg.objContext != null) {
            reqToBeAssembled.offer(msg);
            return true;
        }
        return false;
    }

    public static void start() {
        for (int i = 0; i < DISPATCHER_SIZE; ++i) {
            dispatcher.execute(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            GMsg msg;
                            try {
                                msg = reqToBeAssembled.take();
                            } catch (InterruptedException e) {
                                logger.error("reqToBeAssembled.take() failed: ", e.getMessage());
                                continue;
                            }
                            if (msg == null || msg.objContext == null) {
                                continue;
                            }
                            GSessionInfo sessInfo;
                            if (msg.objContext instanceof GSessionInfo) {
                                sessInfo = (GSessionInfo) msg.objContext;
                                reqSleepThead(sessInfo);
                            } else {
                                logger.error("msg.objContext is not instanceof GSessionInfo");
                                continue;
                            }
                        } catch (Exception e) {
                            logger.error("exception out of anticipation: {}", e.getMessage());
                        }
                    }
                }
            });
        }
    }

    /**
     * 请求休眠线程
     *
     * @param sessInfo
     */
    private static void reqSleepThead(final GSessionInfo sessInfo) {
        final long sleepTime = realSleepTime(sessInfo);
        if (sleepTime <= 0L) {
            SparrowSystem.srvModule().addMsg(SparrowConstants.MSG_ID_SERVICE_ADX_AD_RSP, sessInfo);
            return;
        }
        GDelayModule.addMsg(new DelayMsg(sessInfo, sleepTime));
    }

    /**
     * 实际休眠时间
     * <p>
     * 设置的休眠时间 - 系统消耗时间 - 系统处理时间（预计）
     *
     * @param sessionInfo
     * @return
     */
    private static long realSleepTime(GSessionInfo sessionInfo) {
        long costTime = System.currentTimeMillis() - sessionInfo.millRecvReq;
        int expectedTime = SparrowSystem.sysMgrModule().getSysConfigDTO().getSysDealTime();
        long realTime = sessionInfo.getPlatConfigDTO().getDelayTime() - costTime - expectedTime;

        logger.debug("req has cost time {} ms,expected deal time {} ms,real sleep time {} ms,sid={}",
                costTime, expectedTime, realTime, sessionInfo.sid);

        return realTime;
    }

    public static void shutdown() {
        if (!dispatcher.isShutdown()) {
            dispatcher.shutdown();
        }
    }
}
