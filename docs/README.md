# Spring AI 2.0 学习项目

基于 Spring AI 2.0.0 GA 的完整学习示例项目，涵盖 AI 应用开发的核心功能。

## 版本信息

| 技术 | 版本 |
|------|------|
| Spring AI | 2.0.0 GA |
| Spring Boot | 4.0.5 |
| Java | 21+ |
| Maven | 3.9+ |

## 快速开始

### 1. 环境要求

- JDK 21 或更高版本
- Maven 3.9 或更高版本
- 有效的 AI API Key（默认使用 OpenAI）

### 2. 配置 API Key

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  ai:
    openai:
      api-key: your-api-key-here
      chat:
        options:
          model: gpt-4o
```

### 3. 运行项目

```bash
mvn spring-boot:run
```

### 4. 访问 API

服务启动后访问 `http://localhost:8080`

## 功能模块

### 📁 项目结构

```
src/main/java/com/example/springaidemo/
├── config/           # 配置类
├── controller/       # REST API 控制器
├── service/          # 业务服务层
└── SpringAiDemoApplication.java  # 启动类

src/main/resources/
├── templates/        # 提示词模板
└── knowledge/        # 知识库文档
```

### 🎯 API 接口一览

| 模块 | 路径前缀 | 说明 |
|------|----------|------|
| 基础聊天 | `/api/chat` | 简单对话、流式输出、多轮对话 |
| 提示词模板 | `/api/prompt` | 模板渲染、角色设定 |
| 结构化输出 | `/api/structured` | POJO 映射、List/Map 输出 |
| 多模态 | `/api/multimodal` | 图片分析、OCR |
| 函数调用 | `/api/function` | ToolCallback 注册与调用 |
| 会话记忆 | `/api/memory` | 上下文保持、历史查询 |
| RAG 检索 | `/api/rag` | 向量存储、知识问答 |

## 详细文档

- [01-基础聊天](01-chat.md) - ChatClient 基础用法
- [02-提示词模板](02-prompt.md) - Prompt 模板与角色设定
- [03-结构化输出](03-structured-output.md) - 类型安全输出
- [04-多模态](04-multimodal.md) - 图片处理与视觉分析
- [05-函数调用](05-function-calling.md) - ToolCallback 详解
- [06-会话记忆](06-memory.md) - ChatMemory 机制
- [07-RAG 检索增强](07-rag.md) - 向量存储与问答
- [08-配置详解](08-configuration.md) - 完整配置说明
- [09-2.0迁移指南](09-migration.md) - 从 1.x 迁移指南

## 核心架构

### Spring AI 2.0 核心组件

```
┌─────────────────────────────────────────────┐
│              ChatClient (2.0)               │
│  ┌─────────────────────────────────────┐    │
│  │           Prompt Builder            │    │
│  └─────────────────────────────────────┘    │
│  ┌─────────────────────────────────────┐    │
│  │         Advisors (顾问机制)          │    │
│  │  ┌─────────┐ ┌──────────┐ ┌──────┐ │    │
│  │  │Memory   │ │Q&A       │ │Tool  │ │    │
│  │  │Advisor  │ │Advisor   │ │Callback│    │
│  │  └─────────┘ └──────────┘ └──────┘ │    │
│  └─────────────────────────────────────┘    │
│  ┌─────────────────────────────────────┐    │
│  │    ChatModel (模型抽象)              │    │
│  └─────────────────────────────────────┘    │
└─────────────────────────────────────────────┘
```

### 数据流

1. 用户发送请求到 Controller
2. Controller 构建 Prompt 并调用 ChatClient
3. ChatClient 通过 Advisor 链处理（记忆检索、RAG、函数调用）
4. 发送到 AI 模型
5. 返回结果给用户

## 常见问题

### Q: 如何切换 AI 模型？

修改 `application.yml` 中的 model 配置：

```yaml
spring:
  ai:
    openai:
      chat:
        options:
          model: gpt-4o    # 或其他支持的模型
```

### Q: 如何使用其他 AI 提供商？

Spring AI 2.0 支持多种提供商：
- OpenAI
- Anthropic
- Azure OpenAI
- 阿里云百炼
- 百度千帆
- 等等

只需替换对应的依赖和配置。

### Q: Java 17 能用吗？

不能。Spring AI 2.0 要求 Java 21+。

## 许可证

本项目仅供学习使用。