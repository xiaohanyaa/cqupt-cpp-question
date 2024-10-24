package com.xh.util;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @CreateTime: 2024-10-15  14:53
 * @Author: 小汉同学
 * @Description:
 */
public class OkHttpSingleton {
    private static final OkHttpClient okHttpClient;
    private static final int MAX_RETRIES = 5;
    private static final int DELAY_MS = 3000; // 1 second delay between retrie

    static {
        // 由于使用校园网，所以永不超时，可以根据实际进行调整
        okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor())
                .connectTimeout(0, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                .build();
}

    public static OkHttpClient getInstance() {
        return okHttpClient;
    }

    private static class RetryInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = null;
            IOException exception = null;

            for (int retry = 0; retry <= MAX_RETRIES; retry++) {
                try {
                    response = chain.proceed(request);
                    return response;
                } catch (IOException e) {
                    System.err.println("No route to host, retrying... (" + (retry + 1) + "/" + MAX_RETRIES + ")");
                    exception = e;
                    try {
                        Thread.sleep(DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            throw exception;
        }
    }
}
