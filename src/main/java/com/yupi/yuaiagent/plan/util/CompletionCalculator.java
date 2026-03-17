package com.yupi.yuaiagent.plan.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 完成度计算工具
 * 负责从进度报告中提取各科完成度，并计算总体完成度
 */
@Slf4j
public class CompletionCalculator {
    
    /**
     * 完成度数据类
     */
    @Data
    public static class CompletionData {
        private int mathCompletion;           // 数学完成度 0-100
        private int englishCompletion;        // 英语完成度 0-100
        private int professionalCompletion;   // 专业课完成度 0-100
        private int otherCompletion;          // 其他完成度 0-100
        private double studyHours;            // 学习时长（小时）
        private String statusEvaluation;      // 状态评价：优秀/良好/一般/较差
        private String problems;              // 遇到的问题
        private String notes;                 // 其他备注
        
        /**
         * 计算总体完成度（平均值）
         */
        public int calculateAverageCompletion() {
            int sum = 0;
            int count = 0;
            
            if (mathCompletion > 0) {
                sum += mathCompletion;
                count++;
            }
            if (englishCompletion > 0) {
                sum += englishCompletion;
                count++;
            }
            if (professionalCompletion > 0) {
                sum += professionalCompletion;
                count++;
            }
            if (otherCompletion > 0) {
                sum += otherCompletion;
                count++;
            }
            
            return count > 0 ? sum / count : 0;
        }
        
        /**
         * 计算加权完成度
         * 权重：数学40% + 英语30% + 专业课30%
         */
        public double calculateWeightedCompletion() {
            double weighted = 0;
            
            if (mathCompletion > 0) {
                weighted += mathCompletion * 0.4;
            }
            if (englishCompletion > 0) {
                weighted += englishCompletion * 0.3;
            }
            if (professionalCompletion > 0) {
                weighted += professionalCompletion * 0.3;
            }
            
            return Math.round(weighted * 10.0) / 10.0;  // 保留一位小数
        }
    }
    
    /**
     * 从进度报告中解析完成度数据
     * 支持多种格式的输入
     */
    public static CompletionData parseProgressReport(String progressReport) {
        CompletionData data = new CompletionData();
        
        if (progressReport == null || progressReport.isEmpty()) {
            return data;
        }
        
        log.info("开始解析进度报告");
        
        // 提取学习时长
        data.studyHours = extractStudyHours(progressReport);
        
        // 提取各科完成度
        data.mathCompletion = extractCompletion(progressReport, "数学|math");
        data.englishCompletion = extractCompletion(progressReport, "英语|english");
        data.professionalCompletion = extractCompletion(progressReport, "408|专业课|professional");
        data.otherCompletion = extractCompletion(progressReport, "其他|other");
        
        // 提取状态评价
        data.statusEvaluation = extractStatusEvaluation(progressReport);
        
        // 提取问题
        data.problems = extractProblems(progressReport);
        
        // 提取备注
        data.notes = extractNotes(progressReport);
        
        log.info("解析完成：数学{}% 英语{}% 专业课{}% 平均{}%",
                data.mathCompletion, data.englishCompletion, 
                data.professionalCompletion, data.calculateAverageCompletion());
        
        return data;
    }
    
    /**
     * 提取学习时长
     * 支持格式：学了6小时、6h、6小时等
     */
    private static double extractStudyHours(String text) {
        Pattern pattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:小时|h|hour)");
        Matcher matcher = pattern.matcher(text);
        
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
     * 提取完成度百分比
     * 支持格式：数学完成了80%、数学80%、数学：80%等
     */
    private static int extractCompletion(String text, String keyword) {
        // 构建正则表达式：关键词后面跟着百分比
        Pattern pattern = Pattern.compile(keyword + "[^0-9]*?(\\d+)\\s*%");
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            try {
                int completion = Integer.parseInt(matcher.group(1));
                return Math.min(100, Math.max(0, completion));  // 限制在0-100之间
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    /**
     * 提取状态评价
     * 支持：优秀、良好、一般、较差
     */
    private static String extractStatusEvaluation(String text) {
        String[] evaluations = {"优秀", "良好", "一般", "较差"};
        
        for (String eval : evaluations) {
            if (text.contains(eval)) {
                return eval;
            }
        }
        
        // 如果没有找到，尝试从"状态"关键词后面提取
        Pattern pattern = Pattern.compile("状态[：:](\\S+)");
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        return "未知";
    }
    
    /**
     * 提取遇到的问题
     */
    private static String extractProblems(String text) {
        // 查找"问题"、"困难"、"难"等关键词后面的内容
        Pattern pattern = Pattern.compile("(?:问题|困难|难|遇到)[：:，,]?([^，。\n]+)");
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        return "";
    }
    
    /**
     * 提取其他备注
     */
    private static String extractNotes(String text) {
        // 查找"备注"、"其他"等关键词后面的内容
        Pattern pattern = Pattern.compile("(?:备注|其他|说明)[：:，,]?([^，。\n]+)");
        Matcher matcher = pattern.matcher(text);
        
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        return "";
    }
    
    /**
     * 根据完成度判断调整策略
     */
    public static String determineAdjustmentStrategy(int averageCompletion, String statusEvaluation) {
        if (averageCompletion >= 80) {
            return "按原计划推进";
        } else if (averageCompletion >= 50) {
            return "减少20%任务量";
        } else {
            return "重复昨天未完成内容";
        }
    }
    
    /**
     * 生成完成度分析摘要
     */
    public static String generateCompletionSummary(CompletionData data) {
        StringBuilder summary = new StringBuilder();
        
        int avgCompletion = data.calculateAverageCompletion();
        double weightedCompletion = data.calculateWeightedCompletion();
        
        summary.append("## 📊 完成度分析\n\n");
        summary.append("### 各科完成度\n");
        summary.append("- 📐 数学：").append(data.mathCompletion).append("%\n");
        summary.append("- 📝 英语：").append(data.englishCompletion).append("%\n");
        summary.append("- 💻 专业课：").append(data.professionalCompletion).append("%\n");
        
        if (data.otherCompletion > 0) {
            summary.append("- 📚 其他：").append(data.otherCompletion).append("%\n");
        }
        
        summary.append("\n### 总体完成度\n");
        summary.append("- 平均完成度：").append(avgCompletion).append("%\n");
        summary.append("- 加权完成度：").append(weightedCompletion).append("%\n");
        summary.append("- 学习时长：").append(data.studyHours).append("小时\n");
        summary.append("- 状态评价：").append(data.statusEvaluation).append("\n");
        
        return summary.toString();
    }
}
