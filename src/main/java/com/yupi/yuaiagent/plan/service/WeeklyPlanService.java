package com.yupi.yuaiagent.plan.service;

import com.yupi.yuaiagent.plan.file.FileSystemManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 周计划服务
 * 负责：
 * 1. 接收用户的自然语言周计划描述
 * 2. 调用AI整理成结构化的markdown文件
 * 3. 保存到文件系统
 */
@Component
@Slf4j
public class WeeklyPlanService {
    
    private final FileSystemManager fileSystemManager;
    private final ChatClient chatClient;
    
    public WeeklyPlanService(FileSystemManager fileSystemManager, ChatModel dashscopeChatModel) {
        this.fileSystemManager = fileSystemManager;
        this.chatClient = ChatClient.builder(dashscopeChatModel).build();
    }
    
    /**
     * 创建周计划
     * 用户输入自然语言描述 → AI整理 → 保存为markdown文件
     */
    public Map<String, Object> createWeeklyPlan(String weekDescription, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("开始创建周计划");
            
            // 步骤1：调用AI整理周计划
            String markdownContent = generateMarkdownPlan(weekDescription, startDate, endDate);
            
            // 步骤2：生成文件名
            String filename = generateFilename(startDate, endDate);
            
            // 步骤3：保存到文件系统
            boolean saved = fileSystemManager.writeWeeklyPlan(filename, markdownContent);
            
            if (!saved) {
                result.put("success", false);
                result.put("message", "❌ 保存周计划失败");
                return result;
            }
            
            result.put("success", true);
            result.put("message", "✅ 周计划已创建并保存");
            result.put("filename", filename);
            result.put("weeklyPlan", markdownContent);
            
            log.info("✅ 周计划已创建：{}", filename);
            
            return result;
        } catch (Exception e) {
            log.error("创建周计划失败", e);
            result.put("success", false);
            result.put("message", "❌ 创建周计划失败：" + e.getMessage());
            return result;
        }
    }
    
    /**
     * 调用AI生成markdown格式的周计划
     */
    private String generateMarkdownPlan(String weekDescription, LocalDate startDate, LocalDate endDate) {
        String prompt = buildWeeklyPlanPrompt(weekDescription, startDate, endDate);
        
        String response = chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
        
        log.info("AI已生成周计划markdown");
        return response;
    }
    
    /**
     * 构建周计划生成提示词
     */
    private String buildWeeklyPlanPrompt(String weekDescription, LocalDate startDate, LocalDate endDate) {
        return """
                你是一个考研学习规划专家。用户提供了他们这周的学习计划描述，请你整理成一个结构化的markdown文件。
                
                用户的周计划描述：
                %s
                
                周期：%s 到 %s
                
                请按照以下格式生成markdown文件（不要包含代码块标记，直接输出markdown内容）：
                
                # 第X周（YYYY.MM.DD-YYYY.MM.DD）周计划
                
                ## 本周核心目标
                [总结用户的核心目标]
                
                ## 各科复习任务
                
                ### 📐 数学（每日X小时）
                1. [具体任务1]
                2. [具体任务2]
                3. [具体任务3]
                
                ### 📝 英语（每日X小时）
                1. [具体任务1]
                2. [具体任务2]
                
                ### 💻 专业课（每日X小时）
                1. [具体任务1]
                2. [具体任务2]
                
                [如果有其他科目，继续添加]
                
                ## 周进度追踪表
                | 科目 | 任务 | 完成情况 |
                |------|------|----------|
                | 数学 | [任务] | ⬜️ 未完成 |
                | 英语 | [任务] | ⬜️ 未完成 |
                | 专业课 | [任务] | ⬜️ 未完成 |
                
                ## 每日学习时间分配
                - 总学习时间：X小时/天
                - 数学：X小时/天
                - 英语：X小时/天
                - 专业课：X小时/天
                
                ## 学习建议
                1. [建议1]
                2. [建议2]
                3. [建议3]
                
                要求：
                1. 根据用户的描述提取关键信息
                2. 合理分配每日学习时间
                3. 明确每个科目的具体任务
                4. 保持格式整洁清晰
                5. 使用emoji使内容更易读
                """.formatted(weekDescription, startDate, endDate);
    }
    
    /**
     * 生成文件名
     */
    private String generateFilename(LocalDate startDate, LocalDate endDate) {
        // 计算是第几周
        int weekNumber = getWeekNumber(startDate);
        return String.format("第%d周（%s-%s）周计划.md", 
                weekNumber, 
                startDate, 
                endDate);
    }
    
    /**
     * 获取周数
     */
    private int getWeekNumber(LocalDate date) {
        // 简单计算：假设从3月10日开始为第一周
        LocalDate baseDate = LocalDate.of(2026, 3, 10);
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(baseDate, date);
        return (int) (daysDiff / 7) + 1;
    }
}
