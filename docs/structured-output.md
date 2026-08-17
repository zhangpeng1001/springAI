# 结构化输出

## 概述

结构化输出（Structured Output）是将 AI 返回的文本自动转换为 Java 对象的功能，解决 AI 输出不稳定的问题。

## 为什么需要结构化输出？

AI 返回的文本通常是非结构化的，包含大量自然语言。在实际应用中，我们经常需要：

- 提取特定字段（姓名、日期、数值）
- 生成 JSON 配置
- 分类和标签
- 数据转换

手动解析 AI 输出既繁琐又不可靠，结构化输出通过 Prompt 约束 + 自动解析完美解决这个问题。

## API 列表

### 1. Bean 输出

**GET** `/api/structured/bean`

从文本中提取结构化信息，转换为 Java Bean。

**参数：**
- `text` (String, 必填) - 包含待提取信息的文本

**示例：**
```bash
curl "http://localhost:8080/api/structured/bean?text=我叫张三，今年25岁，邮箱是zhangsan@example.com"
```

**返回：**
```json
{
  "name": "张三",
  "age": 25,
  "email": "zhangsan@example.com"
}
```

---

### 2. 标签生成

**GET** `/api/structured/tags`

为内容生成分类标签列表。

**参数：**
- `content` (String, 必填) - 待分类的内容

**示例：**
```bash
curl "http://localhost:8080/api/structured/tags?content=Spring Boot是一个Java Web框架"
```

**返回：** `["Java", "Spring", "Web框架"]`

---

### 3. 手动解析

**GET** `/api/structured/manual-parse`

获取原始文本后自行解析。

**参数：**
- `text` (String, 必填)

## 核心代码解析

```java
// 1. 定义输出类型（使用 Java Record）
public record Person(String name, Integer age, String email) {}

// 2. 在 Prompt 中指定输出格式
String prompt = """
    从以下文本提取人物信息，以 JSON 格式输出：
    "%s"
    格式：{"name": "张三", "age": 25, "email": "test@test.com"}
    """.formatted(text);

// 3. 使用 entity() 自动解析
Person person = chatClient.prompt()
    .user(prompt)
    .call()
    .entity(Person.class);  // 自动将 JSON 转为对象
```

## Spring AI 2.0 要点

- `entity()` 方法替代旧的 `BeanOutputConverter`
- 支持 `ParameterizedTypeReference` 处理泛型
- 使用 Jackson 进行 JSON 序列化/反序列化
- 输出格式通过 Prompt 约束，不需要额外配置

## 注意事项

1. AI 可能返回无效 JSON，需要异常处理
2. 复杂嵌套结构可能需要多次尝试
3. 建议在 Prompt 中提供清晰的格式示例
4. 对于关键业务场景，建议添加验证逻辑
