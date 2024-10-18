package com.xh.question;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @CreateTime: 2024-10-18  07:50
 * @Author: 小汉同学
 * @Description:
 */
public class Answer {

    static final Pattern pattern = Pattern.compile("\\{'(\\d+)':'([A-D])'\\}");
    public static final String fileName = "httpOk/questions/c++题库--程序--答案汇总.txt";

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
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        list.add(matcher.group().replaceAll("'", "\""));
                    }
                }
            }
        }

        // 把list写入文件
        Files.write(Paths.get(fileName), list, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.APPEND);
    }
}
