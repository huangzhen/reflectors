package com.sywc.reflectors.module;

import com.sywc.reflectors.handler.GServiceServerHandler;
import com.iflytek.sparrow.http.GServer;
import com.sywc.reflectors.monitor.notify.PlatFileMonitorNotifly;
import com.iflytek.sparrow.share.Constants;
import com.sywc.reflectors.share.SparrowConstants;
import com.sywc.reflectors.share.UtilOper;
import com.sywc.reflectors.share.dto.SysConfigDTO;
import com.sywc.reflectors.share.task.GMsg;
import com.sywc.reflectors.share.task.GTaskBase;
import com.sywc.reflectors.monitor.watch.PlatFileMonitorWatch;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GSysMgrModule extends GTaskBase {
    private static final Logger log = LoggerFactory.getLogger(GSysMgrModule.class);

    /**
     * 系统状态控制信息
     */
    private volatile int sysState;
    private volatile int sysInitStep;
    private int timerInInitState = 1;

    /**
     * 以下是系统资源
     */
    private GServer srvHttpServer;
    private SysConfigDTO sysConfigDTO;
    private GServiceModule srvModule;

    public GSysMgrModule(String configFile) {
        super("sys-mgr-task", 0);
        /** 系统处于初始化状态 */
        sysState = SparrowConstants.SERVICE_STATUS_INIT;
        sysConfigDTO = new SysConfigDTO(configFile);
        /** 初始化步骤从0开始, 往后面逐步初始化 */
        sysInitStep = 0;
    }

    @Override
    public boolean startTask() {
        long start = System.currentTimeMillis();

        /** 第一步：初始化系统配置 */
        if (sysInitStep < 1) {
            if (!sysConfigDTO.initConfig()) {
                log.error("init sysConfigDTO fail...init_step is 1, and continue after a while.");
                return false;
            }
            log.info("init sysConfig success...init_step is 1, and continue next.");
            sysInitStep++;

        }
        long start1 = System.currentTimeMillis();
        log.error("第一步初始化系统配置和第二步数据加载耗时{}ms.", (start1 - start));
//        if (!PlatFileMonitorWatch.initMonitor()) {
//
//        }
//        PlatFileMonitorWatch.startMonitor();
        PlatFileMonitorNotifly.startMonitor();
        log.debug("File monitor has start !");

        GSleepModule.start();
        GDelayModule.start();

        /** 第二步：初始化业务模块 */
        if (sysInitStep < 2) {
            srvModule = new GServiceModule();
            if (!srvModule.startModule(sysConfigDTO.getSrvTaskNum())) {
                log.error("init srvModule fail...init step is 3, and continue after a while.");
                return false;
            }

            log.info("init srvModule success...init_step is 3, and continue next.");
            sysInitStep++;
        }
        long start2 = System.currentTimeMillis();
        log.error("第二步初始化业务模块耗时{}ms.", (start2 - start1));

        /** 第三步：初始化广告请求通信服务模块 */
        if (sysInitStep < 3) {
            srvHttpServer = new GServer(sysConfigDTO.getSrvIp(), sysConfigDTO.getSrvPort(), new GServiceServerHandler(),
                    sysConfigDTO.getServerIoThreads());
            if (!srvHttpServer.start()) {
                log.error("init srvHttpServer fail...init step is 14, and continue after a while.");
                return false;
            }

            log.info("init srvHttpServer success...init_step is 14, and continue next.");
            sysInitStep++;
        }
        long start3 = System.currentTimeMillis();
        log.error("第三步初始化广告请求通信服务模块耗时{}ms.", (start3 - start2));

        /** 至此, 系统全部初始化完成 */
        log.error("System init OK,Cost {} ms", System.currentTimeMillis() - start);
        sysState = SparrowConstants.SERVICE_STATUS_WORK;

        return true;
    }


    public GServiceModule getSrvModule() {
        return srvModule;
    }

    public void sysEnterToInitState() {
        addMsg(SparrowConstants.SERVICE_STATUS_INIT, null);
    }

    public void sysEnterToWorkState() {
        addMsg(SparrowConstants.SERVICE_STATUS_WORK, null);
    }

    public void sysEnterToQuitState() {
        addMsg(SparrowConstants.SERVICE_STATUS_QUIT, null);
    }

    @Override
    protected void handlerMsg(int msgId, Object objContext) {
        switch (msgId) {
            case SparrowConstants.SERVICE_STATUS_INIT: {
                sysState = SparrowConstants.SERVICE_STATUS_INIT;
                sysInitStep = 0;

                /** 系统进入初始化状态时, 便定时启动系统各个模块、配置, 直到全部启动成功, 才进入工作状态 */
                while (running) {
                    if (SparrowConstants.SERVICE_STATUS_WORK == sysState) {
                        sysEnterToWorkState();
                        break;
                    } else if (SparrowConstants.SERVICE_STATUS_QUIT == sysState) {
                        sysEnterToQuitState();
                        break;
                    }
                    timerHandlerInit();
                }
                break;
            }

            case SparrowConstants.SERVICE_STATUS_WORK: {
                while (running) {
                    if (SparrowConstants.SERVICE_STATUS_INIT == sysState) {
                        sysEnterToInitState();
                        break;
                    } else if (SparrowConstants.SERVICE_STATUS_QUIT == sysState) {
                        sysEnterToQuitState();
                        break;
                    }
                }

                break;
            }

            case SparrowConstants.SERVICE_STATUS_QUIT: {
                try {

                    /**
                     * 做各种资源回收操作:
                     * 1-释放请求Http通信服务, 保障系统不在接收请求
                     * 2-等待业务线程把队列中的消息处理完成
                     */
                    if (srvHttpServer != null) {
                        try {
                            srvHttpServer.stop();
                        } catch (Throwable e) {
                            log.error("stopping srvHttpServer: ", e);
                        }
                    }
                    if (srvModule != null) {
                        try {
                            srvModule.closeModule();
                        } catch (Throwable e) {
                            log.error("stopping srvModule: ", e);
                        }
                    }

                    PlatFileMonitorWatch.shutdown();
                    GSleepModule.shutdown();
                    GDelayModule.shutdown();

                    try {
                        closeTask();
                        UtilOper.sleep(100);
                    } catch (Throwable e) {
                        log.error("stopping sysMgrModule: ", e);
                    }
                } finally {
                    sysState = SparrowConstants.SERVICE_STATUS_QUIT;
                }
                sysInitStep = 0;
                break;
            }

            default: {
                log.error("{} received a wrong msgId = {}", taskName, msgId);
                break;
            }
        }
    }

    private boolean timerHandlerInit() {
        if (timerInInitState > 0) {
            timerInInitState--;
            return true;
        }
        timerInInitState = 5;

        return startTask();
    }

    @Override
    public boolean closeTask() {
        addMsg(new GMsg(Constants.MSG_ID_SYS_KILL, null));
        return true;
    }

    public int getSysStatus() {
        return sysState;
    }

    public void stop() {
        if (!running) {
            log.warn("GSysMgrModule is not running.");
            return;
        }
        running = false;
        Thread thread = getThreadByName(taskName);
        if (sysState != SparrowConstants.SERVICE_STATUS_WORK && thread != null) {
            thread.interrupt();
        }

        addMsg(SparrowConstants.SERVICE_STATUS_QUIT, null);
    }

    private Thread getThreadByName(String name) {
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (StringUtils.equals(thread.getName(), name)) {
                return thread;
            }
        }
        return null;
    }

    public SysConfigDTO getSysConfigDTO() {
        return sysConfigDTO;
    }
}
