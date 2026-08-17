# 07 - RAG 检索增强生成

## 概述

RAG（Retrieval-Augmented Generation，检索增强生成）是 Spring AI 2.0 的核心功能之一。它通过从知识库中检索相关信息，结合 AI 模型生成更准确、更有依据的回答。

## 核心概念

### RAG 工作流程

```
用户提问
    │
    ▼
┌──────────────────┐
│  向量化问题       │  ← Embedding 模型
└──────────────────┘
    │
    ▼
┌──────────────────┐
│  向量检索        │  ← VectorStore
└──────────────────┘
    │
    ▼
┌──────────────────┐
│  拼接上下文      │
└──────────────────┘
    │
    ▼
┌──────────────────┐
│  AI 生成回答     │  ← ChatModel
└──────────────────┘
```

### 核心组件

- `VectorStore`：向量存储接口
- `EmbeddingModel`：文本向量化模型
- `QuestionAnswerAdvisor`：RAG 核心 Advisor
- `Document`：文档对象

## API 接口

### 1. 文档入库

将知识库文档添加到向量存储。

**请求：**
```
POST /api/rag/add
Body: ["Spring AI 2.0 支持 RAG 功能", "向量存储用于检索"]
```

**代码示例：**
```java
@PostMapping("/add")
public String addDocuments(@RequestBody List<String> contents) {
    List<Document> documents = contents.stream()
            .map(content -> new Document(content))
            .collect(Collectors.toList());

    vectorStore.add(documents);
    return "成功添加 " + documents.size() + " 个文档";
}
```

### 2. 基于知识库问答

**请求：**
```
GET /api/rag/chat?question=Spring AI 2.0 支持什么功能
```

**代码示例：**
```java
@GetMapping("/chat")
public String ragChat(@RequestParam String question) {
    QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore).build();

    return chatClient.prompt()
            .user(question)
            .advisors(qaAdvisor)
            .call()
            .content();
}
```

### 3. 相似度搜索

直接进行向量相似度搜索。

**请求：**
```
GET /api/rag/search?query=RAG&topK=5
```

**代码示例：**
```java
@GetMapping("/search")
public List<Document> similaritySearch(
        @RequestParam String query,
        @RequestParam(defaultValue = "5") int topK) {

    return vectorStore.similaritySearch(
            Search.query(query).withTopK(topK)
    );
}
```

### 4. 删除文档

**请求：**
```
DELETE /api/rag/delete?ids=id1,id2
```

**代码示例：**
```java
@DeleteMapping("/delete")
public String deleteDocuments(@RequestParam List<String> ids) {
    vectorStore.delete(ids);
    return "已删除 " + ids.size() + " 个文档";
}
```

### 5. 带元数据的文档

为文档添加元数据，便于过滤。

**请求：**
```
POST /api/rag/add-with-metadata
Body: { "content": "...", "metadata": { "category": "tutorial", "author": "张三" } }
```

**代码示例：**
```java
@PostMapping("/add-with-metadata")
public String addDocumentWithMetadata(@RequestBody DocumentRequest request) {
    Map<String, Object> metadata = request.getMetadata();
    metadata.put("timestamp", System.currentTimeMillis());

    Document doc = new Document(request.getContent(), metadata);
    vectorStore.add(List.of(doc));
    return "文档已添加";
}
```

## 配置说明

### VectorStore 配置

```java
@Configuration
public class AiConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        // 使用 SimpleVectorStore（内存实现）
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
```

### 支持的向量数据库

Spring AI 2.0 支持多种向量数据库：

| 数据库 | Maven 依赖 |
|--------|-----------|
| PostgreSQL | `spring-ai-pgvector-store` |
| Redis | `spring-ai-redis-store` |
| Pinecone | `spring-ai-pinecone-store` |
| Milvus | `spring-ai-milvus-store` |
| Elasticsearch | `spring-ai-elasticsearch-store` |
| Simple（内存） | `spring-ai-test-support` |

## 进阶用法

### 自定义检索策略

```java
@GetMapping("/custom-rag")
public String customRagChat(@RequestParam String question) {
    // 先进行相似度搜索
    List<Document> relevantDocs = vectorStore.similaritySearch(
            Search.query(question).withTopK(3)
    );

    // 构建上下文
    String context = relevantDocs.stream()
            .map(Document::getContent)
            .collect(Collectors.joining("\n\n"));

    // 带上下文的提问
    return chatClient.prompt()
            .system("请基于以下知识库内容回答问题。如果知识库中没有相关信息，请回答""未知""。\n\n知识库内容：\n" + context)
            .user(question)
            .call()
            .content();
}
```

### 文档预处理

```java
@Component
public class DocumentProcessor {

    public List<Document> preprocess(List<String> rawContents) {
        return rawContents.stream()
                .map(content -> {
                    // 分块处理
                    List<String> chunks = splitIntoChunks(content, 500);
                    return chunks.stream()
                            .map(chunk -> new Document(chunk, Map.of("source", "preprocessed")))
                            .collect(Collectors.toList());
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private List<String> splitIntoChunks(String text, int chunkSize) {
        // 简单的分块逻辑
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            chunks.add(text.substring(i, Math.min(i + chunkSize, text.length())));
        }
        return chunks;
    }
}
```

## 最佳实践

1. **文档预处理**：对长文档进行分块处理
2. **元数据管理**：添加有用的元数据便于过滤
3. **定期更新**：知识库需要定期更新
4. **相似度阈值**：设置合理的相似度阈值
5. **多轮检索**：对于复杂问题可进行多轮检索

## 注意事项

- RAG 的效果取决于知识库的质量
- 向量化过程需要额外的 Token 消耗
- SimpleVectorStore 仅适合学习和测试
- 生产环境请使用持久化的向量数据库