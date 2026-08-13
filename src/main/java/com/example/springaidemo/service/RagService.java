package com.example.springaidemo.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * RAG（检索增强生成）服务
 * <p>
 * RAG 是让 AI 基于你的私有知识库回答问题的技术，解决大模型的两大痛点：
 * <ul>
 *     <li>知识时效性：大模型训练数据有截止日期，不知道最新信息</li>
 *     <li>私有知识：大模型不知道你公司的内部文档、产品手册等</li>
 * </ul>
 * <p>
 * RAG 工作流程：
 * <pre>
 *   ┌─────────────┐    ┌──────────────┐    ┌─────────────┐    ┌────────────┐
 *   │ 1.读取文档   │ -> │ 2.分割成片段  │ -> │ 3.向量化存储  │ -> │ 4.检索+生成 │
 *   │ TextReader  │    │ TextSplitter │    │ VectorStore │    │ Advisor    │
 *   └─────────────┘    └──────────────┘    └─────────────┘    └────────────┘
 * </pre>
 * <p>
 * 本服务在应用启动时自动加载 resources/docs/ 下的文档到向量存储。
 *
 * @author spring-ai-demo
 */
@Service
public class RagService {

    /** 向量存储：存储文档的向量表示，支持相似度检索 */
    private final VectorStore vectorStore;

    @Value("classpath:/docs/spring-ai-intro.txt")
    private Resource springAiIntroDoc;

    @Value("classpath:/docs/company-info.txt")
    private Resource companyInfoDoc;

    /**
     * 构造器注入。
     * VectorStore Bean 在 AiConfig 中手动配置（SimpleVectorStore + EmbeddingModel）。
     *
     * @param vectorStore 向量存储
     */
    public RagService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 应用启动后自动执行：加载知识库文档到向量存储
     * <p>
     * @PostConstruct 保证在 Bean 初始化完成后执行。
     * 实际项目中可以改为定时任务或手动触发接口。
     */
    @PostConstruct
    public void init() {
        loadDocuments();
    }

    /**
     * 加载所有知识库文档到向量存储
     * <p>
     * 这是 RAG 的"写入"阶段（数据准备）：
     * 1. 读取文档 -> Document 对象
     * 2. 分割成片段（避免超过模型 token 限制）
     * 3. 向量化并写入 VectorStore
     */
    public void loadDocuments() {
        List<Document> allDocuments = new ArrayList<>();

        // 1. 读取文档文件
        // TextReader 将文本文件内容读取为 Document 对象
        allDocuments.addAll(readDocument(springAiIntroDoc, "spring-ai-intro"));
        allDocuments.addAll(readDocument(companyInfoDoc, "company-info"));

        // 2. 分割文档
        // 大文档需要分割成小片段，因为：
        // - Embedding 模型有输入长度限制
        // - 检索时只返回相关片段，减少 token 消耗
        // TokenTextSplitter 按 token 数量分割（不是按字符），更准确
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> splitDocuments = splitter.apply(allDocuments);

        // 3. 写入向量存储
        // VectorStore.add() 内部会：
        // - 调用 EmbeddingModel 将文本转为向量
        // - 将向量和文本一起存储
        vectorStore.add(splitDocuments);

        System.out.println("[RAG] 知识库加载完成，共加载 " + splitDocuments.size() + " 个文档片段");
    }

    /**
     * 读取单个文档资源
     */
    private List<Document> readDocument(Resource resource, String sourceName) {
        try {
            TextReader reader = new TextReader(resource);
            // 给文档添加元数据（来源标记），便于后续过滤
            reader.getCustomMetadata().put("source", sourceName);
            return reader.get();
        } catch (Exception e) {
            System.err.println("[RAG] 读取文档失败: " + sourceName + ", " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 手动检索相关文档（不调用大模型，仅返回相关片段）
     * <p>
     * 演示如何单独使用向量检索功能。
     *
     * @param query 查询文本
     * @param topK  返回最相关的 K 条结果
     * @return 检索到的文档片段列表
     */
    public List<String> searchDocuments(String query, int topK) {
        // VectorStore.similaritySearch() 执行向量相似度检索
        // 它会先将 query 转为向量，然后在存储中找最相似的文档
        return vectorStore.similaritySearch(query).stream()
                .limit(topK)
                .map(Document::getText)
                .toList();
    }

    /**
     * 获取向量存储（供 Controller 中的 Advisor 使用）
     */
    public VectorStore getVectorStore() {
        return vectorStore;
    }
}
