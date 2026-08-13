package com.example.springaidemo.controller;

import com.example.springaidemo.common.Result;
import com.example.springaidemo.function.CalculatorTools;
import com.example.springaidemo.function.WeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 函数调用（Function Calling）控制器
 * <p>
 * 函数调用是 Spring AI 的核心功能之一，它允许大模型调用你定义的 Java 方法。
 * 这让 AI 能够：获取实时数据、操作数据库、调用外部 API、执行计算等。
 * <p>
 * 核心概念：
 * <ul>
 *     <li>Tool（工具）：用 @Tool 注解标注的 Java 方法</li>
 *     <li>Spring AI 会自动将工具描述发送给大模型</li>
 *     <li>大模型自主决定是否调用工具以及传什么参数</li>
 *     <li>Spring AI 自动执行方法并把结果返回给大模型</li>
 * </ul>
 *
 * @author spring-ai-demo
 */
@RestController
@RequestMapping("/function")
public class FunctionCallController {

    private final ChatClient chatClient;
    private final WeatherTools weatherTools;
    private final CalculatorTools calculatorTools;

    public FunctionCallController(ChatClient.Builder chatClientBuilder,
                                  WeatherTools weatherTools,
                                  CalculatorTools calculatorTools) {
        this.chatClient = chatClientBuilder.build();
        this.weatherTools = weatherTools;
        this.calculatorTools = calculatorTools;
    }

    /**
     * 接口1：使用天气查询工具
     * <p>
     * AI 会根据用户问题判断是否需要调用天气工具。
     * 访问示例：GET /function/weather?question=北京和上海今天天气怎么样？
     *
     * @param question 用户的问题
     * @return AI 的回答（可能调用了天气工具）
     */
    @GetMapping("/weather")
    public Result<String> weatherCall(@RequestParam(defaultValue = "北京和上海今天天气怎么样？") String question) {
        // .tools(weatherTools) 注册工具：Spring AI 会把 WeatherTools 中所有 @Tool 方法告诉大模型
        // 大模型决定调用 getCurrentWeather("北京") 和 getCurrentWeather("上海")
        // Spring AI 自动执行并返回结果给大模型，大模型整合后给出最终回答
        String response = chatClient.prompt()
                .user(question)
                .tools(weatherTools)
                .call()
                .content();
        return Result.success(response);
    }

    /**
     * 接口2：使用计算器工具
     * <p>
     * 演示 AI 调用计算器进行精确计算。
     * 访问示例：GET /function/calc?question=请计算 1234 乘以 5678 等于多少
     *
     * @param question 用户的问题
     * @return AI 的回答（调用了计算器工具）
     */
    @GetMapping("/calc")
    public Result<String> calculatorCall(@RequestParam(defaultValue = "请计算 1234 乘以 5678 等于多少") String question) {
        String response = chatClient.prompt()
                .user(question)
                .tools(calculatorTools)
                .call()
                .content();
        return Result.success(response);
    }

    /**
     * 接口3：同时使用多个工具
     * <p>
     * 演示在一次对话中注册多个工具，AI 会自主选择合适的工具。
     * 访问示例：GET /function/multi?question=北京天气怎么样？另外帮我算一下 100除以7
     *
     * @param question 用户的问题
     * @return AI 的回答
     */
    @GetMapping("/multi")
    public Result<String> multiToolCall(@RequestParam(defaultValue = "北京今天天气怎么样？另外帮我算一下 100 除以 7 等于多少") String question) {
        // 同时注册天气和计算器两个工具集
        // 大模型会根据问题内容分别调用不同的工具
        String response = chatClient.prompt()
                .user(question)
                .tools(weatherTools, calculatorTools)
                .call()
                .content();
        return Result.success(response);
    }

    /**
     * 接口4：天气预报查询
     * <p>
     * 访问示例：GET /function/forecast?question=广州未来3天天气如何
     *
     * @param question 用户的问题
     * @return AI 的回答
     */
    @GetMapping("/forecast")
    public Result<String> forecastCall(@RequestParam(defaultValue = "广州未来3天天气如何？") String question) {
        String response = chatClient.prompt()
                .user(question)
                .tools(weatherTools)
                .call()
                .content();
        return Result.success(response);
    }
}
