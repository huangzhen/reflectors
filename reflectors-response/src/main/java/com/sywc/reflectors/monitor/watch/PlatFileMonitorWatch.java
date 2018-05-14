package com.sywc.reflectors.monitor.watch;

import com.sywc.reflectors.ReflectorsSystem;
import com.sywc.reflectors.share.SparrowConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 文件监控 WatchService 实现
 *
 * @author zhenhuang
 * @version 1.0.0
 * @date 2018-02-03 11:40
 */
public class PlatFileMonitorWatch {

    private static final Logger logger = LoggerFactory.getLogger(PlatFileMonitorWatch.class);
    private static WatchService watchConf;
    private static WatchService watchRes;
    private static WatchService watchStatic;
    private static ExecutorService fixedThreadPool = Executors.newFixedThreadPool(1);

    /**
     * 初始化文件监控
     *
     * @return true-初始化成功，false-初始化失败
     */
    public static boolean initMonitor() {
        boolean result = Boolean.TRUE;

        try {
            watchConf = FileSystems.getDefault().newWatchService();
            watchRes = FileSystems.getDefault().newWatchService();
            watchStatic = FileSystems.getDefault().newWatchService();

            Paths.get(ReflectorsSystem.upplatDirPath, SparrowConstants.UPPLAT_CONF_DIR_NAME).register(watchConf,
                    StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

            Paths.get(ReflectorsSystem.upplatDirPath, SparrowConstants.UPPLAT_RES_DIR_NAME).register(watchRes,
                    StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

            Paths.get(ReflectorsSystem.staticDirPath).register(watchStatic,
                    StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

        } catch (IOException e) {
            logger.error("IO exception ,errorMsg:{}", e.getMessage());
            result = Boolean.FALSE;
        } catch (Exception e) {
            result = Boolean.FALSE;
            logger.error("Other Exception ,errorMsg:{}", e.getMessage());
        }

        return result;
    }

    /**
     * 开启监听线程
     */
    public static void startMonitor() {
        fixedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    String fileName;
                    WatchKey watchkey;
                    try {
                        while ((watchkey = watchConf.poll(100, TimeUnit.MILLISECONDS)) != null) {
                            for (WatchEvent<?> event : watchkey.pollEvents()) {
                                fileName = event.context().toString();
                                if (fileName.indexOf(".") == 0) {
                                    continue;
                                }
                                logger.debug("check the conf file {} has changed!", fileName);
                                if (ReflectorsSystem.upplatConfMap.containsKey(fileName)) {
                                    ReflectorsSystem.upplatConfMap.remove(fileName);
                                }
                            }
                        }
                        while ((watchkey = watchRes.poll(100, TimeUnit.MILLISECONDS)) != null) {
                            for (WatchEvent<?> event : watchkey.pollEvents()) {
                                fileName = event.context().toString();
                                if (fileName.indexOf(".") == 0) {
                                    continue;
                                }
                                logger.debug("check the response file {} has changed!", fileName);
                                if (ReflectorsSystem.upplatResMap.containsKey(fileName)) {
                                    ReflectorsSystem.upplatResMap.remove(fileName);
                                }
                            }
                        }
                        while ((watchkey = watchStatic.poll(100, TimeUnit.MILLISECONDS)) != null) {
                            for (WatchEvent<?> event : watchkey.pollEvents()) {
                                fileName = event.context().toString();
                                if (fileName.indexOf(".") == 0) {
                                    continue;
                                }
                                logger.debug("check the static file {} has changed!", fileName);
                                if (ReflectorsSystem.upplatStaticMap.containsKey(fileName)) {
                                    ReflectorsSystem.upplatStaticMap.remove(fileName);
                                }
                            }
                        }

                    } catch (InterruptedException e) {
                        logger.debug("Thread interrupted,errMsg:{}", e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * 关闭时，释放监听线程
     */
    public static void shutdown() {
        if (!fixedThreadPool.isShutdown()) {
            fixedThreadPool.shutdown();
        }
    }
}