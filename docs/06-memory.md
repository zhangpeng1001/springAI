# 06 - 会话记忆

## 概述

Spring AI 2.0 的会话记忆（ChatMemory）功能用于保存对话历史，实现多轮对话的上下文保持。这对于需要记住用户之前对话的场景非常重要。

## 核心概念

### ChatMemory 架构

```
┌─────────────────────────────────────────┐
│           MessageChatMemoryAdvisor      │
│  (自动管理对话历史)                       │
└─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────┐
│           ChatMemory                    │
│  (内存存储，支持持久化)                    │
└─────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────┐
│    ChatMemoryRepository                 │
│  (存储接口，可实现数据库持久化)              │
└─────────────────────────────────────────┘
```

### 关键组件

- `MessageWindowChatMemory`：基于消息窗口的记忆实现
- `InMemoryChatMemoryRepository`：内存存储实现
- `MessageChatMemoryAdvisor`：自动管理历史的 Advisor

## API 接口

### 1. 基于 Session 的对话

使用 sessionId 隔离不同用户的对话。

**请求：**
```
GET /api/memory/chat?sessionId=user123&message=我叫张三
```

**代码示例：**
```java
@GetMapping("/chat")
public String memoryChat(
        @RequestParam String sessionId,
        @RequestParam String message) {

    // 为每次请求创建新的 Advisor 实例
    MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

    return chatClient.prompt()
            .user(message)
            .advisors(memoryAdvisor)
            .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, sessionId))
            .call()
            .content();
}
```

### 2. 查询会话历史

获取指定会话的历史消息。

**请求：**
```
GET /api/memory/history?sessionId=user123
```

**代码示例：**
```java
@GetMapping("/history")
public List<Message> getHistory(@RequestParam String sessionId) {
    return chatMemory.get(sessionId);
}
```

### 3. 清空会话历史

**请求：**
```
DELETE /api/memory/clear?sessionId=user123
```

**代码示例：**
```java
@DeleteMapping("/clear")
public String clearHistory(@RequestParam String sessionId) {
    chatMemory.clear(sessionId);
    return "会话历史已清空";
}
```

### 4. 系统提示 + 记忆

结合系统提示和会话记忆。

**请求：**
```
GET /api/memory/system?sessionId=user123&message=介绍一下自己
```

**代码示例：**
```java
@GetMapping("/system")
public String systemMemoryChat(
        @RequestParam String sessionId,
        @RequestParam String message) {

    MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

    return chatClient.prompt()
            .system("你是一个友好的助手，会记住用户之前说过的话。")
            .user(message)
            .advisors(memoryAdvisor)
            .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, sessionId))
            .call()
            .content();
}
```

## 配置说明

### 在 AiConfig 中配置

```java
@Configuration
public class AiConfig {

    @Bean
    public ChatMemory chatMemory() {
        // 使用基于消息窗口的记忆，最多保存 100 条消息
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(100)
                .build();
    }
}
```

### 切换到持久化存储

生产环境中建议使用数据库存储：

```java
@Bean
public ChatMemory chatMemory(DataSource dataSource) {
    // 使用 JDBC 存储（需要额外依赖）
    return MessageWindowChatMemory.builder()
            .chatMemoryRepository(new JdbcChatMemoryRepository(dataSource))
            .maxMessages(200)
            .build();
}
```

## 高级用法

### 自定义 Memory 实现

```java
public class RedisChatMemoryRepository implements ChatMemoryRepository {
    private final RedisTemplate<String, List<Message>> redisTemplate;

    @Override
    public List<Message> load(String conversationId) {
        return redisTemplate.opsForValue().get(conversationId);
    }

    @Override
    public void save(String conversationId, List<Message> messages) {
        redisTemplate.opsForValue().set(conversationId, messages);
    }
}
```

### 多模型记忆

```java
// 为不同模型使用不同的记忆配置
@Bean
public ChatMemory shortTermMemory() {
    return MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .maxMessages(20)  // 短期记忆只保留最近20条
            .build();
}

@Bean
public ChatMemory longTermMemory() {
    return MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .maxMessages(200)  // 长期记忆保留200条
            .build();
}
```

## 最佳实践

1. **合理的窗口大小**：根据业务场景设置合适的 maxMessages
2. **定期清理**：过期会话的记忆需要及时清理
3. **用户隔离**：使用 sessionId 隔离不同用户的对话
4. **安全考虑**：敏感信息不要存储在记忆中
5. **性能优化**：对于大量会话考虑使用 Redis 缓存

## 注意事项

- Spring AI 2.0 使用 `MessageChatMemoryAdvisor` 替代旧的 `PromptChatMemoryAdvisor`
- 必须设置 `ChatMemory.CONVERSATION_ID` 参数
- 内存存储在重启后会丢失数据
- 过长的对话历史会增加 Token 消耗