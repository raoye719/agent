package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.plan.file.FileSystemManager;
import com.yupi.yuaiagent.plan.generator.PlanGenerator;
import com.yupi.yuaiagent.plan.parser.ProgressParser;
import com.yupi.yuaiagent.plan.service.WeeklyPlanService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * AI驱动的规划控制器 V2
 * 基于文件系统和自然语言处理
 */
@RestController
@RequestMapping("/ai/plan/v2")
public class AIPlanControllerV2 {
    
    @Resource
    private FileSystemManager fileSystemManager;
    
    @Resource
    private PlanGenerator planGenerator;
    
    @Resource
    private WeeklyPlanService weeklyPlanService;

    @Resource
    private ProgressParser progressParser;
    
    /**
     * 初始化工作目录
     */
    @PostMapping("/init")
    public Map<String, Object> initializeWorkspace() {
        Map<String, Object> response = new HashMap<>();
        try {
            fileSystemManager.initializeWorkspace();
            response.put("success", true);
            response.put("message", "✅ 工作目录已初始化");
            response.put("workspacePath", fileSystemManager.getWorkspacePath());
            response.put("weeklyPlanDir", fileSystemManager.getWeeklyPlanDirPath());
            response.put("studyRecordsDir", fileSystemManager.getStudyRecordsDirPath());
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "❌ 初始化失败：" + e.getMessage());
            return response;
        }
    }
    
    /**
     * 创建周计划
     * 用户输入自然语言周计划描述，AI整理成markdown文件
     */
    @PostMapping("/weekly/create")
    public Map<String, Object> createWeeklyPlan(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String weekDescription = (String) request.get("weekDescription");
            String startDateStr = (String) request.get("startDate");
            String endDateStr = (String) request.get("endDate");
            
            if (weekDescription == null || weekDescription.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "❌ 周计划描述不能为空");
                return response;
            }
            
            LocalDate startDate = startDateStr != null ? LocalDate.parse(startDateStr) : LocalDate.now();
            LocalDate endDate = endDateStr != null ? LocalDate.parse(endDateStr) : startDate.plusDays(6);
            
            // 调用服务创建周计划
            Map<String, Object> result = weeklyPlanService.createWeeklyPlan(weekDescription, startDate, endDate);
            
            return result;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "❌ 创建周计划失败：" + e.getMessage());
            return response;
        }
    }
    
    /**
     * 早上流程：生成今日计划
     * 自动读取周计划和昨日进度，生成今日计划，并保存到文件
     */
    @GetMapping("/daily/morning")
    public Map<String, Object> morningRoutine(@RequestParam(required = false) String date) {
        Map<String, Object> response = new HashMap<>();
        try {
            LocalDate today = date != null ? LocalDate.parse(date) : LocalDate.now();

            // 生成今日计划（内部已自动保存到 study_records/{date}_学习计划完成情况.md）
            String todayPlan = planGenerator.generateTodayPlan(today);

            response.put("success", true);
            response.put("date", today);
            response.put("message", todayPlan);
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "❌ 生成计划失败：" + e.getMessage());
            return response;
        }
    }
    
    /**
     * 晚上流程：上报学习进度
     * 接收自然语言进度报告，AI生成反馈后追加写入当日文件
     */
    @PostMapping("/daily/evening")
    public Map<String, Object> eveningRoutine(
            @RequestParam(required = false) String date,
            @RequestParam String progressReport) {
        Map<String, Object> response = new HashMap<>();
        try {
            LocalDate today = date != null ? LocalDate.parse(date) : LocalDate.now();

            if (progressReport == null || progressReport.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "❌ 进度报告不能为空");
                return response;
            }

            // 1. 读取早上生成的今日计划文件
            String existingContent = fileSystemManager.readProgressRecord(today);
            if (existingContent == null) {
                existingContent = "";
            }

            // 2. 让 AI 根据进度报告生成完成情况和明日建议
            String aiFeedback = planGenerator.generateEveningFeedback(today, existingContent, progressReport);

            // 3. 追加写入到当日文件
            String updatedContent = existingContent +
                    "\n---\n\n" +
                    "## ✅ 今日完成情况\n\n" + progressReport + "\n\n" +
                    "## 🎯 明日建议\n\n" + aiFeedback + "\n";

            boolean saved = fileSystemManager.writeProgressRecord(today, updatedContent);

            response.put("success", true);
            response.put("date", today);
            response.put("message", saved ? "✅ 进度已记录并保存" : "⚠️ 进度处理完成但文件保存失败");
            response.put("feedback", aiFeedback);
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "❌ 处理进度失败：" + e.getMessage());
            return response;
        }
    }
    
    /**
     * 读取周计划
     */
    @GetMapping("/weekly/read")
    public Map<String, Object> readWeeklyPlan(@RequestParam(required = false) String date) {
        Map<String, Object> response = new HashMap<>();
        try {
            LocalDate today = date != null ? LocalDate.parse(date) : LocalDate.now();
            String weeklyPlan = fileSystemManager.readWeeklyPlan(today);
            
            if (weeklyPlan == null) {
                response.put("success", false);
                response.put("message", "❌ 未找到周计划文件");
                return response;
            }
            
            response.put("success", true);
            response.put("weeklyPlan", weeklyPlan);
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "❌ 读取周计划失败：" + e.getMessage());
            return response;
        }
    }
    
    /**
     * 读取进度记录
     */
    @GetMapping("/progress/read")
    public Map<String, Object> readProgressRecord(@RequestParam String date) {
        Map<String, Object> response = new HashMap<>();
        try {
            LocalDate targetDate = LocalDate.parse(date);
            String progressRecord = fileSystemManager.readProgressRecord(targetDate);
            
            if (progressRecord == null) {
                response.put("success", false);
                response.put("message", "❌ 未找到进度记录文件");
                return response;
            }
            
            response.put("success", true);
            response.put("date", date);
            response.put("progressRecord", progressRecord);
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "❌ 读取进度记录失败：" + e.getMessage());
            return response;
        }
    }
    
    /**
     * 获取工作目录信息
     */
    @GetMapping("/workspace/info")
    public Map<String, Object> getWorkspaceInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("workspacePath", fileSystemManager.getWorkspacePath());
        response.put("weeklyPlanDir", fileSystemManager.getWeeklyPlanDirPath());
        response.put("studyRecordsDir", fileSystemManager.getStudyRecordsDirPath());
        return response;
    }
}
