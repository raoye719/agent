# 动态规划系统 - 测试数据示例

## 完整的周计划示例（可直接复制粘贴）

```json
{
  "weekPlanId": "week-001",
  "weekName": "第一周 2026.3.10-2026.3.16",
  "startDate": "2026-03-10",
  "endDate": "2026-03-16",
  "subjectGoals": {
    "数学": "完成泰勒公式和积分应用",
    "英语": "完成阅读理解20篇，掌握2000个单词",
    "政治": "完成马克思主义基本原理第一章"
  },
  "subjectTasks": {
    "数学": [
      {
        "taskId": "task-001",
        "taskName": "泰勒公式复习",
        "description": "复习泰勒公式的推导和应用",
        "subject": "数学",
        "plannedHours": 2.0,
        "actualHours": 0,
        "status": "NOT_STARTED",
        "completionRate": 0,
        "priority": 5,
        "postponed": false,
        "notes": ""
      },
      {
        "taskId": "task-002",
        "taskName": "做数学真题（2020年）",
        "description": "完成2020年考研数学真题",
        "subject": "数学",
        "plannedHours": 2.0,
        "actualHours": 0,
        "status": "NOT_STARTED",
        "completionRate": 0,
        "priority": 4,
        "postponed": false,
        "notes": ""
      }
    ],
    "英语": [
      {
        "taskId": "task-003",
        "taskName": "英语阅读理解",
        "description": "完成5篇英语阅读理解",
        "subject": "英语",
        "plannedHours": 2.0,
        "actualHours": 0,
        "status": "NOT_STARTED",
        "completionRate": 0,
        "priority": 4,
        "postponed": false,
        "notes": ""
      },
      {
        "taskId": "task-004",
        "taskName": "英语单词背诵",
        "description": "背诵200个考研高频单词",
        "subject": "英语",
        "plannedHours": 1.0,
        "actualHours": 0,
        "status": "NOT_STARTED",
        "completionRate": 0,
        "priority": 3,
        "postponed": false,
        "notes": ""
      }
    ],
    "政治": [
      {
        "taskId": "task-005",
        "taskName": "政治知识点总结",
        "description": "总结马克思主义基本原理第一章",
        "subject": "政治",
        "plannedHours": 2.0,
        "actualHours": 0,
        "status": "NOT_STARTED",
        "completionRate": 0,
        "priority": 3,
        "postponed": false,
        "notes": ""
      }
    ]
  },
  "dailyPlans": [
    {
      "planDate": "2026-03-10",
      "planName": "2026-03-10 学习计划",
      "tasks": [
        {
          "taskId": "task-001",
          "taskName": "泰勒公式复习",
          "description": "复习泰勒公式的推导和应用",
          "subject": "数学",
          "plannedHours": 2.0,
          "actualHours": 0,
          "status": "NOT_STARTED",
          "completionRate": 0,
          "priority": 5,
          "postponed": false,
          "notes": ""
        },
        {
          "taskId": "task-003",
          "taskName": "英语阅读理解",
          "description": "完成5篇英语阅读理解",
          "subject": "英语",
          "plannedHours": 2.0,
          "actualHours": 0,
          "status": "NOT_STARTED",
          "completionRate": 0,
          "priority": 4,
          "postponed": false,
          "notes": ""
        },
        {
          "taskId": "task-005",
          "taskName": "政治知识点总结",
          "description": "总结马克思主义基本原理第一章",
          "subject": "政治",
          "plannedHours": 2.0,
          "actualHours": 0,
          "status": "NOT_STARTED",
          "completionRate": 0,
          "priority": 3,
          "postponed": false,
          "notes": ""
        }
      ],
      "plannedTotalHours": 6.0,
      "actualTotalHours": 0,
      "completionRate": 0,
      "statusEvaluation": "",
      "problems": "",
      "tomorrowSuggestions": [],
      "reported": false,
      "notes": ""
    },
    {
      "planDate": "2026-03-11",
      "planName": "2026-03-11 学习计划",
      "tasks": [
        {
          "taskId": "task-002",
          "taskName": "做数学真题（2020年）",
          "description": "完成2020年考研数学真题",
          "subject": "数学",
          "plannedHours": 2.0,
          "actualHours": 0,
          "status": "NOT_STARTED",
          "completionRate": 0,
          "priority": 4,
          "postponed": false,
          "notes": ""
        },
        {
          "taskId": "task-004",
          "taskName": "英语单词背诵",
          "description": "背诵200个考研高频单词",
          "subject": "英语",
          "plannedHours": 1.0,
          "actualHours": 0,
          "status": "NOT_STARTED",
          "completionRate": 0,
          "priority": 3,
          "postponed": false,
          "notes": ""
        }
      ],
      "plannedTotalHours": 3.0,
      "actualTotalHours": 0,
      "completionRate": 0,
      "statusEvaluation": "",
      "problems": "",
      "tomorrowSuggestions": [],
      "reported": false,
      "notes": ""
    }
  ],
  "plannedTotalHours": 50.0,
  "actualTotalHours": 0,
  "completionRate": 0,
  "progressTracking": {
    "数学": 0,
    "英语": 0,
    "政治": 0
  },
  "weeklySummary": "",
  "nextWeekImprovements": []
}
```

