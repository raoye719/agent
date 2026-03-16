package com.yupi.yuaiagent.plan.model;

/**
 * 任务状态枚举
 */
public enum TaskStatus {
    NOT_STARTED("未开始"),
    IN_PROGRESS("进行中"),
    COMPLETED("已完成"),
    INCOMPLETE("未完成");
    
    private final String description;
    
    TaskStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
