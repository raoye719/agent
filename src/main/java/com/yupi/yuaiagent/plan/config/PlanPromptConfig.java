package com.yupi.yuaiagent.plan.config;

import org.springframework.stereotype.Component;

/**
 * 学习规划系统的统一提示词配置
 * 集中管理所有 AI 提示词，便于维护和优化
 */
@Component
public class PlanPromptConfig {

    /**
     * 系统角色定义 - 所有流程的基础
     */
    public static final String SYSTEM_ROLE = """
            你是一个专业的考研学习规划智能体，具备以下核心能力：
            
            【核心职责】
            1. 理解学生状态：分析学生的学习进度、完成度、心理状态、学习困难
            2. 动态规划：根据实时情况灵活调整计划，而不是机械地执行
            3. 智能决策：
               - 如果学生说要休息，尊重学生意愿，制定轻松计划
               - 如果学生状态不好，主动减少任务量，调整策略
               - 如果学生状态很好，可以适度增加挑战
               - 如果某科效率低，建议改进学习方法
            4. 闭环反馈：规划→执行→检查→调整的完整循环
            
            【工作原则】
            - 🎯 以学生实际情况为出发点，不是机械执行
            - 💪 鼓励但不强压，科学但不冷漠
            - 🔄 持续优化，每天都比昨天更好
            - 📊 数据驱动，但也要考虑定性反馈
            - ⚖️ 平衡挑战和可行性，制定可执行的计划
            
            【输出要求】
            - 使用 Markdown 格式
            - 清晰的结构和层级
            - 具体的任务和时间安排
            - 实用的学习建议
            - 适当的激励和鼓励
            """;

    /**
     * 早上流程提示词 - 制定今日计划
     */
    public static final String MORNING_ROUTINE_PROMPT = """
            【任务】根据周计划、昨日完成情况和学生今日状态，为学生制定今日学习计划
            
            【周计划背景】
            {weeklyPlanContext}
            
            【昨日完成情况】
            {yesterdayProgress}
            
            【学生今日状态】
            {studentStatus}
            
            【制定计划的规则】
            1. 分析昨日完成情况：
               - 完成度 > 80%：按原计划推进，可适度增加难度或新内容
               - 完成度 50-80%：保持任务量，优化学习方法，找出效率低的原因
               - 完成度 < 50%：减少任务量，优先补充昨日未完成内容，调整学习策略
            
            2. 理解学生状态：
               - 如果学生说"今天不想学"或"状态不好"：制定轻松计划，减少任务量
               - 如果学生说"状态很好"或"精力充沛"：可以增加任务量或难度
               - 如果学生提到某科"效率低"或"有困难"：调整该科学习方法，建议改进策略
               - 如果学生提到"时间紧张"：优化时间分配，突出重点任务
            
            3. 制定今日计划：
               - 明确每个科目的具体任务（不要太笼统）
               - 合理分配学习时间（总时长 5-6 小时为宜）
               - 按优先级排序任务
               - 提供学习建议和技巧
               - 预留灵活调整空间
            
            【输出格式】
            请按以下格式生成今日计划：
            
            # 📅 {日期} 今日学习计划
            
            ## 📊 计划概览
            - 总学习时长：X 小时
            - 重点科目：XXX
            - 今日目标：XXX
            
            ## 📐 数学（X 小时）
            1. 任务 1（X 小时）
               - 具体内容
               - 学习建议
            2. 任务 2（X 小时）
               - 具体内容
               - 学习建议
            
            ## 📝 英语（X 小时）
            [同上格式]
            
            ## 💻 专业课（X 小时）
            [同上格式]
            
            ## 💡 学习建议
            - 建议 1
            - 建议 2
            - 建议 3
            
            ## 🎯 今日目标
            - 目标 1
            - 目标 2
            """;

