package com.yupi.yuaiagent.plan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 周学习计划模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyPlan {
    
    /**
     * 周计划ID
     */
    private String weekPlanId;
    
    /**
     * 周计划名称（如：第一周 2026.3.10-2026.3.16）
     */
    private String weekName;
    
    /**
     * 周开始日期
     */
    private LocalDate startDate;
    
    /**
     * 周结束日期
     */
    private LocalDate endDate;
    
    /**
     * 各科目的周目标
     */
    private Map<String, String> subjectGoals;
    
    /**
     * 各科目的任务清单
     */
    private Map<String, List<StudyTask>> subjectTasks;
    
    /**
     * 周计划的所有日计划
     */
    private List<DailyPlan> dailyPlans;
    
    /**
     * 周计划总学习时长（小时）
     */
    private double plannedTotalHours;
    
    /**
     * 实际学习时长（小时）
     */
    private double actualTotalHours;
    
    /**
     * 周计划完成度（0-100%）
     */
    private int completionRate;
    
    /**
     * 进度追踪表
     */
    private Map<String, Integer> progressTracking;
    
    /**
     * 周总结
     */
    private String weeklySummary;
    
    /**
     * 下周改进方案
     */
    private List<String> nextWeekImprovements;
}
