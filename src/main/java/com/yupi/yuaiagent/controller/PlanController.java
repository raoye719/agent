package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.plan.agent.StudyPlanAgent;
import com.yupi.yuaiagent.plan.manager.StudyStateManager;
import com.yupi.yuaiagent.plan.model.DailyPlan;
import com.yupi.yuaiagent.plan.model.StudyTask;
import com.yupi.yuaiagent.plan.model.WeeklyPlan;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习规划控制器
 * 提供规划相关的API接口
 */
@RestController
@RequestMapping("/ai/plan")
public class PlanController {
    
    @Resource
    private StudyPlanAgent studyPlanAgent;
    
    @Resource
    private StudyStateManager stateManager;
    
    /**
     * 初始化周计划
     */
    @PostMapping("/weekly/init")
    public Map<String, Object> initializeWeeklyPlan(@RequestBody WeeklyPlan weeklyPlan) {
        Map<String, Object> response = new HashMap<>();
        try {
            stateManager.initializeWeeklyPlan(weeklyPlan);
            response.put("success", true);
            response.put("message", "周计划已初始化");
            response.put("weekName", weeklyPlan.getWeekName());
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "初始化失败：" + e.getMessage());
            return response;
        }
    }
    
    /**
     * 获取当前周计划
     */
    @GetMapping("/weekly/current")
    public Map<String, Object> getCurrentWeeklyPlan() {
        Map<String, Object> response = new HashMap<>();
        WeeklyPlan plan = stateManager.getCurrentWeeklyPlan();
        if (plan != null) {
            response.put("success", true);
            response.put("data", plan);
        } else {
            response.put("success", false);
            response.put("message", "未找到周计划");
        }
        return response;
    }
    
    /**
     * 早上流程：制定今日计划
     */
    @GetMapping("/daily/morning")
    public Map<String, Object> morningRoutine(@RequestParam(required = false) String date) {
        Map<String, Object> response = new HashMap<>();
        try {
            LocalDate today = date != null ? LocalDate.parse(date) : LocalDate.now();
            String result = studyPlanAgent.morningRoutine(today);
            response.put("success", true);
            response.put("message", result);
            response.put("date", today);
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "早上流程执行失败：" + e.getMessage());
            return response;
        }
    }
    
    /**
     * 晚上流程：记录进度
     */
    @PostMapping("/daily/evening")
    public Map<String, Object> eveningRoutine(
            @RequestParam(required = false) String date,
            @RequestParam double studyHours,
            @RequestParam String statusEvaluation,
            @RequestParam(required = false) String problems) {
        Map<String, Object> response = new HashMap<>();
        try {
            LocalDate today = date != null ? LocalDate.parse(date) : LocalDate.now();
            String result = studyPlanAgent.eveningRoutine(today, studyHours, statusEvaluation, problems);
            response.put("success", true);
            response.put("message", result);
            response.put("date", today);
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "晚上流程执行失败：" + e.getMessage());
            return response;
        }
    }
    
    /**
     * 周总结
     */
    @GetMapping("/weekly/summary")
    public Map<String, Object> weeklySummary() {
        Map<String, Object> response = new HashMap<>();
        try {
            String result = studyPlanAgent.weeklySummary();
            response.put("success", true);
            response.put("message", result);
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "周总结生成失败：" + e.getMessage());
            return response;
        }
    }
    
    /**
     * 更新任务状态
     */
    @PostMapping("/task/update")
    public Map<String, Object> updateTaskStatus(
            @RequestParam String taskId,
            @RequestParam String status,
            @RequestParam double actualHours,
            @RequestParam int completionRate) {
        Map<String, Object> response = new HashMap<>();
        try {
            stateManager.updateTaskStatus(taskId, 
                    com.yupi.yuaiagent.plan.model.TaskStatus.valueOf(status),
                    actualHours, completionRate);
            response.put("success", true);
            response.put("message", "任务已更新");
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "更新失败：" + e.getMessage());
            return response;
        }
    }
    
    /**
     * 获取当前日计划
     */
    @GetMapping("/daily/current")
    public Map<String, Object> getCurrentDailyPlan() {
        Map<String, Object> response = new HashMap<>();
        DailyPlan plan = stateManager.getCurrentDailyPlan();
        if (plan != null) {
            response.put("success", true);
            response.put("data", plan);
        } else {
            response.put("success", false);
            response.put("message", "未找到当前日计划");
        }
        return response;
    }
    
    /**
     * 获取未完成的任务
     */
    @GetMapping("/task/incomplete")
    public Map<String, Object> getIncompleteTasks() {
        Map<String, Object> response = new HashMap<>();
        List<StudyTask> tasks = stateManager.getIncompleteTasks();
        response.put("success", true);
        response.put("data", tasks);
        response.put("count", tasks.size());
        return response;
    }
    
    /**
     * 获取按科目分类的任务
     */
    @GetMapping("/task/by-subject")
    public Map<String, Object> getTasksBySubject() {
        Map<String, Object> response = new HashMap<>();
        Map<String, List<StudyTask>> tasks = stateManager.getTasksBySubject();
        response.put("success", true);
        response.put("data", tasks);
        return response;
    }
    
    /**
     * 获取学习统计
     */
    @GetMapping("/statistics/weekly")
    public Map<String, Object> getWeeklyStatistics() {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> stats = stateManager.getWeeklyStatistics();
        response.put("success", true);
        response.put("data", stats);
        return response;
    }
    
    /**
     * 计算学习效率
     */
    @GetMapping("/statistics/efficiency")
    public Map<String, Object> calculateEfficiency() {
        Map<String, Object> response = new HashMap<>();
        double efficiency = stateManager.calculateStudyEfficiency();
        response.put("success", true);
        response.put("efficiency", String.format("%.1f%%", efficiency));
        return response;
    }
    
    /**
     * 流式规划（使用Agent）
     */
    @GetMapping(value = "/plan/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPlan(@RequestParam String userRequest) {
        return studyPlanAgent.runStream(userRequest);
    }
    
    /**
     * 同步规划（使用Agent）
     */
    @GetMapping("/plan/sync")
    public Map<String, Object> syncPlan(@RequestParam String userRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            String result = studyPlanAgent.run(userRequest);
            response.put("success", true);
            response.put("message", result);
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "规划执行失败：" + e.getMessage());
            return response;
        }
    }
}