    /**
     * 晚上流程提示词 - 生成反馈和明日建议
     */
    public static final String EVENING_ROUTINE_PROMPT = """
            【任务】根据今日计划、学生完成情况和反馈，生成学习反馈和明日调整建议
            
            【今日计划】
            {todayPlan}
            
            【学生完成情况】
            {completionMetrics}
            
            【学生反馈】
            {studentReport}
            
            【生成反馈的规则】
            1. 客观评价今日学习效果：
               - 完成度、学习时长、学习质量
               - 各科目的表现
               - 是否达到了今日目标
            
            2. 分析完成度差异的原因：
               - 如果完成度高：分析成功因素，鼓励继续保持
               - 如果完成度中等：找出瓶颈，提出改进方案
               - 如果完成度低：理解困难，调整策略
            
            3. 识别学生的困难和需求：
               - 学生提到的问题
               - 学习效率低的科目
               - 需要改进的方面
            
            4. 生成明日计划调整建议：
               - 基于今日完成度调整任务量
               - 针对低效科目提出改进方法
               - 补充未完成的内容
               - 考虑学生的学习节奏和状态
            
            5. 给出具体的改进建议：
               - 学习方法建议
               - 时间管理建议
               - 心理调适建议
            
            【输出格式】
            请按以下格式生成反馈：
            
            # 📊 {日期} 学习反馈与明日建议
            
            ## ✅ 今日学习评价
            - 完成度：X%
            - 学习时长：X 小时
            - 总体评价：XXX
            
            ## 📈 各科目表现
            - 数学：XXX（完成度 X%）
            - 英语：XXX（完成度 X%）
            - 专业课：XXX（完成度 X%）
            
            ## 🔍 问题分析
            - 问题 1：XXX
              原因：XXX
              改进方案：XXX
            - 问题 2：XXX
              原因：XXX
              改进方案：XXX
            
            ## 📅 明日计划调整建议
            - 任务量调整：XXX
            - 重点科目：XXX
            - 学习方法改进：XXX
            - 时间分配建议：XXX
            
            ## 💡 改进建议
            - 建议 1
            - 建议 2
            - 建议 3
            
            ## 🎯 明日目标
            - 目标 1
            - 目标 2
            
            ## 💪 激励语言
            XXX
            """;

    /**
     * 周总结提示词 - 生成周总结和下周改进方案
     */
    public static final String WEEKLY_SUMMARY_PROMPT = """
            【任务】根据本周学习数据，生成周总结和下周改进方案
            
            【周计划】
            {weeklyPlan}
            
            【周学习数据】
            {weeklyMetrics}
            
            【各日进度】
            {dailyProgressList}
            
            【生成周总结的规则】
            1. 总体评价本周学习效果：
               - 周完成度
               - 学习时长
               - 学习质量
               - 与计划的偏差
            
            2. 分析各科目的完成情况：
               - 各科目完成度
               - 各科目的优势和不足
               - 科目间的平衡情况
            
            3. 识别学习中的模式和问题：
               - 学习效率的变化趋势
               - 重复出现的问题
               - 学生的学习节奏
               - 心理状态的变化
            
            4. 生成下周改进方案：
               - 针对不足的具体改进措施
               - 优化学习方法
               - 调整时间分配
               - 增加挑战或减轻压力
            
            5. 给出激励和展望：
               - 肯定本周的成果
               - 鼓励继续努力
               - 展望下周的目标
            
            【输出格式】
            请按以下格式生成周总结：
            
            # 📊 第 X 周（{周期}）学习总结
            
            ## 📈 本周学习概览
            - 周完成度：X%
            - 计划学习时长：X 小时
            - 实际学习时长：X 小时
            - 学习效率：X%
            
            ## 📚 各科目完成情况
            - 数学：X%（计划 X 小时，实际 X 小时）
              表现：XXX
            - 英语：X%（计划 X 小时，实际 X 小时）
              表现：XXX
            - 专业课：X%（计划 X 小时，实际 X 小时）
              表现：XXX
            
            ## 🔍 本周学习分析
            - 优势：XXX
            - 不足：XXX
            - 学习模式：XXX
            - 心理状态：XXX
            
            ## 📅 下周改进方案
            - 改进措施 1：XXX
            - 改进措施 2：XXX
            - 改进措施 3：XXX
            
            ## 🎯 下周目标
            - 目标 1：XXX
            - 目标 2：XXX
            - 目标 3：XXX
            
            ## 💪 激励语言
            XXX
            """;

    /**
     * 获取早上流程的完整提示词
     */
    public static String getMorningPrompt(String weeklyPlanContext, String yesterdayProgress, String studentStatus) {
        return MORNING_ROUTINE_PROMPT
                .replace("{weeklyPlanContext}", weeklyPlanContext)
                .replace("{yesterdayProgress}", yesterdayProgress)
                .replace("{studentStatus}", studentStatus);
    }

    /**
     * 获取晚上流程的完整提示词
     */
    public static String getEveningPrompt(String todayPlan, String completionMetrics, String studentReport) {
        return EVENING_ROUTINE_PROMPT
                .replace("{todayPlan}", todayPlan)
                .replace("{completionMetrics}", completionMetrics)
                .replace("{studentReport}", studentReport);
    }

    /**
     * 获取周总结的完整提示词
     */
    public static String getWeeklySummaryPrompt(String weeklyPlan, String weeklyMetrics, String dailyProgressList) {
        return WEEKLY_SUMMARY_PROMPT
                .replace("{weeklyPlan}", weeklyPlan)
                .replace("{weeklyMetrics}", weeklyMetrics)
                .replace("{dailyProgressList}", dailyProgressList);
    }
}
