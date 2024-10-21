package com.xh.question;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.xh.ai.Zhipu.getAnswerByZhiPuAI;
import static com.xh.util.Object.GSON;
import static com.xh.util.Object.OK_HTTP_CLIENT;

/**
 * @CreateTime: 2024-10-15  14:40
 * @Author: 小汉同学
 * @Description:
 */
public class Question {
    static {
        Path questionsPath = Paths.get("httpOk/questions");
        if (!Files.isDirectory(questionsPath)) {
            System.out.println("创建文件夹: questions");
            try {
                Files.createDirectories(questionsPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final String BASE_URL = "http://172.22.214.200/ctas/ajaxpro/CExam.CPractice,App_Web_tzfdzrj8.ashx";
    private static final MediaType MEDIA_TYPE_PLAIN = MediaType.get("text/plain; charset=UTF-8");
    // 填入你的sessionId
    private static final String sessionId = "rwfijv554gohxb45sapol02q";

    // 本次需要刷题的章节id {341, 342, 343, 344, 345, 401, 402, 403, 404, 405}
    int[] cChapterID = {341, 342, 343, 344, 345, 401, 402, 403, 404, 405};
    // 存放章节中的所有程序编号
    public ArrayList<String> CProgramIDList = new ArrayList<>();

    public static int correctNum = 0;

    public void getCProgramID() {
        Type listType = new TypeToken<List<Map<String, String>>>() {
        }.getType();
        Arrays.stream(cChapterID).forEach(chapterId -> {
            String requestBody = String.format("{\"cChapterID\":\"%d\"}", chapterId);
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

            try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                String responseBody = response.body().string()
                        .replace("\\", "")
                        .replaceFirst("\"", "")
                        .replace("\";/*", "");

                List<Map<String, String>> programList = GSON.fromJson(responseBody, listType);

                // 提取 CProgramID 并放入 List 中
                List<String> cProgramIDs = programList.stream()
                        .map(program -> program.get("CProgramID"))
                        .toList();
                CProgramIDList.addAll(cProgramIDs);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void getQuestion() {
        // 参数有待调整
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                5, 10, 5000,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>()
        );

        CProgramIDList.forEach(programId -> executor.submit(() -> {
            System.out.println("Thread: " + programId);
            try {
                doQuestion(programId);
            } catch (Exception e) {
                // 处理异常 线程池会吞掉异常 因为这里用的runnable接口 待解决
                System.err.println("Exception in thread for programId " + programId + ": " + e.getMessage());
            }
        }));
        executor.shutdown(); // 不再接受新任务，等待所有已提交任务完成
    }

    public void doQuestion(String questionId, String questionAnswer, boolean writeLog) {
        String requestBody = String.format("{\"strTestParam\":\"<cTestParam><cQuestion>%s</cQuestion><cUserAnswer>%s</cUserAnswer></cTestParam>\"}", questionId, questionAnswer);
        Request request = new Request.Builder()
                .url(BASE_URL)
                .post(RequestBody.create(requestBody, MEDIA_TYPE_PLAIN))
                .addHeader("Accept", "*/*")
                .addHeader("Accept-Encoding", "gzip, deflate")
                .addHeader("Accept-language", "zh-CN,zh;q=0.9")
                .addHeader("Cache-control", "no-cache")
                .addHeader("Connection", "keep-alive")
                .addHeader("Content-Length", "100")
                .addHeader("Content-Type", "text/plain; charset=UTF-8")
                .addHeader("cookie", "ASP.NET_SessionId=" + sessionId)
                .addHeader("Host", "172.22.214.200")
                .addHeader("Origin", "http://172.22.214.200")
                .addHeader("Pragma", "no-cache")
                .addHeader("Referer", "http://172.22.214.200/ctas/CPractice.aspx")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36")
                .addHeader("X-AjaxPro-Method", "IsOrNotTrue")
                .build();
        try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            String resp = response.body().string();
            chcekSessionVaild(resp);
            if (resp.contains("1")) {
                correctNum++;
                System.out.printf("题目id：%s，答案：%s，正确！%n", questionId, questionAnswer);
                // 回答正确后，发 写入日志请求
                if (writeLog) {
                    writeLog(request.newBuilder().header("X-AjaxPro-Method", "WriteLog").build());
                }
            } else {
                System.err.printf("题目id：%s，答案：%s，错误!%n", questionId, questionAnswer);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void writeLog(Request writeLog) {
        try (Response response = OK_HTTP_CLIENT.newCall(writeLog).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            System.out.println("writeLog 响应：" + response.body().string());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void doQuestion(String programId) {
        int questionIndex = 0;// 记录每个程序的题目数
        boolean addCode = true;// 判断是否是第一次抓取
        // 这个程序下所有问题对话的列表
        JsonObject content = new JsonObject();
        // 初始化
        content.addProperty("model", "glm-4-plus");
        JsonArray messagesArray = new JsonArray();
        JsonObject[] dialogueObj = dialogueObj("我会给你一个程序代码，连续问你几个问题，问题与上一个问题没有关联，尽可能尝试运行代码，根据运行结果回答，回答问题要干净利落且准确,并把正确答案放在最后。比如，我给你的信息： '题目id:22690,问题:1+1=, A:1,B:2,C:3,D:4',你的回复：'解析，换行{'22690':'B'}',22690是题目id}",
                "好的，我会把代码运行，通过结果和题目一起分析得出正确答案,并把正确答案以你提供的格式放在最后");
        messagesArray.add(dialogueObj[0]);
        messagesArray.add(dialogueObj[1]);

        while (true) {
            System.out.println("正在爬取题目 cProgram = " + programId + ", cQuestionIndex = " + questionIndex);
            String requestBody = String.format("{\"strTestParam\":\"<cTest><cProgram>%s</cProgram><cQuestionIndex>%d</cQuestionIndex></cTest>\"}", programId, questionIndex);
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

            try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                String resp = response.body().string();
                chcekSessionVaild(resp);
                if (resp.contains("ArgumentOutOfRangeException")) {
                    System.out.println(programId + "的相关题目已完成！\n");
                    System.out.println("题目 cProgramId = " + programId + ", cQuestionIndex = " + questionIndex + " 不存在");
                    System.out.println("整体对话如下 ：\n" + GSON.toJson(content) + "\n");
                    break;
                }

                // 将 responseBody 中的 HTML 转义字符转换为实际的符号
                String unescapedResponseBody = StringEscapeUtils.unescapeHtml4(resp);
                unescapedResponseBody = unescapedResponseBody
                        .replaceFirst("\"", "")
                        .replaceAll("'{2,}", "'")
                        .replace("'\\\\n'", "'\\n'")
                        .replace("\";/*", "")
                        .replaceAll("\\\\\"", "\"");
                // 使用 Gson 解析 JSON 字符串
                JsonObject jsonObject = GSON.fromJson(unescapedResponseBody, JsonObject.class);
                // 解析题目响应
                JsonObject cQuestion = jsonObject.getAsJsonObject("CQuestion");
                String cQuestionID = cQuestion.get("CQuestionID").getAsString();
                String cQuestionContent = unescapeHtml(cQuestion.get("CQuestionContent").getAsString());
                String cProgramCode = unescapeHtml(cQuestion.get("CProgramCode").getAsString()).replaceAll("\\\\n", "\\n");
                String question = String.format("题目id:%s,问题：%s", cQuestionID, cQuestionContent);
                System.out.println("184行输出：" + question);
                String s = addCode ? String.format("程序代码：%s,  %s", cProgramCode, question) : question;
                // 调用智普AI进行问题解析
                JsonObject[] currDialogueObj = dialogueObj(s, "");
                messagesArray.add(currDialogueObj[0]);
                content.add("messages", messagesArray);
                String answer = Objects.requireNonNull(getAnswerByZhiPuAI(GSON.toJson(content))).replaceAll("#+ ", "");
                System.out.println("通过AI获取解析成功");
                // 拼接问题和解析
                String result = "";
                if (addCode) {
                    // 第一次需要添加程序代码对应的id以及程序代码
                    addCode = false;
                    result = String.format("## %s\n``` c++\n%s\n```\n", programId, cProgramCode);
                }
                result = String.format("%s### 题目%s\n问题：%s\n\n**解析如下：**\n\n------\n\n%s\n\n------\n\n", result, cQuestionID, cQuestionContent, answer);
                // 更新content，让AI知道上下文
                currDialogueObj[1].addProperty("content", answer);
                messagesArray.add(currDialogueObj[1]);
                // 写入到文件中
                String fileName = "httpOk/questions/c++题库--程序" + programId + ".md";
                Files.write(Paths.get(fileName), result.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                System.out.println("题目+解析写入文件成功\n");
                questionIndex++;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void chcekSessionVaild(String resp) {
        if (resp.contains("正在中止线程")) {
            throw new RuntimeException("请重新在浏览器登录后再执行并检查cookie是否已过期");
        }
    }

    private static JsonObject[] dialogueObj(String userQuestion, String aiRespond) {
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userQuestion);
        JsonObject ai = new JsonObject();
        ai.addProperty("role", "assistant");
        ai.addProperty("content", aiRespond);
        return new JsonObject[]{user, ai};
    }

    private static String unescapeHtml(String input) {
        return input.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&nbsp;", " ")
                .replace("<br>", "\n");
    }
}
