package com.yupi.yuaiagent.plan.config;

import com.yupi.yuaiagent.plan.model.DailyPlan;
import com.yupi.yuaiagent.plan.model.StudyTask;
import com.yupi.yuaiagent.plan.model.WeeklyPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 学习规划系统的上下文构建器
 * 负责将数据格式化为 AI 可以理解的上下文
 */
@Component
@Slf4j
public class PlanContextBuilder {

    /**
     * 构建周计划上下文
     */
    public String buildWeeklyPlanContext(WeeklyPlan weeklyPlan) {
        if (weeklyPlan == null) {
            return "暂无周计划";
        }

        StringBuilder context = new StringBuilder();
        context.append("**周计划名称**：").append(weeklyPlan.getWeekName()).append("\n\n");

        if (weeklyPlan.getDailyPlans() != null && !weeklyPlan.getDailyPlans().isEmpty()) {
            context.append("**周计划任务**：\n");
            for (DailyPlan dailyPlan : weeklyPlan.getDailyPlans()) {
                context.append(String.format("- %s：%s\n", 
                    dailyPlan.getPlanDate(), 
                    dailyPlan.getPlanName()));
            }
        }

        context.append("\n**周计划详情**：\n");
        context.append(String.format("- 计划学习时长：%.1f 小时\n", weeklyPlan.getPlannedTotalHours()));
        context.append(String.format("- 计划完成度：%d%%\n", weeklyPlan.getCompletionRate()));

        return context.toString();
    }

    /**
     * 构建昨日进度上下文
     */
    public String buildYesterdayProgressContext(DailyPlan yesterdayPlan) {
        if (yesterdayPlan == null) {
            return "这是第一天，暂无昨日进度";
        }

        StringBuilder context = new StringBuilder();
        context.append(String.format("**日期**：%s\n\n", yesterdayPlan.getPlanDate()));
        context.append(String.format("**完成度**：%d%%\n", yesterdayPlan.getCompletionRate()));
        context.append(String.format("**计划学习时长**：%.1f 小时\n", yesterdayPlan.getPlannedTotalHours()));
        context.append(String.format("**实际学习时长**：%.1f 小时\n", yesterdayPlan.getActualTotalHours()));
        context.append(String.format("**状态评价**：%s\n", yesterdayPlan.getStatusEvaluation()));

        if (yesterdayPlan.getProblems() != null && !yesterdayPlan.getProblems().isEmpty()) {
            context.append(String.format("**遇到的问题**：%s\n", yesterdayPlan.getProblems()));
        }

        // 按科目统计
        if (yesterdayPlan.getTasks() != null && !yesterdayPlan.getTasks().isEmpty()) {
            context.append("\n**各科目完成情况**：\n");
            Map<String, List<StudyTask>> tasksBySubject = yesterdayPlan.getTasks().stream()
                    .collect(java.util.stream.Collectors.groupingBy(StudyTask::getSubject));
            
            tasksBySubject.forEach((subject, tasks) -> {
                int subjectCompletion = (int) tasks.stream()
                        .mapToInt(StudyTask::getCompletionRate)
                        .average()
                        .orElse(0);
                context.append(String.format("- %s：%d%%\n", subject, subjectCompletion));
            });
        }

        if (yesterdayPlan.getTomorrowSuggestions() != null && !yesterdayPlan.getTomorrowSuggestions().isEmpty()) {
            context.append("\n**昨日建议**：\n");
            for (String suggestion : yesterdayPlan.getTomorrowSuggestions()) {
                context.append(String.format("- %s\n", suggestion));
            }
        }

        return context.toString();
    }

    /**
     * 构建学生状态上下文
     */
    public String buildStudentStatusContext(String studentStatus) {
        if (studentStatus == null || studentStatus.trim().isEmpty()) {
            return "学生未提供状态信息";
        }
        return studentStatus;
    }

    /**
     * 构建今日计划上下文
     */
    public String buildTodayPlanContext(DailyPlan todayPlan) {
        if (todayPlan == null) {
            return "暂无今日计划";
        }

        StringBuilder context = new StringBuilder();
        context.append(String.format("**日期**：%s\n\n", todayPlan.getPlanDate()));
        context.append(String.format("**计划学习时长**：%.1f 小时\n", todayPlan.getPlannedTotalHours()));

        if (todayPlan.getTasks() != null && !todayPlan.getTasks().isEmpty()) {
            context.append("\n**计划任务**：\n");
            Map<String, List<StudyTask>> tasksBySubject = todayPlan.getTasks().stream()
                    .collect(java.util.stream.Collectors.groupingBy(StudyTask::getSubject));
            
            tasksBySubject.forEach((subject, tasks) -> {
                context.append(String.format("\n**%s**：\n", subject));
                for (StudyTask task : tasks) {
                    context.append(String.format("- %s（%.1f 小时）\n", task.getTaskName(), task.getPlannedHours()));
                }
            });
        }

        return context.toString();
    }

