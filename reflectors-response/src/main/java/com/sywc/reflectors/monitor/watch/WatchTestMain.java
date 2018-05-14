package com.sywc.reflectors.monitor.watch;

import com.sywc.reflectors.SparrowSystem;
import com.sywc.reflectors.share.SparrowConstants;

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
 * TODO 说明
 *
 * @author zhenhuang
 * @version 1.0.0
 * @date 2018-01-31 20:59
 */
public class WatchTestMain {
    private static ExecutorService fixedThreadPool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        fixedThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    WatchService watchConf = FileSystems.getDefault().newWatchService();
                    WatchService watchRes = FileSystems.getDefault().newWatchService();
                    WatchService watchStatic = FileSystems.getDefault().newWatchService();

                    Paths.get(SparrowSystem.upplatDirPath, SparrowConstants.UPPLAT_CONF_DIR_NAME).register(watchConf,
                            StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

                    Paths.get(SparrowSystem.upplatDirPath, SparrowConstants.UPPLAT_RES_DIR_NAME).register(watchRes,
                            StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

                    Paths.get(SparrowSystem.staticDirPath).register(watchStatic,
                            StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

                    while (true) {
                        String fileName;
                        WatchKey watchkey;
                        while ((watchkey = watchConf.poll(100, TimeUnit.MILLISECONDS)) != null) {
                            for (WatchEvent<?> event : watchkey.pollEvents()) {
                                fileName = event.context().toString();
                                System.out.println(fileName);
                                if (SparrowSystem.upplatConfMap.containsKey(fileName)) {
                                    SparrowSystem.upplatConfMap.remove(fileName);
                                }
                            }
                        }
                        while ((watchkey = watchRes.poll(100, TimeUnit.MILLISECONDS)) != null) {
                            for (WatchEvent<?> event : watchkey.pollEvents()) {
                                fileName = event.context().toString();
                                System.out.println(fileName);
                                if (SparrowSystem.upplatResMap.containsKey(fileName)) {
                                    SparrowSystem.upplatResMap.remove(fileName);
                                }
                            }
                        }
                        while ((watchkey = watchStatic.poll(100, TimeUnit.MILLISECONDS)) != null) {
                            for (WatchEvent<?> event : watchkey.pollEvents()) {
                                fileName = event.context().toString();
                                System.out.println(fileName);
                                if (SparrowSystem.upplatStaticMap.containsKey(fileName)) {
                                    SparrowSystem.upplatStaticMap.remove(fileName);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
