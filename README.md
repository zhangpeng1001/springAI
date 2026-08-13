# Spring AI 学习 Demo 项目

> 一个涵盖 Spring AI 核心功能的学习项目，包含详细的中文注释和文档说明。

## 项目简介

本项目基于 **Spring Boot 3.3 + Spring AI 1.0.0** 构建，实现了 Spring AI 的所有核心功能模块，
每个功能都有完整的代码示例、详细中文注释和对应的文档说明，非常适合作为 Spring AI 的入门学习资料。

## 功能模块一览

| 序号 | 功能模块 | 说明 | 接口前缀 | 文档 |
|------|---------|------|---------|------|
| 1 | 基础聊天 | ChatClient 基本用法、ChatModel、运行时参数覆盖 | `/chat` | [聊天功能文档](docs/01-聊天功能.md) |
| 2 | 提示词模板 | PromptTemplate 变量替换、外部模板文件、系统提示词 | `/prompt` | [提示词模板文档](docs/02-提示词模板.md) |
| 3 | 结构化输出 | 将 AI 输出转为 POJO、List、Map，信息抽取 | `/structured` | [结构化输出文档](docs/03-结构化输出.md) |
| 4 | 多模态 | 图片理解（URL/本地）、文生图 | `/multimodal` | [多模态文档](docs/04-多模态.md) |
| 5 | 函数调用 | @Tool 注解、天气查询、计算器、多工具组合 | `/function` | [函数调用文档](docs/05-函数调用.md) |
| 6 | 会话记忆 | ChatMemory、多轮对话、会话管理 | `/memory` | [会话记忆文档](docs/06-会话记忆.md) |
| 7 | RAG 检索增强 | 文档加载、向量化、知识库问答 | `/rag` | [RAG 文档](docs/07-RAG检索增强.md) |

## 项目结构

```
springAI/
├── pom.xml                              # Maven 配置（依赖管理）
├── README.md                            # 项目说明（本文件）
├── docs/                                # 详细文档目录
│   ├── 01-聊天功能.md
│   ├── 02-提示词模板.md
│   ├── 03-结构化输出.md
│   ├── 04-多模态.md
│   ├── 05-函数调用.md
│   ├── 06-会话记忆.md
│   └── 07-RAG检索增强.md
└── src/main/
    ├── java/com/example/springaidemo/
    │   ├── SpringAiDemoApplication.java     # 启动类
    │   ├── config/
    │   │   └── AiConfig.java                # AI 配置（ChatClient、ChatMemory）
    │   ├── common/
    │   │   └── Result.java                  # 统一返回结果封装
    │   ├── controller/
    │   │   ├── ChatController.java          # 聊天功能
    │   │   ├── PromptController.java        # 提示词模板
    │   │   ├── StructuredOutputController.java  # 结构化输出
    │   │   ├── MultimodalController.java    # 多模态
    │   │   ├── FunctionCallController.java  # 函数调用
    │   │   ├── MemoryController.java        # 会话记忆
    │   │   └── RagController.java           # RAG 检索增强
    │   ├── function/
    │   │   ├── WeatherTools.java            # 天气查询工具
    │   │   └── CalculatorTools.java          # 计算器工具
    │   ├── model/
    │   │   └── ActorFilms.java              # 结构化输出示例模型
    │   └── service/
    │       └── RagService.java              # RAG 知识库服务
    └── resources/
        ├── application.yml                  # 应用配置（API Key 等）
        ├── prompts/                         # 提示词模板文件
        │   ├── translate.st
        │   └── joke.st
        └── docs/                            # RAG 知识库文档
            ├── spring-ai-intro.txt
            └── company-info.txt
```

## 快速开始

### 1. 环境要求

- **JDK 17+**（Spring AI 1.0.0 最低要求）
- **Maven 3.6+**
- 一个 OpenAI 兼容的 API Key（支持 OpenAI 官方或国内兼容服务）

### 2. 配置 API Key

编辑 `src/main/resources/application.yml`，或通过环境变量配置：

**方式一：环境变量（推荐）**

```bash
# Windows PowerShell
$env:OPENAI_API_KEY="sk-your-api-key"
$env:OPENAI_BASE_URL="https://api.openai.com"
$env:OPENAI_CHAT_MODEL="gpt-4o-mini"

# Linux / macOS
export OPENAI_API_KEY=sk-your-api-key
export OPENAI_BASE_URL=https://api.openai.com
export OPENAI_CHAT_MODEL=gpt-4o-mini
```

**方式二：修改配置文件**

直接修改 `application.yml` 中的 `spring.ai.openai` 配置项。

