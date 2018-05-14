package com.iflytek.sparrow.http;

import com.iflytek.sparrow.share.Constants;
import com.iflytek.sparrow.share.GThreadFactory;
import com.iflytek.sparrow.share.OriginRequest;
import com.iflytek.sparrow.share.concurrent.GThreadPoolExecutor;
import org.httpkit.HttpMethod;
import org.httpkit.client.HttpClient;
import org.httpkit.client.IFilter;
import org.httpkit.client.IRespListener;
import org.httpkit.client.IResponseHandler;
import org.httpkit.client.RequestConfig;
import org.httpkit.client.RespListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class GClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(GClient.class);

    private String clientName;
    private HttpClient httpClient;
    private ExecutorService pool = null;
    private final AtomicInteger poolNumber = new AtomicInteger(0);

    public GClient(String clientName) {
        this.clientName = clientName;
        pool = new GThreadPoolExecutor(new GThreadFactory(clientName + "-thread-pool-" + poolNumber.getAndIncrement()));
    }

    private volatile boolean running = false;

    public boolean start() {
        if (running) {
            LOGGER.warn("{} is running.", clientName);
            return true;
        }
        running = true;
        try {
            httpClient = new HttpClient(clientName);
            httpClient.start();
        } catch (IOException e) {
            running = false;
            LOGGER.error("{} failed to start: ", clientName, e);
            return false;
        }
        LOGGER.info("{} is running.", clientName);
        return true;
    }

    public void stop() {
        if (!running) {
            LOGGER.warn("{} is not running.", clientName);
            return;
        }
        running = false;

        if (httpClient != null) {
            httpClient.stop();
        }
        LOGGER.info("{} is stopped.", clientName);
    }

    public void sendRequest(OriginRequest reqInfo, IResponseHandler rspHandler, int timeout) {
        RequestConfig reqConfig = null;
        String url = reqInfo.url;
        if (Constants.HTTP_REQ_TYPE_GET == reqInfo.type) {
            reqConfig = new RequestConfig(HttpMethod.GET, reqInfo.headers, null, timeout, 500);
        } else {
            ByteBuffer byteBuff = ByteBuffer.wrap(reqInfo.postBody);
            reqConfig = new RequestConfig(HttpMethod.POST, reqInfo.headers, byteBuff, timeout, 500);
        }

        IRespListener listener = new RespListener(rspHandler, IFilter.ACCEPT_ALL, pool, 1);
        httpClient.exec(url, reqConfig, null, listener);
    }

    public void sendGetRequest(String url, Map<String, Object> headers, IResponseHandler responseHandler, int timeoutMillis) {
        sendGetRequest(url, headers, responseHandler, timeoutMillis, 500);
    }

    public void sendGetRequest(String url,
                               Map<String, Object> headers,
                               IResponseHandler responseHandler,
                               int timeoutMillis,
                               int keepAliveMillis) {

        IRespListener respListener = new RespListener(responseHandler, IFilter.ACCEPT_ALL, pool, 1);
        RequestConfig requestConfig = new RequestConfig(HttpMethod.GET, headers, null, timeoutMillis, keepAliveMillis);
        httpClient.exec(url, requestConfig, null, respListener);
    }

    public void sendPostRequest(String url, Map<String, Object> headers, ByteBuffer body, IResponseHandler responseHandler,
                                int timeoutMillis) {
        sendPostRequest(url, headers, body, responseHandler, timeoutMillis, 500);
    }

    public void sendPostRequest(String url, Map<String, Object> headers, ByteBuffer body, IResponseHandler responseHandler,
                                int timeoutMillis, int keepAliveMillis) {
        IRespListener respListener = new RespListener(responseHandler, IFilter.ACCEPT_ALL, pool, 1);
        RequestConfig requestConfig = new RequestConfig(HttpMethod.POST, headers, body, timeoutMillis, keepAliveMillis);
        httpClient.exec(url, requestConfig, null, respListener);
    }
}
