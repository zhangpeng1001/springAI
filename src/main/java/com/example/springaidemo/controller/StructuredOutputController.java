package com.example.springaidemo.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 结构化输出控制器
 * <p>
 * 演示 Spring AI 2.0 的结构化输出功能：
 * <ul>
 *     <li>entity(Class) - 输出转换为 Java Bean</li>
 *     <li>entity(ParameterizedTypeReference) - 输出泛型集合</li>
 * </ul>
 * <p>
 * 结构化输出的核心原理：
 * Spring AI 通过 Prompt 模板告诉 AI 需要按特定格式（如 JSON）输出，
 * 然后使用内部的 BeanOutputConverter 将文本解析为 Java 对象。
 * <p>
 * Spring AI 2.0 重要变化：
 * <ul>
 *     <li>使用 entity() 方法替代旧的 BeanOutputConverter</li>
 *     <li>通过 ChatClient.CallResponseSpec.entity() 调用</li>
 * </ul>
 *
 * @author spring-ai-demo
 */
@RestController
@RequestMapping("/api/structured")
public class StructuredOutputController {

    private final ChatClient chatClient;

    public StructuredOutputController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 将 AI 输出转换为 Java Bean
     * <p>
     * 使用 entity(Class) 方法，AI 的输出会被自动解析为指定的 Java 类型。
     * 适用于需要返回固定结构数据的场景，如：
     * <ul>
     *     <li>提取信息（姓名、地址、电话等）</li>
     *     <li>生成报告（标题、摘要、内容等）</li>
     *     <li>分类标签生成</li>
     * </ul>
     *
     * @param text 包含待提取信息的文本
     * @return 解析后的 Person 对象
     */
    @GetMapping("/bean")
    public Person getPersonInfo(@RequestParam String text) {
        String prompt = """
                从以下文本中提取人物信息，以严格的 JSON 格式输出，不要包含其他文字：
                "%s"
                
                输出格式示例：
                {"name": "张三", "age": 25, "email": "zhangsan@example.com"}
                """.formatted(text);

        // 使用 entity() 方法，Spring AI 会自动将 JSON 输出转换为指定类型
        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(Person.class);
    }

    /**
     * 获取 AI 回复并自行解析
     * <p>
     * 如果 entity() 转换失败或需要自定义解析，可以先获取原始文本再解析。
     */
    @GetMapping("/manual-parse")
    public Map<String, Object> manualParse(@RequestParam String text) {
        String prompt = """
                从以下文本中提取关键信息，以严格的 JSON 格式输出，不要包含其他文字：
                "%s"
                """.formatted(text);

        // 先获取文本，然后返回（实际项目中可以用 Jackson 解析）
        String rawContent = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return Map.of("raw", rawContent);
    }

    /**
     * 生成分类标签列表
     * <p>
     * 使用 entity(ParameterizedTypeReference) 处理泛型类型，让 AI 返回标签列表。
     */
    @GetMapping("/tags")
    public List<String> getTags(@RequestParam String content) {
        String prompt = """
                为以下内容生成 3-5 个关键词标签，以严格的 JSON 数组格式输出，不要包含其他文字：
                "%s"
                
                输出格式：["标签1", "标签2", "标签3"]
                """.formatted(content);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(new org.springframework.core.ParameterizedTypeReference<List<String>>() {});
    }

    /**
     * 人员信息 DTO
     * 使用 Java Record 定义不可变的数据结构
     */
    public record Person(
            @JsonProperty("name") String name,
            @JsonProperty("age") Integer age,
            @JsonProperty("email") String email
    ) {}
}
