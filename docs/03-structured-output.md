# 03 - 结构化输出

## 概述

Spring AI 2.0 的结构化输出功能允许将 AI 的响应自动映射到 Java 对象，实现类型安全的数据提取。这对于需要解析 AI 返回结果的场景非常有用。

## 核心概念

### 为什么需要结构化输出？

- AI 默认返回非结构化的文本
- 很多场景需要精确的数据（如 JSON）
- 手动解析文本容易出错且不可靠
- 结构化输出保证了数据格式的一致性

### Spring AI 2.0 的实现

使用 `BeanOutputConverter` 将 AI 响应映射到 Java Bean。

## API 接口

### 1. 映射到 POJO

**请求：**
```
GET /api/structured/recipe?query=红烧肉
```

**代码示例：**
```java
@GetMapping("/recipe")
public Recipe getRecipe(@RequestParam String query) {
    return chatClient.prompt()
            .user("请给我一个" + query + "的做法，包含菜名、食材和步骤")
            .call()
            .entity(Recipe.class);  // 自动映射到 Recipe 类
}
```

**Recipe 类定义：**
```java
public record Recipe(
    String dishName,
    List<String> ingredients,
    List<String> steps,
    int cookingTime
) {}
```

### 2. 返回列表

**请求：**
```
GET /api/structured/list?topic=Spring
```

**代码示例：**
```java
@GetMapping("/list")
public List<String> getList(@RequestParam String topic) {
    return chatClient.prompt()
            .user("请列出5个关于" + topic + "的知识点")
            .call()
            .entity(new BeanOutputConverter<>(new TypeReference<List<String>>() {}));
}
```

### 3. 返回 Map

**请求：**
```
GET /api/structured/map?input=你的名字
```

**代码示例：**
```java
@GetMapping("/map")
public Map<String, Object> getMap(@RequestParam String input) {
    return chatClient.prompt()
            .user("关于"" + input + ""，请以键值对形式返回信息")
            .call()
            .entity(new BeanOutputConverter<>(new TypeReference<Map<String, Object>>() {}));
}
```

### 4. 带自定义提示的结构化输出

**请求：**
```
GET /api/structured/review?code=public class Test {}
```

**代码示例：**
```java
@GetMapping("/review")
public CodeReviewResult reviewCode(@RequestParam String code) {
    return chatClient.prompt()
            .system("你是一个代码审查专家，请以JSON格式返回审查结果")
            .user("请审查以下代码：\n" + code)
            .call()
            .entity(CodeReviewResult.class);
}
```

**CodeReviewResult 类：**
```java
public class CodeReviewResult {
    private int score;           // 评分 1-10
    private List<String> issues; // 问题列表
    private List<String> suggestions; // 建议列表
    private String summary;      // 总结
}
```

## 高级用法

### 自定义 Converter

```java
// 使用 Jackson ObjectMapper 自定义解析
BeanOutputConverter<Recipe> converter = new BeanOutputConverter<>(Recipe.class);

Recipe recipe = chatClient.prompt()
        .user("...")
        .call()
        .entity(converter);
```

### 嵌套结构支持

```java
public class Article {
    private String title;
    private String content;
    private Author author;      // 嵌套对象
    private List<Tag> tags;     // 嵌套列表
}

public class Author {
    private String name;
    private String bio;
}

public class Tag {
    private String name;
    private String category;
}
```

## 最佳实践

1. **使用 Record 类**：不可变的数据结构更安全
2. **简单结构优先**：避免过深的嵌套
3. **字段命名清晰**：使用有意义的字段名
4. **处理空值**：AI 可能返回空或 null
5. **验证输出**：对关键数据进行二次验证

## 注意事项

- 结构化输出依赖 AI 的 JSON 生成能力
- 复杂结构可能需要多轮交互
- Spring AI 2.0 使用 Jackson 进行 JSON 解析
- 确保 POJO 有无参构造器（或使用 Record）