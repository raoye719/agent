package com.yupi.yuaiagent.plan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 日学习计划模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyPlan {
    
    /**
     * 计划日期
     */
    private LocalDate planDate;
    
    /**
     * 计划名称
     */
    private String planName;
    
    /**
     * 该日的所有任务
     */
    private List<StudyTask> tasks;
    
    /**
     * 计划总学习时长（小时）
     */
    private double plannedTotalHours;
    
    /**
     * 实际学习时长（小时）
     */
    private double actualTotalHours;
    
    /**
     * 日计划完成度（0-100%）
     */
    private int completionRate;
    
    /**
     * 学习状态评价：优秀、良好、一般、较差
     */
    private String statusEvaluation;
    
    /**
     * 遇到的问题
     */
    private String problems;
    
    /**
     * 明日建议
     */
    private List<String> tomorrowSuggestions;
    
    /**
     * 是否已上报进度
     */
    private boolean reported;
    
    /**
     * 备注
     */
    private String notes;
}
