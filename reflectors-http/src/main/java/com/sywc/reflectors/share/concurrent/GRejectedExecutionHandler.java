package com.sywc.reflectors.share.concurrent;

/**
 * Created by qlzhang on 2017/1/20.
 */
public interface GRejectedExecutionHandler {
  void rejectedExecution(Runnable r, GThreadPoolExecutor executor);
}
