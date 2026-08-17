# 函数调用

## 概述

函数调用（Function Calling）允许 AI 在对话过程中调用外部工具和 API，使 AI 具备行动能力（Agent 能力）。

## 核心概念

### 工作流程

```
1. 用户提问 → 2. AI 判断需要调用工具 → 3. AI 生成函数调用指令
→ 4. Spring AI 执行函数 → 5. 将结果返回 AI → 6. AI 生成最终回复
```

### 实际应用场景

- **智能客服**：查询订单、办理退款
- **数据分析**：查询数据库、生成报表
- **生活助手**：查询天气、预订机票
- **企业应用**：调用内部 API、操作工作流

## API 列表

### 1. 函数调用对话

**GET** `/api/function/chat`

AI 根据问题自动判断是否调用工具。

**参数：**
- `message` (String, 必填) - 用户自然语言问题

**示例：**
```bash
# 查询天气
curl "http://localhost:8080/api/function/chat?message=北京今天天气怎么样"

# 查询时间
curl "http://localhost:8080/api/function/chat?message=现在几点了"

# 创建订单
curl "http://localhost:8080/api/function/chat?message=我要买3个iPhone"
```

---

### 2. 可用工具列表

**GET** `/api/function/available-tools`

查看当前注册的所有工具。

## 核心代码解析

```java
// 定义工具方法
public String getWeather(String city) {
    return "北京：晴朗，温度 22°C";
}

// 注册工具到 ChatClient
chatClient.prompt()
    .user("北京天气怎么样")
    .tools(
        org.springframework.ai.tool.ToolCallback.builder()
            .name("getWeather")                    // 工具名称
            .description("查询城市天气")            // 工具描述（AI 根据此判断何时调用）
            .inputType(String.class)               // 输入参数类型
            .outputType(String.class)              // 返回值类型
            .function(city -> getWeather((String) city))  // 执行函数
            .build()
    )
    .call()
    .content();
```

## Spring AI 2.0 要点

### ToolCallback

2.0 使用 `ToolCallback` 来注册工具：

```java
ToolCallback.builder()
    .name("toolName")
    .description("描述工具功能，AI 据此决定何时调用")
    .inputType(InputClass.class)
    .outputType(OutputClass.class)
    .function(input -> { ... })
    .build()
```

### 工具描述的重要性

`description` 字段是 AI 决定是否调用工具的关键依据：
- ✅ 好的描述："查询指定城市的天气信息，包括温度、湿度、风力"
- ❌ 差的描述："天气工具"

### 多工具组合

可以同时注册多个工具，AI 会根据用户问题自动选择最合适的：

```java
.tools(tool1, tool2, tool3)
```

## 注意事项

1. **幂等性**：工具函数应该是幂等的，避免重复调用产生副作用
2. **错误处理**：工具函数应妥善处理异常，返回有意义的错误信息
3. **超时控制**：长时间运行的工具需要超时机制
4. **安全校验**：对关键操作（如创建订单）需要用户确认
