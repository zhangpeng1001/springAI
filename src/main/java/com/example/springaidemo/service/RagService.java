package com.example.springaidemo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG (Retrieval-Augmented Generation) 服务
 * <p>
 * RAG 是"检索增强生成"的缩写，核心思想是：
 * <ol>
 *     <li>将外部知识（文档、数据库等）转换为向量存储</li>
 *     <li>用户提问时，先从向量存储中检索相关内容</li>
 *     <li>将检索到的内容作为上下文发送给 AI 模型</li>
 *     <li>AI 基于提供的上下文生成准确回答</li>
 * </ol>
 * <p>
 * 这种方式的优点：
 * <ul>
 *     <li>让 AI 回答基于你的私有知识，而不是依赖训练数据</li>
 *     <li>可以提供可溯源的答案（引用文档来源）</li>
 *     <li>减少 AI 幻觉，提高回答准确性</li>
 * </ul>
 * <p>
 * Spring AI 2.0 重要变化：
 * <ul>
 *     <li>QuestionAnswerAdvisor 通过 builder(VectorStore) 创建</li>
 *     <li>SearchRequest 使用 builder() 静态方法构建</li>
 *     <li>Document 使用 builder() 构建</li>
 * </ul>
 *
 * @author spring-ai-demo
 */
@Service
public class RagService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public RagService(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    /**
     * 将文档添加到向量存储
     * <p>
     * 文档会被自动切片（chunk）并转换为向量存储。
     * Spring AI 2.0 的 VectorStore 接口提供了 add() 方法。
     *
     * @param documents 文档列表，每个文档包含内容和可选的元数据
     */
    public void addDocuments(List<Document> documents) {
        // VectorStore.add() 会自动调用 EmbeddingModel 将文本转换为向量
        // 然后存储到向量数据库中
        vectorStore.add(documents);
    }

    /**
     * 简单相似性搜索
     * <p>
     * 根据查询文本在向量存储中检索最相似的文档。
     *
     * @param query 查询文本
     * @param topK  返回最相似的文档数量
     * @return 相关文档列表
     */
    public List<Document> similaritySearch(String query, int topK) {
        // SearchRequest 构建搜索请求
        // Spring AI 2.0 中 SearchRequest 使用 builder() 静态方法
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();

        return vectorStore.similaritySearch(request);
    }

    /**
     * 使用 RAG 进行问答
     * <p>
     * 这是 RAG 的核心方法：
     * <ol>
     *     <li>创建 QuestionAnswerAdvisor，它会自动从 VectorStore 检索相关文档</li>
     *     <li>将检索到的文档作为上下文注入到 Prompt 中</li>
     *     <li>AI 基于上下文生成回答</li>
     * </ol>
     *
     * @param question 用户问题
     * @return AI 基于知识库的回答
     */
    public String ragChat(String question) {
        // 创建 QuestionAnswerAdvisor
        // Spring AI 2.0 中通过 builder(VectorStore) 创建
        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore).build();

        // 通过 advisors() 将 QA Advisor 注册到本次对话中
        return chatClient.prompt()
                .user(question)
                .advisors(qaAdvisor)
                .call()
                .content();
    }

    /**
     * 使用 RAG 问答并返回引用的文档
     * <p>
     * 除了返回 AI 的回答，还返回检索到的相关文档，便于溯源。
     *
     * @param question 用户问题
     * @return RAG 问答结果，包含回答和引用的文档
     */
    public RagResult ragChatWithSources(String question) {
        // 先执行检索，获取相关文档
        List<Document> relevantDocs = similaritySearch(question, 3);

        // 使用 QA Advisor 进行 RAG 问答
        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore).build();

        String answer = chatClient.prompt()
                .user(question)
                .advisors(qaAdvisor)
                .call()
                .content();

        return new RagResult(answer, relevantDocs);
    }

    /**
     * RAG 结果 DTO
     */
    public record RagResult(String answer, List<Document> sourceDocuments) {}
}
