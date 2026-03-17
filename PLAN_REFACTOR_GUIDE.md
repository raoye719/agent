# 学习规划系统重构指南

## 📋 概述

学习规划系统已经完成重构，核心改进包括：

1. **提示词统一管理** - 所有提示词集中在 `PlanPromptConfig.java`
2. **上下文统一构建** - 数据格式化统一在 `PlanContextBuilder.java`
3. **流程由 AI 驱动** - 代码只负责数据流转，AI 负责决策
4. **降低成本** - 不使用 Tool Calling，每个流程只需一次 LLM 调用

---

## 🏗️ 架构变化

### 旧架构（问题）
```
代码定死流程 → 硬性判断完成度 → 机械调整任务
  ❌ 提示词散落各处
  ❌ 流程不灵活
  ❌ 无法处理混沌逻辑
```

### 新架构（改进）
```
获取数据 → 构建上下文 → AI 生成决策 → 保存结果
  ✅ 提示词统一管理
  ✅ 流程由 AI 驱动
  ✅ 能处理各种混沌逻辑
  ✅ 成本低（一次调用）
```

---

## 📁 文件结构

```
plan/
├── config/
│   ├── PlanPromptConfig.java      # 统一的提示词配置
│   └── PlanContextBuilder.java    # 上下文构建器
├── agent/
│   └── StudyPlanAgent.java        # 规划智能体（重构版）
├── manager/
│   └── StudyStateManager.java     # 状态管理（保持不变）
└── model/
    ├── DailyPlan.java
    ├── WeeklyPlan.java
    └── StudyTask.java
```

---

## 🔧 核心组件

### 1. PlanPromptConfig - 提示词配置

**作用**：集中管理所有 AI 提示词

**包含的提示词**：
- `SYSTEM_ROLE` - 系统角色定义
- `MORNING_ROUTINE_PROMPT` - 早上流程提示词
- `EVENING_ROUTINE_PROMPT` - 晚上流程提示词
- `WEEKLY_SUMMARY_PROMPT` - 周总结提示词

**使用方式**：
```java
// 获取完整的早上流程提示词
String prompt = PlanPromptConfig.getMorningPrompt(
    weeklyContext,      // 周计划上下文
    yesterdayContext,   // 昨日进度上下文
    studentStatus       // 学生状态
);
```

### 2. PlanContextBuilder - 上下文构建器

**作用**：将数据格式化为 AI 可以理解的上下文

**主要方法**：
- `buildWeeklyPlanContext()` - 构建周计划上下文
- `buildYesterdayProgressContext()` - 构建昨日进度上下文
- `buildStudentStatusContext()` - 构建学生状态上下文
- `buildTodayPlanContext()` - 构建今日计划上下文
- `buildCompletionMetricsContext()` - 构建完成情况指标上下文
- `buildWeeklyMetricsContext()` - 构建周学习数据上下文
- `buildDailyProgressListContext()` - 构建日进度列表上下文

**使用方式**：
```java
// 构建早上流程的完整上下文
String prompt = contextBuilder.buildMorningContext(
    weeklyPlan,      // 周计划对象
    yesterdayPlan,   // 昨日计划对象
    studentStatus    // 学生状态字符串
);
```

### 3. StudyPlanAgent - 规划智能体

**作用**：执行三个核心流程

**三个核心方法**：

#### 早上流程
```java
public String morningRoutine(LocalDate today, String studentStatus)
```

**参数**：
- `today` - 日期
- `studentStatus` - 学生状态（如"今天不想学"、"状态很好"等）

**流程**：
1. 获取周计划和昨日进度
2. 构建上下文
3. 调用 AI 生成今日计划（一次调用）
4. 解析并保存计划

**返回**：AI 生成的今日计划（Markdown 格式）

#### 晚上流程
```java
public String eveningRoutine(LocalDate today, String studentReport)
```

**参数**：
- `today` - 日期
- `studentReport` - 学生的完成情况反馈（自由表述）

**流程**：
1. 获取今日计划和周学习统计
2. 构建上下文
3. 调用 AI 生成反馈和明日建议（一次调用）
4. 记录进度

**返回**：AI 生成的反馈和明日建议（Markdown 格式）

#### 周总结流程
```java
public String weeklySummary()
```

**流程**：
1. 获取周计划、周学习统计和所有日进度
2. 构建上下文
3. 调用 AI 生成周总结（一次调用）

**返回**：AI 生成的周总结（Markdown 格式）

---

## 🚀 API 使用示例

### 1. 早上流程 - 制定今日计划

**请求**：
```bash
POST /ai/plan/daily/morning?date=2026-03-18

{
  "studentStatus": "今天状态不太好，有点累"
}
```

**响应**：
```json
{
  "success": true,
  "date": "2026-03-18",
  "plan": "# 📅 2026-03-18 今日学习计划\n\n## 📊 计划概览\n..."
}
```

