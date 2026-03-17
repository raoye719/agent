package com.yupi.yuaiagent.plan.service;

import com.yupi.yuaiagent.plan.file.FileSystemManager;
import com.yupi.yuaiagent.plan.util.CompletionCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 晚上流程服务
 * 完全由AI驱动，AI负责：
 * 1. 读取当天的计划文件
 * 2. 理解进度报告
 * 3. 组织完成情况
 * 4. 生成建议
 * 5. 更新文件
 */
@Component
@Slf4j
public class EveningRoutineService {
    
    private final FileSystemManager fileSystemManager;
    private final ChatClient chatClient;
    
    public EveningRoutineService(FileSystemManager fileSystemManager, ChatModel dashscopeChatModel) {
        this.fileSystemManager = fileSystemManager;
        this.chatClient = ChatClient.builder(dashscopeChatModel).build();
    }
    
    /**
     * 处理晚上流程
     * 用户只需提供进度报告，AI自动处理一切
     */
    public Map<String, Object> processEveningRoutine(LocalDate today, String progressReport) {
        log.info("开始处理晚上流程：{}", today);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 步骤1：读取当天的计划文件
            String planContent = fileSystemManager.readDailyPlanAndProgress(today);
            
            if (planContent == null) {
                result.put("success", false);
                result.put("message", "❌ 未找到今天的计划文件，请先生成今日计划");
                return result;
            }
            
            // 步骤2：解析进度报告，计算完成度
            CompletionCalculator.CompletionData completionData = CompletionCalculator.parseProgressReport(progressReport);
            int averageCompletion = completionData.calculateAverageCompletion();
            double weightedCompletion = completionData.calculateWeightedCompletion();
            
            log.info("完成度计算：平均{}% 加权{}%", averageCompletion, weightedCompletion);
            
            // 步骤3：调用AI处理整个晚上流程
            String updatedContent = processWithAI(today, planContent, progressReport, completionData);
            
            // 步骤4：保存更新后的文件
            boolean saved = fileSystemManager.writeDailyPlanAndProgress(today, updatedContent);
            
            if (!saved) {
                result.put("success", false);
                result.put("message", "❌ 保存文件失败");
                return result;
            }
            
            result.put("success", true);
            result.put("message", "✅ 晚上流程完成，进度已记录");
            result.put("date", today);
            result.put("averageCompletion", averageCompletion);
            result.put("weightedCompletion", weightedCompletion);
            result.put("completionData", completionData);
            result.put("updatedContent", updatedContent);
            
            log.info("✅ 晚上流程处理完成");
            
            return result;
        } catch (Exception e) {
            log.error("晚上流程处理失败", e);
            result.put("success", false);
            result.put("message", "❌ 处理失败：" + e.getMessage());
            return result;
        }
    }
    
    /**
     * 调用AI处理晚上流程
     */
    private String processWithAI(LocalDate today, String planContent, String progressReport, 
                                CompletionCalculator.CompletionData completionData) {
        String prompt = buildEveningPrompt(today, planContent, progressReport, completionData);
        
        String response = chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
        
        log.info("AI已处理晚上流程");
        return response;
    }
    
    /**
     * 构建晚上流程提示词
     */
    private String buildEveningPrompt(LocalDate today, String planContent, String progressReport,
                                     CompletionCalculator.CompletionData completionData) {
        int avgCompletion = completionData.calculateAverageCompletion();
        double weightedCompletion = completionData.calculateWeightedCompletion();
        String adjustmentStrategy = CompletionCalculator.determineAdjustmentStrategy(avgCompletion, completionData.getStatusEvaluation());
        
        return """
                你是一个考研学习规划助手。现在是晚上22:00，需要处理学生的学习进度。
                
                ## 今天的计划文件内容
                %s
                
                ## 学生的进度报告
                %s
                
                ## 系统计算的完成度数据
                - 数学完成度：%d%%
                - 英语完成度：%d%%
                - 专业课完成度：%d%%
                - 平均完成度：%d%%
                - 加权完成度：%.1f%%（权重：数学40%% + 英语30%% + 专业课30%%）
                - 学习时长：%.1f小时
                - 状态评价：%s
                - 调整策略：%s
                
                ## 重要提示（必读）
                1. 仔细阅读学生的进度报告
                2. 如果学生提到以下情况，在状态记录中记录：
                   - \"明天要出去玩\" → 记录为\"用户明天要出去玩，不布置任务\"
                   - \"明天有事\" → 记录为\"用户明天有事，建议休息\"
                   - \"明天休息\" → 记录为\"用户明天休息\"
                3. 如果学生说\"今天有事\"、\"没学\"、\"出去玩了\"等，则：
                   - 在完成情况中标记为\"今天有事，作废\"或\"今天休息，无任务\"
                   - 在明日建议中说\"今天休息了，明天按原计划继续\"
                4. 根据完成度生成明日建议：
                   - 完成度>80%：按原计划推进
                   - 完成度50-80%：减少20%任务量
                   - 完成度<50%：重复昨天未完成内容
                
                ## 你需要做的事
                1. 根据系统计算的完成度数据，更新文件中的\"## ✅ 今日完成情况\"部分
                2. 填入各科的具体完成情况和任务完成情况
                3. 更新文件中的\"## 📝 状态记录\"部分，填入学习时长、状态评价、遇到的问题等
                4. 根据完成度和调整策略生成\"## 🎯 明日建议\"部分
                
                ## 输出格式
                直接输出完整的更新后的文件内容，保持原有的格式和结构。
                
                ## 完成情况部分的格式示例
                ## ✅ 今日完成情况
                
                ### 📐 数学（完成情况：%d%%）
                - 继续学习张宇30讲第一讲「函数极限连续」剩余内容 ✅
                - 完成660题「函数极限连续」模块15道选择题 ✅
                
                ### 📝 英语（完成情况：%d%%）
                - 背诵100个考研核心词汇 ✅
                - 看1节颉斌斌语法课程 ✅
                
                ### 💻 408数据结构（完成情况：%d%%）
                - 学习数据结构1.2-2.2章节 ✅
                - 完成对应章节的选择题 ✅
                
                ## 状态记录部分的格式示例
                ## 📝 状态记录
                - 学习时长：%.1f小时
                - 总体完成度：%d%%（加权：%.1f%%）
                - 状态评价：%s
                - 遇到的问题：%s
                - 其他备注：%s
                
                ## 明日建议部分的格式示例
                ## 🎯 明日建议
                ✅ 今日完成度%s（%d%%），%s
                💡 根据完成情况提供具体建议
                👍 鼓励和激励
                """.formatted(
                planContent, 
                progressReport,
                completionData.getMathCompletion(),
                completionData.getEnglishCompletion(),
                completionData.getProfessionalCompletion(),
                avgCompletion,
                weightedCompletion,
                completionData.getStudyHours(),
                completionData.getStatusEvaluation(),
                adjustmentStrategy,
                completionData.getMathCompletion(),
                completionData.getEnglishCompletion(),
                completionData.getProfessionalCompletion(),
                completionData.getStudyHours(),
                avgCompletion,
                weightedCompletion,
                completionData.getStatusEvaluation(),
                completionData.getProblems(),
                completionData.getNotes(),
                avgCompletion >= 80 ? "优秀" : (avgCompletion >= 50 ? "一般" : "较低"),
                avgCompletion,
                adjustmentStrategy
        );
    }
}
