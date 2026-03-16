package com.yupi.yuaiagent.plan;

import com.yupi.yuaiagent.plan.adjuster.DynamicPlanAdjuster;
import com.yupi.yuaiagent.plan.agent.StudyPlanAgent;
import com.yupi.yuaiagent.plan.manager.StudyStateManager;
import com.yupi.yuaiagent.plan.model.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 动态规划系统测试
 */
@SpringBootTest
class DynamicPlanTest {
    
    @Resource
    private StudyStateManager stateManager;
    
    @Resource
    private DynamicPlanAdjuster planAdjuster;
    
    @Resource
    private StudyPlanAgent studyPlanAgent;
    
    private WeeklyPlan weeklyPlan;
    private DailyPlan dailyPlan;
    
    @BeforeEach
    void setUp() {
        // 创建周计划
        weeklyPlan = WeeklyPlan.builder()
                .weekPlanId("week-001")
                .weekName("第一周 2026.3.10-2026.3.16")
                .startDate(LocalDate.of(2026, 3, 10))
                .endDate(LocalDate.of(2026, 3, 16))
                .plannedTotalHours(50)
                .actualTotalHours(0)
                .completionRate(0)
                .build();
        
        // 创建日计划
        LocalDate today = LocalDate.now();
        List<StudyTask> tasks = createSampleTasks();
        
        dailyPlan = DailyPlan.builder()
                .planDate(today)
                .planName("2026-03-16 学习计划")
                .tasks(tasks)
                .plannedTotalHours(8)
                .actualTotalHours(0)
                .completionRate(0)
                .reported(false)
                .build();
        
        weeklyPlan.setDailyPlans(Arrays.asList(dailyPlan));
    }
    
    /**
     * 创建示例任务
     */
    private List<StudyTask> createSampleTasks() {
        List<StudyTask> tasks = new ArrayList<>();
        
        // 数学任务
        tasks.add(StudyTask.builder()
                .taskId("task-001")
                .taskName("泰勒公式复习")
                .subject("数学")
                .plannedHours(2)
                .actualHours(0)
                .status(TaskStatus.NOT_STARTED)
                .completionRate(0)
                .priority(5)
                .postponed(false)
                .build());
        
        tasks.add(StudyTask.builder()
                .taskId("task-002")
                .taskName("做数学真题（2020年）")
                .subject("数学")
                .plannedHours(2)
                .actualHours(0)
                .status(TaskStatus.NOT_STARTED)
                .completionRate(0)
                .priority(4)
                .postponed(false)
                .build());
        
        // 英语任务
        tasks.add(StudyTask.builder()
                .taskId("task-003")
                .taskName("英语阅读理解")
                .subject("英语")
                .plannedHours(2)
                .actualHours(0)
                .status(TaskStatus.NOT_STARTED)
                .completionRate(0)
                .priority(4)
                .postponed(false)
                .build());
        
        // 政治任务
        tasks.add(StudyTask.builder()
                .taskId("task-004")
                .taskName("政治知识点总结")
                .subject("政治")
                .plannedHours(2)
                .actualHours(0)
                .status(TaskStatus.NOT_STARTED)
                .completionRate(0)
                .priority(3)
                .postponed(false)
                .build());
        
        return tasks;
    }
    
    /**
     * 测试1：初始化周计划和日计划
     */
    @Test
    void testInitializePlans() {
        stateManager.initializeWeeklyPlan(weeklyPlan);
        stateManager.initializeDailyPlan(dailyPlan);
        
        assertNotNull(stateManager.getCurrentWeeklyPlan());
        assertNotNull(stateManager.getCurrentDailyPlan());
        assertEquals("第一周 2026.3.10-2026.3.16", stateManager.getCurrentWeeklyPlan().getWeekName());
        assertEquals(4, stateManager.getCurrentDailyPlan().getTasks().size());
    }
    
    /**
     * 测试2：更新任务状态
     */
    @Test
    void testUpdateTaskStatus() {
        stateManager.initializeDailyPlan(dailyPlan);
        
        // 更新第一个任务为已完成
        stateManager.updateTaskStatus("task-001", TaskStatus.COMPLETED, 2.0, 100);
        
        StudyTask task = stateManager.getCurrentDailyPlan().getTasks().get(0);
        assertEquals(TaskStatus.COMPLETED, task.getStatus());
        assertEquals(100, task.getCompletionRate());
        assertEquals(2.0, task.getActualHours());
    }
    
    /**
     * 测试3：计算日计划完成度
     */
    @Test
    void testCalculateDailyCompletion() {
        stateManager.initializeDailyPlan(dailyPlan);
        
        // 更新所有任务
        stateManager.updateTaskStatus("task-001", TaskStatus.COMPLETED, 2.0, 100);
        stateManager.updateTaskStatus("task-002", TaskStatus.COMPLETED, 2.0, 100);
        stateManager.updateTaskStatus("task-003", TaskStatus.IN_PROGRESS, 1.0, 50);
        stateManager.updateTaskStatus("task-004", TaskStatus.NOT_STARTED, 0, 0);
        
        int completion = stateManager.getCurrentDailyPlan().getCompletionRate();
        assertEquals(62, completion); // (100+100+50+0)/4 = 62.5 -> 62
    }
    
