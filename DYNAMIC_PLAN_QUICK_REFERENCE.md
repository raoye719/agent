# 动态规划系统 - 快速参考

## 🎯 核心概念

### 普通聊天模型 vs 我们的系统

```
普通模型：
用户：怎么学数学？
模型：建议1、建议2、建议3...（一次性回答）

我们的系统：
用户：帮我制定考研计划
系统：
  早上：根据昨天进度制定今天的具体计划
  晚上：记录今天的完成情况
  明天：根据今天的完成度调整明天的计划
  周末：总结本周，制定下周改进方案
```

## 📋 三大流程

### 早上流程（7:30）
```
读取周计划 → 读取昨日进度 → 制定今日计划 → 发送预览
```

### 晚上流程（22:00）
```
提醒上报 → 记录进度 → 调整明日计划
```

### 周总结（周日）
```
汇总周进度 → 分析完成情况 → 制定下周方案
```

## 🔄 动态调整规则

| 昨日完成度 | 今日调整 |
|----------|--------|
| ≥80% | ✅ 按原计划推进 |
| 50-80% | ⚠️ 减少任务量（80%） |
| <50% | ❌ 先补昨天，减少新任务（50%） |

## 🛠️ 核心类

### StudyStateManager（状态管理）
```java
// 初始化
initializeWeeklyPlan(weeklyPlan)
initializeDailyPlan(dailyPlan)

// 更新
updateTaskStatus(taskId, status, hours, rate)
recordDailyProgress(date, evaluation, problems, suggestions)

// 查询
getYesterdayProgress(today)
getIncompleteTasks()
getTasksBySubject()
getWeeklyStatistics()
```

### DynamicPlanAdjuster（动态调整）
```java
// 调整计划
adjustTodayPlan(originalPlan, yesterdayPlan)

// 生成建议
generateAdjustmentSuggestions(dailyPlan)
generateWeeklyImprovements()

// 生成总结
generateWeeklySummary()
```

### StudyPlanAgent（规划智能体）
```java
// 三大流程
morningRoutine(today)
eveningRoutine(today, studyHours, evaluation, problems)
weeklySummary()
```

## 📡 API 速查表

| 功能 | 接口 | 方法 |
|------|------|------|
| 初始化周计划 | /weekly/init | POST |
| 早上流程 | /daily/morning | GET |
| 晚上流程 | /daily/evening | POST |
| 周总结 | /weekly/summary | GET |
| 更新任务 | /task/update | POST |
| 获取未完成任务 | /task/incomplete | GET |
| 周统计 | /statistics/weekly | GET |
| 学习效率 | /statistics/efficiency | GET |

## 📊 数据模型

### StudyTask（任务）
```json
{
  "taskId": "task-001",
  "taskName": "泰勒公式复习",
  "subject": "数学",
  "plannedHours": 2.0,
  "actualHours": 1.5,
  "status": "IN_PROGRESS",
  "completionRate": 75,
  "priority": 5,
  "postponed": false
}
```

### DailyPlan（日计划）
```json
{
  "planDate": "2026-03-16",
  "tasks": [...],
  "plannedTotalHours": 8.0,
  "actualTotalHours": 6.5,
  "completionRate": 75,
  "statusEvaluation": "良好"
}
```

### WeeklyPlan（周计划）
```json
{
  "weekName": "第一周 2026.3.10-2026.3.16",
  "dailyPlans": [...],
  "completionRate": 82,
  "plannedTotalHours": 50.0,
  "actualTotalHours": 41.0
}
```

## 🚀 快速开始

### 1. 初始化周计划
```bash
curl -X POST http://localhost:8123/api/ai/plan/weekly/init \
  -H "Content-Type: application/json" \
  -d '{
    "weekPlanId": "week-001",
    "weekName": "第一周 2026.3.10-2026.3.16",
    "startDate": "2026-03-10",
    "endDate": "2026-03-16",
    "plannedTotalHours": 50
  }'
```

### 2. 早上制定计划
```bash
curl http://localhost:8123/api/ai/plan/daily/morning?date=2026-03-16
```

### 3. 晚上记录进度
```bash
curl -X POST "http://localhost:8123/api/ai/plan/daily/evening?date=2026-03-16&studyHours=6.5&statusEvaluation=良好"
```

### 4. 查看周总结
```bash
curl http://localhost:8123/api/ai/plan/weekly/summary
```

## 💡 使用场景

### 场景1：完成度好（90%）
```
昨天：完成度90%
今天：按原计划推进
建议：保持当前节奏，可以增加难度
```

### 场景2：完成度一般（65%）
```
昨天：完成度65%
今天：减少任务量到80%
建议：明天保持相同任务量
```

### 场景3：完成度差（35%）
```
昨天：完成度35%，有2个任务未完成
今天：优先补昨天的任务，减少新任务50%
建议：分析未完成原因，调整学习策略
```

## 📈 关键指标

### 完成度（Completion Rate）
- 定义：已完成任务数 / 总任务数
- 范围：0-100%
- 用途：评估计划执行情况

### 学习效率（Study Efficiency）
- 定义：实际学习时长 / 计划学习时长
- 范围：0-200%+
- 用途：评估时间利用效率

### 周完成度（Weekly Completion）
- 定义：周内各日完成度的平均值
- 范围：0-100%
- 用途：评估周计划执行情况

## 🔧 常见操作

### 更新任务状态
```bash
curl -X POST "http://localhost:8123/api/ai/plan/task/update?taskId=task-001&status=COMPLETED&actualHours=2.0&completionRate=100"
```

### 获取未完成任务
```bash
curl http://localhost:8123/api/ai/plan/task/incomplete
```

### 按科目分类任务
```bash
curl http://localhost:8123/api/ai/plan/task/by-subject
```

### 获取学习效率
```bash
curl http://localhost:8123/api/ai/plan/statistics/efficiency
```

## 📚 文件位置

| 文件 | 位置 |
|------|------|
| 模型类 | src/main/java/com/yupi/yuaiagent/plan/model/ |
| 状态管理 | src/main/java/com/yupi/yuaiagent/plan/manager/ |
| 动态调整 | src/main/java/com/yupi/yuaiagent/plan/adjuster/ |
| 规划智能体 | src/main/java/com/yupi/yuaiagent/plan/agent/ |
| 控制器 | src/main/java/com/yupi/yuaiagent/plan/controller/ |
| 测试 | src/test/java/com/yupi/yuaiagent/plan/ |
| 文档 | DYNAMIC_PLAN_GUIDE.md |

## ✅ 验证清单

- [x] 模型层完成
- [x] 状态管理完成
- [x] 动态调整完成
- [x] 规划智能体完成
- [x] API接口完成
- [x] 测试用例完成
- [x] 文档完成
- [x] 代码质量检查通过

## 🎓 学习资源

- 完整指南：DYNAMIC_PLAN_GUIDE.md
- 完成报告：DYNAMIC_PLAN_COMPLETION.md
- 测试用例：DynamicPlanTest.java
- API文档：PlanController.java

---

**快速参考卡片** | 版本 1.0 | 2026-03-16
