package com.sywc.reflectors.monitor.notify;

import com.sywc.reflectors.SparrowSystem;
import net.contentobjects.jnotify.JNotifyListener;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 目录监听器
 *
 * @author zhenhuang
 * @version 1.0.0
 * @date 2018-02-07 14:18
 */
public class DirectoryNotifyListener implements JNotifyListener {
    private Logger logger = LoggerFactory.getLogger(DirectoryNotifyListener.class);
    /**
     * 文件类型 1-静态文件 2- 配置文件 3-响应文件
     */
    private Integer fileType;

    public DirectoryNotifyListener() {
        this.fileType = 1;
    }

    public DirectoryNotifyListener(int fileType) {
        this.fileType = fileType;
    }

    @Override
    public void fileCreated(int wd, String rootPath, String name) {
        logger.debug("目录[{}] 下新增了文件 {},程序不做任何处理", rootPath, name);
    }

    @Override
    public void fileDeleted(int wd, String rootPath, String name) {
        if (StringUtils.isEmpty(name) || (name.indexOf(".") == 0)) {
            return;
        }
        logger.debug("目录[{}] 下删除了文件 {}", rootPath, name);
        removeCache(fileType, name);
    }

    @Override
    public void fileModified(int wd, String rootPath, String name) {
        if (StringUtils.isEmpty(name) || (name.indexOf(".") == 0)) {
            return;
        }
        logger.debug("目录[{}] 下修改了文件 {}", rootPath, name);
        removeCache(fileType, name);
    }

    @Override
    public void fileRenamed(int wd, String rootPath, String oldName, String newName) {
        if (StringUtils.isEmpty(oldName) || (oldName.indexOf(".") == 0)) {
            return;
        }
        logger.debug("目录[{}] 下重命名了文件 原文件{} 修改后的文件{}", rootPath, oldName, newName);
        removeCache(fileType, oldName);
    }


    private void removeCache(int fileType, String fileName) {
        switch (fileType) {
            case 1: {
                if (SparrowSystem.upplatStaticMap.containsKey(fileName)) {
                    logger.debug("The static  file {} in cache,remove it!");
                    SparrowSystem.upplatStaticMap.remove(fileName);
                }
                break;
            }
            case 2: {
                if (SparrowSystem.upplatConfMap.containsKey(fileName)) {
                    logger.debug("The conf  file {} in cache,remove it!");
                    SparrowSystem.upplatConfMap.remove(fileName);
                }
                break;
            }
            case 3: {
                if (SparrowSystem.upplatResMap.containsKey(fileName)) {
                    logger.debug("The response  file {} in cache,remove it!");
                    SparrowSystem.upplatResMap.remove(fileName);
                }
                break;
            }
            default: {
                logger.debug("not match fileType,fileType is {}", fileType);
            }
        }
    }
}