    /**
     * 测试4：获取未完成的任务
     */
    @Test
    void testGetIncompleteTasks() {
        stateManager.initializeDailyPlan(dailyPlan);
        
        stateManager.updateTaskStatus("task-001", TaskStatus.COMPLETED, 2.0, 100);
        stateManager.updateTaskStatus("task-002", TaskStatus.COMPLETED, 2.0, 100);
        
        List<StudyTask> incompleteTasks = stateManager.getIncompleteTasks();
        assertEquals(2, incompleteTasks.size());
    }
    
    /**
     * 测试5：按科目分类任务
     */
    @Test
    void testGetTasksBySubject() {
        stateManager.initializeDailyPlan(dailyPlan);
        
        Map<String, List<StudyTask>> tasksBySubject = stateManager.getTasksBySubject();
        
        assertEquals(3, tasksBySubject.size());
        assertEquals(2, tasksBySubject.get("数学").size());
        assertEquals(1, tasksBySubject.get("英语").size());
        assertEquals(1, tasksBySubject.get("政治").size());
    }
    
    /**
     * 测试6：动态调整计划 - 完成度好
     */
    @Test
    void testAdjustPlanGoodCompletion() {
        // 昨天完成度好（90%）
        DailyPlan yesterdayPlan = DailyPlan.builder()
                .planDate(LocalDate.now().minusDays(1))
                .completionRate(90)
                .tasks(createSampleTasks())
                .build();
        
        DailyPlan todayOriginalPlan = DailyPlan.builder()
                .planDate(LocalDate.now())
                .tasks(createSampleTasks())
                .plannedTotalHours(8)
                .build();
        
        DailyPlan adjustedPlan = planAdjuster.adjustTodayPlan(todayOriginalPlan, yesterdayPlan);
        
        // 完成度好，应该保持原计划
        assertEquals(4, adjustedPlan.getTasks().size());
        assertEquals(8, adjustedPlan.getPlannedTotalHours());
    }
    
    /**
     * 测试7：动态调整计划 - 完成度差
     */
    @Test
    void testAdjustPlanPoorCompletion() {
        // 昨天完成度差（30%）
        DailyPlan yesterdayPlan = DailyPlan.builder()
                .planDate(LocalDate.now().minusDays(1))
                .completionRate(30)
                .tasks(createSampleTasks())
                .build();
        
        // 标记一些任务为未完成
        yesterdayPlan.getTasks().get(0).setStatus(TaskStatus.NOT_STARTED);
        yesterdayPlan.getTasks().get(1).setStatus(TaskStatus.NOT_STARTED);
        
        DailyPlan todayOriginalPlan = DailyPlan.builder()
                .planDate(LocalDate.now())
                .tasks(createSampleTasks())
                .plannedTotalHours(8)
                .build();
        
        DailyPlan adjustedPlan = planAdjuster.adjustTodayPlan(todayOriginalPlan, yesterdayPlan);
        
        // 完成度差，应该包含顺延任务
        assertTrue(adjustedPlan.getTasks().stream().anyMatch(StudyTask::isPostponed));
    }
    
    /**
     * 测试8：生成调整建议
     */
    @Test
    void testGenerateAdjustmentSuggestions() {
        DailyPlan plan = DailyPlan.builder()
                .completionRate(75)
                .tasks(createSampleTasks())
                .plannedTotalHours(8)
                .actualTotalHours(6)
                .build();
        
        stateManager.initializeDailyPlan(plan);
        List<String> suggestions = planAdjuster.generateAdjustmentSuggestions(plan);
        
        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.stream().anyMatch(s -> s.contains("建议")));
    }
    
    /**
     * 测试9：记录日计划进度
     */
    @Test
    void testRecordDailyProgress() {
        stateManager.initializeDailyPlan(dailyPlan);
        
        List<String> suggestions = Arrays.asList("建议1", "建议2");
        stateManager.recordDailyProgress(LocalDate.now(), "良好", "无", suggestions);
        
        DailyPlan recorded = stateManager.getDailyPlanHistory().get(LocalDate.now());
        assertNotNull(recorded);
        assertEquals("良好", recorded.getStatusEvaluation());
        assertTrue(recorded.isReported());
    }
    
    /**
     * 测试10：早上流程
     */
    @Test
    void testMorningRoutine() {
        stateManager.initializeWeeklyPlan(weeklyPlan);
        stateManager.initializeDailyPlan(dailyPlan);
        
        String result = studyPlanAgent.morningRoutine(LocalDate.now());
        
        assertNotNull(result);
        assertTrue(result.contains("早上好"));
        assertTrue(result.contains("今日计划预览"));
    }
    
    /**
     * 测试11：晚上流程
     */
    @Test
    void testEveningRoutine() {
        stateManager.initializeDailyPlan(dailyPlan);
        
        String result = studyPlanAgent.eveningRoutine(LocalDate.now(), 6.5, "良好", "无");
        
        assertNotNull(result);
        assertTrue(result.contains("晚上好"));
        assertTrue(result.contains("明日建议"));
    }
    
    /**
     * 测试12：周总结
     */
    @Test
    void testWeeklySummary() {
        stateManager.initializeWeeklyPlan(weeklyPlan);
        
        String summary = studyPlanAgent.weeklySummary();
        
        assertNotNull(summary);
        assertTrue(summary.contains("本周学习总结"));
        assertTrue(summary.contains("下周改进方案"));
    }
}
