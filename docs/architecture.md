# 功能架构总览

## 系统架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         Spring AI Demo                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  ChatController  │  │  PromptController │  │  StructuredOutput  │          │
│  │   基础聊天     │  │   提示词模板    │  │   结构化输出      │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                 │                 │                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  Multimodal   │  │ FunctionCall │  │   Memory       │          │
│  │   多模态      │  │  函数调用     │  │  会话记忆      │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                 │                 │                  │
│  ┌──────────────────────────────────────────────────────┐      │
│  │              RagController + RagService               │      │
│  │                   RAG 检索增强生成                     │      │
│  └──────────────────────┬───────────────────────────────┘      │
│                         │                                       │
│  ┌──────────────────────┴───────────────────────────────┐      │
│  │                    AiConfig                           │      │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐     │      │
│  │  │  ChatClient  │ │  ChatMemory  │ │ VectorStore │     │      │
│  │  └─────────────┘ └─────────────┘ └─────────────┘     │      │
│  └──────────────────────────────────────────────────────┘      │
│                         │                                       │
│  ┌──────────────────────┴───────────────────────────────┐      │
│  │              Spring AI 2.0.0 Core                     │      │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐  │      │
│  │  │  ChatModel    │ │ EmbeddingModel│ │  Advisor      │  │      │
│  │  └──────────────┘ └──────────────┘ └──────────────┘  │      │
│  └──────────────────────────────────────────────────────┘      │
│                         │                                       │
│  ┌──────────────────────┴───────────────────────────────┐      │
│  │           OpenAI Compatible API                        │      │
│  │     (GPT / 智谱 / 通义 / DeepSeek / ...)               │      │
│  └──────────────────────────────────────────────────────┘      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 21+ |
| 框架 | Spring Boot | 4.0.0 |
| AI 框架 | Spring AI | 2.0.0 GA |
| 数据库 | SimpleVectorStore (内存) | - |

## 核心组件

### ChatClient

Spring AI 2.0 推荐的高级客户端，封装了：
- 模型调用（同步/异步/流式）
- Prompt 构建
- Advisor 管理
- 错误处理

### Advisor 机制

Advisor 是 Spring AI 的扩展点，类似 AOP：

```
请求 → [前置 Advisor] → 模型调用 → [后置 Advisor] → 响应
```

本项目使用的 Advisor：

| Advisor | 功能 | 模块 |
|---------|------|------|
| QuestionAnswerAdvisor | RAG 检索增强 | spring-ai-vector-store-advisor |
| MessageChatMemoryAdvisor | 会话记忆 | spring-ai-starter-model-chat-memory |
| ToolCallingAdvisor | 函数调用 | spring-ai-tool-calling-advisor |

### VectorStore

向量存储用于 RAG 功能：

```
文档 → EmbeddingModel → 向量 → VectorStore
查询 → EmbeddingModel → 向量 → 相似度搜索 → 相关文档
```

## 请求流转示例

### RAG 问答流程

```
1. 用户 GET /api/rag/ask?question=Spring AI 是什么
2. RagController.askWithRag()
3. RagService.ragChat()
   ├── 创建 QuestionAnswerAdvisor
   ├── chatClient.prompt()
   │   ├── [QA Advisor 拦截]
   │   │   ├── 将问题向量化
   │   │   ├── VectorStore.similaritySearch()
   │   │   └── 获取相关文档
   │   ├── 将文档注入 Prompt 上下文
   │   ├── 调用 AI 模型
   │   └── 返回 AI 生成的回答
   └── 返回回答字符串
4. 返回 JSON 响应
```

### 多轮对话流程

```
1. 用户 GET /api/memory/chat?sessionId=xxx&message=你好
2. MemoryController.memoryChat()
3. chatClient.prompt()
   ├── [MessageChatMemoryAdvisor 拦截]
   │   ├── 从 ChatMemory 读取 sessionId 对应的历史消息
   │   ├── 将历史消息注入 Prompt
   │   └── 调用 AI 模型
   ├── [MessageChatMemoryAdvisor 后置]
   │   └── 将新对话存入 ChatMemory
   └── 返回 AI 回复
4. 用户第二次请求（相同 sessionId）
   └── AI 自动获取历史上下文
```
