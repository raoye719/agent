package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.agent.YuManus;
import com.yupi.yuaiagent.app.StudyApp;
import com.yupi.yuaiagent.constant.FileConstant;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private StudyApp studyApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    /**
     * 同步调用 AI 考研学习应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/study_app/chat/sync")
    public String doChatWithStudyAppSync(String message, String chatId) {
        return studyApp.doChat(message, chatId);
    }

    /**
     * SSE 流式调用 AI 考研学习应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/study_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithStudyAppSSE(String message, String chatId) {
        return studyApp.doChatByStream(message, chatId);
    }

    /**
     * SSE 流式调用 AI 考研学习应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/study_app/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithStudyAppServerSentEvent(String message, String chatId) {
        return studyApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    /**
     * SSE 流式调用 AI 考研学习应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/study_app/chat/sse_emitter")
    public SseEmitter doChatWithStudyAppServerSseEmitter(String message, String chatId) {
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter sseEmitter = new SseEmitter(180000L); // 3 分钟超时
        // 获取 Flux 响应式数据流并且直接通过订阅推送给 SseEmitter
        studyApp.doChatByStream(message, chatId)
                .subscribe(chunk -> {
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        // 返回
        return sseEmitter;
    }

    /**
     * 流式调用 Manus 超级智能体
     *
     * @param message
     * @return
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        YuManus yuManus = new YuManus(allTools, dashscopeChatModel);
        return yuManus.runStream(message);
    }

    /**
     * 下载生成的 PDF 文件
     *
     * @param fileName 文件名
     * @param response
     */
    @GetMapping("/file/download")
    public void downloadFile(@RequestParam String fileName, HttpServletResponse response) throws IOException {
        // 只允许下载 pdf 和 txt，防止目录穿越
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            response.sendError(400, "Invalid file name");
            return;
        }
        String[] allowedTypes = {".pdf", ".txt", ".md"};
        boolean allowed = false;
        for (String type : allowedTypes) {
            if (fileName.toLowerCase().endsWith(type)) { allowed = true; break; }
        }
        if (!allowed) { response.sendError(400, "File type not allowed"); return; }

        // 先在 pdf 目录找，再在 file 目录找
        File file = new File(FileConstant.FILE_SAVE_DIR + "/pdf/" + fileName);
        if (!file.exists()) {
            file = new File(FileConstant.FILE_SAVE_DIR + "/file/" + fileName);
        }
        if (!file.exists()) {
            response.sendError(404, "File not found: " + fileName);
            return;
        }
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setContentLengthLong(file.length());
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
        }
    }
}
