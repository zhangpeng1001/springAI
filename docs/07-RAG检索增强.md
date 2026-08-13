# 07 - RAG 检索增强生成（Retrieval Augmented Generation）

> 对应代码：`RagController.java`、`RagService.java`，知识库：`resources/docs/`

## 功能说明

RAG（检索增强生成）是让 AI **基于你的私有知识库回答问题**的技术。

解决大模型的两大痛点：
1. **知识时效性**：大模型训练数据有截止日期，不知道最新信息
2. **私有知识**：大模型不知道你公司的内部文档、产品手册等

```
不用 RAG：
  用户："我们公司客服电话是多少？"
  AI："抱歉，我不知道你们公司的客服电话。"

用 RAG：
  AI 先检索知识库 → 找到"客服热线：400-888-9999"
  AI："你们公司的客服热线是400-888-9999。"
```

## 工作流程

### 数据准备阶段（写入）

```
┌──────────┐    ┌──────────────┐    ┌─────────────┐    ┌────────────┐
│ 1.读取文档 │ -> │ 2.分割成片段  │ -> │ 3.向量化(Embedding)│ -> │ 4.存入VectorStore│
│ TextReader│    │TextSplitter  │    │EmbeddingModel│    │ VectorStore │
└──────────┘    └──────────────┘    └─────────────┘    └────────────┘
```

### 问答阶段（检索+生成）

```
用户提问
  ↓
将问题向量化 → 在 VectorStore 中检索最相关的文档片段
  ↓
将检索到的文档片段 + 用户问题一起发送给大模型
  ↓
大模型基于上下文文档生成回答
```

## 核心概念

### Embedding（嵌入）

将文本转换为高维向量（如 1536 维），语义相近的文本向量距离也近。

```
"今天天气真好" → [0.12, -0.34, 0.56, ..., 0.78]  (1536维)
"今日气候不错" → [0.11, -0.35, 0.55, ..., 0.77]  (向量很接近)
"我想吃火锅"   → [0.89, 0.23, -0.67, ..., 0.01]  (向量差异大)
```

### VectorStore（向量存储）

存储文档向量，支持**相似度检索**——给定一个查询向量，找到最相似的文档。

### 相似度检索原理

```
用户问题："Spring AI是什么？"
  ↓ 向量化
查询向量 [0.15, -0.32, ...]
  ↓
VectorStore 计算查询向量与所有文档向量的相似度（余弦相似度）
  ↓
返回相似度最高的 Top-K 个文档片段
```

## 本项目的实现

### 知识库文档

项目在 `resources/docs/` 下提供了两个示例文档：

| 文档 | 内容 |
|------|------|
| `spring-ai-intro.txt` | Spring AI 介绍（功能、设计理念、版本等） |
| `company-info.txt` | 虚拟公司"智云科技"的产品手册（模拟私有知识） |

### 自动加载

`RagService` 在应用启动时（`@PostConstruct`）自动加载文档：

```java
@PostConstruct
public void init() {
    loadDocuments();
}

public void loadDocuments() {
    // 1. 读取文档
    List<Document> docs = readDocument(springAiIntroDoc, "spring-ai-intro");

    // 2. 分割成片段
    TokenTextSplitter splitter = new TokenTextSplitter();
    List<Document> splits = splitter.apply(docs);

    // 3. 写入向量存储（内部自动向量化）
    vectorStore.add(splits);
}
```

### 问答实现

使用 `QuestionAnswerAdvisor` 自动完成检索和注入：

```java
QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(vectorStore)
    .searchRequest(SearchRequest.builder()
        .topK(3)  // 检索最相关的3个片段
        .build())
    .build();

String response = chatClient.prompt()
    .user(question)
    .advisors(advisor)  // RAG 顾问自动检索+注入
    .call()
    .content();
```

`QuestionAnswerAdvisor` 内部自动完成：
1. 将用户问题向量化
2. 在 VectorStore 中检索相关文档
3. 将文档作为上下文注入 Prompt
4. 调用模型生成基于上下文的回答

## 接口说明

### 1. 知识库问答 `GET /rag/ask`

```
GET /rag/ask?question=Spring AI是什么
GET /rag/ask?question=智云科技客服电话是多少
```

AI 会基于知识库文档回答，而不是凭自身知识。

### 2. 检索文档片段 `GET /rag/search`

单独查看检索到了哪些文档片段（不调用大模型）。

```
GET /rag/search?query=Spring&topK=3
```

### 3. 不使用 RAG（对比） `GET /rag/no-rag`

```
GET /rag/no-rag?question=Spring AI是什么
```

直接让 AI 凭自身知识回答，可与 `/rag/ask` 对比效果。

### 4. 重新加载知识库 `GET /rag/reload`

文档更新后重新加载。

```
GET /rag/reload
```

## 测试对比

使用同一问题对比 RAG 效果：

```bash
# 使用 RAG（基于知识库）
curl "http://localhost:8080/rag/ask?question=智云科技的技术栈是什么"

# 不使用 RAG（AI自身知识）
curl "http://localhost:8080/rag/no-rag?question=智云科技的技术栈是什么"
```

- 使用 RAG：AI 能准确回答"Java 17 + Spring Boot 3 + Spring AI..."（来自知识库）
- 不使用 RAG：AI 不知道"智云科技"是什么（因为是虚构的公司）

## ETL 流程详解

### 1. DocumentReader（文档读取）

| Reader | 支持格式 |
|--------|---------|
| `TextReader` | 纯文本 .txt |
| `JsonReader` | JSON 文件 |
| `TikaDocumentReader` | PDF、Word、Excel 等（需额外依赖） |

### 2. TextSplitter（文档分割）

为什么需要分割？
- Embedding 模型有输入长度限制
- 检索时只返回相关片段，减少 token 消耗
- 小片段检索更精准

`TokenTextSplitter` 按 token 数量分割（比按字符更准确）。

### 3. VectorStore（向量存储）

| 实现 | 特点 |
|------|------|
| `SimpleVectorStore` | 内存版，适合学习（本项目使用） |
| `PgVectorStore` | PostgreSQL + pgVector，生产可用 |
| `RedisVectorStore` | Redis 向量存储 |
| `ChromaVectorStore` | Chroma 向量数据库 |
| `MilvusVectorStore` | Milvus 向量数据库 |

## 应用场景

| 场景 | 说明 |
|------|------|
| 企业知识库问答 | 对内部文档、手册、FAQ 进行问答 |
| 客服系统 | 基于产品文档自动回答客户问题 |
| 法律文档助手 | 检索法律条文并回答 |
| 学术研究 | 基于论文库回答研究问题 |
| 代码库问答 | 基于代码文档回答开发问题 |

## 学习要点

1. RAG = 检索 + 生成，先检索相关文档再让 AI 基于文档回答
2. ETL 流程：读取 → 分割 → 向量化 → 存储
3. `QuestionAnswerAdvisor` 自动完成检索和上下文注入
4. `SimpleVectorStore` 适合学习，生产环境用 PgVector/Milvus 等
5. `topK` 参数控制检索的文档片段数量
6. 对比 `/rag/ask` 和 `/rag/no-rag` 可以直观看到 RAG 的效果
