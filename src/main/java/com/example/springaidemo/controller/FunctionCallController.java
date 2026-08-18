package com.example.springaidemo.controller;

import com.example.springaidemo.model.OrderRequest;
import com.example.springaidemo.model.WeatherRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
 *     <li>在调用 ChatClient 时通过 tools() 注册可用的函数</li>
 *     <li>AI 模型判断需要调用哪个函数，并生成函数调用指令</li>
 *     <li>Spring AI 自动执行函数并将结果发送回 AI</li>
 *     <li>AI 根据结果生成最终回复</li>
 * </ol>
 * <p>
 * Spring AI 2.0 重要变化：
 * <ul>
 *     <li>使用 FunctionToolCallback.builder() 创建工具</li>
 *     <li>通过 ChatClientRequestSpec.tools() 注册工具</li>
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
     * <p>
     * 入参使用 {@link WeatherRequest}（对象类型），而非 {@code String}。
     * 原因：LLM 发起工具调用时参数恒为 JSON 对象（{@code {"city":"北京"}}），
     * 若 {@code inputType} 设为 {@code String.class}，Jackson 无法把对象反序列化
     * 为字符串，会抛 {@code MismatchedInputException}。
     *
     * @param request 天气查询请求，包含城市名称
     * @return 天气信息描述
     */
    public String getWeather(WeatherRequest request) {
        String city = request.city();
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
     * <p>
     * 入参使用 {@link OrderRequest}（强类型 record），而非 {@code Map<String, Object>}。
     * 原因：当 {@code inputType} 为 {@code Map.class} 时，Spring AI 生成的参数
     * JSON Schema 只有一个空对象结构 {@code {"type":"object"}}，缺少字段名与类型约束。
     * LLM 因此无法稳定地按 {@code product} / {@code quantity} 字段名传参，
     * 可能传成 {@code count}、{@code num}、{@code 数量} 等任意键名，甚至省略字段，
     * 导致 {@code params.get("quantity")} 返回 {@code null}，
     * 调用 {@code .intValue()} 时抛出 NPE。
     * <p>
     * 改用 record 后，JSON Schema 会生成完整的字段定义与 required 约束，
     * LLM 可稳定传参，并由 Jackson 自动绑定到强类型字段，杜绝 NPE。
     *
     * @param request 订单请求，包含商品名称与数量
     * @return 订单创建结果
     */
    public String createOrder(OrderRequest request) {
        int orderId = orderIdGenerator.incrementAndGet();
        String product = request.product();
        int quantity = request.quantity();
        return "订单创建成功！订单号：" + orderId
                + "，商品：" + product
                + "，数量：" + quantity
                + "，时间：" + LocalDateTime.now();
    }

    /**
     * 让 AI 自动调用工具函数
     * <p>
     * Spring AI 2.0 使用 FunctionToolCallback 注册工具。
     * 通过 tools() 方法将工具注册到对话中。
     *
     * @param message 用户自然语言问题
     * @return AI 回复（可能包含函数调用的结果）
     */
    @GetMapping("/chat")
    public String functionChat(@RequestParam String message) {
        // 注册多个工具，AI 会根据用户问题自动选择最合适的
        return chatClient.prompt()
                .user(message)
                .tools(
                        // 天气查询工具 - 使用 Function 接口
                        // 注意：inputType 必须是对象类型(WeatherRequest)，不能用 String。
                        // 否则 LLM 返回 {"city":"..."} 对象时，Jackson 无法反序列化为 String，
                        // 会抛 MismatchedInputException。
                        FunctionToolCallback.builder("getWeather", (java.util.function.Function<WeatherRequest, String>) this::getWeather)
                                .description("查询指定城市的天气信息，包括温度、湿度、风力等。参数为城市名称")
                                .inputType(WeatherRequest.class)
                                .build(),

                        // 当前时间工具 - 使用 Supplier 接口（无参函数）
                        FunctionToolCallback.builder("getCurrentTime", (java.util.function.Supplier<String>) this::getCurrentTime)
                                .description("获取当前的日期和时间，不需要任何参数")
                                .build(),

                        // 创建订单工具 - 使用 Function 接口，输入为 OrderRequest 强类型对象
                        // 注意：inputType 必须是结构明确的对象类型(如 OrderRequest)，不能用 Map.class。
                        // 原因：Map.class 生成的 JSON Schema 只有 {"type":"object"}，缺少字段名与
                        // 类型约束，LLM 可能传错参数名(如 count/num/数量)或省略字段，导致
                        // params.get("quantity") 返回 null，调用 .intValue() 时抛出 NPE。
                        // 改用 OrderRequest 后，Schema 会带上完整的 properties 与 required 约束。
                        FunctionToolCallback.builder("createOrder", (java.util.function.Function<OrderRequest, String>) this::createOrder)
                                .description("创建一个新订单。参数：product(字符串,商品名称)、quantity(整数,购买数量)，两个参数均为必填")
                                .inputType(OrderRequest.class)
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
