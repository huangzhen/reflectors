package com.sywc.reflectors.share;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class ReflectorsGson {
    private static Gson gson;

    public static Gson getInstance() {
        if (gson == null) {
            gson = new GsonBuilder().disableHtmlEscaping().create();
        }
        return gson;
    }
}
