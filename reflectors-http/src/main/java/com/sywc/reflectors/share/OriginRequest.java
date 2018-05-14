package com.iflytek.sparrow.share;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class OriginRequest {
    public int type;                       /* 0-GET, 1-POST */
    public String url;
    public String getQuery;
    public byte[] postBody;
    public Map<String, Object> headers = Maps.newHashMap();

    public String getHeaderValue(String name) {
        String value = null;
        if (headers.containsKey(name)) {
            Object valueObj = headers.get(name);
            if (valueObj instanceof String) {
                value = (String) valueObj;
            }
        }
        return value;
    }

    public boolean isHeaderEmpty() {
        return headers == null || headers.isEmpty();
    }

    public String toString() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", type);
        jsonObject.addProperty("url", url);
        jsonObject.addProperty("getQuery", getQuery);
        if (postBody != null) {
            try {
                jsonObject.addProperty("postBody", new String(postBody, "utf-8"));
            } catch (UnsupportedEncodingException e) {
                jsonObject.addProperty("postBody", "parse postBody failed");
            }
        } else {
            jsonObject.addProperty("postBody", "");
        }
        jsonObject.addProperty("headers", JSONObject.toJSONString(this));
        return jsonObject.toString();
    }

}
