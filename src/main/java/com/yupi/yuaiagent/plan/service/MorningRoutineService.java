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
 * 早上流程服务
 * 完全由AI驱动，AI负责：
 * 1. 读取周计划
 * 2. 读取昨天的完成情况
 * 3. 分析昨天的完成度
 * 4. 生成今天的计划（拆分周计划）
 * 5. 保存计划文件
 */
@Component
@Slf4j
public class MorningRoutineService {
    
    private final FileSystemManager fileSystemManager;
    private final ChatClient chatClient;
    
    public MorningRoutineService(FileSystemManager fileSystemManager, ChatModel dashscopeChatModel) {
        this.fileSystemManager = fileSystemManager;
        this.chatClient = ChatClient.builder(dashscopeChatModel).build();
    }
    
    /**
     * 处理早上流程
     * 自动生成今日计划并保存
     */
    public Map<String, Object> processMorningRoutine(LocalDate today) {
        log.info("开始处理早上流程：{}", today);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 步骤1：读取周计划
            String weeklyPlan = fileSystemManager.findWeeklyPlanByDate(today);
            
            if (weeklyPlan == null) {
                result.put("success", false);
                result.put("message", "❌ 未找到包含该日期的周计划，请先创建周计划");
                return result;
            }
            
            log.info("✅ 已找到周计划");
            
            // 步骤2：读取昨天的完成情况
            String yesterdayProgress = fileSystemManager.readDailyPlanAndProgress(today.minusDays(1));
            
            // 步骤3：调用AI生成今日计划
            String todayPlan = generateTodayPlanWithAI(today, weeklyPlan, yesterdayProgress);
            
            // 步骤4：保存计划文件
            boolean saved = fileSystemManager.writeDailyPlanAndProgress(today, todayPlan);
            
            if (!saved) {
                result.put("success", false);
                result.put("message", "❌ 保存计划文件失败");
                return result;
            }
            
            result.put("success", true);
            result.put("message", todayPlan);
            result.put("date", today);
            result.put("fileSaved", true);
            
            log.info("✅ 早上流程完成");
            
            return result;
        } catch (Exception e) {
            log.error("早上流程处理失败", e);
            result.put("success", false);
            result.put("message", "❌ 处理失败：" + e.getMessage());
            return result;
        }
    }
    
    /**
     * 调用AI生成今日计划
     */
    private String generateTodayPlanWithAI(LocalDate today, String weeklyPlan, String yesterdayProgress) {
        String prompt = buildMorningPrompt(today, weeklyPlan, yesterdayProgress);
        
        String response = chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
        
        log.info("AI已生成今日计划");
        return response;
    }
    
    /**
     * 构建早上流程提示词
     */
    private String buildMorningPrompt(LocalDate today, String weeklyPlan, String yesterdayProgress) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一个考研学习规划助手。现在是早上7:30，需要为学生生成今天的学习计划。\n\n");
        
        prompt.append("## 周计划\n");
        prompt.append(weeklyPlan).append("\n\n");
        
        if (yesterdayProgress != null && !yesterdayProgress.isEmpty()) {
            prompt.append("## 昨天的完成情况\n");
            prompt.append(yesterdayProgress).append("\n\n");
        }
        
        prompt.append("## 任务\n");
        prompt.append("根据周计划和昨天的完成情况，为").append(today).append("生成具体的今日学习计划。\n\n");
        
        prompt.append("## 要求\n");
        prompt.append("1. 将周计划拆分成7份（每天一份），今天是第").append(getDayOfWeek(today)).append("天\n");
        prompt.append("2. 根据昨天的完成度动态调整：\n");
        prompt.append("   - 如果昨天完成度>80%：按原计划推进\n");
        prompt.append("   - 如果昨天完成度50-80%：减少20%任务量\n");
        prompt.append("   - 如果昨天完成度<50%：重复昨天未完成内容\n");
        prompt.append("3. 每个科目的任务要具体、可执行\n");
        prompt.append("4. 标注每个任务的预计时长\n");
        prompt.append("5. 提供学习建议和技巧\n\n");
        
        prompt.append("## 输出格式\n");
        prompt.append("# ").append(today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy年M月d日")))
               .append(" 学习计划与完成情况\n\n");
        prompt.append("## 📅 今日学习计划\n");
        prompt.append("> 调整思路：[说明为什么这样调整]\n");
        prompt.append("> 周计划进度：[说明已完成的内容]\n\n");
        prompt.append("### 📐 数学（今日目标X小时）\n");
        prompt.append("1. [具体任务1] ✅\n");
        prompt.append("2. [具体任务2] ✅\n\n");
        prompt.append("### 📝 英语（今日目标X小时）\n");
        prompt.append("1. [具体任务1] ✅\n");
        prompt.append("2. [具体任务2] ✅\n\n");
        prompt.append("### 💻 专业课（今日目标X小时）\n");
        prompt.append("1. [具体任务1] ✅\n");
        prompt.append("2. [具体任务2] ✅\n\n");
        prompt.append("### 额外建议：\n");
        prompt.append("[提供学习技巧和时间管理建议]\n\n");
        prompt.append("## ✅ 今日完成情况\n");
        prompt.append("（晚上更新）\n\n");
        prompt.append("## 📝 状态记录\n");
        prompt.append("（晚上更新）\n");
        
        return prompt.toString();
    }
    
    /**
     * 获取周几（1-7）
     */
    private int getDayOfWeek(LocalDate date) {
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        int dayNum = dayOfWeek.getValue();
        return dayNum == 7 ? 7 : dayNum;
    }
}
