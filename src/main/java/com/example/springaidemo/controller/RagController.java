package com.example.springaidemo.controller;

import com.example.springaidemo.service.RagService;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 检索增强生成控制器
 * <p>
 * 提供知识库管理和 RAG 问答的 REST API。
 * RAG = Retrieval-Augmented Generation，检索增强生成。
 * <p>
 * 完整工作流程：
 * <pre>
 * 用户提问 → 向量检索 → 获取相关文档 → 注入上下文 → AI 生成回答
 * </pre>
 * <p>
 * Spring AI 2.0 重要变化：
 * <ul>
 *     <li>Document 使用 builder() 构建</li>
 *     <li>QuestionAnswerAdvisor 通过 builder(VectorStore) 创建</li>
 * </ul>
 *
 * @author spring-ai-demo
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * 添加知识文档到向量存储
     * <p>
     * 将文本内容作为文档添加到知识库，支持元数据标记。
     *
     * @param request 包含文档内容和元数据的请求
     * @return 添加结果
     */
    @PostMapping("/documents")
    public Map<String, Object> addDocuments(@RequestBody AddDocumentRequest request) {
        List<Document> documents = new ArrayList<>();

        for (AddDocumentRequest.DocItem item : request.getDocuments()) {
            // Spring AI 2.0 中 Document 使用 builder() 构建
            Map<String, Object> metadata = new HashMap<>();
            if (item.getSource() != null) {
                metadata.put("source", item.getSource());
            }
            if (item.getType() != null) {
                metadata.put("type", item.getType());
            }
            metadata.put("timestamp", System.currentTimeMillis());

            Document doc = Document.builder()
                    .text(item.getContent())
                    .metadata(metadata)
                    .build();
            documents.add(doc);
        }

        ragService.addDocuments(documents);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("addedCount", documents.size());
        return result;
    }

    /**
     * 使用 RAG 进行问答
     * <p>
     * 用户提问后，系统先从知识库检索相关内容，再让 AI 基于检索结果回答。
     *
     * @param question 用户问题
     * @return AI 基于知识库的回答
     */
    @GetMapping("/ask")
    public String askWithRag(@RequestParam String question) {
        return ragService.ragChat(question);
    }

    /**
     * 使用 RAG 问答并返回引用来源
     * <p>
     * 除了返回 AI 回答，还返回检索到的相关文档内容，便于验证答案来源。
     *
     * @param question 用户问题
     * @return 包含回答和引用文档的结果
     */
    @GetMapping("/ask-with-sources")
    public Map<String, Object> askWithSources(@RequestParam String question) {
        RagService.RagResult result = ragService.ragChatWithSources(question);

        Map<String, Object> response = new HashMap<>();
        response.put("answer", result.answer());

        List<Map<String, Object>> sources = new ArrayList<>();
        for (Document doc : result.sourceDocuments()) {
            Map<String, Object> source = new HashMap<>();
            source.put("content", doc.getText().length() > 200
                    ? doc.getText().substring(0, 200) + "..."
                    : doc.getText());
            source.put("metadata", doc.getMetadata());
            sources.add(source);
        }
        response.put("sources", sources);

        return response;
    }

    /**
     * 执行相似度搜索（仅检索，不生成回答）
     * <p>
     * 用于调试和验证知识库中的内容是否正确。
     *
     * @param query 查询文本
     * @param topK  返回最相似的文档数量
     * @return 相关文档列表
     */
    @GetMapping("/search")
    public List<Map<String, Object>> similaritySearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK) {

        List<Document> docs = ragService.similaritySearch(query, topK);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Document doc : docs) {
            Map<String, Object> item = new HashMap<>();
            item.put("content", doc.getText());
            item.put("metadata", doc.getMetadata());
            result.add(item);
        }

        return result;
    }

    /**
     * 初始化示例知识库数据
     * <p>
     * 预置一些常见问题的知识条目，方便快速体验 RAG 功能。
     */
    @PostMapping("/init-sample-data")
    public Map<String, Object> initSampleData() {
        List<Document> sampleDocs = List.of(
                Document.builder()
                        .text("Spring AI 是一个用于构建 AI 应用的 Java 框架，它提供了统一的 API 来对接各种 AI 模型和向量数据库。")
                        .metadata(Map.of("source", "intro", "type", "overview"))
                        .build(),
                Document.builder()
                        .text("Spring AI 2.0.0 于 2026年6月12日 正式发布，主要更新包括：ChatClient API 优化、函数调用改进、多模态支持增强等。")
                        .metadata(Map.of("source", "changelog", "type", "version"))
                        .build(),
                Document.builder()
                        .text("RAG（检索增强生成）的工作流程：1.将文档转换为向量存储 2.用户提问时检索相关文档 3.将文档作为上下文发送给 AI 4.AI 基于上下文生成回答。")
                        .metadata(Map.of("source", "rag-guide", "type", "tutorial"))
                        .build(),
                Document.builder()
                        .text("Spring AI 支持多种向量数据库：PgVector、Redis、Milvus、Elasticsearch、Neo4j、SimpleVectorStore(内存)等。")
                        .metadata(Map.of("source", "vectordb", "type", "overview"))
                        .build(),
                Document.builder()
                        .text("Spring AI 的 ChatMemory 功能支持多轮对话上下文管理，提供 MessageWindowChatMemory 实现。")
                        .metadata(Map.of("source", "memory-guide", "type", "tutorial"))
                        .build(),
                Document.builder()
                        .text("Spring AI 函数调用（Function Calling）通过 FunctionToolCallback 实现，允许 AI 模型在对话过程中调用外部工具和 API。")
                        .metadata(Map.of("source", "function-calling", "type", "tutorial"))
                        .build(),
                Document.builder()
                        .text("Spring AI 多模态功能支持同时处理文本和图片输入，通过 Media.builder() 创建媒体对象，实现图片分析、OCR 等功能。")
                        .metadata(Map.of("source", "multimodal", "type", "tutorial"))
                        .build(),
                Document.builder()
                        .text("Spring AI 结构化输出功能可以将 AI 返回的文本自动转换为 Java 对象，使用 entity() 方法支持 Bean、List、Map 等类型。")
                        .metadata(Map.of("source", "structured-output", "type", "tutorial"))
                        .build()
        );

        ragService.addDocuments(sampleDocs);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "示例数据已添加");
        result.put("count", sampleDocs.size());
        return result;
    }

    /**
     * 添加文档请求 DTO
     */
    public static class AddDocumentRequest {
        private List<DocItem> documents;

        public List<DocItem> getDocuments() { return documents; }
        public void setDocuments(List<DocItem> documents) { this.documents = documents; }

        public static class DocItem {
            private String content;
            private String source;
            private String type;

            public String getContent() { return content; }
            public void setContent(String content) { this.content = content; }
            public String getSource() { return source; }
            public void setSource(String source) { this.source = source; }
            public String getType() { return type; }
            public void setType(String type) { this.type = type; }
        }
    }
}