    /**
     * 构建完成情况指标上下文
     */
    public String buildCompletionMetricsContext(DailyPlan todayPlan, Map<String, Object> weeklyStats) {
        StringBuilder context = new StringBuilder();

        if (todayPlan != null) {
            context.append("**今日完成情况**：\n");
            context.append(String.format("- 完成度：%d%%\n", todayPlan.getCompletionRate()));
            context.append(String.format("- 计划时长：%.1f 小时\n", todayPlan.getPlannedTotalHours()));
            context.append(String.format("- 实际时长：%.1f 小时\n", todayPlan.getActualTotalHours()));
            
            if (todayPlan.getPlannedTotalHours() > 0) {
                double efficiency = (todayPlan.getActualTotalHours() / todayPlan.getPlannedTotalHours()) * 100;
                context.append(String.format("- 学习效率：%.1f%%\n", efficiency));
            }

            if (todayPlan.getTasks() != null && !todayPlan.getTasks().isEmpty()) {
                context.append("\n**各科目完成情况**：\n");
                Map<String, List<StudyTask>> tasksBySubject = todayPlan.getTasks().stream()
                        .collect(java.util.stream.Collectors.groupingBy(StudyTask::getSubject));
                
                tasksBySubject.forEach((subject, tasks) -> {
                    int subjectCompletion = (int) tasks.stream()
                            .mapToInt(StudyTask::getCompletionRate)
                            .average()
                            .orElse(0);
                    context.append(String.format("- %s：%d%%\n", subject, subjectCompletion));
                });
            }
        }

        if (weeklyStats != null && !weeklyStats.isEmpty()) {
            context.append("\n**周学习统计**：\n");
            context.append(String.format("- 周完成度：%d%%\n", weeklyStats.getOrDefault("completionRate", 0)));
            context.append(String.format("- 计划时长：%.1f 小时\n", weeklyStats.getOrDefault("plannedHours", 0)));
            context.append(String.format("- 实际时长：%.1f 小时\n", weeklyStats.getOrDefault("actualHours", 0)));
        }

        return context.toString();
    }

    /**
     * 构建周学习数据上下文
     */
    public String buildWeeklyMetricsContext(Map<String, Object> weeklyStats) {
        if (weeklyStats == null || weeklyStats.isEmpty()) {
            return "暂无周学习数据";
        }

        StringBuilder context = new StringBuilder();
        context.append(String.format("**周名称**：%s\n\n", weeklyStats.getOrDefault("weekName", "未知")));
        context.append(String.format("**周完成度**：%d%%\n", weeklyStats.getOrDefault("completionRate", 0)));
        context.append(String.format("**计划学习时长**：%.1f 小时\n", weeklyStats.getOrDefault("plannedHours", 0)));
        context.append(String.format("**实际学习时长**：%.1f 小时\n", weeklyStats.getOrDefault("actualHours", 0)));

        @SuppressWarnings("unchecked")
        Map<String, Integer> subjectCompletion = (Map<String, Integer>) weeklyStats.get("subjectCompletion");
        if (subjectCompletion != null && !subjectCompletion.isEmpty()) {
            context.append("\n**各科目完成情况**：\n");
            subjectCompletion.forEach((subject, completion) ->
                    context.append(String.format("- %s：%d%%\n", subject, completion))
            );
        }

        return context.toString();
    }

    /**
     * 构建日进度列表上下文
     */
    public String buildDailyProgressListContext(List<DailyPlan> dailyPlans) {
        if (dailyPlans == null || dailyPlans.isEmpty()) {
            return "暂无日进度数据";
        }

        StringBuilder context = new StringBuilder();
        context.append("**本周各日进度**：\n\n");

        for (DailyPlan dailyPlan : dailyPlans) {
            context.append(String.format("**%s**\n", dailyPlan.getPlanDate()));
            context.append(String.format("- 完成度：%d%%\n", dailyPlan.getCompletionRate()));
            context.append(String.format("- 学习时长：%.1f 小时\n", dailyPlan.getActualTotalHours()));
            context.append(String.format("- 状态：%s\n", dailyPlan.getStatusEvaluation()));
            context.append("\n");
        }

        return context.toString();
    }

    /**
     * 构建完整的早上流程上下文
     */
    public String buildMorningContext(WeeklyPlan weeklyPlan, DailyPlan yesterdayPlan, String studentStatus) {
        String weeklyContext = buildWeeklyPlanContext(weeklyPlan);
        String yesterdayContext = buildYesterdayProgressContext(yesterdayPlan);
        String statusContext = buildStudentStatusContext(studentStatus);

        return PlanPromptConfig.getMorningPrompt(weeklyContext, yesterdayContext, statusContext);
    }

    /**
     * 构建完整的晚上流程上下文
     */
    public String buildEveningContext(DailyPlan todayPlan, String studentReport, Map<String, Object> weeklyStats) {
        String todayContext = buildTodayPlanContext(todayPlan);
        String metricsContext = buildCompletionMetricsContext(todayPlan, weeklyStats);

        return PlanPromptConfig.getEveningPrompt(todayContext, metricsContext, studentReport);
    }

    /**
     * 构建完整的周总结上下文
     */
    public String buildWeeklySummaryContext(WeeklyPlan weeklyPlan, Map<String, Object> weeklyStats, List<DailyPlan> dailyPlans) {
        String weeklyContext = buildWeeklyPlanContext(weeklyPlan);
        String metricsContext = buildWeeklyMetricsContext(weeklyStats);
        String dailyContext = buildDailyProgressListContext(dailyPlans);

        return PlanPromptConfig.getWeeklySummaryPrompt(weeklyContext, metricsContext, dailyContext);
    }

    /**
     * 构建学生报告上下文
     */
    public String buildStudentReportContext(String studentReport) {
        if (studentReport == null || studentReport.trim().isEmpty()) {
            return "学生未提供反馈";
        }
        return studentReport;
    }

    /**
     * 构建周计划上下文（用于总结）
     */
    public String buildWeeklyPlanForSummary(WeeklyPlan weeklyPlan) {
        return buildWeeklyPlanContext(weeklyPlan);
    }
}
