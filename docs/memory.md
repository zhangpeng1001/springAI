# 会话记忆

## 概述

会话记忆（ChatMemory）让 AI 能够记住多轮对话的上下文，实现真正的"多轮对话"体验。

## 核心概念

### 为什么需要会话记忆？

没有记忆的 AI 对话是无状态的，每次提问 AI 都不知道之前聊了什么。会话记忆解决这个问题：

- **上下文保持**：AI 能理解指代（"它"、"这个"、"上面提到的"）
- **个性化交互**：记住用户偏好和历史
- **复杂任务**：支持需要多步骤完成的任务

### 记忆类型

| 类型 | 特点 | 适用场景 |
|------|------|----------|
| InMemoryChatMemory | 内存存储，重启丢失 | 开发测试 |
| JdbcChatMemory | 数据库持久化 | 生产环境 |

## API 列表

### 1. 带记忆的对话

**GET** `/api/memory/chat`

使用 `sessionId` 标识会话，相同 ID 的请求共享记忆。

**参数：**
- `sessionId` (String, 必填) - 会话唯一标识
- `message` (String, 必填) - 用户消息

**示例：**
```bash
# 第一轮对话
curl "http://localhost:8080/api/memory/chat?sessionId=user1&message=我叫张三"

# 第二轮对话（AI 记得名字）
curl "http://localhost:8080/api/memory/chat?sessionId=user1&message=我叫什么名字？"
```

---

### 2. 带系统提示的记忆对话

**GET** `/api/memory/chat-with-system`

同时使用系统提示和会话记忆。

**参数：**
- `sessionId` (String, 必填) - 会话 ID
- `message` (String, 必填) - 用户消息
- `systemPrompt` (String, 可选) - 系统提示

---

### 3. 清除会话记忆

**DELETE** `/api/memory/clear`

清除指定会话的所有历史。

**参数：**
- `sessionId` (String, 必填) - 要清除的会话 ID

---

### 4. 查看记忆信息

**GET** `/api/memory/info`

查看当前使用的 ChatMemory 实现类型。

## 核心代码解析

```java
// 1. 创建 ChatMemory Bean
@Bean
public ChatMemory chatMemory() {
    return new InMemoryChatMemory();
}

// 2. 在对话中使用
chatClient.prompt()
    .user(message)
    // 添加记忆 Advisor
    .advisors(new MessageChatMemoryAdvisor(chatMemory))
    // 2.0 中 conversationId 是必选参数
    .advisors(advisorSpec -> advisorSpec
        .param(ChatMemory.CONVERSATION_ID, sessionId))
    .call()
    .content();
```

## Spring AI 2.0 要点

### 重要变化

1. **conversationId 必选**：2.0 中不再有默认的会话 ID，必须显式指定
2. **PromptChatMemoryAdvisor 移除**：使用 `MessageChatMemoryAdvisor` 替代
3. **消息统一存储**：所有消息（包括 System）统一存储

### 持久化方案

```java
// 使用 JDBC 持久化（需要数据库支持）
@Bean
public ChatMemory chatMemory(DataSource dataSource) {
    return JdbcChatMemory.builder(dataSource).build();
}
```

## 最佳实践

1. **合理设置会话 ID**：使用用户 ID + 会话 ID 的组合
2. **定期清理**：长时间不活跃的会话应清理
3. **Token 限制**：注意 Token 限制，过长的对话历史需要摘要
4. **安全考虑**：敏感对话的记忆需要加密存储
