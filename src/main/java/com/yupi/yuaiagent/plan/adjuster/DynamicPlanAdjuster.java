package com.yupi.yuaiagent.plan.adjuster;

import com.yupi.yuaiagent.plan.model.DailyPlan;
import com.yupi.yuaiagent.plan.model.StudyTask;
import com.yupi.yuaiagent.plan.model.TaskStatus;
import com.yupi.yuaiagent.plan.manager.StudyStateManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态计划调整器（改进版）
 * 分离数据计算和AI决策
 */
@Component
@Slf4j
public class DynamicPlanAdjuster {
    
    private final StudyStateManager stateManager;
    
    public DynamicPlanAdjuster(StudyStateManager stateManager) {
        this.stateManager = stateManager;
    }
    
    /**
     * 根据昨日进度调整今日计划
     */
    public DailyPlan adjustTodayPlan(DailyPlan originalPlan, DailyPlan yesterdayPlan) {
        if (yesterdayPlan == null) {
            log.info("没有昨日进度，使用原计划");
            return originalPlan;
        }
        
        int yesterdayCompletion = yesterdayPlan.getCompletionRate();
        log.info("昨日完成度：{}%", yesterdayCompletion);
        
        DailyPlan adjustedPlan = new DailyPlan();
        adjustedPlan.setPlanDate(originalPlan.getPlanDate());
        adjustedPlan.setPlanName(originalPlan.getPlanName());
        
        // 根据昨日完成度调整
        if (yesterdayCompletion >= 80) {
            log.info("昨日完成度良好，按原计划推进");
            adjustedPlan.setTasks(new ArrayList<>(originalPlan.getTasks()));
            adjustedPlan.setPlannedTotalHours(originalPlan.getPlannedTotalHours());
        } else if (yesterdayCompletion >= 50) {
            log.info("昨日完成度一般，减少任务量");
            List<StudyTask> reducedTasks = reduceTaskLoad(originalPlan.getTasks(), 0.8);
            adjustedPlan.setTasks(reducedTasks);
            adjustedPlan.setPlannedTotalHours(originalPlan.getPlannedTotalHours() * 0.8);
        } else {
            log.info("昨日完成度较差，先补昨天未完成的任务");
            List<StudyTask> adjustedTasks = new ArrayList<>();
            
            List<StudyTask> incompleteTasks = yesterdayPlan.getTasks().stream()
                    .filter(task -> task.getStatus() != TaskStatus.COMPLETED)
                    .collect(Collectors.toList());
            
            for (StudyTask task : incompleteTasks) {
                task.setPostponed(true);
                task.setNotes("从" + yesterdayPlan.getPlanDate() + "顺延");
                adjustedTasks.add(task);
            }
            
            List<StudyTask> newTasks = reduceTaskLoad(originalPlan.getTasks(), 0.5);
            adjustedTasks.addAll(newTasks);
            
            adjustedPlan.setTasks(adjustedTasks);
            adjustedPlan.setPlannedTotalHours(
                    incompleteTasks.stream().mapToDouble(StudyTask::getPlannedHours).sum() +
                    newTasks.stream().mapToDouble(StudyTask::getPlannedHours).sum()
            );
        }
        
        return adjustedPlan;
    }
    
