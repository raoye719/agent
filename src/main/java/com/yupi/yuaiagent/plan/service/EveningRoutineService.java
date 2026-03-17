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
            
            // 步骤2：调用AI处理整个晚上流程
            String updatedContent = processWithAI(today, planContent, progressReport);
            
            // 步骤3：保存更新后的文件
            boolean saved = fileSystemManager.writeDailyPlanAndProgress(today, updatedContent);
            
            if (!saved) {
                result.put("success", false);
                result.put("message", "❌ 保存文件失败");
                return result;
            }
            
            result.put("success", true);
            result.put("message", "✅ 晚上流程完成，进度已记录");
            result.put("date", today);
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
    private String processWithAI(LocalDate today, String planContent, String progressReport) {
        String prompt = buildEveningPrompt(today, planContent, progressReport);
        
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
    private String buildEveningPrompt(LocalDate today, String planContent, String progressReport) {
        return """
                你是一个考研学习规划助手。现在是晚上22:00，需要处理学生的学习进度。
                
                ## 任务
                根据今天的计划和学生的进度报告，更新计划文件中的完成情况部分。
                
                ## 今天的计划文件内容
                %s
                
                ## 学生的进度报告
                %s
                
                ## 你需要做的事
                1. 理解学生的进度报告，提取各科完成度、学习时长、状态等信息
                2. 更新文件中的"## ✅ 今日完成情况"部分，填入具体的完成情况
                3. 更新文件中的"## 📝 状态记录"部分，填入学习时长、状态评价、遇到的问题等
                4. 根据今天的完成情况生成"## 🎯 明日建议"部分
                
                ## 输出格式
                直接输出完整的更新后的文件内容，保持原有的格式和结构。
                
                ## 完成情况部分的格式示例
                ## ✅ 今日完成情况
                
                ### 📐 数学（完成情况：80%%）
                - 继续学习张宇30讲第一讲「函数极限连续」剩余内容 ✅
                - 完成660题「函数极限连续」模块15道选择题 ✅
                - 巩固昨天复习的不等式、三角函数等基础公式 ✅
                
                ### 📝 英语（完成情况：100%%）
                - 背诵100个考研核心词汇 ✅
                - 看1节颉斌斌语法课程 ✅
                
                ### 💻 408数据结构（完成情况：70%%）
                - 学习数据结构1.2-2.2章节 ✅
                - 完成对应章节的选择题 ✅
                
                ## 状态记录部分的格式示例
                ## 📝 状态记录
                - 学习时长：6小时
                - 总体完成度：83%%
                - 状态评价：良好
                - 遇到的问题：数学有些难
                - 其他备注：英语单词少背了20个
                
                ## 明日建议部分的格式示例
                ## 🎯 明日建议
                ✅ 今日完成度优秀（83%%），明日按原计划推进
                💡 可以考虑增加难度或深度学习
                👍 保持当前学习节奏和方法
                """.formatted(planContent, progressReport);
    }
}
