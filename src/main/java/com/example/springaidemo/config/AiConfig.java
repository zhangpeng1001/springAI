package com.example.springaidemo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置类
 * <p>
 * 该类负责配置 Spring AI 的核心组件，主要是 {@link ChatClient}。
 * <p>
 * ChatClient 是 Spring AI 提供的聊天客户端入口，类似于 RestTemplate / WebClient，
 * 用于与大语言模型交互。它通过 Builder 模式构建，可以设置：
 * <ul>
 *     <li>默认系统提示词（defaultSystem）：为所有请求设定角色/行为</li>
 *     <li>默认工具（defaultTools）：注入可调用的函数</li>
 *     <li>默认顾问（defaultAdvisors）：注入如记忆、RAG 等顾问</li>
 * </ul>
 *
 * @author spring-ai-demo
 */
@Configuration
public class AiConfig {

    /**
     * 注册一个全局的 ChatClient.Builder Bean
     * <p>
     * Spring AI 自动配置已经提供了 ChatClient.Builder，我们在此基础上设置默认系统提示词，
     * 这样所有注入该 Builder 的地方都会带上这个默认角色设定。
     * <p>
     * 使用 Builder 而非直接 ChatClient 的好处：
     * 1. 可以在不同场景下克隆并定制（如添加不同的 advisor）
     * 2. 每次调用 build() 都得到独立的 ChatClient，互不干扰
     *
     * @param builder Spring AI 自动注入的 ChatClient.Builder
     * @return 配置好默认系统提示词的 Builder
     */
    @Bean
    public ChatClient.Builder chatClientBuilder(ChatClient.Builder builder) {
        // 设置默认系统提示词：让 AI 扮演一个友好的中文学习助手
        return builder.defaultSystem("你是一个友好的中文 AI 学习助手，回答要简洁、准确，并使用中文。"
                + "在回答技术问题时，可以适当给出示例。");
    }

    /**
     * 会话记忆仓库 Bean（内存版）
     * <p>
     * ChatMemoryRepository 负责持久化存储对话历史。
     * InMemoryChatMemoryRepository 将对话保存在内存中，应用重启后丢失。
     * 生产环境可替换为 JDBC 版（spring-ai-starter-model-chat-memory-repository-jdbc）实现持久化。
     */
    @Bean
    public ChatMemoryRepository chatMemoryRepository() {
        return new InMemoryChatMemoryRepository();
    }

    /**
     * 会话记忆 Bean
     * <p>
     * MessageWindowChatMemory 是 ChatMemory 的实现，它保留最近 N 条消息（滑动窗口），
     * 超出窗口的旧消息会被丢弃，避免 token 超限。
     *
     * @param repository 记忆仓库
     * @return 配置好的 ChatMemory
     */
    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(20)  // 保留最近 20 条消息
                .build();
    }

    /**
     * 向量存储 Bean（内存版）
     * <p>
     * SimpleVectorStore 是 Spring AI 提供的内存版向量存储，无需安装额外数据库。
     * 它通过 EmbeddingModel 将文本转为向量并存储在内存中。
     * <p>
     * 注意：SimpleVectorStore 没有对应的 starter 自动配置，需要手动创建 Bean。
     * 生产环境请替换为 PgVectorStore、RedisVectorStore 等持久化实现。
     *
     * @param embeddingModel 嵌入模型（由 spring-ai-starter-model-openai 自动配置）
     * @return 配置好的 VectorStore
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
