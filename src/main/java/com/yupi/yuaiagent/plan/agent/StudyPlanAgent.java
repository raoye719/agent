package com.yupi.yuaiagent.plan.agent;

import com.yupi.yuaiagent.advisor.MyLoggerAdvisor;
import com.yupi.yuaiagent.plan.config.PlanContextBuilder;
import com.yupi.yuaiagent.plan.config.PlanPromptConfig;
import com.yupi.yuaiagent.plan.manager.StudyStateManager;
import com.yupi.yuaiagent.plan.model.DailyPlan;
import com.yupi.yuaiagent.plan.model.StudyTask;
import com.yupi.yuaiagent.plan.model.WeeklyPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 考研学习规划智能体（重构版）
 * 核心改进：
 * 1. 提示词统一管理（PlanPromptConfig）
 * 2. 上下文统一构建（PlanContextBuilder）
 * 3. 流程由 AI 驱动，代码只负责数据流转
 * 4. 不使用 Tool Calling，保持高效
 */
@Component
@Slf4j
public class StudyPlanAgent {

    private final ChatClient chatClient;
    private final StudyStateManager stateManager;
    private final PlanContextBuilder contextBuilder;

    public StudyPlanAgent(ChatModel dashscopeChatModel,
                         StudyStateManager stateManager,
                         PlanContextBuilder contextBuilder) {
        this.stateManager = stateManager;
        this.contextBuilder = contextBuilder;

        // 初始化 ChatClient，使用统一的系统角色
        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(PlanPromptConfig.SYSTEM_ROLE)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
    }

    /**
     * 早上流程：AI 根据周计划、昨日进度和学生状态，动态生成今日计划
     *
     * @param today 今天的日期
     * @param studentStatus 学生的状态描述（可选）
     * @return 今日计划
     */
    public String morningRoutine(LocalDate today, String studentStatus) {
        log.info("🌅 开始早上流程：{}", today);

        try {
            // 1. 获取数据
            WeeklyPlan weeklyPlan = stateManager.getCurrentWeeklyPlan();
            if (weeklyPlan == null) {
                log.error("未找到周计划");
                return "❌ 错误：未找到周计划，请先创建周计划";
            }

            DailyPlan yesterdayPlan = stateManager.getYesterdayProgress(today);

            // 2. 构建上下文
            String weeklyContext = contextBuilder.buildWeeklyPlanContext(weeklyPlan);
            String yesterdayContext = contextBuilder.buildYesterdayProgressContext(yesterdayPlan);
            String statusContext = contextBuilder.buildStudentStatusContext(studentStatus);

            // 3. 构建完整提示词
            String prompt = PlanPromptConfig.getMorningPrompt(weeklyContext, yesterdayContext, statusContext);

            // 4. 调用 AI 生成计划（一次调用）
            log.info("📋 调用 AI 生成今日计划...");
            String todayPlan = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 5. 解析并保存计划
            DailyPlan parsedPlan = parsePlanFromAI(todayPlan, today);
            stateManager.initializeDailyPlan(parsedPlan);

            log.info("✅ 早上流程完成");
            return todayPlan;

        } catch (Exception e) {
            log.error("早上流程执行失败", e);
            return "❌ 早上流程执行失败：" + e.getMessage();
        }
    }

    /**
     * 晚上流程：AI 根据今日完成情况和学生反馈，生成反馈和明日建议
     *
     * @param today 今天的日期
     * @param studentReport 学生的完成情况和反馈
     * @return 反馈和明日建议
     */
    public String eveningRoutine(LocalDate today, String studentReport) {
        log.info("🌙 开始晚上流程：{}", today);

        try {
            // 1. 获取数据
            DailyPlan todayPlan = stateManager.getCurrentDailyPlan();
            if (todayPlan == null) {
                log.error("未找到今日计划");
                return "❌ 错误：未找到今日计划";
            }

            Map<String, Object> weeklyStats = stateManager.getWeeklyStatistics();

            // 2. 构建上下文
            String todayPlanContext = contextBuilder.buildTodayPlanContext(todayPlan);
            String metricsContext = contextBuilder.buildCompletionMetricsContext(todayPlan, weeklyStats);
            String reportContext = contextBuilder.buildStudentReportContext(studentReport);

            // 3. 构建完整提示词
            String prompt = PlanPromptConfig.getEveningPrompt(todayPlanContext, metricsContext, reportContext);

            // 4. 调用 AI 生成反馈（一次调用）
            log.info("💡 调用 AI 生成反馈和建议...");
            String feedback = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // 5. 记录进度
            stateManager.recordDailyProgress(today, studentReport, null, null);

            log.info("✅ 晚上流程完成");
            return feedback;

        } catch (Exception e) {
            log.error("晚上流程执行失败", e);
            return "❌ 晚上流程执行失败：" + e.getMessage();
        }
    }

    /**
     * 周总结流程：AI 根据周学习数据，生成周总结和下周改进方案
     *
     * @return 周总结
     */
    public String weeklySummary() {
        log.info("📊 开始周总结流程");

        try {
            // 1. 获取数据
            WeeklyPlan weeklyPlan = stateManager.getCurrentWeeklyPlan();
            if (weeklyPlan == null) {
                log.error("未找到周计划");
                return "❌ 错误：未找到周计划";
            }

            Map<String, Object> weeklyStats = stateManager.getWeeklyStatistics();
            List<DailyPlan> dailyPlans = new java.util.ArrayList<>(stateManager.getDailyPlanHistory().values());

            // 2. 构建上下文
            String weeklyPlanContext = contextBuilder.buildWeeklyPlanForSummary(weeklyPlan);
            String metricsContext = contextBuilder.buildWeeklyMetricsContext(weeklyStats);
            String dailyProgressContext = contextBuilder.buildDailyProgressListContext(dailyPlans);

            // 3. 构建完整提示词
            String prompt = PlanPromptConfig.getWeeklySummaryPrompt(weeklyPlanContext, metricsContext, dailyProgressContext);

            // 4. 调用 AI 生成周总结（一次调用）
            log.info("📈 调用 AI 生成周总结...");
            String summary = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("✅ 周总结流程完成");
            return summary;

        } catch (Exception e) {
            log.error("周总结流程执行失败", e);
            return "❌ 周总结流程执行失败：" + e.getMessage();
        }
    }

    /**
     * 从 AI 生成的计划中解析出结构化数据
     * 这是一个简单的实现，可以根据需要改进
     */
    private DailyPlan parsePlanFromAI(String aiPlan, LocalDate date) {
        DailyPlan plan = new DailyPlan();
        plan.setPlanDate(date);
        plan.setPlanName("AI 生成的计划");
        
        // 简单实现：直接使用 AI 生成的计划文本
        // 实际应用中可以调用 AI 进行结构化提取，或使用正则表达式解析
        // 这里为了保持简单，暂时不做复杂的解析
        
        return plan;
    }
}
