package com.xh.util;

import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

/**
 * @CreateTime: 2024-10-15  14:53
 * @Author: 小汉同学
 * @Description:
 */
public class OkHttpSingleton {
    private static final OkHttpClient okHttpClient;

    static {
        // 由于使用校园网，所以永不超时，可以根据实际进行调整
        okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(0, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                .build();
}

    public static OkHttpClient getInstance() {
        return okHttpClient;
    }
}
