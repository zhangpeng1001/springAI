package com.example.springaidemo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.MimeType;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * 多模态控制器
 * <p>
 * 演示 Spring AI 2.0 的多模态功能，支持：
 * <ul>
 *     <li>图片理解 - 分析图片内容</li>
 *     <li>结合文本和图片的混合问答</li>
 * </ul>
 * <p>
 * 多模态模型（如 GPT-4o）能够同时处理文本和图片输入，
 * 实现"看图说话"、"图片分析"等功能。
 * <p>
 * Spring AI 2.0 重要变化：
 * <ul>
 *     <li>Media 使用 Builder 模式创建：Media.builder().mimeType(...).data(...).build()</li>
 *     <li>不再使用 Media.from() 静态方法</li>
 *     <li>user() 方法的 lambda 支持 media(Media...) 和 media(MimeType, URL)</li>
 * </ul>
 *
 * @author spring-ai-demo
 */
@RestController
@RequestMapping("/api/multimodal")
public class MultimodalController {

    private final ChatClient chatClient;

    public MultimodalController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 分析图片内容
     * <p>
     * 传入图片 URL 和问题，AI 会分析图片内容并回答。
     * Spring AI 2.0 中使用 Media.builder() 创建媒体对象。
     *
     * @param imageUrl 图片的 URL 地址
     * @param question 关于图片的问题
     * @return AI 对图片的分析结果
     */
    @GetMapping("/analyze")
    public String analyzeImage(
            @RequestParam String imageUrl,
            @RequestParam(defaultValue = "请描述这张图片的内容") String question) throws MalformedURLException {

        // Spring AI 2.0 中使用 Media.builder() 创建媒体对象
        // 支持通过 URI、Resource 或 byte[] 传入数据
        Media imageMedia = Media.builder()
                .mimeType(MimeType.valueOf("image/jpeg"))
                .data(URI.create(imageUrl))
                .build();

        // 使用 ChatClient 2.0 的 fluent API 构建带图片的消息
        // user() 方法支持 media() 参数来添加图片
        return chatClient.prompt()
                .user(u -> u.text(question).media(imageMedia))
                .call()
                .content();
    }

    /**
     * 图片描述生成
     * <p>
     * 让 AI 自动描述图片内容，不需要额外提问。
     */
    @GetMapping("/describe")
    public String describeImage(@RequestParam String imageUrl) throws MalformedURLException {
        Media imageMedia = Media.builder()
                .mimeType(MimeType.valueOf("image/jpeg"))
                .data(URI.create(imageUrl))
                .build();

        return chatClient.prompt()
                .user(u -> u.text("请详细描述这张图片的内容，包括：1.图片中有什么 2.场景是什么 3.可能的用途").media(imageMedia))
                .call()
                .content();
    }

    /**
     * 对比分析两张图片
     * <p>
     * 传入两张图片的 URL，让 AI 进行对比分析。
     * 支持通过 media(MimeType, URL) 快捷方式添加图片。
     */
    @GetMapping("/compare")
    public String compareImages(
            @RequestParam String imageUrl1,
            @RequestParam String imageUrl2,
            @RequestParam(defaultValue = "请对比这两张图片的异同") String question) throws MalformedURLException {

        // 使用 Media.builder() 分别构建两个媒体对象
        Media image1 = Media.builder()
                .mimeType(MimeType.valueOf("image/jpeg"))
                .data(URI.create(imageUrl1))
                .build();
        Media image2 = Media.builder()
                .mimeType(MimeType.valueOf("image/jpeg"))
                .data(URI.create(imageUrl2))
                .build();

        return chatClient.prompt()
                .user(u -> u.text(question)
                        .media(image1)
                        .media(image2))
                .call()
                .content();
    }

    /**
     * 支持的图片格式列表
     */
    @GetMapping("/supported-formats")
    public String[] getSupportedFormats() {
        return new String[]{
                "image/jpeg",
                "image/png",
                "image/gif",
                "image/webp"
        };
    }
}
