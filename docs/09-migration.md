# 09 - Spring AI 2.0 迁移指南

## 概述

Spring AI 2.0 是一次重大版本升级，包含了许多 API 变更和改进。本指南帮助你从 1.x 版本迁移到 2.0。

## 前置条件

- Java 21+（从 Java 17 升级）
- Spring Boot 4.0.x 或 4.1.x
- Maven 3.9+ 或 Gradle 8.x

## 依赖更新

### Maven

```xml
<!-- 1.x 版本 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-bom</artifactId>
    <version>1.0.0</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>

<!-- 2.0 版本 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-bom</artifactId>
    <version>2.0.0</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

### 模块重命名

| 1.x | 2.0 |
|-----|-----|
| `spring-ai-advisors-vector-store` | `spring-ai-vector-store-advisor` |
| `spring-ai-advisors-vector-store-pgvector` | `spring-ai-vector-store-pgvector-store` |

## API 变更

### 1. ChatClient 成为推荐 API

```java
// 1.x 旧方式
ChatClient client = ChatClient.builder(chatModel).build();

// 2.0 新方式（推荐）
ChatClient client = ChatClient.builder(chatModel).build();

// 使用方式变化
// 1.x
String response = chatClient.call("你好");

// 2.0
String response = chatClient.prompt()
        .user("你好")
        .call()
        .content();
```

### 2. Options 类变为不可变

```java
// 1.x 可变
ChatOptions options = new ChatOptions();
options.setModel("gpt-4");
options.setTemperature(0.7);

// 2.0 不可变，使用 Builder
ChatOptions options = ChatOptions.builder()
        .model("gpt-4o")
        .temperature(0.7)
        .build();
```

### 3. ChatMemory 变更

```java
// 1.x
PromptChatMemoryAdvisor memoryAdvisor = PromptChatMemoryAdvisor.builder(chatMemory).build();

// 2.0
MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

// 记忆实现变化
// 1.x
ChatMemory memory = new InMemoryChatMemory();

// 2.0
ChatMemory memory = MessageWindowChatMemory.builder()
        .chatMemoryRepository(new InMemoryChatMemoryRepository())
        .maxMessages(100)
        .build();
```

### 4. ToolCallback 变更

```java
// 1.x
ToolCallback callback = ToolCallback.builder()
        .toolDefinition(toolDefinition)
        .toolExecutor(toolExecutor)
        .build();

// 2.0 简化
FunctionToolCallback callback = FunctionToolCallback.builder("getWeather", 
        (Function<String, String>) this::getWeather)
        .description("查询天气")
        .inputType(String.class)
        .build();
```

### 5. Media 类变更

```java
// 1.x
Media media = Media.from(MimeType.IMAGE_JPEG, new URL(imageUrl));

// 2.0 使用 Builder
Media media = Media.builder()
        .mimeType(MimeType.valueOf("image/jpeg"))
        .data(URI.create(imageUrl))
        .build();
```

### 6. Prompt 类变更

```java
// 1.x
Prompt prompt = new Prompt();
prompt.add(new UserMessage("你好"));

// 2.0
Prompt prompt = new Prompt(List.of(new UserMessage("你好")));
```

### 7. Advisor 参数传递

```java
// 1.x
.advisors(memoryAdvisor)
.param(ChatMemory.CONVERSATION_ID, sessionId)

// 2.0
.advisors(memoryAdvisor)
.advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, sessionId))
```

## 配置变更

### application.yml

```yaml
# 1.x
spring:
  ai:
    openai:
      api-key: xxx
      chat:
        options:
          model: gpt-4

# 2.0 基本兼容，但部分选项可能有变化
spring:
  ai:
    openai:
      api-key: xxx
      chat:
        options:
          model: gpt-4o
```

## 常见问题

### Q: 为什么我的代码编译不通过？

检查以下几点：
1. 是否使用了 Java 21+
2. BOM 版本是否为 2.0.0
3. 是否使用了已重命名的模块

### Q: 运行时找不到 Bean？

2.0 可能调整了一些自动配置。检查：
1. 是否添加了必要的依赖
2. 是否正确配置了 API Key
3. 是否手动创建了必要的 Bean

### Q: 流式输出不工作？

```java
// 确保使用 stream() 而不是 call()
Flux<String> stream = chatClient.prompt()
        .user("你好")
        .stream()  // 注意这里
        .content();
```

### Q: 会话记忆不生效？

确保设置了 CONVERSATION_ID：
```java
.advisors(memoryAdvisor)
.advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, sessionId))
```

## 迁移检查清单

- [ ] Java 版本升级到 21+
- [ ] Spring Boot 升级到 4.0.x
- [ ] 更新 BOM 版本到 2.0.0
- [ ] 更新依赖模块名称
- [ ] 替换 Options 构造方式为 Builder
- [ ] 替换 ChatMemory 实现
- [ ] 替换 ToolCallback 为 FunctionToolCallback
- [ ] 替换 Media.from() 为 Media.builder()
- [ ] 替换 Prompt.add() 为构造函数
- [ ] 调整 Advisor 参数传递方式
- [ ] 测试所有功能正常

## 相关资源

- [Spring AI 2.0 发布说明](https://github.com/spring-projects/spring-ai/releases)
- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Spring AI 示例项目](https://github.com/spring-projects/spring-ai-examples)
- [Spring AI 问题追踪](https://github.com/spring-projects/spring-ai/issues)