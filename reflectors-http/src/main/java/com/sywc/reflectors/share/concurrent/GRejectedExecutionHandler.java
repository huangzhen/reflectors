package com.iflytek.sparrow.share.concurrent;

/**
 * Created by qlzhang on 2017/1/20.
 */
public interface GRejectedExecutionHandler {
  void rejectedExecution(Runnable r, GThreadPoolExecutor executor);
}
