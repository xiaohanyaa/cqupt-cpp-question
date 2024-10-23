package com.xh.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

import static com.xh.util.Object.GSON;
import static com.xh.util.Object.OK_HTTP_CLIENT;

/**
 * @CreateTime: 2024-10-15  14:35
 * @Author: 小汉同学
 * @Description:
 */
public class DeepSeek {
    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final String API_KEY = "sk-1d00b8e0aeasd896a6b0bbcee716d";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public static String getAnswerByDeepSeekAI(String question) {
        System.out.println("正在调用DeepSeekAI：\n" + question);
        RequestBody body = RequestBody.create(question, JSON);
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();
        try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            // 处理响应
            JsonObject jsonObject = GSON.fromJson(response.body().string(), JsonObject.class);
            JsonArray choices = jsonObject.getAsJsonArray("choices");
            return choices.get(0).getAsJsonObject().
                    get("message").getAsJsonObject()
                    .get("content").getAsString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
