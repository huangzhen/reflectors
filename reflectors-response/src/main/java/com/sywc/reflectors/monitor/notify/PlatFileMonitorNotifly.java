package com.sywc.reflectors.monitor.notify;

import com.sywc.reflectors.SparrowSystem;
import com.sywc.reflectors.share.SparrowConstants;
import com.sywc.reflectors.share.UtilOper;
import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 文件监控 notify 实现
 *
 * @author zhenhuang
 * @version 1.0.0
 * @date 2018-02-06 19:03
 */
public class PlatFileMonitorNotifly {
    private static final Logger logger = LoggerFactory.getLogger(PlatFileMonitorNotifly.class);
    private static ExecutorService fixedThreadPool = Executors.newFixedThreadPool(1);

    /**
     * 开启监听线程
     */
    public static void startMonitor() {
        fixedThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                StringBuilder fileBuilder = new StringBuilder();
                fileBuilder.append(SparrowSystem.upplatDirPath).append(File.separator).append(SparrowConstants.UPPLAT_CONF_DIR_NAME);
                String confFilePath = fileBuilder.toString();
                fileBuilder.setLength(0);
                fileBuilder.append(SparrowSystem.upplatDirPath).append(File.separator).append(SparrowConstants.UPPLAT_RES_DIR_NAME);
                String resFilePath = fileBuilder.toString();
                /**只监听 修改删除和重命名*/
                int mask = JNotify.FILE_DELETED | JNotify.FILE_MODIFIED | JNotify.FILE_RENAMED;
                /**是否监控子目录*/
                boolean watchSubtree = false;
                int staticId = 0;
                int confId = 0;
                int resId = 0;
                try {
                    staticId = JNotify.addWatch(SparrowSystem.staticDirPath, mask, watchSubtree, new DirectoryNotifyListener(1));
                    confId = JNotify.addWatch(confFilePath, mask, watchSubtree, new DirectoryNotifyListener(2));
                    resId = JNotify.addWatch(resFilePath, mask, watchSubtree, new DirectoryNotifyListener(3));
                } catch (JNotifyException e) {
                    logger.error("JNotify add monitor Exception,errorMs ={}", e.getMessage());
                    try {
                        if (0 != staticId) {
                            JNotify.removeWatch(staticId);
                        }
                        if (0 != confId) {
                            JNotify.removeWatch(confId);
                        }
                        if (0 != resId) {
                            JNotify.removeWatch(resId);
                        }
                    } catch (JNotifyException f) {
                        logger.error("JNotify remove monitor Exception,errorMs ={}", f.getMessage());

                    }
                }
                /**保持监听线程启动*/
                while (true) {
                    UtilOper.sleep(1000);
                }
            }
        });
    }
}
