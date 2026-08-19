package com.example.springaidemo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring AI 核心组件配置类
 * <p>
 * 在 Spring AI 2.0.0 中，主要配置以下核心组件：
 * <ul>
 *     <li>ChatClient - AI 对话客户端，提供流式和非流式调用</li>
 *     <li>ChatMemory - 会话记忆，支持多轮对话上下文</li>
 *     <li>VectorStore - 向量存储，用于 RAG 检索增强</li>
 * </ul>
 * <p>
 * Spring Boot 自动配置已经为我们创建了 ChatModel 和 EmbeddingModel，
 * 这里我们手动组装高级组件。
 *
 * @author spring-ai-demo
 */
@Configuration
public class AiConfig {

    /**
     * 配置 ChatClient 客户端
     * <p>
     * ChatClient 是 Spring AI 2.0 推荐使用的高级 API，
     * 它封装了 ChatModel、Prompt、Advisor 等底层组件，
     * 提供更简洁、更易扩展的调用方式。
     * <p>
     * 使用示例：
     * <pre>
     * chatClient.prompt()
     *     .user("你好")
     *     .call()
     *     .content();
     * </pre>
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * 配置会话记忆（ChatMemory）
     * <p>
     * Spring AI 2.0.0 中 ChatMemory 的实现发生了变化：
     * <ul>
     *     <li>MessageWindowChatMemory - 内存版，限制窗口大小</li>
     *     <li>ChatMemoryRepository - 存储仓库接口</li>
     *     <li>InMemoryChatMemoryRepository - 内存仓库实现</li>
     * </ul>
     * <p>
     * 2.0 中不再提供 InMemoryChatMemory，
     * 使用 MessageWindowChatMemory + InMemoryChatMemoryRepository 组合替代。
     */
    @Bean
    public ChatMemory chatMemory() {
        // 使用 MessageWindowChatMemory，支持窗口大小限制
        // InMemoryChatMemoryRepository 存储在内存中，应用重启后数据会丢失
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                // 设置最大保留消息数（每轮对话包含 user+assistant 两条）
                .maxMessages(100)
                .build();
    }

    /**
     * 配置内存向量存储
     * <p>
     * SimpleVectorStore 是一个基于内存的向量存储实现，
     * 适合学习和演示。数据存储在内存中，应用重启后会丢失。
     * <p>
     * 生产环境可选择以下持久化向量存储：
     * <ul>
     *     <li>PgVector (PostgreSQL)</li>
     *     <li>Redis</li>
     *     <li>Milvus</li>
     *     <li>Elasticsearch</li>
     *     <li>Neo4j</li>
     * </ul>
     *
     * @param embeddingModel 嵌入模型，用于将文档内容转换为向量
     */
    @Bean
    @Primary
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
