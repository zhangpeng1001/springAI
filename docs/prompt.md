# 提示词模板

## 概述

提示词模板（Prompt Template）允许我们将 AI 指令与业务逻辑分离，实现动态、可维护的提示词管理。

## 核心概念

### 为什么需要模板？

- **可维护性**：提示词作为模板存储，修改不需要改代码
- **可复用性**：同一模板可以在不同场景复用
- **参数化**：通过变量填充实现动态提示词

## API 列表

### 1. 变量模板

**GET** `/api/prompt/template`

使用变量替换的提示词模板。

**参数：**
- `role` (String, 必填) - AI 角色
- `task` (String, 必填) - 要完成的任务

**示例：**
```bash
curl "http://localhost:8080/api/prompt/template?role=翻译专家&task=将这段话翻译成英文"
```

---

### 2. 翻译助手

**GET** `/api/prompt/translate`

专业的多语言翻译。

**参数：**
- `text` (String, 必填) - 要翻译的文本
- `targetLanguage` (String, 可选) - 目标语言，默认"英文"
- `style` (String, 可选) - 翻译风格，默认"正式"

**示例：**
```bash
curl "http://localhost:8080/api/prompt/translate?text=你好世界&targetLanguage=日文&style=口语"
```

---

### 3. 代码审查

**POST** `/api/prompt/code-review`

AI 扮演代码审查专家。

**请求体：**
```json
{
  "code": "public void hello() {\n    System.out.println(\"Hello\");\n}"
}
```

---

### 4. 知识讲解

**GET** `/api/prompt/explain`

根据用户水平调整讲解深度。

**参数：**
- `concept` (String, 必填) - 要解释的概念
- `level` (String, 可选) - 用户水平，默认"初学者"

**示例：**
```bash
curl "http://localhost:8080/api/prompt/explain?concept=量子计算&level=专家"
```

## 核心代码解析

```java
// 模板定义 - 使用 {变量名} 语法
String template = "你是一个{role}。请帮我完成以下任务：{task}";

// 创建模板实例
SystemPromptTemplate systemTemplate = new SystemPromptTemplate(template);

// 使用 Map 填充变量
Message systemMessage = systemTemplate.createMessage(
    Map.of("role", "翻译专家", "task", "翻译这段话")
);
```

### 2.0 新写法

```java
// ChatClient 流式 API 支持内联模板
chatClient.prompt()
    .system("你是{role}")
    .system(p -> p.param("role", "翻译专家"))
    .user("你好")
    .call()
    .content();
```

## 最佳实践

1. **使用占位符**：用 `{变量名}` 代替字符串拼接
2. **明确角色**：给 AI 设定清晰的角色定位
3. **指定格式**：明确要求的输出格式
4. **Few-shot**：提供示例（可选）
