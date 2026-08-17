# 05 - 函数调用（Function Calling）

## 概述

Spring AI 2.0 的函数调用功能允许 AI 在对话过程中调用外部工具或 API。这使得 AI 能够执行实际的操作，如查询数据库、调用第三方服务等。

## 核心概念

### FunctionToolCallback

Spring AI 2.0 使用 `FunctionToolCallback` 来注册可调用的函数。它支持：
- 静态方法引用
- Lambda 表达式
- 多个参数
- 复杂返回类型

### 工作流程

1. 用户发送消息给 AI
2. AI 判断是否需要调用工具
3. 如果需要，AI 返回工具调用请求
4. Spring AI 自动执行工具调用
5. 将结果返回给 AI
6. AI 基于结果生成最终回答

## API 接口

### 1. 简单函数调用

注册一个天气查询函数。

**请求：**
```
GET /api/function/weather?message=北京今天天气怎么样
```

**代码示例：**
```java
@GetMapping("/weather")
public String weatherChat(@RequestParam String message) {
    return chatClient.prompt()
            .user(message)
            .toolCallbacks(
                    FunctionToolCallback.builder("getWeather", (Function<String, String>) this::getWeather)
                            .description("查询指定城市的天气信息")
                            .inputType(String.class)
                            .build()
            )
            .call()
            .content();
}

// 模拟天气查询
private String getWeather(String city) {
    Map<String, String> weatherData = Map.of(
            "北京", "晴，温度22°C，微风",
            "上海", "多云，温度25°C",
            "广州", "阵雨，温度28°C"
    );
    return weatherData.getOrDefault(city, "抱歉，无法获取" + city + "的天气信息");
}
```

### 2. 多函数调用

同时注册多个函数供 AI 选择。

**请求：**
```
GET /api/function/multi?message=查询股票并计算
```

**代码示例：**
```java
@GetMapping("/multi")
public String multiFunctionChat(@RequestParam String message) {
    return chatClient.prompt()
            .user(message)
            .toolCallbacks(
                    FunctionToolCallback.builder("getStockPrice", (Function<String, Double>) this::getStockPrice)
                            .description("查询股票价格，参数为股票代码")
                            .inputType(String.class)
                            .build(),
                    FunctionToolCallback.builder("calculate", (BiFunction<Double, Double, Double>) (a, b) -> a + b)
                            .description("计算两个数字的和")
                            .inputType(Double.class)
                            .build()
            )
            .call()
            .content();
}
```

### 3. Supplier 类型函数

适用于不需要输入参数的场景。

**代码示例：**
```java
@GetMapping("/supplier")
public String supplierChat(@RequestParam String message) {
    return chatClient.prompt()
            .user(message)
            .toolCallbacks(
                    FunctionToolCallback.builder("currentTime", (Supplier<String>) this::getCurrentTime)
                            .description("获取当前时间")
                            .inputType(Void.class)
                            .build()
            )
            .call()
            .content();
}

private String getCurrentTime() {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
}
```

### 4. 带复杂参数的函数

使用自定义类型作为参数。

**代码示例：**
```java
@GetMapping("/complex")
public String complexFunctionChat(@RequestParam String message) {
    return chatClient.prompt()
            .user(message)
            .toolCallbacks(
                    FunctionToolCallback.builder("searchDatabase", 
                            (Function<SearchRequest, SearchResult>) this::searchDatabase)
                            .description("搜索数据库，支持按关键词和日期范围搜索")
                            .inputType(SearchRequest.class)
                            .outputType(SearchResult.class)
                            .build()
            )
            .call()
            .content();
}

public record SearchRequest(String keyword, LocalDate startDate, LocalDate endDate) {}
public record SearchResult(List<String> items, int totalCount) {}
```

## 进阶技巧

### 函数描述的重要性

函数的 `description` 字段是 AI 选择函数的唯一依据：

```java
// 好的描述
.description("查询指定城市的当前天气，返回温度、天气状况等信息")

// 不好的描述
.description("获取数据")
```

### 处理函数调用结果

```java
// 获取完整的工具调用信息
ChatResponse response = chatClient.prompt()
        .user(message)
        .toolCallbacks(toolCallback)
        .call()
        .chatResponse();

// 检查是否有工具调用
List<ToolCall> toolCalls = response.getResults().stream()
        .flatMap(result -> result.getToolCalls().stream())
        .toList();
```

## 最佳实践

1. **清晰的函数描述**：让 AI 准确理解函数用途
2. **合理的参数类型**：使用简单类型，避免过于复杂的嵌套
3. **错误处理**：函数内部要有异常处理
4. **幂等性**：函数调用应该是幂等的
5. **超时控制**：对于耗时的操作要有超时机制

## 注意事项

- AI 可能会选择错误的函数，通过清晰的描述减少这种情况
- 函数调用会增加延迟，考虑异步处理
- 敏感操作需要额外的验证
- 函数参数通过 JSON 进行序列化/反序列化