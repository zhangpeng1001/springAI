package com.example.springaidemo.controller;

import com.example.springaidemo.common.Result;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 聊天功能控制器 - 演示 Spring AI 基础聊天能力
 * <p>
 * 本控制器演示三种调用大模型的方式：
 * <ol>
 *     <li>使用 ChatClient（推荐，高级 API，支持流式、工具、记忆等）</li>
 *     <li>使用 ChatModel（底层 API，直接操作 Prompt 和获取完整 ChatResponse）</li>
 *     <li>覆盖运行时参数（在调用时动态调整 temperature 等参数）</li>
 * </ol>
 *
 * @author spring-ai-demo
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    /** ChatClient：Spring AI 推荐使用的聊天客户端，链式 API，功能丰富 */
    private final ChatClient chatClient;

    /** ChatModel：底层聊天模型，提供更细粒度的控制 */
    private final ChatModel chatModel;

    /**
     * 构造器注入。
     * Spring AI 自动配置会创建 ChatClient.Builder 和 ChatModel，
     * 我们通过 Builder 构建 ChatClient 实例。
     */
    public ChatController(ChatClient.Builder chatClientBuilder, ChatModel chatModel) {
        // 通过 builder.build() 得到 ChatClient 实例
        // 此时已带上了 AiConfig 中设置的默认系统提示词
        this.chatClient = chatClientBuilder.build();
        this.chatModel = chatModel;
    }

    /**
     * 接口1：使用 ChatClient 进行简单对话（推荐方式）
     * <p>
     * 访问示例：GET /chat/simple?message=你好
     *
     * @param message 用户输入的消息
     * @return AI 的回复内容
     */
    @GetMapping("/simple")
    public Result<String> chatSimple(@RequestParam(defaultValue = "你好，请介绍一下你自己") String message) {
        // ChatClient.prompt() 开始一个请求构建器
        // .user() 设置用户消息
        // .call() 同步调用模型
        // .content() 直接获取文本内容
        String response = chatClient.prompt()
                .user(message)
                .call()
                .content();
        return Result.success(response);
    }

    /**
     * 接口2：使用 ChatClient 获取完整的响应对象
     * <p>
     * ChatResponse 包含更多元信息：token 使用量、finish_reason 等
     * 访问示例：GET /chat/detail?message=讲个笑话
     *
     * @param message 用户输入的消息
     * @return 完整的响应信息（字符串形式，便于查看）
     */
    @GetMapping("/detail")
    public Result<String> chatDetail(@RequestParam(defaultValue = "讲一个简短的笑话") String message) {
        // .chatResponse() 返回 ChatResponse 对象，包含 token 使用统计等信息
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .call()
                .chatResponse();

        // 获取返回内容
        String content = chatResponse.getResult().getOutput().getText();
        // 获取 token 使用量（输入/输出/总计）
        var usage = chatResponse.getMetadata().getUsage();

        String detail = String.format(
                "回复内容：%s%n---%nToken 使用：输入 %d / 输出 %d / 总计 %d",
                content,
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens()
        );
        return Result.success(detail);
    }

    /**
     * 接口3：使用底层 ChatModel 直接调用
     * <p>
     * ChatModel 是更底层的 API，ChatClient 内部也是调用它。
     * 直接使用 ChatModel 可以更灵活地构造 Prompt。
     * 访问示例：GET /chat/model?message=什么是Spring AI
     *
     * @param message 用户输入的消息
     * @return AI 的回复内容
     */
    @GetMapping("/model")
    public Result<String> chatWithModel(@RequestParam(defaultValue = "什么是 Spring AI？") String message) {
        // 构造 Prompt 对象
        Prompt prompt = new Prompt(message);
        // 调用模型并获取响应
        ChatResponse response = chatModel.call(prompt);
        // 提取文本内容
        String content = response.getResult().getOutput().getText();
        return Result.success(content);
    }

    /**
     * 接口4：在运行时覆盖模型参数（如 temperature）
     * <p>
     * 演示如何在单次请求中动态调整参数，而不修改全局配置。
     * 例如：创意写作用高 temperature，事实问答用低 temperature。
     * 访问示例：GET /chat/creative?message=写一首关于春天的诗
     *
     * @param message 用户输入的消息
     * @return AI 的回复内容
     */
    @GetMapping("/creative")
    public Result<String> chatCreative(@RequestParam(defaultValue = "写一首关于春天的四行诗") String message) {
        // OpenAiChatOptions 可以在运行时覆盖单个请求的参数
        // 这里将 temperature 调高到 1.2，让回答更有创造力
        String response = chatClient.prompt()
                .user(message)
                .options(OpenAiChatOptions.builder()
                        .temperature(1.2)  // 高温度，更有创造力
                        .build())
                .call()
                .content();
        return Result.success(response);
    }
}
