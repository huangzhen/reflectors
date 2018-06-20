package com.sywc.reflectors.share;

import com.codahale.metrics.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by qlzhang on 8/16/2016.
 * <p>
 * similar to Executors.DefaultThreadFactory, but GThreadFactory supports you to name the thread pool.
 */
public class GThreadFactory implements ThreadFactory {
    private static Logger log = LoggerFactory.getLogger(GThreadFactory.class);
    //  private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    private Meter metric;
    private String poolName;

    public GThreadFactory(String poolName) {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() :
                Thread.currentThread().getThreadGroup();
        namePrefix = poolName + "-thread-";
        this.poolName = poolName;
        if (poolName == null || poolName.isEmpty()) {
            log.error("poolName is null or empty");
        } else {
            try {
                metric = MetricsHandle.getMetrics().getRegistry().meter(poolName);
            } catch (Exception e) {
                log.error("generating new meter failed: ", e);
            }
        }
    }

    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r,
                namePrefix + threadNumber.getAndIncrement(),
                0);
        if (t.isDaemon())
            t.setDaemon(false);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
        if (metric != null) {
            metric.mark();
        }
        return t;
    }

    public String getPoolName() {
        return poolName;
    }

    public Meter getMetric() {
        return metric;
    }
}
