package com.example.springaidemo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * 基础聊天控制器
 * <p>
 * 演示 Spring AI 2.0 中最核心的 ChatClient API 使用：
 * <ul>
 *     <li>简单问答 - 单轮对话</li>
 *     <li>流式输出 - token 逐个返回</li>
 *     <li>多轮对话 - 传入历史消息</li>
 *     <li>系统提示 - 使用 system() 方法</li>
 * </ul>
 * <p>
 * Spring AI 2.0 重要变化：
 * <ul>
 *     <li>Prompt 使用构造器传入消息列表，不再有 add() 方法</li>
 *     <li>CallResponseSpec 使用 chatResponse() 替代 response()</li>
 * </ul>
 *
 * @author spring-ai-demo
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 简单问答 - 单轮对话
     * <p>
     * 使用 ChatClient 的基础 API，发送一个用户消息，获取 AI 回复。
     * 这是 Spring AI 2.0 中最简洁的调用方式。
     *
     * @param message 用户问题
     * @return AI 回复文本
     */
    @GetMapping("/simple")
    public String simpleChat(@RequestParam String message) {
        // 一行代码实现 AI 对话：prompt() 创建请求，user() 添加用户消息，call() 发送，content() 获取文本
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    /**
     * 流式输出 - 逐 token 返回
     * <p>
     * 流式输出可以让用户看到 AI 的生成过程，提升交互体验。
     * Spring AI 2.0 使用 Project Reactor 的 Flux 来支持流式响应。
     * 前端可以通过 SSE (Server-Sent Events) 接收。
     *
     * @param message 用户问题
     * @return Flux 流，每个元素是一个 token 片段
     */
    @GetMapping("/stream")
    public Flux<String> streamChat(@RequestParam String message) {
        // stream() 方法返回 Flux<String>，每个 onNext 事件是一个 token
        return chatClient.prompt()
                .user(message)
                .stream()
                .content();
    }

    /**
     * 多轮对话 - 传入历史消息
     * <p>
     * 通过 Spring AI 2.0 的 ChatClient API，我们可以在请求中包含历史消息，
     * 让 AI 模型理解对话上下文。
     * <p>
     * 2.0 中 Prompt 使用构造器传入消息列表。
     *
     * @param request 包含历史消息列表的请求
     * @return AI 回复文本
     */
    @PostMapping("/multi-turn")
    public String multiTurnChat(@RequestBody MultiTurnRequest request) {
        // 构建消息列表
        List<Message> messages = new ArrayList<>();
        // 添加历史对话消息
        for (String historyMsg : request.getHistory()) {
            messages.add(new UserMessage(historyMsg));
        }
        // 添加当前用户消息
        messages.add(new UserMessage(request.getMessage()));

        // 使用 Prompt 构造器创建 Prompt
        Prompt prompt = new Prompt(messages);

        return chatClient.prompt(prompt)
                .call()
                .content();
    }

    /**
     * 带系统提示的对话
     * <p>
     * 通过 system() 方法设定 AI 的角色和行为规范，
     * 例如设定 AI 为专业的翻译助手、代码专家等。
     * <p>
     * Spring AI 2.0 中 system() 方法支持内联模板：
     * <pre>
     * .system(s -> s.text("你是{role}").param("role", "翻译专家"))
     * </pre>
     *
     * @param message 用户问题
     * @return AI 回复文本
     */
    @GetMapping("/with-system")
    public String chatWithSystemPrompt(
            @RequestParam String message,
            @RequestParam(defaultValue = "你是一个友好的AI助手。") String systemPrompt) {

        // 使用 system() 设置系统提示
        return chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .call()
                .content();
    }

    /**
     * 获取完整的 ChatResponse（包含元数据）
     * <p>
     * content() 只返回 AI 的回复文本。
     * chatResponse() 返回完整的 ChatResponse，包含：
     * <ul>
     *     <li>AI 回复的消息内容</li>
     *     <li>使用的 token 数量（prompt tokens, completion tokens）</li>
     *     <li>模型信息</li>
     * </ul>
     * <p>
     * 注意：2.0 中使用 chatResponse() 替代了 response()。
     */
    @GetMapping("/response")
    public ChatResponse getFullResponse(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .chatResponse();
    }

    /**
     * 请求 DTO - 多轮对话请求体
     */
    public static class MultiTurnRequest {
        /** 当前用户消息 */
        private String message;
        /** 历史对话消息列表 */
        private List<String> history;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public List<String> getHistory() { return history; }
        public void setHistory(List<String> history) { this.history = history; }
    }
}
