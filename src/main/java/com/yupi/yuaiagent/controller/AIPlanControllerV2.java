package com.yupi.yuaiagent.controller;

import com.yupi.yuaiagent.plan.file.FileSystemManager;
import com.yupi.yuaiagent.plan.generator.PlanGenerator;
import com.yupi.yuaiagent.plan.parser.ProgressParser;
import com.yupi.yuaiagent.plan.service.WeeklyPlanService;
import com.yupi.yuaiagent.plan.service.MorningRoutineService;
import com.yupi.yuaiagent.plan.service.EveningRoutineService;
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
    private ProgressParser progressParser;
    
    @Resource
    private WeeklyPlanService weeklyPlanService;
    
    @Resource
    private MorningRoutineService morningRoutineService;
    
    @Resource
    private EveningRoutineService eveningRoutineService;
    
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
     * 支持两种方式：
     * 1. 纯文本：直接发送周计划描述（Content-Type: text/plain）
     * 2. JSON格式：{"weekDescription": "...", "startDate": "2026-03-10", "endDate": "2026-03-16"}
     */
    @PostMapping("/weekly/create")
    public Map<String, Object> createWeeklyPlan(
            @RequestBody(required = false) String bodyText,
            @RequestParam(required = false) String weekDescription,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Map<String, Object> response = new HashMap<>();
        try {
            String description = null;
            String startDateStr = startDate;
            String endDateStr = endDate;
            
            // 方式1：从请求体获取（可能是JSON或纯文本）
            if (bodyText != null && !bodyText.trim().isEmpty()) {
                try {
                    // 尝试解析为JSON
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, Object> jsonMap = mapper.readValue(bodyText, Map.class);
                    description = (String) jsonMap.get("weekDescription");
                    startDateStr = (String) jsonMap.get("startDate");
                    endDateStr = (String) jsonMap.get("endDate");
                } catch (Exception e) {
                    // 如果JSON解析失败，直接当作纯文本周计划描述
                    description = bodyText;
                }
            }
            
            // 方式2：从查询参数获取
            if (description == null && weekDescription != null) {
                description = weekDescription;
            }
            
            if (description == null || description.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "❌ 周计划描述不能为空");
                return response;
            }
            
            LocalDate startDateObj = startDateStr != null ? LocalDate.parse(startDateStr) : LocalDate.now();
            LocalDate endDateObj = endDateStr != null ? LocalDate.parse(endDateStr) : startDateObj.plusDays(6);
            
            // 调用服务创建周计划
            Map<String, Object> result = weeklyPlanService.createWeeklyPlan(description, startDateObj, endDateObj);
            
            return result;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "❌ 创建周计划失败：" + e.getMessage());
            return response;
        }
    }
    
    /**
     * 早上流程：生成今日计划
     * 简单到极致：只需要调用，自动用当天日期
     * AI负责处理一切：读周计划、读昨天进度、生成计划、保存文件
     */
    @GetMapping("/daily/morning")
    public Map<String, Object> morningRoutine(@RequestParam(required = false) String date) {
        Map<String, Object> response = new HashMap<>();
        try {
            LocalDate today = date != null ? LocalDate.parse(date) : LocalDate.now();
            
            // 调用AI驱动的早上流程服务
            Map<String, Object> result = morningRoutineService.processMorningRoutine(today);
            
            return result;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "❌ 生成计划失败：" + e.getMessage());
            return response;
        }
    }
    
    /**
     * 晚上流程：上报学习进度
     * 简单到极致：只接收纯文本进度报告，自动用当天日期
     * AI负责处理一切：读文件、理解进度、组织语言、写文件
     */
    @PostMapping("/daily/evening")
    public Map<String, Object> eveningRoutine(
            org.springframework.http.HttpEntity<String> httpEntity) {
        Map<String, Object> response = new HashMap<>();
        try {
            LocalDate today = LocalDate.now();
            String progressReport = httpEntity.getBody();
            
            if (progressReport == null || progressReport.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "❌ 进度报告不能为空");
                return response;
            }
            
            // 调用AI驱动的晚上流程服务
            Map<String, Object> result = eveningRoutineService.processEveningRoutine(today, progressReport);
            
            return result;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "❌ 处理失败：" + e.getMessage());
            return response;
        }
    }
    
    /**
     * 读取周计划
     * 自动查找包含该日期的周计划（无需手动输入日期）
     */
    @GetMapping("/weekly/read")
    public Map<String, Object> readWeeklyPlan(@RequestParam(required = false) String date) {
        Map<String, Object> response = new HashMap<>();
        try {
            LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
            String weeklyPlan = fileSystemManager.findWeeklyPlanByDate(targetDate);
            
            if (weeklyPlan == null) {
                response.put("success", false);
                response.put("message", "❌ 未找到包含该日期的周计划");
                return response;
            }
            
            response.put("success", true);
            response.put("date", targetDate);
            response.put("weeklyPlan", weeklyPlan);
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "❌ 读取周计划失败：" + e.getMessage());
            return response;
        }
    }
    
    /**
     * 查询历史周计划
     * 可以手动输入日期查询特定的周计划
     */
    @GetMapping("/weekly/history")
    public Map<String, Object> getHistoryWeeklyPlan(@RequestParam String date) {
        Map<String, Object> response = new HashMap<>();
        try {
            LocalDate targetDate = LocalDate.parse(date);
            String weeklyPlan = fileSystemManager.findWeeklyPlanByDate(targetDate);
            
            if (weeklyPlan == null) {
                response.put("success", false);
                response.put("message", "❌ 未找到包含该日期的周计划");
                return response;
            }
            
            response.put("success", true);
            response.put("date", targetDate);
            response.put("weeklyPlan", weeklyPlan);
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "❌ 查询历史周计划失败：" + e.getMessage());
            return response;
        }
    }
    
    /**
     * 读取今日计划和完成情况
     */
    @GetMapping("/daily/plan-and-progress")
    public Map<String, Object> readDailyPlanAndProgress(@RequestParam(required = false) String date) {
        Map<String, Object> response = new HashMap<>();
        try {
            LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
            String content = fileSystemManager.readDailyPlanAndProgress(targetDate);
            
            if (content == null) {
                response.put("success", false);
                response.put("message", "❌ 未找到该日期的计划和完成情况记录");
                return response;
            }
            
            response.put("success", true);
            response.put("date", targetDate);
            response.put("content", content);
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "❌ 读取计划和完成情况失败：" + e.getMessage());
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
