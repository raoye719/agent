package com.yupi.yuaiagent.plan.generator;

import com.yupi.yuaiagent.plan.file.FileSystemManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 计划生成模块
 * 负责：
 * 1. 解析周计划文件
 * 2. 分析昨日进度
 * 3. 生成今日计划（带动态调整）
 */
@Component
@Slf4j
public class PlanGenerator {
    
    private final FileSystemManager fileSystemManager;
    private final ChatClient chatClient;
    
    public PlanGenerator(FileSystemManager fileSystemManager, ChatModel dashscopeChatModel) {
        this.fileSystemManager = fileSystemManager;
        this.chatClient = ChatClient.builder(dashscopeChatModel).build();
    }
    
    /**
     * 生成今日计划
     * 核心流程：
     * 1. 读取周计划
     * 2. 读取昨日进度
     * 3. 分析昨日完成度
     * 4. 根据完成度动态调整
     * 5. 生成今日计划
     */
    public String generateTodayPlan(LocalDate today) {
        log.info("开始生成今日计划：{}", today);
        
        // 步骤1：读取周计划
        String weeklyPlan = fileSystemManager.readWeeklyPlan(today);
        if (weeklyPlan == null) {
            return "❌ 错误：未找到周计划文件，请先创建周计划";
        }
        log.info("✅ 已读取周计划");
        
        // 步骤2：读取昨日进度
        String yesterdayProgress = fileSystemManager.readYesterdayProgress(today);
        
        // 步骤3：分析昨日完成度
        Map<String, Object> yesterdayAnalysis = analyzeYesterdayProgress(yesterdayProgress);
        int completionRate = (int) yesterdayAnalysis.getOrDefault("completionRate", 0);
        String statusEvaluation = (String) yesterdayAnalysis.getOrDefault("statusEvaluation", "");
        
        log.info("昨日完成度：{}%，状态：{}", completionRate, statusEvaluation);
        
        // 步骤4：根据完成度动态调整
        String adjustmentStrategy = determineAdjustmentStrategy(completionRate, statusEvaluation);
        
        // 步骤5：调用AI生成今日计划
        String todayPlan = generatePlanWithAI(weeklyPlan, yesterdayProgress, adjustmentStrategy, today);
        
        return todayPlan;
    }
    
    /**
     * 分析昨日进度
     * 从进度记录文件中提取完成度和状态
     */
    private Map<String, Object> analyzeYesterdayProgress(String yesterdayProgress) {
        Map<String, Object> analysis = new HashMap<>();
        
        if (yesterdayProgress == null) {
            analysis.put("completionRate", 0);
            analysis.put("statusEvaluation", "无");
            analysis.put("hasYesterdayData", false);
            return analysis;
        }
        
        analysis.put("hasYesterdayData", true);
        
        // 提取完成度（简单的正则匹配）
        int completionRate = extractCompletionRate(yesterdayProgress);
        analysis.put("completionRate", completionRate);
        
        // 提取状态评价
        String statusEvaluation = extractStatusEvaluation(yesterdayProgress);
        analysis.put("statusEvaluation", statusEvaluation);
        
        return analysis;
    }
    
    /**
     * 从进度记录中提取完成度
     */
    private int extractCompletionRate(String progressRecord) {
        // 查找 "完成度：XX%" 或 "完成情况：XX%"
        String[] patterns = {"完成度：(\\d+)%", "完成情况：(\\d+)%"};
        
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(progressRecord);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        }
        
        // 如果没有找到，尝试计算平均完成度
        return calculateAverageCompletion(progressRecord);
    }
    
    /**
     * 计算平均完成度
     */
    private int calculateAverageCompletion(String progressRecord) {
        // 查找所有的百分比
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)%");
        java.util.regex.Matcher matcher = pattern.matcher(progressRecord);
        
        int sum = 0;
        int count = 0;
        while (matcher.find()) {
            sum += Integer.parseInt(matcher.group(1));
            count++;
        }
        
        return count > 0 ? sum / count : 0;
    }
    
    /**
     * 从进度记录中提取状态评价
     */
    private String extractStatusEvaluation(String progressRecord) {
        // 查找 "状态评价：XX" 或 "状态：XX"
        String[] patterns = {"状态评价：([^\\n]+)", "状态：([^\\n]+)", "今日状态：([^\\n]+)"};
        
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(progressRecord);
            if (m.find()) {
                return m.group(1).trim();
            }
        }
        
        return "未知";
    }
    
    /**
     * 确定调整策略
     * 核心逻辑：
     * - 完成度 > 80%：按原计划推进
     * - 完成度 50-80%：减少20%任务量
     * - 完成度 < 50%：重复昨天未完成内容
     */
    private String determineAdjustmentStrategy(int completionRate, String statusEvaluation) {
        if (completionRate >= 80) {
            return "按原计划推进";
        } else if (completionRate >= 50) {
            return "减少20%任务量";
        } else {
            return "重复昨天未完成内容";
        }
    }
    
    /**
     * 调用AI生成今日计划
     */
    private String generatePlanWithAI(String weeklyPlan, String yesterdayProgress, 
                                      String adjustmentStrategy, LocalDate today) {
        String prompt = buildPrompt(weeklyPlan, yesterdayProgress, adjustmentStrategy, today);
        
        String response = chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
        
        log.info("AI已生成今日计划");
        return response;
    }
    
    /**
     * 构建提示词
     */
    private String buildPrompt(String weeklyPlan, String yesterdayProgress, 
                               String adjustmentStrategy, LocalDate today) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一个考研学习规划助手。\n\n");
        
        prompt.append("我检查了周计划和昨天进度，这是今天的计划预览\n\n");
        
        prompt.append("## 周计划\n");
        prompt.append(weeklyPlan).append("\n\n");
        
        if (yesterdayProgress != null) {
            prompt.append("## 昨日进度\n");
            prompt.append(yesterdayProgress).append("\n\n");
        }
        
        prompt.append("## 调整策略\n");
        prompt.append(adjustmentStrategy).append("\n\n");
        
        prompt.append("## 任务\n");
        prompt.append("基于上述周计划、昨日进度和调整策略，为").append(today).append("生成今日学习计划。\n\n");
        
        prompt.append("要求：\n");
        prompt.append("1. 所有任务必须来自周计划，不能私自添加或修改\n");
        prompt.append("2. 根据调整策略动态调整任务量\n");
        prompt.append("3. 按科目分类显示任务\n");
        prompt.append("4. 标注每个任务的预计时长\n");
        prompt.append("5. 提供学习建议\n\n");
        
        prompt.append("输出格式：\n");
        prompt.append("📅 今日学习计划（").append(today).append("）\n");
        prompt.append("================\n\n");
        prompt.append("📐 数学（X小时）\n");
        prompt.append("- 任务1\n");
        prompt.append("- 任务2\n\n");
        prompt.append("📝 英语（X小时）\n");
        prompt.append("- 任务1\n");
        prompt.append("- 任务2\n\n");
        prompt.append("💻 专业课（X小时）\n");
        prompt.append("- 任务1\n");
        prompt.append("- 任务2\n\n");
        prompt.append("💡 学习建议\n");
        prompt.append("- 建议1\n");
        prompt.append("- 建议2\n");
        
        return prompt.toString();
    }
}
