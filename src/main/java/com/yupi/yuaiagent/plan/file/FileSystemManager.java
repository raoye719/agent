package com.yupi.yuaiagent.plan.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 文件系统管理器
 * 负责读写周计划和进度记录文件
 */
@Component
@Slf4j
public class FileSystemManager {
    
    @Value("${study.workspace.path:./workspace}")
    private String workspacePath;
    
    private static final String WEEKLY_PLAN_DIR = "周计划";
    private static final String STUDY_RECORDS_DIR = "study_records";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * 初始化工作目录
     */
    public void initializeWorkspace() {
        try {
            Path workspace = Paths.get(workspacePath);
            Path weeklyPlanDir = workspace.resolve(WEEKLY_PLAN_DIR);
            Path studyRecordsDir = workspace.resolve(STUDY_RECORDS_DIR);
            
            Files.createDirectories(weeklyPlanDir);
            Files.createDirectories(studyRecordsDir);
            
            log.info("工作目录已初始化：{}", workspacePath);
        } catch (IOException e) {
            log.error("初始化工作目录失败", e);
            throw new RuntimeException("无法创建工作目录", e);
        }
    }
    
    /**
     * 读取周计划文件
     * 查找最新的周计划文件
     */
    public String readWeeklyPlan(LocalDate date) {
        try {
            Path weeklyPlanDir = Paths.get(workspacePath, WEEKLY_PLAN_DIR);
            
            // 查找包含该日期的周计划文件
            File[] files = weeklyPlanDir.toFile().listFiles((dir, name) -> 
                name.endsWith("周计划.md")
            );
            
            if (files == null || files.length == 0) {
                log.warn("未找到周计划文件");
                return null;
            }
            
            // 返回最新的周计划文件
            File latestFile = files[files.length - 1];
            String content = new String(Files.readAllBytes(latestFile.toPath()), StandardCharsets.UTF_8);
            log.info("已读取周计划文件：{}", latestFile.getName());
            return content;
        } catch (IOException e) {
            log.error("读取周计划文件失败", e);
            return null;
        }
    }
    
    /**
     * 读取昨日进度记录
     */
    public String readYesterdayProgress(LocalDate today) {
        LocalDate yesterday = today.minusDays(1);
        return readProgressRecord(yesterday);
    }
    
    /**
     * 读取指定日期的进度记录
     */
    public String readProgressRecord(LocalDate date) {
        try {
            String filename = date.format(DATE_FORMATTER) + "_学习计划完成情况.md";
            Path filePath = Paths.get(workspacePath, STUDY_RECORDS_DIR, filename);
            
            if (!Files.exists(filePath)) {
                log.info("进度记录文件不存在：{}", filename);
                return null;
            }
            
            String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            log.info("已读取进度记录：{}", filename);
            return content;
        } catch (IOException e) {
            log.error("读取进度记录失败", e);
            return null;
        }
    }
    
    /**
     * 写入进度记录文件
     */
    public boolean writeProgressRecord(LocalDate date, String content) {
        try {
            String filename = date.format(DATE_FORMATTER) + "_学习进度记录.md";
            Path filePath = Paths.get(workspacePath, STUDY_RECORDS_DIR, filename);
            
            Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
            log.info("已保存进度记录：{}", filename);
            return true;
        } catch (IOException e) {
            log.error("写入进度记录失败", e);
            return false;
        }
    }
    
    /**
     * 获取工作目录路径
     */
    public String getWorkspacePath() {
        return workspacePath;
    }
    
    /**
     * 获取周计划目录路径
     */
    public String getWeeklyPlanDirPath() {
        return Paths.get(workspacePath, WEEKLY_PLAN_DIR).toString();
    }
    
    /**
     * 获取进度记录目录路径
     */
    public String getStudyRecordsDirPath() {
        return Paths.get(workspacePath, STUDY_RECORDS_DIR).toString();
    }
    
    /**
     * 写入周计划文件
     */
    public boolean writeWeeklyPlan(String filename, String content) {
        try {
            Path filePath = Paths.get(workspacePath, WEEKLY_PLAN_DIR, filename);
            Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
            log.info("已保存周计划：{}", filename);
            return true;
        } catch (IOException e) {
            log.error("写入周计划失败", e);
            return false;
        }
    }
    
    /**
     * 读取今日计划和完成情况文件
     */
    public String readDailyPlanAndProgress(LocalDate date) {
        try {
            String filename = date.format(DATE_FORMATTER) + "_学习计划完成情况.md";
            Path filePath = Paths.get(workspacePath, STUDY_RECORDS_DIR, filename);
            
            if (!Files.exists(filePath)) {
                log.info("计划和完成情况文件不存在：{}", filename);
                return null;
            }
            
            String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            log.info("已读取计划和完成情况：{}", filename);
            return content;
        } catch (IOException e) {
            log.error("读取计划和完成情况失败", e);
            return null;
        }
    }
    
    /**
     * 写入今日计划和完成情况文件
     */
    public boolean writeDailyPlanAndProgress(LocalDate date, String content) {
        try {
            String filename = date.format(DATE_FORMATTER) + "_学习计划完成情况.md";
            Path filePath = Paths.get(workspacePath, STUDY_RECORDS_DIR, filename);
            Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
            log.info("已保存计划和完成情况：{}", filename);
            return true;
        } catch (IOException e) {
            log.error("写入计划和完成情况失败", e);
            return false;
        }
    }
    
    /**
     * 查找包含指定日期的周计划文件
     * 根据日期自动查找对应的周计划
     */
    public String findWeeklyPlanByDate(LocalDate date) {
        try {
            Path weeklyPlanDir = Paths.get(workspacePath, WEEKLY_PLAN_DIR);
            
            File[] files = weeklyPlanDir.toFile().listFiles((dir, name) -> 
                name.endsWith("周计划.md")
            );
            
            if (files == null || files.length == 0) {
                log.warn("未找到周计划文件");
                return null;
            }
            
            // 遍历所有周计划文件，查找包含该日期的文件
            for (File file : files) {
                String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                // 检查文件名中的日期范围
                if (isDateInWeeklyPlan(file.getName(), date)) {
                    log.info("找到包含日期{}的周计划：{}", date, file.getName());
                    return content;
                }
            }
            
            log.warn("未找到包含日期{}的周计划", date);
            return null;
        } catch (IOException e) {
            log.error("查找周计划失败", e);
            return null;
        }
    }
    
    /**
     * 检查日期是否在周计划的日期范围内
     */
    private boolean isDateInWeeklyPlan(String filename, LocalDate date) {
        // 文件名格式：第X周（YYYY-MM-DD-YYYY-MM-DD）周计划.md
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("（(\\d{4}-\\d{2}-\\d{2})-(\\d{4}-\\d{2}-\\d{2})）");
            java.util.regex.Matcher matcher = pattern.matcher(filename);
            
            if (matcher.find()) {
                LocalDate startDate = LocalDate.parse(matcher.group(1));
                LocalDate endDate = LocalDate.parse(matcher.group(2));
                
                return !date.isBefore(startDate) && !date.isAfter(endDate);
            }
        } catch (Exception e) {
            log.error("解析周计划日期范围失败", e);
        }
        
        return false;
    }
}
