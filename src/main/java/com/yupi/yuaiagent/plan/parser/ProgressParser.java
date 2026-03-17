package com.yupi.yuaiagent.plan.parser;

import com.yupi.yuaiagent.plan.file.FileSystemManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 进度解析模块
 * 负责：
 * 1. 解析自然语言进度输入
 * 2. 计算完成度
 * 3. 生成进度记录文件
 * 4. 生成明日建议
 */
@Component
@Slf4j
public class ProgressParser {
    
    private final FileSystemManager fileSystemManager;
    private final ChatClient chatClient;
    
    public ProgressParser(FileSystemManager fileSystemManager, ChatModel dashscopeChatModel) {
        this.fileSystemManager = fileSystemManager;
        this.chatClient = ChatClient.builder(dashscopeChatModel).build();
    }
    
    /**
     * 处理进度上报
     * 核心流程：
     * 1. 解析自然语言进度
     * 2. 生成进度记录文件
     * 3. 生成明日建议
     */
    public Map<String, Object> processProgressReport(LocalDate today, String progressReport) {
        log.info("开始处理进度上报：{}", today);
        
        Map<String, Object> result = new HashMap<>();
        
        // 步骤1：解析进度
        Map<String, Object> parsedProgress = parseProgressReport(progressReport);
        result.put("parsedProgress", parsedProgress);
        
        // 步骤2：生成进度记录文件
        String progressRecord = generateProgressRecord(today, parsedProgress, progressReport);
        boolean saved = fileSystemManager.writeProgressRecord(today, progressRecord);
        result.put("recordSaved", saved);
        result.put("progressRecord", progressRecord);
        
        // 步骤3：生成明日建议
        String tomorrowSuggestions = generateTomorrowSuggestions(parsedProgress);
        result.put("tomorrowSuggestions", tomorrowSuggestions);
        
        log.info("✅ 进度已记录，明日建议已生成");
        
        return result;
    }
    
    /**
     * 解析自然语言进度报告
     * 调用AI提取结构化数据
     */
    private Map<String, Object> parseProgressReport(String progressReport) {
        String prompt = buildParsePrompt(progressReport);
        
        String response = chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
        
        // 解析AI的响应
        Map<String, Object> parsed = extractProgressData(response);
        
        log.info("已解析进度报告");
        return parsed;
    }
    
    /**
     * 构建解析提示词
     */
    private String buildParsePrompt(String progressReport) {
        return """
                请解析以下学习进度报告，提取结构化数据：
                
                进度报告：
                %s
                
                请提取以下信息（如果没有提到，用0或"未知"表示）：
                1. 总学习时长（小时）
                2. 数学完成度（百分比）
                3. 英语完成度（百分比）
                4. 专业课完成度（百分比）
                5. 其他科目完成度（百分比）
                6. 今日状态评价（优秀/良好/一般/较差）
                7. 遇到的问题
                8. 其他备注
                
                请按以下格式回复（每行一个数据）：
                总学习时长: X
                数学完成度: X%
                英语完成度: X%
                专业课完成度: X%
                其他完成度: X%
                状态评价: X
                问题: X
                备注: X
                """.formatted(progressReport);
    }
    
    /**
     * 从AI响应中提取进度数据
     */
    private Map<String, Object> extractProgressData(String response) {
        Map<String, Object> data = new HashMap<>();
        
        // 提取总学习时长
        double studyHours = extractDoubleValue(response, "总学习时长");
        data.put("studyHours", studyHours);
        
        // 提取各科完成度
        int mathCompletion = extractIntValue(response, "数学完成度");
        int englishCompletion = extractIntValue(response, "英语完成度");
        int professionalCompletion = extractIntValue(response, "专业课完成度");
        int otherCompletion = extractIntValue(response, "其他完成度");
        
        data.put("mathCompletion", mathCompletion);
        data.put("englishCompletion", englishCompletion);
        data.put("professionalCompletion", professionalCompletion);
        data.put("otherCompletion", otherCompletion);
        
        // 计算平均完成度
        int avgCompletion = calculateAverageCompletion(mathCompletion, englishCompletion, 
                                                       professionalCompletion, otherCompletion);
        data.put("averageCompletion", avgCompletion);
        
        // 提取状态评价
        String statusEvaluation = extractStringValue(response, "状态评价");
        data.put("statusEvaluation", statusEvaluation);
        
        // 提取问题
        String problems = extractStringValue(response, "问题");
        data.put("problems", problems);
        
        // 提取备注
        String notes = extractStringValue(response, "备注");
        data.put("notes", notes);
        
        return data;
    }
    