## 使用方法

### 1. 初始化周计划（POST请求）

**URL**: `http://localhost:8123/api/ai/plan/weekly/init`

**Headers**:
```
Content-Type: application/json
```

**Body**: 直接复制上面的JSON

**cURL命令**:
```bash
curl -X POST http://localhost:8123/api/ai/plan/weekly/init \
  -H "Content-Type: application/json" \
  -d '{
  "weekPlanId": "week-001",
  "weekName": "第一周 2026.3.10-2026.3.16",
  "startDate": "2026-03-10",
  "endDate": "2026-03-16",
  "subjectGoals": {
    "数学": "完成泰勒公式和积分应用",
    "英语": "完成阅读理解20篇，掌握2000个单词",
    "政治": "完成马克思主义基本原理第一章"
  },
  "subjectTasks": {
    "数学": [
      {
        "taskId": "task-001",
        "taskName": "泰勒公式复习",
        "description": "复习泰勒公式的推导和应用",
        "subject": "数学",
        "plannedHours": 2.0,
        "actualHours": 0,
        "status": "NOT_STARTED",
        "completionRate": 0,
        "priority": 5,
        "postponed": false,
        "notes": ""
      },
      {
        "taskId": "task-002",
        "taskName": "做数学真题（2020年）",
        "description": "完成2020年考研数学真题",
        "subject": "数学",
        "plannedHours": 2.0,
        "actualHours": 0,
        "status": "NOT_STARTED",
        "completionRate": 0,
        "priority": 4,
        "postponed": false,
        "notes": ""
      }
    ],
    "英语": [
      {
        "taskId": "task-003",
        "taskName": "英语阅读理解",
        "description": "完成5篇英语阅读理解",
        "subject": "英语",
        "plannedHours": 2.0,
        "actualHours": 0,
        "status": "NOT_STARTED",
        "completionRate": 0,
        "priority": 4,
        "postponed": false,
        "notes": ""
      },
      {
        "taskId": "task-004",
        "taskName": "英语单词背诵",
        "description": "背诵200个考研高频单词",
        "subject": "英语",
        "plannedHours": 1.0,
        "actualHours": 0,
        "status": "NOT_STARTED",
        "completionRate": 0,
        "priority": 3,
        "postponed": false,
        "notes": ""
      }
    ],
    "政治": [
      {
        "taskId": "task-005",
        "taskName": "政治知识点总结",
        "description": "总结马克思主义基本原理第一章",
        "subject": "政治",
        "plannedHours": 2.0,
        "actualHours": 0,
        "status": "NOT_STARTED",
        "completionRate": 0,
        "priority": 3,
        "postponed": false,
        "notes": ""
      }
    ]
  },
  "dailyPlans": [
    {
      "planDate": "2026-03-10",
      "planName": "2026-03-10 学习计划",
      "tasks": [
        {
          "taskId": "task-001",
          "taskName": "泰勒公式复习",
          "description": "复习泰勒公式的推导和应用",
          "subject": "数学",
          "plannedHours": 2.0,
          "actualHours": 0,
          "status": "NOT_STARTED",
          "completionRate": 0,
          "priority": 5,
          "postponed": false,
          "notes": ""
        },
        {
          "taskId": "task-003",
          "taskName": "英语阅读理解",
          "description": "完成5篇英语阅读理解",
          "subject": "英语",
          "plannedHours": 2.0,
          "actualHours": 0,
          "status": "NOT_STARTED",
          "completionRate": 0,
          "priority": 4,
          "postponed": false,
          "notes": ""
        },
        {
          "taskId": "task-005",
          "taskName": "政治知识点总结",
          "description": "总结马克思主义基本原理第一章",
          "subject": "政治",
          "plannedHours": 2.0,
          "actualHours": 0,
          "status": "NOT_STARTED",
          "completionRate": 0,
          "priority": 3,
          "postponed": false,
          "notes": ""
        }
      ],
      "plannedTotalHours": 6.0,
      "actualTotalHours": 0,
      "completionRate": 0,
      "statusEvaluation": "",
      "problems": "",
      "tomorrowSuggestions": [],
      "reported": false,
      "notes": ""
    },
    {
      "planDate": "2026-03-11",
      "planName": "2026-03-11 学习计划",
      "tasks": [
        {
          "taskId": "task-002",
          "taskName": "做数学真题（2020年）",
          "description": "完成2020年考研数学真题",
          "subject": "数学",
          "plannedHours": 2.0,
          "actualHours": 0,
          "status": "NOT_STARTED",
          "completionRate": 0,
          "priority": 4,
          "postponed": false,
          "notes": ""
        },
        {
          "taskId": "task-004",
          "taskName": "英语单词背诵",
          "description": "背诵200个考研高频单词",
          "subject": "英语",
          "plannedHours": 1.0,
          "actualHours": 0,
          "status": "NOT_STARTED",
          "completionRate": 0,
          "priority": 3,
          "postponed": false,
          "notes": ""
        }
      ],
      "plannedTotalHours": 3.0,
      "actualTotalHours": 0,
      "completionRate": 0,
      "statusEvaluation": "",
      "problems": "",
      "tomorrowSuggestions": [],
      "reported": false,
      "notes": ""
    }
  ],
  "plannedTotalHours": 50.0,
  "actualTotalHours": 0,
  "completionRate": 0,
  "progressTracking": {
    "数学": 0,
    "英语": 0,
    "政治": 0
  },
  "weeklySummary": "",
  "nextWeekImprovements": []
}'
```

