package com.sywc.reflectors.share;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU
 *
 * @author zhenhuang
 * @version 0.0.1
 * @date 2018-01-22 16:01
 */
public class LruCacheMap<K, V> extends LinkedHashMap<K, V> {
    private final int MAX_CACHE_SIZE;

    public LruCacheMap(int max_cache_size) {
        super((int) Math.ceil(max_cache_size / 0.75) + 1, 0.75f, true);
        MAX_CACHE_SIZE = max_cache_size;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > MAX_CACHE_SIZE;
    }
}
