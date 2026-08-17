# Spring AI 2.0 新特性

## 概述

Spring AI 2.0.0 GA 于 2026 年 6 月 12 日正式发布，带来了重大更新和若干 Breaking Changes。本文档总结关键变化，帮助你从 1.x 版本平滑迁移。

## 版本要求

| 组件 | 1.x | 2.0 |
|------|-----|-----|
| Java | 17+ | **21+** |
| Spring Boot | 3.x | **4.0+** |
| Spring Framework | 6.x | **7.0+** |

## 新功能

### 1. ChatClient API 增强

2.0 中 ChatClient 成为一等公民，提供更强大的功能：

```java
// 2.0 新的内联系统提示方式
chatClient.prompt()
    .system("你是{role}")
    .system(p -> p.param("role", "翻译专家"))
    .user(text)
    .call()
    .content();
```

### 2. ToolCallback 改进

函数调用 API 更简洁：

```java
// 2.0 使用 ToolCallback 构建工具
ToolCallback.builder()
    .name("search")
    .description("搜索内容")
    .inputType(String.class)
    .outputType(String.class)
    .function(input -> search((String) input))
    .build();
```

### 3. 结构化输出增强

```java
// 2.0 entity() 方法
Person person = chatClient.prompt()
    .user(text)
    .call()
    .entity(Person.class);  // 自动解析
```

### 4. 多模态支持增强

更好的图片处理 API：

```java
Media.from(new URL(imageUrl))  // 2.0 使用工厂方法
```

### 5. 官方 OpenAI SDK 集成

2.0 使用官方 `openai-java` SDK，获得更好的性能和维护性。

## Breaking Changes

### 1. 模块重命名

| 1.x | 2.0 |
|-----|-----|
| `spring-ai-advisors-vector-store` | **`spring-ai-vector-store-advisor`** |
| `spring-ai-advisors-tool-calling` | `spring-ai-tool-calling-advisor` |
| - | `spring-ai-rag` (新增) |

### 2. ChatMemory 变化

```java
// 1.x - conversationId 有默认值
.memory("default")

// 2.0 - conversationId 必选
.advisors(advisorSpec -> advisorSpec
    .param(ChatMemory.CONVERSATION_ID, sessionId))
```

**注意：** `PromptChatMemoryAdvisor` 已被移除，使用 `MessageChatMemoryAdvisor`。

### 3. Options 不可变

所有 Options 类现在严格不可变：

```java
// 1.x - 可以直接 set
ChatOptions options = new ChatOptions();
options.setModel("gpt-4o");  // ❌ 2.0 中不再支持

// 2.0 - 必须使用 Builder
ChatOptions options = ChatOptions.builder()
    .model("gpt-4o")
    .temperature(0.7)
    .build();  // ✅ 正确方式
```

### 4. Advisor 包路径变化

| 1.x | 2.0 |
|-----|-----|
| `...advisor.vectorstore.QuestionAnswerAdvisor` | `...advisor.vectorstore.QuestionAnswerAdvisor` (保持不变) |
| `...advisor.ChatMemoryAdvisor` | `...advisor.MessageChatMemoryAdvisor` |

### 5. Media 创建方式

```java
// 1.x
Media.builder().url(uri).build()

// 2.0
Media.from(url)  // 使用静态工厂方法
```

### 6. Document 创建方式

```java
// 1.x
new Document("content", metadata)

// 2.0
Document.builder()
    .text("content")
    .metadata(metadata)
    .build()
```

## 迁移指南

### 从 1.x 升级到 2.0  Checklist

- [x] JDK 17 → **JDK 21**
- [x] Spring Boot 3.x → **Spring Boot 4.0**
- [x] 更新 BOM 版本到 **2.0.0**
- [x] 替换 `spring-ai-advisors-vector-store` → `spring-ai-vector-store-advisor`
- [x] 将 `PromptChatMemoryAdvisor` → `MessageChatMemoryAdvisor`
- [x] ChatMemory 添加显式 conversationId
- [x] Options 使用 Builder 模式
- [x] Media 使用 `Media.from()`
- [x] Document 使用 Builder 模式
- [x] Prompt 模板使用 ChatClient 内联 API

## 参考链接

- [Spring AI 2.0 发布说明](https://github.com/spring-projects/spring-ai/releases)
- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Spring AI 示例项目](https://github.com/spring-projects/spring-ai-examples)
