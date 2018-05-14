package com.sywc.reflectors;

import com.sywc.reflectors.module.GServiceModule;
import com.sywc.reflectors.module.GSysMgrModule;
import com.iflytek.sparrow.share.Constants;
import com.sywc.reflectors.share.LruCacheMap;
import com.sywc.reflectors.share.UtilOper;
import com.sywc.reflectors.share.dto.PlatConfigDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 主线程类
 *
 * @author huangzhen
 */
public class ReflectorsSystem {
    private static final Logger logger = LoggerFactory.getLogger(ReflectorsSystem.class);
    private static final String configFile = Thread.currentThread().getContextClassLoader().getResource("reflectors.conf").getPath();

    private static final int mapCacheSize = UtilOper.getIntValue(configFile, "map_cache_size", 100);
    public static volatile LruCacheMap<String, PlatConfigDTO> upplatConfMap = new LruCacheMap<>(mapCacheSize);
    public static volatile LruCacheMap<String, String> upplatResMap = new LruCacheMap<>(mapCacheSize);
    public static volatile LruCacheMap<String, String> upplatStaticMap = new LruCacheMap<>(mapCacheSize);

    public static final String upplatDirPath = UtilOper.getStringValue(configFile, "upplat_dir_path", "");
    public static final String staticDirPath = UtilOper.getStringValue(configFile, "static_dir_path", "");

    private static GSysMgrModule sysMgrTask;

    public static void main(String[] args) {
        Thread.currentThread().setName("SystemMainTask");
        sysMgrTask = new GSysMgrModule(args[0]);
        sysMgrTask.addMsg(Constants.MSG_ID_SYS_INIT, null);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                sysMgrTask.stop();
                logger.warn("addShutdownHook...run");
            }
        });
    }

    public static GSysMgrModule sysMgrModule() {
        return sysMgrTask;
    }

    public static GServiceModule srvModule() {
        return sysMgrTask.getSrvModule();
    }

}
