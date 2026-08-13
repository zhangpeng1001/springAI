package com.example.springaidemo.controller;

import com.example.springaidemo.common.Result;
import com.example.springaidemo.model.ActorFilms;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 结构化输出（Structured Output）控制器
 * <p>
 * 结构化输出是 Spring AI 非常强大的功能：它可以将大模型返回的自由文本
 * 自动转换为 Java 对象（POJO）、List、Map 等结构化数据。
 * <p>
 * 核心方法：ChatClient.prompt().user(...).call().entity(MyClass.class)
 * <p>
 * 应用场景：
 * <ul>
 *     <li>信息抽取：从非结构化文本中提取结构化信息</li>
 *     <li>数据填充：让 AI 生成符合特定 schema 的数据</li>
 *     <li>意图识别：将用户输入解析为程序可处理的结构</li>
 *     <li>自动化流水线：AI 输出直接进入下游业务逻辑</li>
 * </ul>
 *
 * @author spring-ai-demo
 */
@RestController
@RequestMapping("/structured")
public class StructuredOutputController {

    private final ChatClient chatClient;

    public StructuredOutputController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 接口1：将 AI 输出转换为自定义 Java 对象（POJO）
     * <p>
     * 演示如何让 AI 返回结构化的演员信息。
     * Spring AI 会根据 ActorFilms 类生成 JSON Schema 约束模型输出。
     * 访问示例：GET /structured/actor?name=周星驰
     *
     * @param name 演员姓名
     * @return 结构化的演员信息对象
     */
    @GetMapping("/actor")
    public Result<ActorFilms> getActorInfo(@RequestParam(defaultValue = "周星驰") String name) {
        // .entity(ActorFilms.class) 是关键：
        // 它告诉 Spring AI 将返回结果解析为 ActorFilms 对象
        ActorFilms actorFilms = chatClient.prompt()
                .user("请告诉我关于演员 " + name + " 的信息，包括姓名、至少3部代表作电影列表和简短生平介绍。")
                .call()
                .entity(ActorFilms.class);

        return Result.success(actorFilms);
    }

    /**
     * 接口2：将 AI 输出转换为 List（列表）
     * <p>
     * 演示如何让 AI 返回一个字符串列表。
     * 访问示例：GET /structured/list?topic=Java学习路线
     *
     * @param topic 列表主题
     * @return 字符串列表
     */
    @GetMapping("/list")
    public Result<List<String>> getList(@RequestParam(defaultValue = "Java 学习路线") String topic) {
        // entity() 也支持集合类型，通过 TypeReference 或直接传 Class
        // 这里直接返回 List<String>
        List<String> items = chatClient.prompt()
                .user("请列出关于「" + topic + "」的 5 个要点，每点不超过 20 个字。")
                .call()
                .entity(new org.springframework.core.ParameterizedTypeReference<List<String>>() {
                });

        return Result.success(items);
    }

    /**
     * 接口3：将 AI 输出转换为 Map（键值对）
     * <p>
     * 演示如何让 AI 返回键值对结构的数据。
     * 访问示例：GET /structured/map?question=Java和Python的对比
     *
     * @param question 要分析的问题
     * @return 键值对形式的分析结果
     */
    @GetMapping("/map")
    public Result<Map<String, Object>> getMap(@RequestParam(defaultValue = "Java 和 Python 的对比") String question) {
        // 让 AI 返回 Map 结构，适合不确定字段名的场景
        Map<String, Object> result = chatClient.prompt()
                .user("请分析：" + question + "。用键值对形式返回，key 是维度名（如\"语法特点\"\"适用领域\"），value 是简短说明。")
                .call()
                .entity(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {
                });

        return Result.success(result);
    }

    /**
     * 接口4：信息抽取 - 从非结构化文本中提取结构化数据
     * <p>
     * 这是结构化输出最实用的场景之一。
     * 访问示例：GET /structured/extract?text=张三今年25岁，在北京工作，是一名软件工程师
     *
     * @param text 包含信息的非结构化文本
     * @return 提取出的结构化人员信息
     */
    @GetMapping("/extract")
    public Result<Map<String, String>> extractInfo(
            @RequestParam(defaultValue = "张三今年25岁，在北京工作，是一名软件工程师，电话13800138000") String text) {
        // 让 AI 从文本中提取姓名、年龄、城市、职业等信息
        Map<String, String> info = chatClient.prompt()
                .user("从以下文本中提取人员信息，返回键值对，key 为 name/age/city/occupation/phone，找不到的值为空字符串。文本：" + text)
                .call()
                .entity(new org.springframework.core.ParameterizedTypeReference<Map<String, String>>() {
                });

        return Result.success(info);
    }
}
