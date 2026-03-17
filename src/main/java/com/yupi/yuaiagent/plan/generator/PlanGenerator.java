package com.yupi.yuaiagent.plan.generator;

import com.yupi.yuaiagent.plan.config.PlanPromptConfig;
import com.yupi.yuaiagent.plan.file.FileSystemManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 计划生成模块
 * 负责：
 * 1. 读取周计划文件
 * 2. 读取昨日进度文件
 * 3. 调用 AI 动态生成今日计划
 * 4. 将生成的计划保存到文件
 */
@Component
@Slf4j
public class PlanGenerator {

    private final FileSystemManager fileSystemManager;
    private final ChatClient chatClient;

    public PlanGenerator(FileSystemManager fileSystemManager, ChatModel dashscopeChatModel) {
        this.fileSystemManager = fileSystemManager;
        this.chatClient = ChatClient.builder(dashscopeChatModel).build();
    }

    /**
     * 生成今日计划，并保存到 study_records 目录
     *
     * @param today 今天的日期
     * @return AI 生成的今日计划文本
     */
    public String generateTodayPlan(LocalDate today) {
        log.info("开始生成今日计划：{}", today);

        // 步骤1：读取周计划
        String weeklyPlan = fileSystemManager.readWeeklyPlan(today);
        if (weeklyPlan == null) {
            return "❌ 错误：未找到周计划文件，请先创建周计划";
        }
        log.info("✅ 已读取周计划");

        // 步骤2：读取昨日进度
        String yesterdayProgress = fileSystemManager.readYesterdayProgress(today);
        if (yesterdayProgress != null) {
            log.info("✅ 已读取昨日进度");
        } else {
            log.info("ℹ️ 没有昨日进度记录（第一天）");
        }

        // 步骤3：调用 AI 生成今日计划
        String prompt = buildPrompt(weeklyPlan, yesterdayProgress, today);
        String todayPlanContent = chatClient.prompt()
                .system(PlanPromptConfig.SYSTEM_ROLE)
                .user(prompt)
                .call()
                .content();
        log.info("✅ AI 已生成今日计划");

        // 步骤4：包装成完整的 markdown 文件格式
        String fileContent = buildPlanFileContent(today, todayPlanContent);

        // 步骤5：保存到 study_records/{date}_学习计划完成情况.md
        boolean saved = fileSystemManager.writeProgressRecord(today, fileContent);
        if (saved) {
            log.info("✅ 今日计划已保存：{}_学习计划完成情况.md", today);
        } else {
            log.warn("⚠️ 今日计划保存失败");
        }

        return todayPlanContent;
    }

    /**
     * 构建 AI 提示词
     */
    private String buildPrompt(String weeklyPlan, String yesterdayProgress, LocalDate today) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("【任务】根据周计划和昨日进度，为 ").append(today.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")))
              .append(" 生成今日学习计划\n\n");

        prompt.append("【周计划】\n");
        prompt.append(weeklyPlan).append("\n\n");

        if (yesterdayProgress != null) {
            prompt.append("【昨日进度】\n");
            prompt.append(yesterdayProgress).append("\n\n");
        } else {
            prompt.append("【昨日进度】\n这是第一天，暂无昨日进度\n\n");
        }

        prompt.append("【要求】\n");
        prompt.append("1. 所有任务必须来自周计划，不能私自添加\n");
        prompt.append("2. 根据昨日完成情况动态调整今日任务量：\n");
        prompt.append("   - 昨日完成度 > 80%：按原计划推进\n");
        prompt.append("   - 昨日完成度 50-80%：适当减少任务量\n");
        prompt.append("   - 昨日完成度 < 50%：优先补昨日未完成内容\n");
        prompt.append("   - 学生说不想学、状态不好：减少任务量，保留核心任务\n");
        prompt.append("3. 按科目分类，标注每个任务的预计时长\n");
        prompt.append("4. 提供调整思路说明和学习建议\n");
        prompt.append("5. 在每个任务末尾加 ✅ 作为完成情况占位符（晚上上报时填写）\n\n");

        prompt.append("【输出格式】\n");
        prompt.append("## 📅 今日学习计划（日期 | 周第X天）\n");
        prompt.append("> 调整思路：XXX\n");
        prompt.append("> 周计划进度：XXX\n\n");
        prompt.append("### 📐 数学（今日目标X小时）\n");
        prompt.append("1. 任务描述 ✅ Xh\n\n");
        prompt.append("### 📝 英语（今日目标X小时）\n");
        prompt.append("1. 任务描述 ✅ Xh\n\n");
        prompt.append("### 💻 专业课（今日目标X小时）\n");
        prompt.append("1. 任务描述 ✅ Xh\n\n");
        prompt.append("### 额外建议：\n");
        prompt.append("XXX\n");

        return prompt.toString();
    }

    /**
     * 将 AI 生成的计划包装成完整的 markdown 文件格式
     * 包含「今日学习计划」和「今日完成情况」两个部分
     */
    private String buildPlanFileContent(LocalDate today, String aiPlanContent) {
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyy年M月d日"));

        return "# " + dateStr + " 学习计划与完成情况\n\n" +
               "## 📅 今日学习计划\n" +
               aiPlanContent + "\n\n" +
               "---\n\n" +
               "## ✅ 今日完成情况\n\n" +
               "（晚上上报进度后更新）\n\n" +
               "## 📝 状态记录\n\n" +
               "- 学习时长：\n" +
               "- 总体完成度：\n" +
               "- 状态评价：\n" +
               "- 遇到的问题：\n" +
               "- 其他备注：\n\n" +
               "## 🎯 明日建议\n\n" +
               "（晚上上报进度后生成）\n";
    }

    /**
     * 生成晚上反馈和明日建议
     *
     * @param today          今天的日期
     * @param todayPlan      今日计划文件内容
     * @param progressReport 学生的进度报告
     * @return AI 生成的明日建议
     */
    public String generateEveningFeedback(LocalDate today, String todayPlan, String progressReport) {
        log.info("开始生成晚上反馈：{}", today);

        String prompt = "你是一个考研学习规划助手。\n\n" +
                "【今日计划】\n" + todayPlan + "\n\n" +
                "【学生今日完成情况】\n" + progressReport + "\n\n" +
                "请根据今日计划和完成情况，生成：\n" +
                "1. 对今日学习的简短评价（1-2句）\n" +
                "2. 明日计划调整建议（具体任务，来自周计划）\n";

        String feedback = chatClient.prompt()
                .system(PlanPromptConfig.SYSTEM_ROLE)
                .user(prompt)
                .call()
                .content();

        log.info("✅ AI 已生成晚上反馈");
        return feedback;
    }
}
