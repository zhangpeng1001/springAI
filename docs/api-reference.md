# API 参考

## 基础路径

```
http://localhost:8080/api
```

## 聊天 API

### 简单对话

```
GET /api/chat/simple?message={message}
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | String | ✅ | 用户问题 |

**返回：** String（AI 回复文本）

---

### 流式对话

```
GET /api/chat/stream?message={message}
```

**返回：** SSE 流（text/event-stream）

---

### 多轮对话

```
POST /api/chat/multi-turn
Content-Type: application/json

{
  "message": "继续",
  "history": ["你好", "Spring AI 是什么？"]
}
```

---

### 系统提示对话

```
GET /api/chat/with-system?message={msg}&systemPrompt={prompt}
```

---

### 完整响应

```
GET /api/chat/response?message={message}
```

**返回：** ChatResponse JSON 对象

```json
{
  "results": [...],
  "metadata": {
    "id": "chatcmpl-xxx",
    "usage": {
      "promptTokens": 10,
      "completionTokens": 50,
      "totalTokens": 60
    }
  }
}
```

---

## 提示词 API

### 变量模板

```
GET /api/prompt/template?role={role}&task={task}
```

### 翻译助手

```
GET /api/prompt/translate?text={text}&targetLanguage={lang}&style={style}
```

### 代码审查

```
POST /api/prompt/code-review
Content-Type: application/json

{
  "code": "your code here"
}
```

### 知识讲解

```
GET /api/prompt/explain?concept={concept}&level={level}
```

---

## 结构化输出 API

### Bean 输出

```
GET /api/structured/bean?text={text}
```

**返回：**
```json
{
  "name": "张三",
  "age": 25,
  "email": "zhangsan@example.com"
}
```

### 标签生成

```
GET /api/structured/tags?content={content}
```

**返回：** `["标签1", "标签2"]`

---

## 多模态 API

### 图片分析

```
GET /api/multimodal/analyze?imageUrl={url}&question={q}
```

### 图片描述

```
GET /api/multimodal/describe?imageUrl={url}
```

### 图片对比

```
GET /api/multimodal/compare?imageUrl1={url1}&imageUrl2={url2}
```

---

## 函数调用 API

### 工具对话

```
GET /api/function/chat?message={message}
```

### 可用工具

```
GET /api/function/available-tools
```

---

## 会话记忆 API

### 记忆对话

```
GET /api/memory/chat?sessionId={id}&message={msg}
```

### 带系统提示

```
GET /api/memory/chat-with-system?sessionId={id}&message={msg}&systemPrompt={prompt}
```

### 清除记忆

```
DELETE /api/memory/clear?sessionId={id}
```

### 记忆信息

```
GET /api/memory/info
```

---

## RAG API

### 初始化知识库

```
POST /api/rag/init-sample-data
```

### 添加文档

```
POST /api/rag/documents
Content-Type: application/json

{
  "documents": [
    {
      "content": "文档内容",
      "source": "来源标签",
      "type": "类型标签"
    }
  ]
}
```

### RAG 问答

```
GET /api/rag/ask?question={question}
```

### 带来源问答

```
GET /api/rag/ask-with-sources?question={question}
```

**返回：**
```json
{
  "answer": "AI 回答",
  "sources": [
    {
      "content": "引用的文档内容...",
      "metadata": {"source": "intro"}
    }
  ]
}
```

### 相似度搜索

```
GET /api/rag/search?query={query}&topK={n}
```

---

## 使用 curl 测试

### Windows PowerShell

```powershell
# 简单对话
curl.exe "http://localhost:8080/api/chat/simple?message=你好"

# RAG 问答
curl.exe -X POST "http://localhost:8080/api/rag/init-sample-data"
curl.exe "http://localhost:8080/api/rag/ask?question=Spring%20AI%20是什么"
```

### Linux/Mac

```bash
# 简单对话
curl "http://localhost:8080/api/chat/simple?message=你好"

# RAG 问答
curl -X POST "http://localhost:8080/api/rag/init-sample-data"
curl "http://localhost:8080/api/rag/ask?question=Spring%20AI%20是什么"
```
