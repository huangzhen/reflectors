package com.iflytek.sparrow.share;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by mingzhang2 on 16/11/15.
 */
public class AdxMetrics implements Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(AdxMetrics.class);

  private static final String METRICS_DOMAIN = "gnome.adx";

//  private static final String HTTP_REQUESTS = "httpRequests";
  private static final String HTTP_REQUESTS = "HTTP_REQUESTS";
//  private static final String AD_REQUESTS = "adRequests";
  private static final String AD_REQUESTS = "AD_REQUESTS";
  private static final String IMPRESS_REQUESTS = "IMPRESS_REQUESTS";
  private static final String CLICK_REQUESTS = "CLICK_REQUESTS";
  private static final String INSTALL_REQUESTS = "INSTALL_REQUESTS";

  // 所有的http请求
  private Meter httpRequests;
  // 所有的广告请求
  private Meter adRequests;
  // 曝光监控请求
  private Meter impressRequests;
  // 点击监控请求
  private Meter clickRequests;
  // 安装监控请求
  private Meter installRequests;

  private final ConcurrentMap<String, Metric> threadPoolMetricOfPlats;

  // 每次会话的响应时间
  private Histogram sessionTime;

  private Counter queueSize;

  private final MetricRegistry registry;
  private final JmxReporter jmxReporter;

  public AdxMetrics() {
    registry = new MetricRegistry();
    jmxReporter = JmxReporter.forRegistry(registry).inDomain(METRICS_DOMAIN)
      .build();

    httpRequests = registry.meter(HTTP_REQUESTS);
    adRequests = registry.meter(AD_REQUESTS);
    impressRequests = registry.meter(IMPRESS_REQUESTS);
    clickRequests = registry.meter(CLICK_REQUESTS);
    installRequests = registry.meter(INSTALL_REQUESTS);
    threadPoolMetricOfPlats = new ConcurrentHashMap<>();
  }

  public MetricRegistry getRegistry() {
    return registry;
  }

  public ConcurrentMap<String, Metric> getThreadPoolMetricOfPlats() {
    return threadPoolMetricOfPlats;
  }

  public void start() {
    jmxReporter.start();
  }

  public void stop() {
    jmxReporter.stop();
  }

  @Override
  public void close() {
    jmxReporter.close();
  }

  public void incrHttpRequest() {
    httpRequests.mark();
  }

  public void incrAdRequests() {
    adRequests.mark();
  }

  public void incrHttpRequest(long n) {
    httpRequests.mark(n);
  }

  public void incrImpressRequests() {
    impressRequests.mark();
  }

  public void incrClickRequests() {
    clickRequests.mark();
  }

  public void incrInstallRequests() {
    installRequests.mark();
  }
}
