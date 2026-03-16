# 研途智伴：考研学习自主规划智能体 - 快速开始指南

## 项目概述

**研途智伴** 是一个基于AI的考研学习自主规划智能体，提供：
- 🎓 **聊天路径**：考研学习问答和指导（StudyApp + RAG知识库）
- 📋 **规划路径**：自主规划和工具调用（YuManus Agent）
- 🛠️ **工具层**：搜索、下载、PDF生成等

## 快速开始

### 1. 环境准备

```bash
# 克隆项目
git clone <项目地址>
cd yu-ai-agent-master

# 配置API Key
# 编辑 src/main/resources/application.yml
# 替换 spring.ai.dashscope.api-key 为你的阿里云百炼API Key
```

### 2. 启动应用

```bash
# 使用Maven启动
./mvnw spring-boot:run

# 或者编译后运行
./mvnw clean package
java -jar target/yu-ai-agent-*.jar
```

应用启动后访问：http://localhost:8123/api/swagger-ui.html

### 3. 测试聊天路径

#### 方式一：同步调用
```bash
curl "http://localhost:8123/api/ai/study_app/chat/sync?message=泰勒公式怎么记？&chatId=test-001"
```

#### 方式二：SSE流式调用
```bash
curl "http://localhost:8123/api/ai/study_app/chat/sse?message=泰勒公式怎么记？&chatId=test-001"
```

#### 方式三：使用Swagger UI
1. 打开 http://localhost:8123/api/swagger-ui.html
2. 找到 `/ai/study_app/chat/sse` 接口
3. 输入参数：
   - message: "泰勒公式怎么记？"
   - chatId: "test-001"
4. 点击 "Try it out"

### 4. 聊天示例

#### 示例1：基础阶段问题
```
用户：我刚开始准备考研，应该怎么制定复习计划？
系统：
1. RAG检索：从"基础篇"知识库检索复习计划相关内容
2. 结合记忆：根据用户的学习阶段提供个性化建议
3. 输出：详细的复习计划制定指南
```

#### 示例2：进阶阶段问题
```
用户：强化阶段数学怎么突破？
系统：
1. RAG检索：从"进阶篇"知识库检索数学突破方法
2. 结合记忆：提供做题技巧和常见陷阱
3. 输出：系统的数学强化方案
```

#### 示例3：冲刺阶段问题
```
用户：考前一周应该怎么调整状态？
系统：
1. RAG检索：从"冲刺篇"知识库检索考前调整方法
2. 结合记忆：提供心理调适和时间管理建议
3. 输出：完整的考前准备方案
```

## API接口说明

### 聊天接口

#### 1. 同步聊天
```
GET /api/ai/study_app/chat/sync
参数：
  - message: 用户消息（必需）
  - chatId: 对话ID（必需）
返回：字符串响应
```

#### 2. SSE流式聊天
```
GET /api/ai/study_app/chat/sse
参数：
  - message: 用户消息（必需）
  - chatId: 对话ID（必需）
返回：流式文本数据
```

#### 3. ServerSentEvent流式聊天
```
GET /api/ai/study_app/chat/server_sent_event
参数：
  - message: 用户消息（必需）
  - chatId: 对话ID（必需）
返回：SSE格式数据
```

#### 4. SseEmitter流式聊天
```
GET /api/ai/study_app/chat/sse_emitter
参数：
  - message: 用户消息（必需）
  - chatId: 对话ID（必需）
返回：SseEmitter对象
```

### 规划接口

```
GET /api/ai/manus/chat
参数：
  - message: 用户需求（必需）
返回：SseEmitter对象（流式规划过程）
```

## 知识库文档

系统包含三个考研知识库文档：

### 📚 基础篇（考研常见问题和回答 - 基础篇.md）
- 复习计划制定
- 数学基础学习
- 英语单词记忆
- 焦虑克服
- 参考书选择
- 碎片化时间利用

### 📚 进阶篇（考研常见问题和回答 - 进阶篇.md）
- 数学难题突破
- 英语阅读理解
- 政治复习方法
- 专业课复习
- 学习效率维持
- 错题分析

### 📚 冲刺篇（考研常见问题和回答 - 冲刺篇.md）
- 时间安排
- 模拟考试
- 考前调整
- 考试当天注意事项
- 心理健康
- 知识漏洞补救

## 配置说明

### application.yml 关键配置

```yaml
spring:
  ai:
    dashscope:
      api-key: sk-xxxxx  # 替换为你的API Key
      chat:
        options:
          model: deepseek-v3  # 使用的模型
    mcp:
      client:
        enabled: false  # MCP服务（可选）
```

### 启用PgVector向量数据库

如需使用PgVector存储知识库，取消注释以下配置：

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/chatmemory
    username: postgres
    password: xxxxx
  ai:
    vectorstore:
      pgvector:
        index-type: HNSW
        dimensions: 1536
```

并在 `PgVectorVectorStoreConfig.java` 中取消 `@Configuration` 注解。

## 常见问题

### Q: 如何添加更多知识库文档？
A: 在 `src/main/resources/document/` 目录下添加新的Markdown文件，文件名格式为 `考研常见问题和回答 - XXX篇.md`，系统会自动加载。

### Q: 如何修改系统提示词？
A: 编辑 `StudyApp.java` 中的 `SYSTEM_PROMPT` 常量。

### Q: 如何启用MCP服务？
A: 
1. 在 `application.yml` 中设置 `spring.ai.mcp.client.enabled: true`
2. 启动MCP服务器
3. 调用 `doChatWithMcp` 方法

### Q: 如何使用工具调用功能？
A: 调用 `doChatWithTools` 方法，系统会自动调用相关工具（搜索、下载、PDF生成等）。

## 项目结构

```
src/main/java/com/yupi/yuaiagent/
├── app/
│   ├── LoveApp.java          # 原恋爱应用（保留）
│   └── StudyApp.java         # 考研学习应用（新）
├── controller/
│   └── AiController.java     # API控制器
├── rag/
│   ├── StudyAppDocumentLoader.java
│   ├── StudyAppVectorStoreConfig.java
│   ├── StudyAppRagCloudAdvisorConfig.java
│   ├── StudyAppRagCustomAdvisorFactory.java
│   └── ...
├── agent/
│   └── YuManus.java          # 规划智能体
├── tools/
│   ├── WebSearchTool.java
│   ├── PDFGenerationTool.java
│   └── ...
└── ...

src/main/resources/
├── document/
│   ├── 考研常见问题和回答 - 基础篇.md
│   ├── 考研常见问题和回答 - 进阶篇.md
│   └── 考研常见问题和回答 - 冲刺篇.md
└── application.yml
```

## 下一步

1. ✅ 启动应用并测试聊天功能
2. ✅ 根据需要扩展知识库文档
3. ✅ 集成前端应用
4. ✅ 部署到生产环境

## 技术栈

- **后端框架**：Spring Boot 3 + Spring AI
- **AI模型**：阿里云百炼（Deepseek-v3）
- **向量数据库**：SimpleVectorStore（内存）/ PgVector（可选）
- **RAG框架**：Spring AI RAG
- **智能体**：ReAct模式
- **工具调用**：Spring AI Tool Calling
- **MCP**：模型上下文协议（可选）

## 许可证

本项目为教学项目，遵循原项目许可证。

## 联系方式

如有问题，请联系编程导航：https://www.codefather.cn
