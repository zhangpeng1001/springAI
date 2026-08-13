package com.example.springaidemo.controller;

import com.example.springaidemo.common.Result;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 提示词模板（Prompt Template）控制器
 * <p>
 * 提示词模板是 Spring AI 的核心功能之一，作用类似于模板引擎（如 Thymeleaf），
 * 可以将变量动态填充到提示词中，实现提示词的复用和管理。
 * <p>
 * Spring AI 使用 StringTemplate 语法，变量使用 {变量名} 形式。
 * <p>
 * 本控制器演示：
 * <ol>
 *     <li>内联提示词模板：直接在代码中定义模板字符串</li>
 *     <li>外部提示词模板：从 .st 文件加载模板（便于管理和版本控制）</li>
 *     <li>系统提示词：定义 AI 的角色和行为</li>
 * </ol>
 *
 * @author spring-ai-demo
 */
@RestController
@RequestMapping("/prompt")
public class PromptController {

    private final ChatClient chatClient;

    /**
     * 从 classpath 加载外部提示词模板文件。
     * <p>
     * 使用 @Value 注入 Resource，Spring 会自动加载 resources/prompts/ 下的文件。
     * 将提示词放在外部文件的好处：
     * 1. 提示词与代码分离，便于修改和迭代
     * 2. 可以由非开发人员（如产品经理）维护
     * 3. 便于做 A/B 测试和多语言
     */
    @Value("classpath:/prompts/translate.st")
    private Resource translatePromptResource;

    public PromptController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 接口1：使用内联提示词模板
     * <p>
     * 直接在代码中创建 PromptTemplate，并使用 Map 传入变量。
     * 适合简单的、一次性的提示词。
     * 访问示例：GET /prompt/inline?topic=Java&language=Python
     *
     * @param topic    要比较的主题
     * @param language 对比的语言
     * @return AI 的回复
     */
    @GetMapping("/inline")
    public Result<String> inlineTemplate(
            @RequestParam(defaultValue = "面向对象编程") String topic,
            @RequestParam(defaultValue = "Python") String language) {

        // 1. 创建提示词模板，使用 {变量名} 占位符
        PromptTemplate promptTemplate = new PromptTemplate(
                "你是一个技术讲师。请简要对比 {topic} 在 Java 和 {language} 中的实现差异。\n" +
                        "要求：用中文回答，不超过 200 字。"
        );

        // 2. 填充变量，生成 Prompt
        Prompt prompt = promptTemplate.create(Map.of(
                "topic", topic,
                "language", language
        ));

        // 3. 调用模型
        String response = chatClient.prompt(prompt).call().content();
        return Result.success(response);
    }

    /**
     * 接口2：使用外部提示词模板文件
     * <p>
     * 从 classpath:/prompts/translate.st 加载模板。
     * 这种方式适合较长的、需要频繁修改的提示词。
     * 访问示例：GET /prompt/external?text=Hello World&targetLang=日语
     *
     * @param text      要翻译的文本
     * @param targetLang 目标语言
     * @return 翻译结果
     */
    @GetMapping("/external")
    public Result<String> externalTemplate(
            @RequestParam(defaultValue = "Hello, World! Spring AI is awesome.") String text,
            @RequestParam(defaultValue = "中文") String targetLang) {

        // 从 Resource 创建 PromptTemplate（加载 .st 文件）
        PromptTemplate promptTemplate = new PromptTemplate(translatePromptResource);

        // 填充变量
        Prompt prompt = promptTemplate.create(Map.of(
                "text", text,
                "targetLang", targetLang
        ));

        // 调用模型
        String response = chatClient.prompt(prompt).call().content();
        return Result.success(response);
    }

    /**
     * 接口3：使用系统提示词设定 AI 角色
     * <p>
     * 系统提示词（System Prompt）用于定义 AI 的角色、行为准则和限制。
     * 它在对话开始前设定，对后续所有对话生效。
     * 访问示例：GET /prompt/system?question=如何学习编程
     *
     * @param question 用户的问题
     * @return 以指定角色回答的内容
     */
    @GetMapping("/system")
    public Result<String> systemPrompt(
            @RequestParam(defaultValue = "如何学习编程？") String question) {

        // 使用 .system() 方法设置本次请求的系统提示词（覆盖默认值）
        // 这里让 AI 扮演一个有 10 年经验的编程导师
        String response = chatClient.prompt()
                .system("你是一位有 10 年经验的编程导师，擅长用通俗易懂的方式讲解技术概念。" +
                        "回答时请：1.给出清晰的步骤 2.适当举例 3.使用中文")
                .user(question)
                .call()
                .content();
        return Result.success(response);
    }
}
