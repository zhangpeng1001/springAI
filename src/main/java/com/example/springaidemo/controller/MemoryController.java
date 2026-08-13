package com.example.springaidemo.controller;

import com.example.springaidemo.common.Result;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 会话记忆（Chat Memory）控制器
 * <p>
 * 默认情况下，大模型是无状态的，每次请求都是独立的，不记得之前的对话。
 * 会话记忆功能让 AI 能够"记住"之前的对话内容，实现真正的多轮对话。
 * <p>
 * 核心概念：
 * <ul>
 *     <li>{@link ChatMemory}：管理对话历史的接口</li>
 *     <li>{@link MessageChatMemoryAdvisor}：Advisor（顾问），自动在请求中加入历史消息</li>
 *     <li>conversationId（会话 ID）：区分不同用户的对话，类似 session ID</li>
 * </ul>
 * <p>
 * 工作原理：
 * <ol>
 *     <li>每次请求时，Advisor 从 ChatMemory 读取该会话的历史消息</li>
 *     <li>将历史消息 + 当前消息一起发送给大模型</li>
 *     <li>大模型生成回复后，Advisor 自动将新消息存入 ChatMemory</li>
 * </ol>
 *
 * @author spring-ai-demo
 */
@RestController
@RequestMapping("/memory")
public class MemoryController {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public MemoryController(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
        this.chatClient = chatClientBuilder.build();
        this.chatMemory = chatMemory;
    }

    /**
     * 接口1：带记忆的多轮对话
     * <p>
     * 通过 conversationId 关联同一会话的消息，AI 会记住之前的对话。
     * <p>
     * 测试步骤（使用同一个 sessionId）：
     * 1. GET /memory/chat?sessionId=user1&message=我叫张三
     * 2. GET /memory/chat?sessionId=user1&message=我喜欢打篮球
     * 3. GET /memory/chat?sessionId=user1&message=我叫什么名字？我喜欢什么？
     * <p>
     * 第3步 AI 能正确回答"张三"和"打篮球"，因为它记住了之前的对话。
     *
     * @param sessionId 会话 ID，用于区分不同用户/对话
     * @param message   用户消息
     * @return AI 的回复
     */
    @GetMapping("/chat")
    public Result<String> chatWithMemory(
            @RequestParam(defaultValue = "default-session") String sessionId,
            @RequestParam String message) {

        // MessageChatMemoryAdvisor 的作用：
        // 1. 请求前：从 ChatMemory 读取 sessionId 对应的历史消息，加入当前请求
        // 2. 请求后：将本次的 user 消息和 assistant 回复存入 ChatMemory
        String response = chatClient.prompt()
                .user(message)
                .advisors(MessageChatMemoryAdvisor.builder(chatMemory)
                        .conversationId(sessionId)  // 绑定会话 ID
                        .build())
                .call()
                .content();

        return Result.success(response);
    }

    /**
     * 接口2：查看某个会话的历史消息
     * <p>
     * 演示如何从 ChatMemory 中获取存储的对话历史。
     * 访问示例：GET /memory/history?sessionId=user1
     *
     * @param sessionId 会话 ID
     * @return 该会话的所有消息列表
     */
    @GetMapping("/history")
    public Result<List<String>> getHistory(@RequestParam String sessionId) {
        // 从 ChatMemory 获取该会话的所有消息
        var messages = chatMemory.get(sessionId);

        // 将消息转换为可读格式
        List<String> history = new ArrayList<>();
        for (var message : messages) {
            String role = message.getMessageType().getValue();
            String content = message.getText();
            history.add(String.format("[%s]: %s", role, content));
        }

        return Result.success(history);
    }

    /**
     * 接口3：清除某个会话的记忆
     * <p>
     * 演示如何手动清除会话记忆。
     * 访问示例：GET /memory/clear?sessionId=user1
     *
     * @param sessionId 要清除的会话 ID
     * @return 操作结果
     */
    @GetMapping("/clear")
    public Result<String> clearMemory(@RequestParam String sessionId) {
        // 清除指定会话的所有消息
        chatMemory.clear(sessionId);
        return Result.success("会话 " + sessionId + " 的记忆已清除");
    }

    /**
     * 接口4：无记忆对话（对比演示）
     * <p>
     * 不使用 Advisor，每次请求都是独立的，AI 不会记得之前说了什么。
     * 可以与 /memory/chat 对比测试，体会记忆功能的差异。
     * 访问示例：GET /memory/no-memory?message=我叫什么名字？
     *
     * @param message 用户消息
     * @return AI 的回复
     */
    @GetMapping("/no-memory")
    public Result<String> chatWithoutMemory(@RequestParam String message) {
        // 不添加 MemoryAdvisor，每次都是全新对话
        String response = chatClient.prompt()
                .user(message)
                .call()
                .content();
        return Result.success(response);
    }
}
