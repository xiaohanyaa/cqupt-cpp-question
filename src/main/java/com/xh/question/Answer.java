package com.xh.question;

import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.xh.util.Object.GSON;

/**
 * @CreateTime: 2024-10-18  07:50
 * @Author: 小汉同学
 * @Description:
 */
public class Answer {

    static final Pattern pattern = Pattern.compile("\\{'(\\d+)':'([A-D])'\\}");

    public static final String fileName = "httpOk/questions/c++题库--答案汇总byDeepSeek.txt";

    public static void main(String[] args) throws Exception {
        Answer answer = new Answer();
        answer.collectAnswer();
        //answer.countChapterNumber();
        // answer.checkAnswer(answer, new Question());
    }

    private void countChapterNumber() throws IOException {
        int[] result = new int[10];
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();

        File dir = new File("httpOk/questions");
        System.out.println(dir.getAbsolutePath());
        if (!dir.exists()) {
            throw new RuntimeException(dir.getName() + "目录不存在");
        }

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            throw new RuntimeException(dir.getName() + "，该目录下不存在文件");
        }

        Pattern pat = Pattern.compile("c\\+\\+题库--程序ch");
        for (File file : files) {
            String fileName = file.getName();
            Matcher matcher = pat.matcher(fileName);
            if (!matcher.find()) {
                break;
            }
            int chapterId = Integer.parseInt(fileName.substring(11, 14)); // 章节
            fileName = fileName.replace("c++题库--程序", "")
                    .replace(".md", "");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher1 = pattern.matcher(line);// {'22722':'D'}
                    if (matcher1.find()) {
                        result[chapterId - 1]++;
                        map.putIfAbsent(fileName, 0);
                        map.computeIfPresent(fileName, (k, v) -> v + 1);
                    }
                }
            }
        }
        for (int i = 0; i < result.length; i++) {
            System.out.printf("第 %d 章共有 %d 道题目\n", i + 1, result[i]);
        }

        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " -- " + entry.getValue());
        }
    }

    public void collectAnswer() throws Exception {
        ArrayList<String> list = new ArrayList<>();
        File dir = new File("httpOk/questions");
        System.out.println(dir.getAbsolutePath());
        if (!dir.exists()) {
            throw new RuntimeException(dir.getName() + "目录不存在");
        }

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            throw new RuntimeException(dir.getName() + "，该目录下不存在文件");
        }

        for (File file : files) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher matcher1 = pattern.matcher(line);// {'22722':'D'}
                    if (matcher1.find()) {
                        list.add(matcher1.group().replaceAll("'", "\""));
                    }
                }
            }
        }

        // 把list写入文件
        Files.write(Paths.get(fileName), list, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public void boot(Answer answer, Question question) throws IOException, InterruptedException {
        Set<Map.Entry<String, String>> set = answer.readAnswerToSet();
        for (Map.Entry<String, String> e : set) {
            question.doQuestion(e.getKey(), e.getValue(), true);
            // 模拟刷题
            Thread.sleep(ThreadLocalRandom.current().nextInt(3000, 6000));
        }
        int total = set.size();
        System.out.printf("题目总数: %d, 正确数量: %d, 错误数量: %d, 正确率：%f%n",
                total, Question.correctNum, total - Question.correctNum, Question.correctNum * 1.0 / total);
    }

    private void checkAnswer(Answer answer, Question question) throws IOException, InterruptedException {
        Set<Map.Entry<String, String>> set = answer.readAnswerToSet();
        int i = 0;
        for (Map.Entry<String, String> e : set) {
            // 校对答案 不写入日志
            question.doQuestion(e.getKey(), e.getValue(), false);
            i++;
            if (i >= 100) {
                // 降低一次性QPS
                Thread.sleep(10 * 1000);
                i = 0;
            }
        }
        int total = set.size();
        System.out.printf("题目总数: %d, 正确数量: %d, 错误数量: %d, 正确率：%f%n",
                total, Question.correctNum, total - Question.correctNum, Question.correctNum * 1.0 / total);
    }

    private Set<Map.Entry<String, String>> readAnswerToSet() throws IOException {
        File file = new File(fileName);
        System.out.println(file.getAbsolutePath());
        if (!file.exists()) {
            throw new RuntimeException(file.getName() + "文件不存在");
        }
        LinkedHashMap<String, String> map = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            Type type = new TypeToken<Map<String, String>>() {
            }.getType();

            while ((line = reader.readLine()) != null) {
                Map<String, String> lineMap = GSON.fromJson(line, type);
                Set<Map.Entry<String, String>> entrySet = lineMap.entrySet();
                for (Map.Entry<String, String> entry : entrySet) {// 每次只有一个set 待优化成其他方式
                    map.put(entry.getKey(), entry.getValue());// 由于题目只有1200题，量不是很大，所以直接全部存内存中
                }
            }
        }
        return map.entrySet();
    }
}
