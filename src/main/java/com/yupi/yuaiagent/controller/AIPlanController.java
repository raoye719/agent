package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.plan.file.FileSystemManager;
import com.yupi.yuaiagent.plan.generator.PlanGenerator;
import com.yupi.yuaiagent.plan.parser.ProgressParser;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * AI驱动的规划控制器
 * 基于文件系统和自然语言处理
 */
@RestController
@RequestMapping("/ai/plan/v2")
public class AIPlanController {
    
    @Resource
    private FileSystemManager fileSystemManager;
    
    @Resource
    private PlanGenerator planGenerator;
    
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
     * 早上流程：生成今日计划
     * 自动读取周计划和昨日进度，生成今日计划
     */
    @GetMapping("/daily/morning")
    public Map<String, Object> morningRoutine(@RequestParam(required = false) String date) {
        Map<String, Object> response = new HashMap<>();
        try {
            LocalDate today = date != null ? LocalDate.parse(date) : LocalDate.now();
            
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
     * 接收自然语言进度报告，生成进度记录和明日建议
     */
    @PostMapping("/daily/evening")
    public Map<String, Object> eveningRoutine(
            @RequestParam(required = false) String date,
            @RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            LocalDate today = date != null ? LocalDate.parse(date) : LocalDate.now();
            String progressReport = request.get("progressReport");
            
            if (progressReport == null || progressReport.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "❌ 进度报告不能为空");
                return response;
            }
            
            // 处理进度上报
            Map<String, Object> result = progressParser.processProgressReport(today, progressReport);
            
            response.put("success", true);
            response.put("date", today);
            response.put("message", "✅ 进度已记录");
            response.put("parsedProgress", result.get("parsedProgress"));
            response.put("recordSaved", result.get("recordSaved"));
            response.put("tomorrowSuggestions", result.get("tomorrowSuggestions"));
            response.put("progressRecord", result.get("progressRecord"));
            
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
