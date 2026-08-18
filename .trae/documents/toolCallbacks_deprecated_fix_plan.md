# Spring AI 2.0 `toolCallbacks()` 弃用问题修复计划

## 问题分析结论

### 问题原因
在 Spring AI 2.0.0 版本中，`ChatClient.prompt().toolCallbacks(ToolCallback...)` 方法已被标记为 `@Deprecated(forRemoval = true)`，这意味着：
- 该方法在当前版本仍可使用，但已不推荐
- 在未来的小版本中会被完全移除
- IDEA 会报告编译警告/错误

### 替代方案
根据项目文档（[function-calling.md](file:///e:/project/agent/springAI/docs/function-calling.md) 和 [05-函数调用.md](file:///e:/project/agent/springAI/docs/05-函数调用.md)），新的推荐 API 是使用 **`.tools()`** 方法。

`.tools()` 方法有两种使用方式：
1. **方式一（兼容现有代码）**：传入 `ToolCallback` 对象（`FunctionToolCallback` 实现了 `ToolCallback` 接口，可直接复用）
2. **方式二（注解方式）**：使用 `@Tool` 注解标注 Spring Bean 方法，然后传入 Bean 对象

本项目选择 **方式一**，因为改动最小，只需替换方法名，工具构建逻辑保持不变。

---

## 需要修改的文件和模块

### 1. 源代码文件
| 文件 | 修改内容 |
|------|---------|
| [FunctionCallController.java](file:///e:/project/agent/springAI/src/main/java/com/example/springaidemo/controller/FunctionCallController.java) | `.toolCallbacks()` → `.tools()`，更新类注释和方法注释 |

### 2. 文档文件（同步更新代码示例）
| 文件 | 修改内容 |
|------|---------|
| [05-function-calling.md](file:///e:/project/agent/springAI/docs/05-function-calling.md) | 所有 `.toolCallbacks()` → `.tools()` |
| [09-migration.md](file:///e:/project/agent/springAI/docs/09-migration.md) | 更新迁移指南，增加 toolCallbacks → tools 的迁移说明 |
| [whats-new-2.0.md](file:///e:/project/agent/springAI/docs/whats-new-2.0.md) | 如有需要，同步更新 |

---

## 修改步骤

### 步骤 1：修改 FunctionCallController.java
1. **第 97 行**：`.toolCallbacks(` → `.tools(`
2. **类注释（第 24 行）**：`通过 toolCallbacks() 注册` → `通过 tools() 注册`
3. **类注释（第 33 行）**：`ChatClientRequestSpec.toolCallbacks()` → `ChatClientRequestSpec.tools()`
4. **方法注释（第 87 行）**：`通过 toolCallbacks() 方法` → `通过 tools() 方法`

### 步骤 2：修改 05-function-calling.md
替换文档中所有代码示例的 `.toolCallbacks(` 为 `.tools(`，涉及位置：
- 第 43 行（简单函数调用）
- 第 79 行（多函数调用）
- 第 104 行（Supplier 类型函数）
- 第 129 行（带复杂参数的函数）
- 第 165 行（处理函数调用结果）

### 步骤 3：修改 09-migration.md
在 **API 变更** 部分（ToolCallback 变更小节后），新增一条：
```
### 5. toolCallbacks() 重命名为 tools()

// 2.0.0-Mx/早期版本（已弃用）
.toolCallbacks(toolCallback)

// 2.0.0 GA 正式版本
.tools(toolCallback)
```

并在迁移检查清单中新增一项：
- [ ] 替换 `.toolCallbacks()` 为 `.tools()`

---

## 潜在依赖与注意事项

1. **兼容性**：`FunctionToolCallback` 实现了 `ToolCallback` 接口，`.tools(ToolCallback...)` 方法签名完全兼容，无需修改工具构建逻辑
2. **导入不变**：`import org.springframework.ai.tool.ToolCallback` 和 `import org.springframework.ai.tool.function.FunctionToolCallback` 两个导入保持不变
3. **功能等价**：`.tools()` 与 `.toolCallbacks()` 内部实现逻辑完全一致，只是方法名变更，运行时行为无差异

---

## 风险处理

| 风险项 | 影响 | 处理方式 |
|--------|------|---------|
| `.tools()` 方法签名不匹配 | 编译失败 | 实际签名为 `tools(ToolCallback...)`，与 `toolCallbacks(ToolCallback...)` 完全一致，无风险 |
| 其他控制器也使用了弃用 API | 遗漏修复 | 已全局搜索，仅 FunctionCallController.java 使用了 `toolCallbacks()` |
