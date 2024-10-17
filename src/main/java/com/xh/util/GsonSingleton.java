package com.xh.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @CreateTime: 2024-10-15  14:52
 * @Author: 小汉同学
 * @Description:
 */
public class GsonSingleton {
    private static final Gson gson;

    static {
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    public static Gson getInstance() {
        return gson;
    }
}
