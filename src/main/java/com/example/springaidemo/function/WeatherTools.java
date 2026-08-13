package com.example.springaidemo.function;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * 天气查询工具集 - 演示 Spring AI 函数调用（Function Calling / Tool Calling）
 * <p>
 * 函数调用让大模型能够调用外部函数/方法，从而获取实时数据或执行操作。
 * 例如：查天气、查数据库、调用 API、计算等。
 * <p>
 * 工作流程：
 * <ol>
 *     <li>用户提问（如"北京今天天气怎么样"）</li>
 *     <li>大模型识别需要调用工具，返回工具调用请求（函数名 + 参数）</li>
 *     <li>Spring AI 自动执行对应的 Java 方法</li>
 *     <li>将方法返回值发回给大模型</li>
 *     <li>大模型基于返回值生成最终回答</li>
 * </ol>
 * <p>
 * 使用方式：通过 {@code @Tool} 注解标注方法，在 ChatClient 调用时通过 .tools() 传入。
 *
 * @author spring-ai-demo
 */
@Component
public class WeatherTools {

    /**
     * 查询指定城市的天气
     * <p>
     * {@code @Tool} 声明这是一个可被 AI 调用的工具，description 非常重要：
     * AI 会根据 description 判断是否需要调用此工具，所以要描述清楚工具的作用。
     * <p>
     * {@code @ToolParam} 描述参数含义，帮助 AI 正确传参。
     *
     * @param city 城市名称
     * @return 天气信息字符串
     */
    @Tool(description = "查询指定城市的实时天气情况，包括天气状况和温度")
    public String getCurrentWeather(@ToolParam(description = "城市名称，如：北京、上海、广州") String city) {
        // 这里是模拟数据，实际项目中可以调用真实天气 API
        // 如和风天气、OpenWeatherMap 等
        String[] weathers = {"晴天", "多云", "小雨", "大雨", "雷阵雨", "阴天"};
        Random random = new Random();
        String weather = weathers[random.nextInt(weathers.length)];
        int temperature = 15 + random.nextInt(20); // 15~35度

        return String.format("%s今天的天气：%s，温度 %d°C", city, weather, temperature);
    }

    /**
     * 查询指定城市的未来几天天气预报
     *
     * @param city 城市名称
     * @param days 天数
     * @return 未来天气信息
     */
    @Tool(description = "查询指定城市未来几天的天气预报")
    public String getWeatherForecast(
            @ToolParam(description = "城市名称") String city,
            @ToolParam(description = "查询未来几天，1-7") int days) {

        // 模拟数据
        StringBuilder forecast = new StringBuilder();
        forecast.append(String.format("%s未来%d天天气预报：\n", city, days));

        String[] weathers = {"晴天", "多云", "小雨", "阴天"};
        Random random = new Random();
        for (int i = 1; i <= days; i++) {
            String weather = weathers[random.nextInt(weathers.length)];
            int temp = 15 + random.nextInt(20);
            forecast.append(String.format("  第%d天：%s，%d°C\n", i, weather, temp));
        }
        return forecast.toString();
    }
}
