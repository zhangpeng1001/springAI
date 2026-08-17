# 基础聊天功能

## 概述

基础聊天是 Spring AI 最核心的功能，通过 `ChatClient` API 与 AI 模型进行对话。

## API 列表

### 1. 简单对话

**GET** `/api/chat/simple`

最简单的单轮对话方式。

**参数：**
- `message` (String, 必填) - 用户输入的问题

**示例：**
```bash
curl "http://localhost:8080/api/chat/simple?message=你好"
```

**返回：** AI 回复文本

---

### 2. 流式对话

**GET** `/api/chat/stream`

逐 token 返回 AI 生成内容，适合长文本生成场景。

**参数：**
- `message` (String, 必填) - 用户输入的问题

**示例：**
```bash
curl "http://localhost:8080/api/chat/stream?message=请写一篇关于Spring AI的文章"
```

**返回：** Server-Sent Events (SSE) 流，每个事件是一个 token 片段

---

### 3. 多轮对话

**POST** `/api/chat/multi-turn`

传入历史消息，让 AI 理解上下文。

**请求体：**
```json
{
  "message": "继续，详细说说",
  "history": [
    "你好",
    "Spring AI 是什么？"
  ]
}
```

**字段说明：**
- `message` (String) - 当前用户消息
- `history` (List\<String\>) - 历史对话消息

---

### 4. 系统提示对话

**GET** `/api/chat/with-system`

通过系统提示设定 AI 角色。

**参数：**
- `message` (String, 必填) - 用户问题
- `systemPrompt` (String, 可选) - 系统提示词，默认为"你是一个友好的AI助手。"

**示例：**
```bash
curl "http://localhost:8080/api/chat/with-system?message=用Python写排序算法&systemPrompt=你是Python专家"
```

---

### 5. 获取完整响应

**GET** `/api/chat/response`

获取包含元数据（token 使用量等）的完整响应。

**参数：**
- `message` (String, 必填) - 用户问题

**返回：** `ChatResponse` 对象，包含：
- AI 回复内容
- Token 使用量（prompt tokens、completion tokens、total tokens）
- 模型信息

## 核心代码解析

```java
// ChatClient 最简调用
String reply = chatClient.prompt()  // 1. 创建请求
    .user("你好")                    // 2. 添加用户消息
    .call()                         // 3. 同步调用
    .content();                     // 4. 获取文本结果

// 流式调用
Flux<String> stream = chatClient.prompt()
    .user("写一首诗")
    .stream()                       // 异步流式调用
    .content();                     // 返回 Flux<String>
```

## Spring AI 2.0 要点

- `ChatClient` 是 2.0 推荐的高级 API，替代旧的 `ChatModel` 直接调用
- 支持链式调用，代码更简洁
- 内置 Advisor 机制，便于扩展功能
- 流式输出使用 Project Reactor 的 `Flux`
