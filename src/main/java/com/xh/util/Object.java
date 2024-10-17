package com.xh.util;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;

public interface Object {
    // 暴露
    OkHttpClient OK_HTTP_CLIENT = OkHttpSingleton.getInstance();
    Gson GSON = GsonSingleton.getInstance();
}
