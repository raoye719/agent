package com.yupi.yuaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class StudyAppTest {

    @Resource
    private StudyApp studyApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是考研学生";
        String answer = studyApp.doChat(message, chatId);
        // 第二轮
        message = "我想提高数学成绩";
        answer = studyApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第三轮
        message = "我之前说过我想提高什么科目吗？";
        answer = studyApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是考研学生，我想提高数学成绩，但我不知道该怎么做";
        StudyApp.StudyReport studyReport = studyApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(studyReport);
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "泰勒公式怎么记？";
        String answer = studyApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithTools() {
        // 测试联网搜索问题的答案
        testMessage("2024年考研数学真题在哪里可以下载？");

        // 测试网页抓取：考研资讯
        testMessage("查看编程导航网站（codefather.cn）上有没有考研相关的资源？");

        // 测试资源下载：PDF下载
        testMessage("直接下载一份考研数学复习资料为文件");

        // 测试终端操作：执行代码
        testMessage("执行 Python3 脚本来分析考研数据");

        // 测试文件操作：保存学习计划
        testMessage("保存我的考研学习计划为文件");

        // 测试 PDF 生成
        testMessage("生成一份'考研复习计划'PDF，包含各阶段目标、时间安排和学习资源");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = studyApp.doChatWithTools(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        String message = "帮我搜索一些考研学习的相关图片";
        String answer =  studyApp.doChatWithMcp(message, chatId);
        Assertions.assertNotNull(answer);
    }
}
