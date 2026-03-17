package com.yupi.yuaiagent.plan.agent;

import com.yupi.yuaiagent.advisor.MyLoggerAdvisor;
import com.yupi.yuaiagent.agent.ToolCallAgent;
import com.yupi.yuaiagent.plan.adjuster.DynamicPlanAdjuster;
import com.yupi.yuaiagent.plan.manager.StudyStateManager;
import com.yupi.yuaiagent.plan.model.DailyPlan;
import com.yupi.yuaiagent.plan.model.StudyTask;
import com.yupi.yuaiagent.plan.model.WeeklyPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 考研学习规划智能体
 * 实现动态规划、状态管理、闭环反馈
 */
@Component
@Slf4j
public class StudyPlanAgent extends ToolCallAgent {
    
    private final StudyStateManager stateManager;
    private final DynamicPlanAdjuster planAdjuster;
    
    public StudyPlanAgent(ToolCallback[] allTools, 
                         ChatModel dashscopeChatModel,
                         StudyStateManager stateManager,
                         DynamicPlanAdjuster planAdjuster) {
        super(allTools);
        this.stateManager = stateManager;
        this.planAdjuster = planAdjuster;
        
        this.setName("StudyPlanAgent");
        
        String SYSTEM_PROMPT = """
                你是一个专业的考研学习规划智能体，具备以下核心能力：
                
                1. **动态规划**：根据学生的学习进度自动调整学习计划
                2. **状态管理**：记录和跟踪每日学习进度和任务完成情况
                3. **闭环反馈**：规划→执行→检查→调整的完整循环
                4. **智能顺延**：未完成任务自动加入后续计划
                
                你的工作流程：
                - 早上：读取周计划 → 读取昨日进度 → 制定今日计划 → 发送预览
                - 晚上：提醒上报 → 记录进度 → 调整明日计划
                - 周末：汇总周进度 → 分析完成情况 → 制定下周方案
                
                核心原则：
                - 绝不私自添加或修改任务，所有任务必须来自周计划
                - 根据昨天完成情况动态调整今日任务量
                - 实事求是：如果昨天没完成，今天不增加新任务
                - 昨天完成好 → 按原计划推进
                - 昨天完成差 → 减少任务量或重复昨天内容
                - 昨天没完成 → 先补昨天，再考虑新任务
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        
        String NEXT_STEP_PROMPT = """
                根据用户需求，选择合适的工具来完成任务。
                你可以使用文件操作工具来读取周计划和进度记录。
                你可以使用PDF生成工具来生成学习报告。
                如果任务完成，使用 terminate 工具结束。
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        
        this.setMaxSteps(20);
        
        // 初始化 AI 对话客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }
    
    /**
     * 早上流程：制定今日计划
     */
    public String morningRoutine(LocalDate today) {
        log.info("开始早上流程：{}", today);
        
        StringBuilder result = new StringBuilder();
        result.append("🌅 早上好！开始制定今日学习计划\n\n");
        
        // 步骤1：读取周计划
        WeeklyPlan weeklyPlan = stateManager.getCurrentWeeklyPlan();
        if (weeklyPlan == null) {
            result.append("❌ 错误：未找到周计划，请先创建周计划\n");
            return result.toString();
        }
        result.append("✅ 步骤1：已读取周计划 - ").append(weeklyPlan.getWeekName()).append("\n");
        
        // 步骤2：读取昨日进度
        DailyPlan yesterdayPlan = stateManager.getYesterdayProgress(today);
        if (yesterdayPlan != null) {
            result.append("✅ 步骤2：已读取昨日进度\n");
            result.append(String.format("   - 昨日完成度：%d%%\n", yesterdayPlan.getCompletionRate()));
            result.append(String.format("   - 学习时长：%.1f小时\n", yesterdayPlan.getActualTotalHours()));
            result.append(String.format("   - 状态评价：%s\n", yesterdayPlan.getStatusEvaluation()));
        } else {
            result.append("ℹ️ 步骤2：没有昨日进度记录\n");
        }
        
        // 步骤3：制定今日计划
        result.append("\n📋 步骤3：制定今日计划\n");
        
        // 从周计划中获取今日的原始计划
        DailyPlan originalPlan = findDailyPlanFromWeekly(weeklyPlan, today);
        if (originalPlan == null) {
            result.append("❌ 错误：周计划中未找到今日计划\n");
            return result.toString();
        }
        
        // 根据昨日进度调整今日计划
        DailyPlan adjustedPlan = planAdjuster.adjustTodayPlan(originalPlan, yesterdayPlan);
        stateManager.initializeDailyPlan(adjustedPlan);
        
        // 步骤4：发送计划预览
        result.append("\n📝 步骤4：今日计划预览\n");
        result.append("================\n");
        result.append(formatDailyPlan(adjustedPlan));
        
        return result.toString();
    }
    
    /**
     * 晚上流程：记录进度并调整明日计划（改进版：使用AI生成建议）
     */
    public String eveningRoutine(LocalDate today, double studyHours, String statusEvaluation, String problems) {
        log.info("开始晚上流程：{}", today);
        
        StringBuilder result = new StringBuilder();
        result.append("🌙 晚上好！开始记录今日进度\n\n");
        
        DailyPlan todayPlan = stateManager.getCurrentDailyPlan();
        if (todayPlan == null) {
            result.append("❌ 错误：未找到今日计划\n");
            return result.toString();
        }
        
        // 步骤1：记录进度
        result.append("✅ 步骤1：记录今日进度\n");
        result.append(String.format("   - 学习时长：%.1f小时\n", studyHours));
        result.append(String.format("   - 完成度：%d%%\n", todayPlan.getCompletionRate()));
        result.append(String.format("   - 状态评价：%s\n", statusEvaluation));
        
        // 步骤2：使用AI生成个性化建议（关键改进！）
        result.append("\n💡 步骤2：AI生成的个性化建议\n");
        String aiPrompt = planAdjuster.generatePromptForAI(todayPlan);
        String aiSuggestions = getChatClient().prompt()
                .user(aiPrompt)
                .call()
                .content();
        result.append(aiSuggestions).append("\n");
        
        // 将AI建议转换为列表（简单处理）
        List<String> suggestions = Arrays.asList(aiSuggestions.split("\n"));
        
        // 记录进度
        stateManager.recordDailyProgress(today, statusEvaluation, problems, suggestions);
        
        // 步骤3：预览明日计划调整
        result.append("\n📅 步骤3：明日计划将根据今日完成情况动态调整\n");
        if (todayPlan.getCompletionRate() >= 80) {
            result.append("   ✅ 今日完成度良好，明日按原计划推进\n");
        } else if (todayPlan.getCompletionRate() >= 50) {
            result.append("   ⚠️ 今日完成度一般，明日将减少任务量\n");
        } else {
            result.append("   ❌ 今日完成度较低，明日将优先补今日未完成任务\n");
        }
        
        return result.toString();
    }
    
    /**
     * 周总结流程（改进版：使用AI生成）
     */
    public String weeklySummary() {
        log.info("开始周总结流程");
        
        StringBuilder result = new StringBuilder();
        result.append("📊 本周学习总结\n");
        result.append("================\n\n");
        
        // 使用AI生成周总结（关键改进！）
        String aiPrompt = planAdjuster.generateWeeklyPromptForAI();
        String aiSummary = getChatClient().prompt()
                .user(aiPrompt)
                .call()
                .content();
        
        result.append(aiSummary);
        
        return result.toString();
    }
    
    /**
     * 从周计划中查找指定日期的日计划
     */
    private DailyPlan findDailyPlanFromWeekly(WeeklyPlan weeklyPlan, LocalDate date) {
        if (weeklyPlan.getDailyPlans() == null) {
            return null;
        }
        
        return weeklyPlan.getDailyPlans().stream()
                .filter(plan -> plan.getPlanDate().equals(date))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 格式化日计划输出
     */
    private String formatDailyPlan(DailyPlan plan) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("日期：%s\n", plan.getPlanDate()));
        sb.append(String.format("计划学习时长：%.1f小时\n\n", plan.getPlannedTotalHours()));
        
        // 按科目分组显示任务
        Map<String, List<StudyTask>> tasksBySubject = plan.getTasks().stream()
                .collect(java.util.stream.Collectors.groupingBy(StudyTask::getSubject));
        
        tasksBySubject.forEach((subject, tasks) -> {
            sb.append(String.format("📚 %s\n", subject));
            for (StudyTask task : tasks) {
                sb.append(String.format("   - %s (%.1f小时)", task.getTaskName(), task.getPlannedHours()));
                if (task.isPostponed()) {
                    sb.append(" [顺延]");
                }
                sb.append("\n");
            }
            sb.append("\n");
        });
        
        return sb.toString();
    }
}
