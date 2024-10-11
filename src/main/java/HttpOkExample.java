import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class HttpOkExample {

    public static void main(String[] args) {
        // 获取所有的程序代码编号
        getCProgramID();
        //CProgramIDList.add("ch001_001");
        getQuestion();
    }

    static OkHttpClient client = new OkHttpClient().newBuilder()
            .connectTimeout(0, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .writeTimeout(0, TimeUnit.SECONDS)
            .build();
    // {341, 342, 343, 344, 345, 401, 402, 403, 404, 405}
    // 已完成id 341, 342, 343, 344,345, 401, 402
    // 未完成id 403, 404, 405
    static int[] cChapterID = {403, 404, 405};
    static ArrayList<String> CProgramIDList = new ArrayList<>();
    private static final String BASE_URL = "http://172.22.214.200/ctas/ajaxpro/CExam.CPractice,App_Web_tzfdzrj8.ashx";
    private static final MediaType MEDIA_TYPE_PLAIN = MediaType.get("text/plain; charset=UTF-8");
    private static final String sessionId = "jdpr3345cpoojkfbw25lyf55";

    private static final String API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";
    private static final String API_KEY = "ae4fca85815eb058a11e66a114d5d29c.WELoXJ7WBx5tjRGb";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static String getAnswerByZhiPuAI(String question) {
        String json = buildRequestBody(question);
        System.out.println("正在询问AI：\n" + json);
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            // 处理响应
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(response.body().string(), JsonObject.class);
            JsonArray choices = jsonObject.getAsJsonArray("choices");
            return choices.get(0).getAsJsonObject().
                    get("message").getAsJsonObject()
                    .get("content").getAsString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String buildRequestBody(String question) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("model", "glm-4-plus");
        JsonObject messageObject = new JsonObject();
        messageObject.addProperty("role", "user");
        messageObject.addProperty("content", question);
        JsonArray messagesArray = new JsonArray();
        messagesArray.add(messageObject);
        jsonObject.add("messages", messagesArray);
        Gson gson = new Gson();
        return gson.toJson(jsonObject);
    }

    private static void getCProgramID() {
        Arrays.stream(cChapterID).forEach(
                id -> {
                    String requestBody = String.format("{\"cChapterID\":\"%d\"}", id);
                    Request request = new Request.Builder()
                            .url(BASE_URL)
                            .post(RequestBody.create(requestBody, MEDIA_TYPE_PLAIN))
                            .addHeader("accept", "*/*")
                            .addHeader("accept-language", "zh-CN,zh;q=0.9")
                            .addHeader("cache-control", "no-cache")
                            .addHeader("content-type", "text/plain; charset=UTF-8")
                            .addHeader("pragma", "no-cache")
                            .addHeader("x-ajaxpro-method", "GetJSONProgramList")
                            .addHeader("cookie", "ASP.NET_SessionId=" + sessionId)
                            .addHeader("Referer", "http://172.22.214.200/ctas/CPractice.aspx")
                            .addHeader("Referrer-Policy", "strict-origin-when-cross-origin")
                            .build();

                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            throw new IOException("Unexpected code " + response);
                        }
                        String responseBody = response.body().string()
                                .replace("\\", "")
                                .replaceFirst("\"", "")
                                .replace("\";/*", "");
                        // 使用 Gson 解析 JSON 数组
                        Gson gson = new Gson();
                        Type listType = new TypeToken<List<Map<String, String>>>() {
                        }.getType();
                        List<Map<String, String>> programList = gson.fromJson(responseBody, listType);

                        // 提取 CProgramID 并放入 List 中
                        List<String> cProgramIDs = programList.stream()
                                .map(program -> program.get("CProgramID"))
                                .toList();
                        CProgramIDList.addAll(cProgramIDs);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    private static void getQuestion() {
        OkHttpClient client = new OkHttpClient();
        CProgramIDList.forEach(cProgram -> {
            int questionIndex = 0;
            boolean addCode = true;
            while (true) {
                // 防止被识别为爬虫 问AI的时候是同步的 会花不定的时间
                //try {
                //    Thread.sleep(ThreadLocalRandom.current().nextInt(0, 2000));
                //} catch (InterruptedException e) {
                //    throw new RuntimeException(e);
                //}
                System.out.println("正在爬取题目 cProgram = " + cProgram + ", cQuestionIndex = " + questionIndex);
                String requestBody = String.format("{\"strTestParam\":\"<cTest><cProgram>%s</cProgram><cQuestionIndex>%d</cQuestionIndex></cTest>\"}", cProgram, questionIndex);
                Request request = new Request.Builder()
                        .url(BASE_URL)
                        .post(RequestBody.create(requestBody, MEDIA_TYPE_PLAIN))
                        .addHeader("Accept", "*/*")
                        .addHeader("Accept-Encoding", "gzip, deflate")
                        .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
                        .addHeader("Cache-Control", "no-cache")
                        .addHeader("Connection", "keep-alive")
                        .addHeader("Content-Type", "text/plain; charset=UTF-8")
                        .addHeader("Cookie", "ASP.NET_SessionId=" + sessionId)
                        .addHeader("Host", "172.22.214.200")
                        .addHeader("Origin", "http://172.22.214.200")
                        .addHeader("Pragma", "no-cache")
                        .addHeader("Referer", "http://172.22.214.200/ctas/CPractice.aspx")
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36")
                        .addHeader("X-AjaxPro-Method", "GetJSONTest")
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }
                    String resp = response.body().string();
                    if (resp.contains("ArgumentOutOfRangeException")) {
                        System.out.println("题目 cProgram = " + cProgram + ", cQuestionIndex = " + questionIndex + " 不存在");
                        System.out.println(cProgram + "的相关题目已完成！\n");
                        break;
                    }
                    String responseBody = resp
                            .replace("\\", "")
                            .replaceFirst("\"", "")
                            .replace("\";/*", "");
                    Gson gson = new Gson();
                    // 将 responseBody 中的 HTML 转义字符转换为实际的符号
                    String unescapedResponseBody = StringEscapeUtils.unescapeHtml4(responseBody);
                    if (unescapedResponseBody.contains("正在中止线程")) {
                        throw new RuntimeException("请重新登录后再执行");
                    }
                    // 使用 Gson 解析 JSON 字符串
                    JsonObject jsonObject = gson.fromJson(unescapedResponseBody, JsonObject.class);

                    JsonObject cQuestion = jsonObject.getAsJsonObject("CQuestion");
                    String cQuestionID = cQuestion.get("CQuestionID").getAsString();
                    String cQuestionContent = unescapeHtml(cQuestion.get("CQuestionContent").getAsString());
                    String cProgramCode = unescapeHtml(cQuestion.get("CProgramCode").getAsString());
                    String question = String.format("%s问题：%s", cProgramCode, cQuestionContent);
                    // 调用智普AI进行问题解析
                    String answer = Objects.requireNonNull(getAnswerByZhiPuAI(question)).replaceAll("#+ ", "");
                    System.out.println("通过AI获取解析成功");
                    // 拼接问题和解析
                    String result = "";
                    if (addCode) {
                        // 第一次需要添加程序代码对应的id以及程序代码
                        addCode = false;
                        result = String.format("## %s\n``` c++\n%s\n```\n", cProgram, cProgramCode);
                    }
                    result = String.format("%s### 题目%s\n问题：%s\n\n**解析如下：**\n\n------\n\n%s\n\n------\n\n", result, cQuestionID, cQuestionContent, answer);
                    // 写入到文件中
                    String fileName = "c++题库.md";
                    Files.write(Paths.get(fileName), result.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    System.out.println("题目+解析写入文件成功\n");
                    questionIndex++;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private static String unescapeHtml(String input) {
        return input
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&nbsp;", " ")
                .replace("<br>", "\n");
    }
}