package com.yupi.yuaiagent.plan.manager;

import com.yupi.yuaiagent.plan.model.DailyPlan;
import com.yupi.yuaiagent.plan.model.StudyTask;
import com.yupi.yuaiagent.plan.model.TaskStatus;
import com.yupi.yuaiagent.plan.model.WeeklyPlan;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学习状态管理器
 * 负责记录和管理学习进度、任务完成情况等
 */
@Component
@Slf4j
public class StudyStateManager {
    
    /**
     * 当前周计划
     */
    private WeeklyPlan currentWeeklyPlan;
    
    /**
     * 当前日计划
     */
    private DailyPlan currentDailyPlan;
    
    /**
     * 历史日计划记录
     */
    private final Map<LocalDate, DailyPlan> dailyPlanHistory = new HashMap<>();
    
    /**
     * 历史周计划记录
     */
    private final Map<String, WeeklyPlan> weeklyPlanHistory = new HashMap<>();
    
    /**
     * 初始化周计划
     */
    public void initializeWeeklyPlan(WeeklyPlan weeklyPlan) {
        this.currentWeeklyPlan = weeklyPlan;
        log.info("周计划已初始化：{}", weeklyPlan.getWeekName());
    }
    
    /**
     * 初始化日计划
     */
    public void initializeDailyPlan(DailyPlan dailyPlan) {
        this.currentDailyPlan = dailyPlan;
        log.info("日计划已初始化：{}", dailyPlan.getPlanDate());
    }
    
    /**
     * 更新任务状态
     */
    public void updateTaskStatus(String taskId, TaskStatus status, double actualHours, int completionRate) {
        if (currentDailyPlan == null || currentDailyPlan.getTasks() == null) {
            log.warn("当前日计划为空，无法更新任务状态");
            return;
        }
        
        for (StudyTask task : currentDailyPlan.getTasks()) {
            if (task.getTaskId().equals(taskId)) {
                task.setStatus(status);
                task.setActualHours(actualHours);
                task.setCompletionRate(completionRate);
                log.info("任务已更新：{} - 状态：{} - 完成度：{}%", taskId, status, completionRate);
                break;
            }
        }
        
        // 更新日计划的完成度
        updateDailyPlanCompletion();
    }
    
    /**
     * 更新日计划完成度
     */
    private void updateDailyPlanCompletion() {
        if (currentDailyPlan == null || currentDailyPlan.getTasks() == null) {
            return;
        }
        
        List<StudyTask> tasks = currentDailyPlan.getTasks();
        if (tasks.isEmpty()) {
            currentDailyPlan.setCompletionRate(0);
            return;
        }
        
        // 计算平均完成度
        int avgCompletion = (int) tasks.stream()
                .mapToInt(StudyTask::getCompletionRate)
                .average()
                .orElse(0);
        
        currentDailyPlan.setCompletionRate(avgCompletion);
        
        // 计算实际学习时长
        double totalActualHours = tasks.stream()
                .mapToDouble(StudyTask::getActualHours)
                .sum();
        currentDailyPlan.setActualTotalHours(totalActualHours);
        
        log.info("日计划完成度已更新：{}%", avgCompletion);
    }
    
    /**
     * 记录日计划进度
     */
    public void recordDailyProgress(LocalDate date, String statusEvaluation, String problems, List<String> suggestions) {
        if (currentDailyPlan == null) {
            log.warn("当前日计划为空，无法记录进度");
            return;
        }
        
        currentDailyPlan.setStatusEvaluation(statusEvaluation);
        currentDailyPlan.setProblems(problems);
        currentDailyPlan.setTomorrowSuggestions(suggestions);
        currentDailyPlan.setReported(true);
        
        // 保存到历史记录
        dailyPlanHistory.put(date, currentDailyPlan);
        log.info("日计划进度已记录：{}", date);
    }
    
    /**
     * 获取昨日进度
     */
    public DailyPlan getYesterdayProgress(LocalDate today) {
        LocalDate yesterday = today.minusDays(1);
        return dailyPlanHistory.get(yesterday);
    }
    
    /**
     * 计算周完成度
     */
    public int calculateWeeklyCompletion() {
        if (currentWeeklyPlan == null || currentWeeklyPlan.getDailyPlans() == null) {
            return 0;
        }
        
        List<DailyPlan> dailyPlans = currentWeeklyPlan.getDailyPlans();
        if (dailyPlans.isEmpty()) {
            return 0;
        }
        
        int avgCompletion = (int) dailyPlans.stream()
                .mapToInt(DailyPlan::getCompletionRate)
                .average()
                .orElse(0);
        
        currentWeeklyPlan.setCompletionRate(avgCompletion);
        return avgCompletion;
    }
    
    /**
     * 获取未完成的任务
     */
    public List<StudyTask> getIncompleteTasks() {
        if (currentDailyPlan == null || currentDailyPlan.getTasks() == null) {
            return new ArrayList<>();
        }
        
        return currentDailyPlan.getTasks().stream()
                .filter(task -> task.getStatus() != TaskStatus.COMPLETED)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取按科目分类的任务
     */
    public Map<String, List<StudyTask>> getTasksBySubject() {
        if (currentDailyPlan == null || currentDailyPlan.getTasks() == null) {
            return new HashMap<>();
        }
        
        return currentDailyPlan.getTasks().stream()
                .collect(Collectors.groupingBy(StudyTask::getSubject));
    }
    
    /**
     * 计算学习效率
     */
    public double calculateStudyEfficiency() {
        if (currentDailyPlan == null) {
            return 0;
        }
        
        double plannedHours = currentDailyPlan.getPlannedTotalHours();
        double actualHours = currentDailyPlan.getActualTotalHours();
        
        if (plannedHours == 0) {
            return 0;
        }
        
        return (actualHours / plannedHours) * 100;
    }
    
    /**
     * 获取周学习统计
     */
    public Map<String, Object> getWeeklyStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        if (currentWeeklyPlan == null) {
            return stats;
        }
        
        stats.put("weekName", currentWeeklyPlan.getWeekName());
        stats.put("completionRate", currentWeeklyPlan.getCompletionRate());
        stats.put("plannedHours", currentWeeklyPlan.getPlannedTotalHours());
        stats.put("actualHours", currentWeeklyPlan.getActualTotalHours());
        
        // 按科目统计
        if (currentWeeklyPlan.getSubjectTasks() != null) {
            Map<String, Integer> subjectCompletion = new HashMap<>();
            currentWeeklyPlan.getSubjectTasks().forEach((subject, tasks) -> {
                int completion = (int) tasks.stream()
                        .mapToInt(StudyTask::getCompletionRate)
                        .average()
                        .orElse(0);
                subjectCompletion.put(subject, completion);
            });
            stats.put("subjectCompletion", subjectCompletion);
        }
        
        return stats;
    }
    
    /**
     * 清空当前状态（用于新的一天）
     */
    public void clearCurrentState() {
        currentDailyPlan = null;
        log.info("当前状态已清空");
    }
    
    // Getters
    public WeeklyPlan getCurrentWeeklyPlan() {
        return currentWeeklyPlan;
    }
    
    public DailyPlan getCurrentDailyPlan() {
        return currentDailyPlan;
    }
    
    public Map<LocalDate, DailyPlan> getDailyPlanHistory() {
        return dailyPlanHistory;
    }
}