### 2. 晚上流程 - 记录进度和生成反馈

**请求**：
```bash
POST /ai/plan/daily/evening?date=2026-03-18

{
  "studentReport": "今天完成了数学和英语，专业课没做。数学做了30讲第一讲和660题，英语背了80个单词。感觉有点累，效率不太高。"
}
```

**响应**：
```json
{
  "success": true,
  "date": "2026-03-18",
  "feedback": "# 📊 2026-03-18 学习反馈与明日建议\n\n## ✅ 今日学习评价\n..."
}
```

### 3. 周总结 - 生成周总结

**请求**：
```bash
GET /ai/plan/weekly/summary
```

**响应**：
```json
{
  "success": true,
  "summary": "# 📊 第 X 周（2026.03.17-2026.03.25）学习总结\n\n## 📈 本周学习概览\n..."
}
```

---

## 💡 关键特性

### 1. 动态规划

AI 会根据学生的实际情况动态调整计划：

- **完成度 > 80%** → 按原计划推进，可适度增加难度
- **完成度 50-80%** → 保持任务量，优化学习方法
- **完成度 < 50%** → 减少任务量，补充昨日内容

### 2. 混沌逻辑处理

AI 能理解和处理各种混沌逻辑：

- 学生说"今天不想学" → 制定轻松计划
- 学生说"某科效率低" → 调整该科学习方法
- 学生说"状态很好" → 可以增加任务量
- 学生说"时间紧张" → 优化时间分配

### 3. 低成本

每个流程只需要一次 LLM 调用：

- 早上流程：1 次调用
- 晚上流程：1 次调用
- 周总结：1 次调用

**不使用 Tool Calling**，避免多轮调用的成本。

---

## 📊 数据流

### 早上流程数据流
```
周计划 + 昨日进度 + 学生状态
    ↓
PlanContextBuilder 格式化
    ↓
PlanPromptConfig 构建提示词
    ↓
ChatClient 调用 AI（一次）
    ↓
AI 生成今日计划
    ↓
StudyStateManager 保存计划
```

### 晚上流程数据流
```
今日计划 + 完成情况 + 学生反馈
    ↓
PlanContextBuilder 格式化
    ↓
PlanPromptConfig 构建提示词
    ↓
ChatClient 调用 AI（一次）
    ↓
AI 生成反馈和建议
    ↓
StudyStateManager 记录进度
```

---

## 🔄 工作流程示例

### 完整的一天流程

**早上 8:00**
```
学生：早上好，我今天状态不太好，有点累
系统：调用 morningRoutine("2026-03-18", "今天状态不太好，有点累")
AI：根据周计划、昨日进度和学生状态，生成轻松的今日计划
系统：保存计划，返回给学生
```

**晚上 22:00**
```
学生：今天完成了数学和英语，专业课没做。数学做了30讲第一讲和660题，英语背了80个单词。感觉有点累，效率不太高。
系统：调用 eveningRoutine("2026-03-18", "今天完成了数学和英语...")
AI：分析完成情况，生成反馈和明日建议
系统：记录进度，返回反馈给学生
```

**周日 20:00**
```
系统：调用 weeklySummary()
AI：分析整周的学习数据，生成周总结和下周改进方案
系统：返回周总结给学生
```

---

## 🎯 优化建议

### 1. 提示词优化

如果 AI 的决策不够理想，可以在 `PlanPromptConfig` 中调整提示词：

- 增加更多的规则和约束
- 提供更多的示例
- 调整语气和风格

### 2. 上下文优化

如果 AI 缺少某些信息，可以在 `PlanContextBuilder` 中添加更多的上下文：

- 添加学生的学习历史
- 添加科目的难度等级
- 添加学生的学习风格

### 3. 结果解析

目前 `parsePlanFromAI()` 是一个简单的实现，可以改进为：

- 使用正则表达式解析 AI 生成的计划
- 调用 AI 进行结构化提取
- 实现更复杂的验证逻辑

---

## 📝 注意事项

1. **学生状态很重要** - 早上流程中的 `studentStatus` 参数很关键，学生应该真实表述自己的状态
2. **反馈要详细** - 晚上流程中的 `studentReport` 应该尽可能详细，包括完成情况、遇到的问题等
3. **周计划要完整** - 周计划应该包含所有的任务和目标，AI 会基于此进行调整
4. **定期总结** - 周总结很重要，可以帮助发现学习模式和问题

---

## 🔗 相关文件

- `PlanPromptConfig.java` - 提示词配置
- `PlanContextBuilder.java` - 上下文构建器
- `StudyPlanAgent.java` - 规划智能体
- `PlanController.java` - API 控制器
- `StudyStateManager.java` - 状态管理器