### 2. 早上流程（GET请求）

**URL**: `http://localhost:8123/api/ai/plan/daily/morning?date=2026-03-10`

**响应示例**:
```
🌅 早上好！开始制定今日学习计划

✅ 步骤1：已读取周计划 - 第一周 2026.3.10-2026.3.16
ℹ️ 步骤2：没有昨日进度记录

📋 步骤3：制定今日计划
📝 步骤4：今日计划预览
================
日期：2026-03-10
计划学习时长：6.0小时

📚 数学
   - 泰勒公式复习 (2.0小时)

📚 英语
   - 英语阅读理解 (2.0小时)

📚 政治
   - 政治知识点总结 (2.0小时)
```

### 3. 更新任务状态（POST请求）

**URL**: `http://localhost:8123/api/ai/plan/task/update`

**参数**:
```
taskId=task-001
status=COMPLETED
actualHours=2.0
completionRate=100
```

**完整URL**:
```
http://localhost:8123/api/ai/plan/task/update?taskId=task-001&status=COMPLETED&actualHours=2.0&completionRate=100
```

### 4. 晚上流程（POST请求）

**URL**: `http://localhost:8123/api/ai/plan/daily/evening`

**参数**:
```
date=2026-03-10
studyHours=5.5
statusEvaluation=良好
problems=数学有些难
```