    /**
     * 提取数值
     */
    private double extractDoubleValue(String text, String key) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(key + ":\\s*([\\d.]+)");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    /**
     * 提取整数值
     */
    private int extractIntValue(String text, String key) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(key + ":\\s*(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    /**
     * 提取字符串值
     */
    private String extractStringValue(String text, String key) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(key + ":\\s*([^\\n]+)");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
    
    /**
     * 计算平均完成度
     */
    private int calculateAverageCompletion(int... completions) {
        int sum = 0;
        int count = 0;
        for (int completion : completions) {
            if (completion > 0) {
                sum += completion;
                count++;
            }
        }
        return count > 0 ? sum / count : 0;
    }
    
    /**
     * 生成进度记录文件内容
     */
    private String generateProgressRecord(LocalDate date, Map<String, Object> parsedProgress, 
                                         String originalReport) {
        StringBuilder record = new StringBuilder();
        
        record.append("# ").append(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
              .append(" 学习进度记录\n\n");
        
        record.append("## 📊 今日完成情况\n\n");
        
        // 数学
        record.append("### 📐 数学（完成情况：")
              .append(parsedProgress.get("mathCompletion")).append("%）\n");
        record.append("- 完成度：").append(parsedProgress.get("mathCompletion")).append("%\n\n");
        
        // 英语
        record.append("### 📝 英语（完成情况：")
              .append(parsedProgress.get("englishCompletion")).append("%）\n");
        record.append("- 完成度：").append(parsedProgress.get("englishCompletion")).append("%\n\n");
        
        // 专业课
        record.append("### 💻 专业课（完成情况：")
              .append(parsedProgress.get("professionalCompletion")).append("%）\n");
        record.append("- 完成度：").append(parsedProgress.get("professionalCompletion")).append("%\n\n");
        
        // 其他
        if ((int) parsedProgress.get("otherCompletion") > 0) {
            record.append("### 📚 其他（完成情况：")
                  .append(parsedProgress.get("otherCompletion")).append("%）\n");
            record.append("- 完成度：").append(parsedProgress.get("otherCompletion")).append("%\n\n");
        }
        
        record.append("## 📈 今日状态评价\n\n");
        record.append("- 学习时长：").append(parsedProgress.get("studyHours")).append("小时\n");
        record.append("- 总体完成度：").append(parsedProgress.get("averageCompletion")).append("%\n");
        record.append("- 状态评价：").append(parsedProgress.get("statusEvaluation")).append("\n");
        record.append("- 遇到的问题：").append(parsedProgress.get("problems")).append("\n");
        record.append("- 其他备注：").append(parsedProgress.get("notes")).append("\n\n");
        
        record.append("## 📝 原始报告\n\n");
        record.append(originalReport).append("\n");
        
        return record.toString();
    }
    
    /**
     * 生成明日建议
     */
    private String generateTomorrowSuggestions(Map<String, Object> parsedProgress) {
        int avgCompletion = (int) parsedProgress.get("averageCompletion");
        String statusEvaluation = (String) parsedProgress.get("statusEvaluation");
        
        StringBuilder suggestions = new StringBuilder();
        
        suggestions.append("## 🎯 明日建议\n\n");
        
        // 根据完成度生成建议
        if (avgCompletion >= 80) {
            suggestions.append("✅ 今日完成度优秀（").append(avgCompletion).append("%），明日按原计划推进\n");
            suggestions.append("💡 可以考虑增加难度或深度学习\n");
        } else if (avgCompletion >= 50) {
            suggestions.append("⚠️ 今日完成度一般（").append(avgCompletion).append("%），明日减少20%任务量\n");
            suggestions.append("🎯 重点关注未完成的任务，优先完成\n");
        } else {
            suggestions.append("❌ 今日完成度较低（").append(avgCompletion).append("%），明日优先补缺\n");
            suggestions.append("🔍 分析未完成原因，调整学习策略\n");
        }
        
        // 根据状态评价生成建议
        if ("优秀".equals(statusEvaluation) || "良好".equals(statusEvaluation)) {
            suggestions.append("👍 保持当前学习节奏和方法\n");
        } else if ("一般".equals(statusEvaluation)) {
            suggestions.append("⚠️ 检查是否有干扰因素，调整学习环境\n");
        } else if ("较差".equals(statusEvaluation)) {
            suggestions.append("❌ 需要重新评估学习方法，考虑调整计划\n");
        }
        
        return suggestions.toString();
    }
}
