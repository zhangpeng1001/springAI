# 02 - 提示词模板

## 概述

Spring AI 支持使用外部模板文件来管理 Prompt，实现提示词与业务逻辑的分离。这使得提示词可以被非开发人员管理和迭代。

## 模板文件

### 模板位置

默认在 `src/main/resources/templates/` 目录下。

### 模板语法

使用 `{variable}` 作为占位符，与 Spring 的 `StringTemplate` 语法一致。

**示例 `system.st`：**
```
你是一个专业的{role}，擅长{skill}。
请用{style}的方式回答用户的问题。
```

## API 接口

### 1. 基于模板的对话

**请求：**
```
GET /api/prompt/template?role=工程师&skill=Spring AI&style=简洁
```

**代码示例：**
```java
@GetMapping("/template")
public String templateChat(
        @RequestParam String role,
        @RequestParam String skill,
        @RequestParam(defaultValue = "专业") String style) {

    return chatClient.prompt()
            .system(s -> s
                    .template("system.st")
                    .param("role", role)
                    .param("skill", skill)
                    .param("style", style))
            .user("请介绍一下你的能力")
            .call()
            .content();
}
```

### 2. 角色设定

**请求：**
```
GET /api/prompt/role?role=产品经理&question=如何规划产品
```

**代码示例：**
```java
@GetMapping("/role")
public String rolePlay(
        @RequestParam String role,
        @RequestParam String question) {

    return chatClient.prompt()
            .system("你现在扮演一个{role}的角色")
            .user(question)
            .call()
            .content();
}
```

### 3. 学习助手

根据用户水平调整回答复杂度。

**请求：**
```
GET /api/prompt/explain?concept=递归&level=初学者
```

**代码示例：**
```java
@GetMapping("/explain")
public String explainConcept(
        @RequestParam String concept,
        @RequestParam(defaultValue = "初学者") String level) {

    return chatClient.prompt()
            .system(s -> s.text("你是一位耐心的教师。请用{level}能理解的方式，解释「{concept}」这个概念。")
                    .param("level", level)
                    .param("concept", concept))
            .system("要求：\n1. 使用简单易懂的语言\n2. 给出具体的例子\n3. 总结要点")
            .call()
            .content();
}
```

### 4. 代码审查

**请求：**
```
POST /api/prompt/code-review
Body: { "code": "public class Hello { ... }" }
```

**代码示例：**
```java
@PostMapping("/code-review")
public String codeReview(@RequestBody CodeReviewRequest request) {
    return chatClient.prompt()
            .system(s -> s
                    .template("code-review.st")
                    .param("code", request.getCode()))
            .call()
            .content();
}
```

## 模板管理技巧

### 动态模板加载

```java
@GetMapping("/dynamic-template")
public String dynamicTemplate(
        @RequestParam String templateName,
        @RequestParam Map<String, String> params) {

    return chatClient.prompt()
            .system(s -> {
                s.template(templateName);
                params.forEach(s::param);
            })
            .user("请基于以上背景回答问题")
            .call()
            .content();
}
```

### 模板变量校验

```java
@GetMapping("/safe-template")
public String safeTemplate(@RequestParam String name) {
    // 确保变量不为空
    String safeName = (name != null && !name.isBlank()) ? name : "用户";

    return chatClient.prompt()
            .system(s -> s.text("你好{name}，欢迎使用系统").param("name", safeName))
            .call()
            .content();
}
```

## 最佳实践

1. **模板版本管理**：将模板文件纳入 Git 管理
2. **变量校验**：检查模板变量是否为空
3. **错误处理**：捕获模板加载失败的异常
4. **A/B 测试**：准备多个模板进行效果对比
5. **本地化支持**：为不同语言准备不同模板

## 注意事项

- 模板文件扩展名通常为 `.st` 或 `.txt`
- 模板中的 `{` 和 `}` 会被解析为变量
- 如果需要字面量花括号，使用 `{{` 和 `}}`