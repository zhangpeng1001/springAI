# RAG 检索增强生成

## 概述

RAG（Retrieval-Augmented Generation，检索增强生成）是一种结合信息检索和文本生成的 AI 技术，让 AI 能够基于你的私有知识进行回答。

## 核心概念

### 为什么需要 RAG？

| 纯 LLM | RAG |
|--------|-----|
| 知识截止到训练日期 | 实时知识更新 |
| 无法访问私有数据 | 基于你的知识库回答 |
| 容易产生幻觉 | 有据可查，提供引用 |
| 回答不可控 | 回答可控、可追溯 |

### RAG 工作流程

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  用户提问    │ → │  向量检索    │ → │  注入上下文  │ → │  AI 生成    │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
       ↓                  ↓                  ↓                  ↓
  Query Text        Vector Search      Prompt + Context    Final Answer
```

1. **用户提问**：接收自然语言问题
2. **向量检索**：将问题转换为向量，在向量库中搜索最相关的文档
3. **注入上下文**：将检索到的文档作为上下文添加到 Prompt 中
4. **AI 生成**：AI 基于提供的上下文生成回答

## API 列表

### 1. 初始化知识库

**POST** `/api/rag/init-sample-data`

预置示例数据，快速体验 RAG 功能。

**示例：**
```bash
curl -X POST "http://localhost:8080/api/rag/init-sample-data"
```

---

### 2. 添加知识文档

**POST** `/api/rag/documents`

将自定义文档添加到知识库。

**请求体：**
```json
{
  "documents": [
    {
      "content": "你的知识内容...",
      "source": "文档来源",
      "type": "文档类型"
    }
  ]
}
```

---

### 3. RAG 问答

**GET** `/api/rag/ask`

基于知识库的 AI 问答。

**参数：**
- `question` (String, 必填) - 用户问题

**示例：**
```bash
# 先初始化知识库
curl -X POST "http://localhost:8080/api/rag/init-sample-data"

# 然后提问
curl "http://localhost:8080/api/rag/ask?question=Spring AI 是什么"
```

---

### 4. 带来源的 RAG 问答

**GET** `/api/rag/ask-with-sources`

返回 AI 回答和引用的文档。

**参数：**
- `question` (String, 必填)

**返回：**
```json
{
  "answer": "Spring AI 是一个...",
  "sources": [
    {
      "content": "Spring AI 是一个用于构建 AI 应用的 Java 框架...",
      "metadata": {"source": "intro", "type": "overview"}
    }
  ]
}
```

---

### 5. 相似度搜索

**GET** `/api/rag/search`

直接执行向量相似度搜索（不经过 AI 生成），用于调试。

**参数：**
- `query` (String, 必填) - 查询文本
- `topK` (int, 可选) - 返回数量，默认 5

## 核心代码解析

### 文档入库

```java
// 创建文档
Document doc = Document.builder()
    .text("Spring AI 2.0 于 2026 年 6 月发布")
    .metadata(Map.of("source", "changelog", "version", "2.0.0"))
    .build();

// 添加到向量存储（自动调用 EmbeddingModel 生成向量）
vectorStore.add(List.of(doc));
```

### RAG 问答

```java
// 创建 QA Advisor
QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore).build();

// 使用 Advisor 进行 RAG 问答
String answer = chatClient.prompt()
    .user("Spring AI 2.0 什么时候发布的？")
    .advisors(qaAdvisor)  // 注入 QA Advisor，自动执行检索+增强
    .call()
    .content();
```

## 向量存储选择

| 存储 | 特点 | 适用场景 |
|------|------|----------|
| SimpleVectorStore | 内存实现，无需外部依赖 | 学习/演示 |
| PgVector | PostgreSQL 扩展 | 已有 PG 的项目 |
| Redis | Redis 支持向量 | 高性能需求 |
| Milvus | 专业向量数据库 | 大规模数据 |
| Elasticsearch | ES 支持向量 | 全文+向量混合 |

## Spring AI 2.0 要点

- `QuestionAnswerAdvisor` 移到 `spring-ai-vector-store-advisor` 模块
- `Document.builder()` 替代旧的构造方式
- `SearchRequest.builder()` 构建搜索请求
- `VectorStore.add()` 自动向量化
