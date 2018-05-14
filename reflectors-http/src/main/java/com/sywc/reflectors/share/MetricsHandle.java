package com.sywc.reflectors.share;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by qlzhang on 2017/1/18.
 */
public class MetricsHandle {
  private static final AdxMetrics metrics = new AdxMetrics();
  private static Logger log = LoggerFactory.getLogger(MetricsHandle.class);
  static {
    metrics.start();
    log.info("metrics related modules have been started.");
  }

  public static AdxMetrics getMetrics() {
    return metrics;
  }

  private MetricsHandle() {

  }
}
