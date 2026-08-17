package com.example.springaidemo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

/**
 * 提示词模板控制器
 * <p>
 * 演示 Spring AI 2.0 中的提示词模板功能。
 * Spring AI 2.0 中推荐使用 ChatClient 的内联模板 API，
 * 通过 lambda 表达式实现变量替换。
 * <p>
 * Spring AI 2.0 重要变化：
 * <ul>
 *     <li>移除了 UserPromptTemplate，统一使用内联模板</li>
 *     <li>system()/user() 方法支持 Consumer 风格的模板参数</li>
 *     <li>通过 .param() 或 .params() 传入模板变量</li>
 * </ul>
 *
 * @author spring-ai-demo
 */
@RestController
@RequestMapping("/api/prompt")
public class PromptController {

    private final ChatClient chatClient;

    public PromptController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 使用变量替换的提示词模板
     * <p>
     * 通过 system() 方法的内联模板功能，使用 {变量名} 语法和 param() 传入值。
     * 这种方式比字符串拼接更安全、更易维护。
     *
     * @param role AI 角色
     * @param task 要完成的任务
     */
    @GetMapping("/template")
    public String templatePrompt(
            @RequestParam String role,
            @RequestParam String task) {

        // 使用内联模板，通过 param() 传入变量
        return chatClient.prompt()
                .system(s -> s.text("你是一个{role}。请帮我完成以下任务：{task}")
                        .param("role", role)
                        .param("task", task))
                .call()
                .content();
    }

    /**
     * 翻译助手 - 实用案例
     * <p>
     * 使用模板化提示词实现多语言翻译，支持指定目标语言和风格。
     */
    @GetMapping("/translate")
    public String translate(
            @RequestParam String text,
            @RequestParam(defaultValue = "英文") String targetLanguage,
            @RequestParam(defaultValue = "正式") String style) {

        // 系统提示：设定翻译专家角色
        return chatClient.prompt()
                .system(s -> s.text("你是一位专业的翻译专家。请将用户输入的文本翻译成{language}。")
                        .param("language", targetLanguage))
                .system(s -> s.text("翻译要求：{style}风格，保持原意，语句通顺。")
                        .param("style", style))
                .user(text)
                .call()
                .content();
    }

    /**
     * 代码审查助手
     * <p>
     * 通过模板提示词让 AI 扮演代码审查专家，分析代码质量。
     */
    @PostMapping("/code-review")
    public String codeReview(@RequestBody CodeReviewRequest request) {
        // 系统提示：代码审查专家
        String systemPrompt = """
                你是一位资深的代码审查专家。请审查以下代码，从以下方面给出建议：
                1. 代码质量和可读性
                2. 潜在的 Bug 和安全问题
                3. 性能优化建议
                4. 最佳实践建议
                """;

        return chatClient.prompt()
                .system(systemPrompt)
                .user(request.getCode())
                .call()
                .content();
    }

    /**
     * 学习助手 - 根据技能水平调整解释深度
     */
    @GetMapping("/explain")
    public String explainConcept(
            @RequestParam String concept,
            @RequestParam(defaultValue = "初学者") String level) {

        return chatClient.prompt()
                .system(s -> s.text("你是一位耐心的教师。请用{level}能理解的方式，解释「{concept}」这个概念。")
                        .param("level", level)
                        .param("concept", concept))
                .system("要求：\n1. 使用简单易懂的语言\n2. 给出具体的例子\n3. 总结要点")
                .call()
                .content();
    }

    public static class CodeReviewRequest {
        private String code;
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}
