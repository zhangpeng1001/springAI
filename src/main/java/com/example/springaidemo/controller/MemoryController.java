package com.example.springaidemo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.web.bind.annotation.*;

/**
 * 会话记忆控制器
 * <p>
 * 演示 Spring AI 2.0 的会话记忆（ChatMemory）功能：
 * <ul>
 *     <li>MessageChatMemoryAdvisor - 自动管理多轮对话历史</li>
 *     <li>会话隔离 - 每个用户有独立的对话历史</li>
 *     <li>记忆清除 - 支持清除指定会话的历史</li>
 * </ul>
 * <p>
 * Spring AI 2.0 重要变化：
 * <ul>
 *     <li>使用 MessageWindowChatMemory + InMemoryChatMemoryRepository</li>
 *     <li>MessageChatMemoryAdvisor 通过 builder(ChatMemory) 创建</li>
 *     <li>通过 ChatMemory.CONVERSATION_ID 常量传递会话 ID</li>
 * </ul>
 *
 * @author spring-ai-demo
 */
@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public MemoryController(ChatClient chatClient, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    /**
     * 带会话记忆的对话
     * <p>
     * 使用 MessageChatMemoryAdvisor 实现多轮对话记忆。
     * 相同 sessionId 的请求会共享记忆。
     * <p>
     * 通过 advisors() 方法添加 MessageChatMemoryAdvisor，
     * 并在 AdvisorSpec 中设置 CONVERSATION_ID 参数。
     *
     * @param sessionId 会话唯一标识，相同 ID 的请求会共享记忆
     * @param message   用户消息
     * @return AI 回复
     */
    @GetMapping("/chat")
    public String memoryChat(
            @RequestParam String sessionId,
            @RequestParam String message) {

        // 创建 MessageChatMemoryAdvisor
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        return chatClient.prompt()
                .user(message)
                // 添加记忆 Advisor
                .advisors(memoryAdvisor)
                // 设置会话 ID
                .advisors(advisorSpec -> advisorSpec
                        .param(ChatMemory.CONVERSATION_ID, sessionId))
                .call()
                .content();
    }

    /**
     * 带系统提示的记忆对话
     * <p>
     * 同时使用系统提示和会话记忆，让 AI 既有角色设定又能记住上下文。
     */
    @GetMapping("/chat-with-system")
    public String memoryChatWithSystem(
            @RequestParam String sessionId,
            @RequestParam String message,
            @RequestParam(defaultValue = "你是一个友好的助手，能够记住之前的对话上下文。") String systemPrompt) {

        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

        return chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .advisors(memoryAdvisor)
                .advisors(advisorSpec -> advisorSpec
                        .param(ChatMemory.CONVERSATION_ID, sessionId))
                .call()
                .content();
    }

    /**
     * 清除指定会话的记忆
     * <p>
     * Spring AI 2.0 中 ChatMemory.clear() 方法需要传 conversationId。
     */
    @DeleteMapping("/clear")
    public String clearMemory(@RequestParam String sessionId) {
        // 调用 ChatMemory 的 clear 方法清除指定会话
        chatMemory.clear(sessionId);
        return "会话 " + sessionId + " 的记忆已清除";
    }

    /**
     * 获取当前会话记忆实现类型
     */
    @GetMapping("/info")
    public String getMemoryInfo() {
        return "当前 ChatMemory 实现：" + chatMemory.getClass().getSimpleName()
                + "\n说明：MessageWindowChatMemory 数据存储在内存中，重启后丢失"
                + "\n通过 InMemoryChatMemoryRepository 持久化到内存"
                + "\n生产环境可实现 ChatMemoryRepository 接口来持久化数据";
    }
}
