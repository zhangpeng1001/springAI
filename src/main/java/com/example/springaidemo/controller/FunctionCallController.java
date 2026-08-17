package com.example.springaidemo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 函数调用控制器
 * <p>
 * 演示 Spring AI 2.0 的函数调用（Function Calling）功能：
 * <ul>
 *     <li>注册工具函数给 AI 使用</li>
 *     <li>AI 自动判断何时调用函数</li>
 *     <li>获取函数调用的结果返回给 AI</li>
 * </ul>
 * <p>
 * 函数调用的工作流程：
 * <ol>
 *     <li>在调用 ChatClient 时通过 toolCallbacks() 注册可用的函数</li>
 *     <li>AI 模型判断需要调用哪个函数，并生成函数调用指令</li>
 *     <li>Spring AI 自动执行函数并将结果发送回 AI</li>
 *     <li>AI 根据结果生成最终回复</li>
 * </ol>
 * <p>
 * Spring AI 2.0 重要变化：
 * <ul>
 *     <li>使用 FunctionToolCallback.builder() 创建工具</li>
 *     <li>通过 ChatClientRequestSpec.toolCallbacks() 注册工具</li>
 *     <li>支持 Supplier、Consumer、Function、BiFunction 等函数式接口</li>
 * </ul>
 *
 * @author spring-ai-demo
 */
@RestController
@RequestMapping("/api/function")
public class FunctionCallController {

    private final ChatClient chatClient;
    private final AtomicInteger orderIdGenerator = new AtomicInteger(1000);

    public FunctionCallController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 模拟查询天气的函数
     */
    public String getWeather(String city) {
        return switch (city) {
            case "北京" -> "北京：晴朗，温度 22°C，西北风 3 级，空气质量优良";
            case "上海" -> "上海：多云，温度 25°C，东南风 4 级，湿度 65%";
            case "广州" -> "广州：阵雨，温度 28°C，南风 3 级，湿度 80%";
            case "深圳" -> "深圳：多云转晴，温度 27°C，东风 3 级，湿度 70%";
            default -> city + "：数据不足，请稍后再试";
        };
    }

    /**
     * 模拟查询当前时间的函数（无参，使用 Supplier）
     */
    public String getCurrentTime() {
        return "当前时间：" + LocalDateTime.now().toString();
    }

    /**
     * 模拟创建订单的函数
     */
    public String createOrder(Map<String, Object> params) {
        int orderId = orderIdGenerator.incrementAndGet();
        String product = (String) params.get("product");
        int quantity = ((Number) params.get("quantity")).intValue();
        return "订单创建成功！订单号：" + orderId
                + "，商品：" + product
                + "，数量：" + quantity
                + "，时间：" + LocalDateTime.now();
    }

    /**
     * 让 AI 自动调用工具函数
     * <p>
     * Spring AI 2.0 使用 FunctionToolCallback 注册工具。
     * 通过 toolCallbacks() 方法将工具注册到对话中。
     *
     * @param message 用户自然语言问题
     * @return AI 回复（可能包含函数调用的结果）
     */
    @GetMapping("/chat")
    public String functionChat(@RequestParam String message) {
        // 注册多个工具，AI 会根据用户问题自动选择最合适的
        return chatClient.prompt()
                .user(message)
                .toolCallbacks(
                        // 天气查询工具 - 使用 Function 接口
                        FunctionToolCallback.builder("getWeather", (java.util.function.Function<String, String>) this::getWeather)
                                .description("查询指定城市的天气信息，包括温度、湿度、风力等。参数为城市名称")
                                .inputType(String.class)
                                .build(),

                        // 当前时间工具 - 使用 Supplier 接口（无参函数）
                        FunctionToolCallback.builder("getCurrentTime", (java.util.function.Supplier<String>) this::getCurrentTime)
                                .description("获取当前的日期和时间，不需要任何参数")
                                .build(),

                        // 创建订单工具 - 使用 Function 接口，输入为 Map
                        FunctionToolCallback.builder("createOrder", (java.util.function.Function<Map<String, Object>, String>) this::createOrder)
                                .description("创建一个新订单，需要商品名称(product)和数量(quantity)两个参数")
                                .inputType(Map.class)
                                .build()
                )
                .call()
                .content();
    }

    /**
     * 查看有哪些可用的工具
     */
    @GetMapping("/available-tools")
    public String[] getAvailableTools() {
        return new String[]{
                "getWeather - 查询城市天气",
                "getCurrentTime - 获取当前时间",
                "createOrder - 创建订单"
        };
    }
}
