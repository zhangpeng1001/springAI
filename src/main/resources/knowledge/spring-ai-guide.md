# Spring AI 2.0 知识库

## Spring AI 简介

Spring AI 是一个用于 AI 工程的应用框架，它的目标是将 AI 能力集成到 Spring Boot 应用中。

## 核心特性

### 1. ChatClient（聊天客户端）
Spring AI 2.0 推荐使用 ChatClient 作为主要 API，它提供了：
- 流式和非流式的模型调用
- 对话管理和历史记录
- 函数调用（Function Calling）
- 结构化输出
- 多模态支持（图片、音频）

### 2. Advisors（顾问机制）
Advisors 是 Spring AI 的核心扩展机制，允许在模型调用前后插入自定义逻辑：
- `MessageChatMemoryAdvisor`：会话记忆
- `QuestionAnswerAdvisor`：RAG 检索增强
- `FunctionToolCallback`：函数/工具调用

### 3. ChatMemory（聊天记忆）
支持多种记忆实现：
- `MessageWindowChatMemory`：基于消息窗口的记忆
- `InMemoryChatMemoryRepository`：内存存储（生产环境可使用数据库）

### 4. VectorStore（向量存储）
用于存储和检索向量数据：
- `SimpleVectorStore`：内存实现（适合学习）
- 支持多种向量数据库（PostgreSQL、Redis、Pinecone 等）

### 5. 结构化输出
支持将 AI 响应映射为 Java 对象：
- 接口定义输出结构
- 自动 JSON 解析
- 类型安全

## 版本信息

- Spring AI 版本：2.0.0 GA
- 发布日期：2026年6月12日
- 要求 Java 版本：21+
- 要求 Spring Boot 版本：4.0.x / 4.1.x

## 2.0 主要变更

1. 模块重命名：`spring-ai-advisors-vector-store` → `spring-ai-vector-store-advisor`
2. Options 类变为不可变，使用 Builder 模式
3. ChatClient 成为推荐的高级 API
4. 官方 OpenAI Java SDK 支持
5. 新的 ChatMemory 实现
6. 更灵活的 ToolCallback 机制