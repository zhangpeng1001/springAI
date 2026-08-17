# Spring AI 2.0.0 GA 学习项目

> 本项目基于 Spring AI **2.0.0 GA**（2026年6月12日发布），演示 Spring AI 的核心功能。

## 📋 版本信息

| 技术栈 | 版本 |
|--------|------|
| Java | **21+** (Spring AI 2.0 最低要求) |
| Spring Boot | **4.0.0** |
| Spring AI | **2.0.0 GA** |
| Maven | 3.8+ |

## 🚀 快速开始

### 1. 环境准备

确保已安装：
- **JDK 21+**
- **Maven 3.8+**
- 一个兼容 OpenAI 接口的 AI 服务账号

### 2. 配置 API Key

在 `application.yml` 中配置，或通过环境变量：

```bash
# Windows PowerShell
$env:OPENAI_API_KEY = "your-api-key"
$env:OPENAI_BASE_URL = "https://api.openai.com"

# Linux/Mac
export OPENAI_API_KEY="your-api-key"
```

### 3. 启动项目

```bash
mvn spring-boot:run
```

### 4. 测试 API

打开浏览器或使用 curl/Postman 访问：

```bash
# 简单对话
curl "http://localhost:8080/api/chat/simple?message=你好"

# 流式对话
curl "http://localhost:8080/api/chat/stream?message=介绍一下Spring AI"

# 使用 RAG 问答（先初始化知识库）
curl -X POST "http://localhost:8080/api/rag/init-sample-data"
curl "http://localhost:8080/api/rag/ask?question=Spring AI 是什么"
```

## 📚 功能模块

| 模块 | API 路径 | 说明 |
|------|----------|------|
| [基础聊天](docs/chat.md) | `/api/chat/*` | 单轮、流式、多轮对话 |
| [提示词模板](docs/prompt.md) | `/api/prompt/*` | 变量替换、角色设定 |
| [结构化输出](docs/structured-output.md) | `/api/structured/*` | AI 输出转 Java 对象 |
| [多模态](docs/multimodal.md) | `/api/multimodal/*` | 图片理解 |
| [函数调用](docs/function-calling.md) | `/api/function/*` | AI 调用外部工具 |
| [会话记忆](docs/memory.md) | `/api/memory/*` | 多轮对话上下文 |
| [RAG 检索增强](docs/rag.md) | `/api/rag/*` | 向量检索+AI 生成 |

## 📖 详细文档

- [功能架构总览](docs/architecture.md)
- [Spring AI 2.0 新特性](docs/whats-new-2.0.md)
- [API 参考](docs/api-reference.md)

## 🎯 核心概念

### ChatClient

Spring AI 2.0 推荐的高级 API，封装了模型调用、Prompt 构建、Advisor 管理等功能。

```java
// 最简调用
String reply = chatClient.prompt()
    .user("你好")
    .call()
    .content();
```

### Advisor

Advisor 是 Spring AI 的核心扩展机制，类似 Spring AOP，可以在不修改核心逻辑的情况下添加功能：

- **QuestionAnswerAdvisor** - RAG 检索增强
- **MessageChatMemoryAdvisor** - 会话记忆
- **ToolCallingAdvisor** - 函数调用（2.0 中需要额外引入）

### RAG 工作流

```
用户提问 → 向量化 → 检索相关文档 → 注入上下文 → AI 生成回答
```

## 🔧 国内 AI 服务配置示例

### 智谱 AI

```yaml
spring:
  ai:
    openai:
      base-url: https://open.bigmodel.cn/api/paas/v4
      api-key: your-zhipu-api-key
      chat:
        options:
          model: glm-4
      embedding:
        options:
          model: embedding-3
```

### 通义千问

```yaml
spring:
  ai:
    openai:
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      api-key: your-dashscope-api-key
      chat:
        options:
          model: qwen-plus
      embedding:
        options:
          model: text-embedding-v1
```

### DeepSeek

```yaml
spring:
  ai:
    openai:
      base-url: https://api.deepseek.com
      api-key: your-deepseek-api-key
      chat:
        options:
          model: deepseek-chat
```

## ⚠️ Spring AI 2.0 重要变化

1. **Java 版本要求**：从 Java 17 提升到 **Java 21+**
2. **Spring Boot 版本**：支持 **Spring Boot 4.0+**
3. **模块重命名**：`spring-ai-advisors-vector-store` → `spring-ai-vector-store-advisor`
4. **ChatMemory**：`conversationId` 变为必选参数
5. **PromptChatMemoryAdvisor**：已移除，使用 `MessageChatMemoryAdvisor`
6. **Options 不可变**：所有 Options 类现在严格不可变，需使用 Builder
7. **结构化输出**：使用 `entity()` 方法替代旧的 BeanOutputConverter

## 📁 项目结构

```
src/main/java/com/example/springaidemo/
├── SpringAiDemoApplication.java    # 启动类
├── config/
│   └── AiConfig.java               # AI 组件配置
├── controller/
│   ├── ChatController.java         # 基础聊天
│   ├── PromptController.java       # 提示词模板
│   ├── StructuredOutputController.java  # 结构化输出
│   ├── MultimodalController.java   # 多模态
│   ├── FunctionCallController.java # 函数调用
│   ├── MemoryController.java       # 会话记忆
│   └── RagController.java          # RAG 检索增强
└── service/
    └── RagService.java             # RAG 业务逻辑
```

## 📝 License

本项目仅供学习使用。
