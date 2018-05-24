package com.sywc.reflectors.share;

import java.util.Objects;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class DelayMsg implements Delayed {
    private static AtomicLong seq = new AtomicLong(0);

    private Object objContext;
    private long delayTime;
    private long dmId;

    public DelayMsg(Object objContext, long delayTime) {
        this.objContext = objContext;
        this.delayTime = System.currentTimeMillis() + delayTime;
        this.dmId = seq.getAndIncrement();
    }

    /**
     * 该值是在 take 或 poll 时使用，Delayed 队列根据 该值 是否 小于等于 0 来判断要不要弹出
     * 所以在调用该方法时，前后两次应是返回不同的值（根时间单位有关）
     *
     * @param unit
     * @return
     */
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(delayTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        long d = this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS);
        return (d <= 0L) ? -1 : 1;
    }

    public Object getObjContext() {
        return objContext;
    }

    @Override
    public int hashCode() {
        return Objects.hash(objContext, delayTime, dmId);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof DelayMsg) {
            return object.hashCode() == hashCode() ? true : false;
        }
        return false;
    }

    public long getDelayTime() {
        return delayTime;
    }
}
