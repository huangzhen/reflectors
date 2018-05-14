package com.sywc.reflectors.module;

import com.sywc.reflectors.share.task.GMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author huangzhen
 * @version 1.0.0
 */
public class GServiceModule {
    private static final Logger log = LoggerFactory.getLogger(GServiceModule.class);

    private long handlerMsgNum = 0;
    private int srvTaskNum;
    private List<GServiceTask> lstSrvTask = new ArrayList<GServiceTask>();

    public GServiceModule() {
    }

    public boolean startModule(int taskNum) {
        handlerMsgNum = 0;
        srvTaskNum = taskNum;
        for (int i = 0; i < srvTaskNum; i++) {
            GServiceTask srvTask = new GServiceTask("service-task-" + i);
            srvTask.startTask();
            lstSrvTask.add(srvTask);
        }

        return true;
    }

    public void addMsg(int msgId, Object objContext) {
        ++handlerMsgNum;

        int taskIdx = (int) (handlerMsgNum % srvTaskNum);
        if (taskIdx < 0) {
            taskIdx = 0 - taskIdx;
        }

        lstSrvTask.get(taskIdx).addMsg(new GMsg(msgId, objContext));
    }

    public void closeModule() {
        if (lstSrvTask != null) {
            for (GServiceTask srvTask : lstSrvTask) {
                srvTask.closeTask();
            }
            log.info("all service tasks are stopped.");
        }
    }
}