    /**
     * 减少任务负载
     */
    private List<StudyTask> reduceTaskLoad(List<StudyTask> tasks, double ratio) {
        if (tasks == null || tasks.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<StudyTask> sortedTasks = tasks.stream()
                .sorted(Comparator.comparingInt(StudyTask::getPriority).reversed())
                .collect(Collectors.toList());
        
        int keepCount = Math.max(1, (int) (sortedTasks.size() * ratio));
        return sortedTasks.stream()
                .limit(keepCount)
                .collect(Collectors.toList());
    }
    
    /**
     * 生成AI决策的提示词（关键改进！）
     * 不再硬性生成建议，而是生成提示词给AI
     */
    public String generatePromptForAI(DailyPlan dailyPlan) {
        if (dailyPlan == null) {
            return "";
        }
        
        int completionRate = dailyPlan.getCompletionRate();
        double efficiency = stateManager.calculateStudyEfficiency();
        List<StudyTask> incompleteTasks = stateManager.getIncompleteTasks();
        
        // 构建客观数据
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下学生今日学习数据，生成个性化的学习建议和明日计划调整方案：\n\n");
        
        prompt.append("📊 今日学习数据：\n");
        prompt.append(String.format("- 完成度：%d%%\n", completionRate));
        prompt.append(String.format("- 学习效率：%.1f%%\n", efficiency * 100));
        prompt.append(String.format("- 计划学习时长：%.1f小时\n", dailyPlan.getPlannedTotalHours()));
        prompt.append(String.format("- 实际学习时长：%.1f小时\n", dailyPlan.getActualTotalHours()));
        prompt.append(String.format("- 状态评价：%s\n", dailyPlan.getStatusEvaluation()));
        
        if (dailyPlan.getProblems() != null && !dailyPlan.getProblems().isEmpty()) {
            prompt.append(String.format("- 遇到的问题：%s\n", dailyPlan.getProblems()));
        }
        
        // 未完成任务
        if (!incompleteTasks.isEmpty()) {
            prompt.append("\n📋 未完成的任务：\n");
            for (StudyTask task : incompleteTasks) {
                prompt.append(String.format("- %s（%s，%.1f小时）\n", 
                        task.getTaskName(), task.getSubject(), task.getPlannedHours()));
            }
        }
        
        // 按科目统计
        Map<String, List<StudyTask>> tasksBySubject = stateManager.getTasksBySubject();
        if (!tasksBySubject.isEmpty()) {
            prompt.append("\n📚 各科目完成情况：\n");
            tasksBySubject.forEach((subject, tasks) -> {
                int subjectCompletion = (int) tasks.stream()
                        .mapToInt(StudyTask::getCompletionRate)
                        .average()
                        .orElse(0);
                prompt.append(String.format("- %s：%d%%\n", subject, subjectCompletion));
            });
        }
        
        prompt.append("\n请提供：\n");
        prompt.append("1. 对今日学习效果的评价（1-2句）\n");
        prompt.append("2. 明日计划调整建议（具体的任务调整）\n");
        prompt.append("3. 学习策略建议（如何提高效率）\n");
        prompt.append("4. 心理鼓励（根据完成度）\n");
        
        return prompt.toString();
    }
    
    /**
     * 生成周计划的AI决策提示词
     */
    public String generateWeeklyPromptForAI() {
        Map<String, Object> stats = stateManager.getWeeklyStatistics();
        
        if (stats.isEmpty()) {
            return "";
        }
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("请根据以下本周学习数据，生成周总结和下周改进方案：\n\n");
        
        prompt.append("📊 本周学习统计：\n");
        prompt.append(String.format("- 周计划：%s\n", stats.get("weekName")));
        prompt.append(String.format("- 完成度：%d%%\n", stats.get("completionRate")));
        prompt.append(String.format("- 计划学习时长：%.1f小时\n", stats.get("plannedHours")));
        prompt.append(String.format("- 实际学习时长：%.1f小时\n", stats.get("actualHours")));
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> subjectCompletion = (Map<String, Integer>) stats.get("subjectCompletion");
        if (subjectCompletion != null && !subjectCompletion.isEmpty()) {
            prompt.append("\n📚 各科目完成情况：\n");
            subjectCompletion.forEach((subject, completion) ->
                    prompt.append(String.format("- %s：%d%%\n", subject, completion))
            );
        }
        
        prompt.append("\n请提供：\n");
        prompt.append("1. 本周学习总体评价\n");
        prompt.append("2. 各科目的具体分析\n");
        prompt.append("3. 下周改进方案（具体可行的建议）\n");
        prompt.append("4. 激励和鼓励\n");
        
        return prompt.toString();
    }
    
    /**
     * 处理未完成任务的顺延
     */
    public List<StudyTask> postponeIncompleteTasks(List<StudyTask> incompleteTasks) {
        return incompleteTasks.stream()
                .peek(task -> {
                    task.setPostponed(true);
                    task.setStatus(TaskStatus.NOT_STARTED);
                    task.setCompletionRate(0);
                    task.setActualHours(0);
                    log.info("任务已顺延：{}", task.getTaskName());
                })
                .collect(Collectors.toList());
    }
}
