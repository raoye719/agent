package com.yupi.yuaiagent.plan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 学习任务模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyTask {
    
    /**
     * 任务ID
     */
    private String taskId;
    
    /**
     * 任务名称
     */
    private String taskName;
    
    /**
     * 任务描述
     */
    private String description;
    
    /**
     * 所属科目（数学、英语、政治、专业课）
     */
    private String subject;
    
    /**
     * 计划完成时间（小时）
     */
    private double plannedHours;
    
    /**
     * 实际完成时间（小时）
     */
    private double actualHours;
    
    /**
     * 任务状态：未开始、进行中、已完成、未完成
     */
    private TaskStatus status;
    
    /**
     * 完成度（0-100%）
     */
    private int completionRate;
    
    /**
     * 优先级（1-5，5最高）
     */
    private int priority;
    
    /**
     * 是否顺延到下一天
     */
    private boolean postponed;
    
    /**
     * 备注
     */
    private String notes;
}