### 3. 常见模型服务配置

| 服务商 | base-url | 推荐模型 |
|--------|----------|---------|
| OpenAI | `https://api.openai.com` | `gpt-4o-mini` / `gpt-4o` |
| 智谱 GLM | `https://open.bigmodel.cn/api/paas/v4` | `glm-4-flash` / `glm-4` |
| 通义千问 | `https://dashscope.aliyuncs.com/compatible-mode/v1` | `qwen-turbo` / `qwen-plus` |
| DeepSeek | `https://api.deepseek.com` | `deepseek-chat` |
| 火山方舟 | `https://ark.cn-beijing.volces.com/api/v3` | `doubao-pro` |

### 4. 启动项目

```bash
# 在项目根目录执行
mvn spring-boot:run
```

启动成功后，应用运行在 `http://localhost:8080`。

### 5. 测试接口

打开浏览器访问以下接口（或使用 curl / Postman）：

```bash
# 基础聊天
curl "http://localhost:8080/chat/simple?message=你好"

# 提示词模板
curl "http://localhost:8080/prompt/inline?topic=面向对象编程&language=Python"

# 结构化输出
curl "http://localhost:8080/structured/actor?name=周星驰"

# 函数调用 - 天气查询
curl "http://localhost:8080/function/weather?question=北京今天天气怎么样"

# 会话记忆（多轮对话）
curl "http://localhost:8080/memory/chat?sessionId=user1&message=我叫张三"
curl "http://localhost:8080/memory/chat?sessionId=user1&message=我叫什么名字"

# RAG 知识库问答
curl "http://localhost:8080/rag/ask?question=Spring AI是什么"
```

## 接口总览

### 聊天功能 `/chat`
| 接口 | 说明 |
|------|------|
| `GET /chat/simple` | 简单对话 |
| `GET /chat/detail` | 获取完整响应（含 token 统计） |
| `GET /chat/model` | 使用底层 ChatModel |
| `GET /chat/creative` | 运行时覆盖参数（高温度创意写作） |

### 提示词模板 `/prompt`
| 接口 | 说明 |
|------|------|
| `GET /prompt/inline` | 内联模板 |
| `GET /prompt/external` | 外部模板文件 |
| `GET /prompt/system` | 系统提示词 |

### 结构化输出 `/structured`
| 接口 | 说明 |
|------|------|
| `GET /structured/actor` | 输出为 POJO |
| `GET /structured/list` | 输出为 List |
| `GET /structured/map` | 输出为 Map |
| `GET /structured/extract` | 信息抽取 |

### 多模态 `/multimodal`
| 接口 | 说明 |
|------|------|
| `GET /multimodal/url` | 分析网络图片 |
| `GET /multimodal/local` | 分析本地图片 |
| `GET /multimodal/generate` | 文生图 |

### 函数调用 `/function`
| 接口 | 说明 |
|------|------|
| `GET /function/weather` | 天气查询 |
| `GET /function/calc` | 计算器 |
| `GET /function/multi` | 多工具组合 |
| `GET /function/forecast` | 天气预报 |

### 会话记忆 `/memory`
| 接口 | 说明 |
|------|------|
| `GET /memory/chat` | 带记忆的多轮对话 |
| `GET /memory/history` | 查看会话历史 |
| `GET /memory/clear` | 清除会话记忆 |
| `GET /memory/no-memory` | 无记忆对话（对比） |

### RAG `/rag`
| 接口 | 说明 |
|------|------|
| `GET /rag/ask` | 基于知识库问答 |
| `GET /rag/search` | 检索文档片段 |
| `GET /rag/no-rag` | 不使用 RAG（对比） |
| `GET /rag/reload` | 重新加载知识库 |

## 学习路径建议

建议按以下顺序学习各模块：

1. **基础聊天** → 理解 ChatClient 和 ChatModel 的基本用法
2. **提示词模板** → 学会管理复用提示词
3. **结构化输出** → 掌握让 AI 输出程序可处理的数据
4. **函数调用** → 让 AI 能调用外部工具获取实时数据
5. **会话记忆** → 实现多轮对话
6. **多模态** → 处理图片等非文本输入
7. **RAG 检索增强** → 构建基于私有知识库的问答系统

## 技术栈

- **Java 17**
- **Spring Boot 3.3.0**
- **Spring AI 1.0.0**
- **Maven**

## 参考文档

- [Spring AI 官方文档](https://docs.spring.io/spring-ai/reference/)
- [Spring AI GitHub](https://github.com/spring-projects/spring-ai)