**完整URL**:
```
http://localhost:8123/api/ai/plan/daily/evening?date=2026-03-10&studyHours=5.5&statusEvaluation=良好&problems=数学有些难
```

**响应示例**:
```
🌙 晚上好！开始记录今日进度

✅ 步骤1：记录今日进度
   - 学习时长：5.5小时
   - 完成度：75%
   - 状态评价：良好

💡 步骤2：明日建议
   👍 今日学习效果良好，继续保持
   📝 建议明天保持相同的任务量

📅 步骤3：明日计划将根据今日完成情况动态调整
   ✅ 今日完成度良好，明日按原计划推进
```

### 5. 获取未完成任务（GET请求）

**URL**: `http://localhost:8123/api/ai/plan/task/incomplete`

**响应示例**:
```json
{
  "success": true,
  "data": [
    {
      "taskId": "task-002",
      "taskName": "做数学真题（2020年）",
      "status": "NOT_STARTED",
      "completionRate": 0
    },
    {
      "taskId": "task-004",
      "taskName": "英语单词背诵",
      "status": "NOT_STARTED",
      "completionRate": 0
    }
  ],
  "count": 2
}
```

### 6. 周总结（GET请求）

**URL**: `http://localhost:8123/api/ai/plan/weekly/summary`

**响应示例**:
```
📊 本周学习总结
================
周计划：第一周 2026.3.10-2026.3.16
完成度：82%
计划学习时长：50.0小时
实际学习时长：41.0小时

各科目完成情况：
  数学：85%
  英语：80%
  政治：75%

💪 下周改进方案
================
1. 📈 本周学习效果优秀，下周可以增加难度
2. 🎯 继续保持当前的学习计划和节奏
3. 💪 重点加强完成度较低的科目
```

## 完整的测试流程

```bash
# 1. 初始化周计划
curl -X POST http://localhost:8123/api/ai/plan/weekly/init \
  -H "Content-Type: application/json" \
  -d '{ ... }'

# 2. 早上制定计划
curl http://localhost:8123/api/ai/plan/daily/morning?date=2026-03-10

# 3. 更新任务1完成
curl -X POST "http://localhost:8123/api/ai/plan/task/update?taskId=task-001&status=COMPLETED&actualHours=2.0&completionRate=100"

# 4. 更新任务3完成
curl -X POST "http://localhost:8123/api/ai/plan/task/update?taskId=task-003&status=COMPLETED&actualHours=2.0&completionRate=100"

# 5. 更新任务5完成
curl -X POST "http://localhost:8123/api/ai/plan/task/update?taskId=task-005&status=COMPLETED&actualHours=1.5&completionRate=100"

# 6. 晚上记录进度
curl -X POST "http://localhost:8123/api/ai/plan/daily/evening?date=2026-03-10&studyHours=5.5&statusEvaluation=良好&problems=无"

# 7. 查看未完成任务
curl http://localhost:8123/api/ai/plan/task/incomplete

# 8. 查看周总结
curl http://localhost:8123/api/ai/plan/weekly/summary
```

## 在Postman中测试

### 1. 创建新的Request
- Method: POST
- URL: `http://localhost:8123/api/ai/plan/weekly/init`
- Headers: `Content-Type: application/json`
- Body: 选择 `raw` → `JSON`，粘贴上面的JSON数据

### 2. 点击Send

### 3. 查看响应

---

**提示**：
- 所有日期格式都是 `YYYY-MM-DD`
- 所有时间单位都是小时（hours）
- 完成度范围是 0-100
- 优先级范围是 1-5（5最高）
