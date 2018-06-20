package org.httpkit.server;

import org.httpkit.BytesInputStream;
import org.httpkit.HttpMethod;
import org.httpkit.HttpUtils;
import org.httpkit.HttpVersion;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import static org.httpkit.HttpUtils.CHARSET;
import static org.httpkit.HttpUtils.CONNECTION;
import static org.httpkit.HttpUtils.CONTENT_TYPE;
import static org.httpkit.HttpUtils.getStringValue;
import static org.httpkit.HttpVersion.HTTP_1_1;

public class HttpRequest {
    public final String queryString;
    public final String uri;
    public final HttpMethod method;
    public final HttpVersion version;
    // package visible
    int serverPort = 80;
    String serverName;
    Map<String, Object> headers;
    int contentLength = 0;
    String contentType;
    String charset = "utf8";
    boolean isKeepAlive = false;
    boolean isWebSocket = false;
    InetSocketAddress remoteAddr;
    InetSocketAddress localAddr;
    AsyncChannel channel;
    private byte[] body;

    public HttpRequest(HttpMethod method, String url, HttpVersion version) {
        this.method = method;
        this.version = version;
        int idx = url.indexOf('?');
        if (idx > 0) {
            uri = url.substring(0, idx);
            queryString = url.substring(idx + 1);
        } else {
            uri = url;
            queryString = null;
        }
    }

    public InputStream getBody() {
        if (body != null) {
            return new BytesInputStream(body, contentLength);
        }
        return null;
    }

    public String getRemoteAddr() {
        String h = getStringValue(headers, HttpUtils.X_FORWARDED_FOR);
        if (null != h) {
            int idx = h.indexOf(',');
            if (idx == -1) {
                return h;
            } else {
                // X-Forwarded-For: client, proxy1, proxy2
                return h.substring(0, idx);
            }
        } else {
            return remoteAddr.getAddress().getHostAddress();
        }
    }

    public void setBody(byte[] body, int count) {
        this.body = body;
        this.contentLength = count;
    }

    public byte[] getPostBody() {
        return body;
    }

    public Map<String, Object> getHeaders() {
        Map<String, Object> mapHeaders = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            mapHeaders.put(entry.getKey(), entry.getValue());
        }

        return mapHeaders;
    }

    public void setHeaders(Map<String, Object> headers) {
        String h = getStringValue(headers, "host");
        if (h != null) {
            int idx = h.lastIndexOf(':');
            if (idx != -1) {
                this.serverName = h.substring(0, idx);
                serverPort = Integer.valueOf(h.substring(idx + 1));
            } else {
                this.serverName = h;
            }
        }

        String ct = getStringValue(headers, CONTENT_TYPE);
        if (ct != null) {
            int idx = ct.indexOf(";");
            if (idx != -1) {
                int cidx = ct.indexOf(CHARSET, idx);
                if (cidx != -1) {
                    contentType = ct.substring(0, idx);
                    charset = ct.substring(cidx + CHARSET.length());
                } else {
                    contentType = ct;
                }
            } else {
                contentType = ct;
            }
        }

        String con = getStringValue(headers, CONNECTION);
        if (con != null) {
            con = con.toLowerCase();
        }

        isKeepAlive = (version == HTTP_1_1 && !"close".equals(con)) || "keep-alive".equals(con);
        isWebSocket = "websocket".equalsIgnoreCase(getStringValue(headers, "upgrade"));
        this.headers = headers;
    }

    public InetSocketAddress getLocalAddr() {
        return localAddr;
    }
}
