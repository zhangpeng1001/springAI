# 01 - 基础聊天功能

## 概述

Spring AI 2.0 推荐使用 `ChatClient` 作为与 AI 模型交互的主要 API。它提供了流畅的 Builder 模式，支持同步调用、流式输出和多轮对话。

## 核心概念

### ChatClient 实例

```java
@Bean
public ChatClient chatClient(ChatModel chatModel) {
    return ChatClient.builder(chatModel).build();
}
```

### 基本调用流程

```java
String response = chatClient.prompt()  // 1. 创建 Prompt
    .user("你好")                       // 2. 添加用户消息
    .call()                             // 3. 发起调用
    .content();                         // 4. 获取结果
```

## API 接口

### 1. 简单问答

**请求：**
```
GET /api/chat/simple?message=你好
```

**代码示例：**
```java
@GetMapping("/simple")
public String simpleChat(@RequestParam String message) {
    return chatClient.prompt()
            .user(message)
            .call()
            .content();
}
```

### 2. 流式输出

流式输出允许 AI 逐字返回响应，提升用户体验。

**请求：**
```
GET /api/chat/stream?message=写一首关于春天的诗
```

**代码示例：**
```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> streamChat(@RequestParam String message) {
    return chatClient.prompt()
            .user(message)
            .stream()
            .content();
}
```

### 3. 完整响应对象

获取包含元数据的完整响应。

**请求：**
```
GET /api/chat/response?message=介绍一下Spring AI
```

**代码示例：**
```java
@GetMapping("/response")
public ChatResponse getFullResponse(@RequestParam String message) {
    return chatClient.prompt()
            .user(message)
            .call()
            .chatResponse();  // 返回 ChatResponse 对象
}
```

### 4. 多消息对话

在一次请求中传入多条消息。

**请求：**
```
POST /api/chat/messages
Body: ["你好", "介绍一下自己"]
```

**代码示例：**
```java
@PostMapping("/messages")
public String multiMessageChat(@RequestBody List<String> messages) {
    List<Message> chatMessages = messages.stream()
            .map(UserMessage::new)
            .collect(Collectors.toList());

    return chatClient.prompt()
            .messages(chatMessages)
            .call()
            .content();
}
```

### 5. 带系统提示的对话

通过系统消息设定 AI 角色。

**请求：**
```
GET /api/chat/system?message=帮我写代码
```

**代码示例：**
```java
@GetMapping("/system")
public String systemChat(@RequestParam String message) {
    return chatClient.prompt()
            .system("你是一位专业的Java程序员，代码要简洁、注释清晰。")
            .user(message)
            .call()
            .content();
}
```

## 关键返回类型

### String
最简单的返回类型，只包含 AI 的文本响应。

### ChatResponse
包含完整的响应信息：
- `getResult()` - 主结果
- `getResults()` - 所有候选结果
- `getMetadata()` - 元数据（token 使用量等）

### Flux<String>
流式响应，使用 SSE（Server-Sent Events）协议。

## 最佳实践

1. **使用系统提示**：通过 system message 设定 AI 的角色和行为
2. **错误处理**：捕获并处理 AI 调用异常
3. **超时设置**：配置合理的超时时间
4. **流式输出**：长响应场景使用流式输出

## 注意事项

- Spring AI 2.0 的 `call()` 返回类型是 `CallResponseSpec`
- 同步调用使用 `.content()` 或 `.chatResponse()`
- 流式调用使用 `.stream()` 而不是 `.call()`