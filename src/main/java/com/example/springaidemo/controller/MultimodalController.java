package com.example.springaidemo.controller;

import com.example.springaidemo.common.Result;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.Resource;

import java.net.URL;

/**
 * 多模态（Multimodal）控制器
 * <p>
 * 多模态是指模型能够同时处理多种类型的输入，如文本、图片、音频等。
 * Spring AI 支持在用户消息中嵌入图片，让大模型"看图说话"。
 * <p>
 * 支持多模态的模型：GPT-4o、GPT-4o-mini、Claude 3、Gemini 等。
 * <p>
 * 本控制器演示：
 * <ol>
 *     <li>通过图片 URL 让 AI 分析网络图片</li>
 *     <li>通过本地文件让 AI 分析本地图片</li>
 *     <li>使用 ImageModel 生成图片（文生图）</li>
 * </ol>
 *
 * @author spring-ai-demo
 */
@RestController
@RequestMapping("/multimodal")
public class MultimodalController {

    private final ChatClient chatClient;
    private final ImageModel imageModel;

    public MultimodalController(ChatClient.Builder chatClientBuilder, ImageModel imageModel) {
        this.chatClient = chatClientBuilder.build();
        this.imageModel = imageModel;
    }

    /**
     * 接口1：通过图片 URL 分析网络图片
     * <p>
     * 使用 ChatClient 的 fluent API，在 user 消息中嵌入图片。
     * .user(u -> u.text(...).media(...)) 是推荐的多模态调用方式。
     * 访问示例：GET /multimodal/url?imageUrl=https://...&question=描述这张图片
     *
     * @param imageUrl 图片的 URL 地址
     * @param question 关于图片的问题
     * @return AI 对图片的描述/回答
     */
    @GetMapping("/url")
    public Result<String> analyzeImageUrl(
            @RequestParam String imageUrl,
            @RequestParam(defaultValue = "请详细描述这张图片的内容") String question) {

        // 提前创建 URL 对象（lambda 内不能抛出受检异常）
        URL url;
        try {
            url = new URL(imageUrl);
        } catch (java.net.MalformedURLException e) {
            return Result.error("图片 URL 格式错误：" + e.getMessage());
        }

        // 使用 ChatClient 的 fluent API 处理多模态：
        // .user(Consumer) 中可以设置文本和媒体（图片）
        // .media(MimeType, URL) 指定图片的 MIME 类型和 URL
        // 注意：多模态需要使用支持视觉的模型（如 gpt-4o）
        String response = chatClient.prompt()
                .user(u -> u.text(question).media(MimeTypeUtils.IMAGE_PNG, url))
                .options(OpenAiChatOptions.builder().model("gpt-4o").build())
                .call()
                .content();

        return Result.success(response);
    }

    /**
     * 接口2：分析本地图片文件
     * <p>
     * 从 classpath 加载本地图片（放在 resources/images/ 目录下），
     * 通过 .media(MimeType, Resource) 传入模型。
     * 访问示例：GET /multimodal/local?question=这张图里有什么
     *
     * @param question 关于图片的问题
     * @return AI 对图片的描述/回答
     */
    @GetMapping("/local")
    public Result<String> analyzeLocalImage(
            @RequestParam(defaultValue = "请描述这张图片的内容") String question) {

        // 从 classpath 加载图片资源
        // 请在 src/main/resources/images/ 目录下放置一张测试图片（如 sample.png）
        Resource imageResource = new ClassPathResource("images/sample.png");

        if (!imageResource.exists()) {
            return Result.error("未找到测试图片，请在 src/main/resources/images/ 目录下放置 sample.png");
        }

        // .media(MimeType, Resource) 可以直接传入本地文件资源
        String response = chatClient.prompt()
                .user(u -> u.text(question).media(MimeTypeUtils.IMAGE_PNG, imageResource))
                .options(OpenAiChatOptions.builder().model("gpt-4o").build())
                .call()
                .content();

        return Result.success(response);
    }

    /**
     * 接口3：文生图 - 使用 ImageModel 生成图片
     * <p>
     * ImageModel 是 Spring AI 提供的图像生成接口，
     * 调用 DALL-E 等文生图模型，根据文字描述生成图片。
     * 访问示例：GET /multimodal/generate?prompt=一只在月亮上的猫
     *
     * @param prompt 图片描述提示词
     * @return 生成图片的 URL
     */
    @GetMapping("/generate")
    public Result<String> generateImage(@RequestParam(defaultValue = "一只可爱的橘猫坐在窗台上看夕阳") String prompt) {
        // 构建图像生成请求
        ImagePrompt imagePrompt = new ImagePrompt(prompt);

        // 调用模型生成图片
        ImageResponse response = imageModel.call(imagePrompt);

        // 获取生成图片的 URL
        String imageUrl = response.getResult().getOutput().getUrl();

        return Result.success("图片生成成功，URL：" + imageUrl);
    }
}
