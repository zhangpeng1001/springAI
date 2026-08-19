# 质检规范知识库问答系统（Spring AI 版）实现计划

## 概述

将 LlamaIndex `qualityScheme` 模块的全部业务功能（8 大核心功能 + 质检方案编排）移植到当前 Spring AI 2.0 项目中。使用 Milvus 向量数据库（collection=`qualityScheme_springAI`），读取 `standard/` 目录下的 7 份 PDF 规范文档，提供完整的 RAG 问答、来源追踪、元数据过滤、全文总结与方案编排能力。

---

## 一、当前状态分析

### 1.1 现有 Spring AI 项目结构
- **配置**：[AiConfig.java](file:///e:/project/agent/springAI/src/main/java/com/example/springaidemo/config/AiConfig.java) 已有 `ChatClient`、`ChatMemory`、`SimpleVectorStore`（内存向量存储）
- **RAG**：[RagController.java](file:///e:/project/agent/springAI/src/main/java/com/example/springaidemo/controller/RagController.java) + [RagService.java](file:///e:/project/agent/springAI/src/main/java/com/example/springaidemo/service/RagService.java) 使用 `QuestionAnswerAdvisor` + `SimpleVectorStore`
- **模型配置**：[application.yml](file:///e:/project/agent/springAI/src/main/resources/application.yml) 配置了 OpenAI 兼容接口（`base-url: https://ai2-api.i-tudou.com/v1`，`model: gpt-5.4`，`embed-model: Qwen3-Embedding-0.6B`）
- **前端**：[test.html](file:///e:/project/agent/springAI/test.html) 单页应用（侧边栏 + 多标签页），CORS 已在 [CorsConfig.java](file:///e:/project/agent/springAI/src/main/java/com/example/springaidemo/config/CorsConfig.java) 中全局开启
- **依赖**：[pom.xml](file:///e:/project/agent/springAI/pom.xml) 使用 Spring AI 2.0.0 BOM，已有 `spring-ai-vector-store`、`spring-ai-vector-store-advisor`、`spring-ai-rag`

### 1.2 LlamaIndex 源模块功能映射

| LlamaIndex 模块 | 功能 | Spring AI 对应方案 |
|---|---|---|
| `document_loader.py` | 文档加载 | `PagePdfDocumentReader` 读取 PDF |
| `document_parser.py` | 数据摄取（切块+嵌入） | `TokenTextSplitter` + `VectorStore.add()` |
| `vector_index.py` | 向量索引 | `MilvusVectorStore` |
| `index_persistence.py` | 索引持久化 | Milvus 自带持久化 + 首次摄取判断 |
| `query_engine.py` | RAG 问答 | `QuestionAnswerAdvisor` + `ChatClient` |
| `source_tracker.py` | 来源追踪 | `SearchRequest` + `Document.metadata` |
| `metadata_filter.py` | 元数据过滤 | `SearchRequest.builder().filterExpression()` |
| `summary_engine.py` | 全文总结 | Map-Reduce 摘要（分块总结→合并） |
| `check_items.py` | 检查项清单 | Java 常量类（28 项预定义） |
| `scheme_generator.py` | 方案生成 | `ChatClient.entity(QualityScheme.class)` 结构化输出 |
| `scheme_intent.py` | 意图识别 | `ChatClient.entity(IntentResult.class)` 结构化输出 |
| `web.py` | FastAPI 路由 | `@RestController` REST API |

### 1.3 关键技术决策

1. **Milvus 集成方式**：使用核心模块 `spring-ai-milvus-store`（非 starter），手动创建 `MilvusServiceClient` + `MilvusVectorStore` Bean，避免与现有 `SimpleVectorStore` 的自动配置冲突。在 `AiConfig` 的 `SimpleVectorStore` 上加 `@Primary`，确保现有 `RagService` 仍注入 SimpleVectorStore。
2. **PDF 读取**：使用 `spring-ai-pdf-document-reader` 的 `PagePdfDocumentReader`，按页读取 PDF，保留 `page_number`、`file_name` 元数据用于来源追踪。
3. **嵌入维度**：Qwen3-Embedding-0.6B 输出 1024 维，通过 `EmbeddingModel.dimensions()` 运行时探测，配置为可调属性。
4. **向量检索相似度**：使用 `COSINE`（与 LlamaIndex 版一致）。
5. **结构化输出**：复用 Spring AI 的 `ChatClient.entity()` 实现方案生成与意图识别的结构化输出。
6. **全文总结**：采用 Map-Reduce 策略（分块摘要→合并摘要），等价于 LlamaIndex 的 `tree_summarize`。

---

## 二、依赖与配置变更

### 2.1 pom.xml 新增依赖

在 [pom.xml](file:///e:/project/agent/springAI/pom.xml) `<dependencies>` 中新增：

```xml
<!-- Spring AI Milvus 向量存储核心模块（手动配置，避免与 SimpleVectorStore 自动配置冲突） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-milvus-store</artifactId>
</dependency>

<!-- Spring AI PDF 文档读取器（PagePdfDocumentReader） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-pdf-document-reader</artifactId>
</dependency>
```

### 2.2 application.yml 新增配置

在 [application.yml](file:///e:/project/agent/springAI/src/main/resources/application.yml) 末尾新增质检规范业务配置：

```yaml
# ====================================================================
# 质检规范知识库（qualityScheme）配置
# ====================================================================
quality:
  scheme:
    # 标准规范 PDF 文档目录
    standard-dir: classpath:standard
    # Milvus 向量数据库连接配置（免认证，直接使用）
    milvus:
      uri: http://milvus-dev1.e-tudou.com:19530
      database-name: kernel_data_platform
      collection-name: qualityScheme_springAI
      # 嵌入向量维度（Qwen3-Embedding-0.6B = 1024）
      embedding-dimension: 1024
      # 索引类型：IVF_FLAT 适合中小规模数据
      index-type: IVF_FLAT
      # 相似度度量：COSINE（与 LlamaIndex 版一致）
      metric-type: COSINE
    # 文档切块参数
    chunk:
      # TokenTextSplitter 默认每块 token 数
      chunk-size: 256
      # 相邻块重叠 token 数
      chunk-overlap: 40
    # 检索默认参数
    retrieve:
      default-top-k: 3
    # 方案生成默认上下文检索数
    scheme:
      context-top-k: 5
```

### 2.3 AiConfig.java 修改

在 [AiConfig.java](file:///e:/project/agent/springAI/src/main/java/com/example/springaidemo/config/AiConfig.java) 的 `vectorStore` 方法上加 `@Primary` 注解：

```java
@Bean
@Primary
public VectorStore vectorStore(EmbeddingModel embeddingModel) {
    return SimpleVectorStore.builder(embeddingModel).build();
}
```

---

## 三、新增文件清单

所有新增 Java 文件位于 `com.example.springaidemo` 包下：

| 文件 | 路径 | 职责 |
|---|---|---|
| `QualitySchemeProperties.java` | `config/` | 质检业务配置属性绑定 |
| `QualitySchemeConfig.java` | `config/` | MilvusServiceClient + MilvusVectorStore Bean |
| `CheckItem.java` | `model/qualityscheme/` | 检查项实体类 |
| `QualityScheme.java` | `model/qualityscheme/` | 质检方案实体类 |
| `IntentResult.java` | `model/qualityscheme/` | 意图识别结果实体类 |
| `SourceDto.java` | `model/qualityscheme/` | 来源追踪 DTO |
| `QualitySchemeService.java` | `service/` | 全部业务逻辑 |
| `QualitySchemeController.java` | `controller/` | REST API 控制器 |
| `qualityScheme.html` | 项目根目录 | 前端独立页面 |

---

## 四、详细实现方案

### 4.1 QualitySchemeProperties.java — 配置属性绑定

**文件**：`src/main/java/com/example/springaidemo/config/QualitySchemeProperties.java`

```java
@ConfigurationProperties(prefix = "quality.scheme")
public class QualitySchemeProperties {
    private String standardDir;
    private Milvus milvus = new Milvus();
    private Chunk chunk = new Chunk();
    private Retrieve retrieve = new Retrieve();
    private Scheme scheme = new Scheme();
    // 嵌套类 Milvus、Chunk、Retrieve、Scheme + getter/setter
}
```

需在 `SpringAiDemoApplication` 上加 `@EnableConfigurationProperties(QualitySchemeProperties.class)`。

### 4.2 QualitySchemeConfig.java — Milvus Bean 配置

**文件**：`src/main/java/com/example/springaidemo/config/QualitySchemeConfig.java`

```java
@Configuration
public class QualitySchemeConfig {

    @Bean("qualitySchemeMilvusClient")
    public MilvusServiceClient milvusServiceClient(QualitySchemeProperties props) {
        // 使用 io.milvus.client.MilvusServiceClient + ConnectParam
        // 连接 http://milvus-dev1.e-tudou.com:19530，无认证
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withUri(props.getMilvus().getUri())
                .build();
        return new MilvusServiceClient(connectParam);
    }

    @Bean("qualitySchemeVectorStore")
    public MilvusVectorStore qualitySchemeVectorStore(
            @Qualifier("qualitySchemeMilvusClient") MilvusServiceClient client,
            EmbeddingModel embeddingModel,
            QualitySchemeProperties props) {
        // 手动构建 MilvusVectorStore，指向 qualityScheme_springAI collection
        return MilvusVectorStore.builder(client, embeddingModel)
                .collectionName(props.getMilvus().getCollectionName())
                .databaseName(props.getMilvus().getDatabaseName())
                .indexType(IndexType.IVF_FLAT)
                .metricType(MetricType.COSINE)
                .embeddingDimension(props.getMilvus().getEmbeddingDimension())
                .batchingStrategy(new TokenCountBatchingStrategy())
                .initializeSchema(true)
                .build();
    }
}
```

> 注意：`MilvusVectorStore.builder()` 的确切 API 需在实现时通过 IDE 自动补全或查阅 Spring AI 2.0.0 源码确认。核心参数为 `collectionName`、`databaseName`、`indexType`、`metricType`、`embeddingDimension`、`initializeSchema`。

### 4.3 实体类（model/qualityscheme/）

#### CheckItem.java
```java
public class CheckItem {
    private String checkCode;     // 检查项编码
    private String checkName;     // 中文名称
    private String checkDesc;     // 说明
    private String checkObjType;  // 检查对象类型（VECTOR）
    private String checkParam;    // 参数名 JSON 字符串
    private List<String> paramNames;  // 解析后的参数名列表
}
```

#### QualityScheme.java（方案生成结构化输出）
```java
public class QualityScheme {
    private String schemeName;
    private String description;
    private List<CheckItem> checkItem;
}
```

#### IntentResult.java（意图识别结构化输出）
```java
public class IntentResult {
    private boolean isQualityRequirement;
    private String reason;
    private String suggestion;
}
```

#### SourceDto.java（来源追踪 DTO）
```java
public class SourceDto {
    private int position;
    private String fileName;
    private String filePath;
    private Double score;
    private String preview;
    private String documentId;
}
```

### 4.4 CheckItems 常量注册 — 内嵌于 QualitySchemeService

将 LlamaIndex `check_items.py` 中 28 项预定义检查项移植为 Java 静态列表。包含：`qualityCheckFieldLength`、`qualityCheckRequiredFieldMismatch`、`QualityCheckUniqueValue` 等全部 28 项。提供方法：
- `listCheckItems()` — 返回全部检查项
- `getCheckItem(checkCode)` — 按 code 查询
- `isValidCheckCode(code)` — 校验合法性
- `formatCheckItemsForPrompt()` — 格式化为 Markdown 表格供 LLM 使用

### 4.5 QualitySchemeService.java — 核心业务逻辑

**文件**：`src/main/java/com/example/springaidemo/service/QualitySchemeService.java`

注入：`ChatClient`、`MilvusVectorStore`（@Qualifier）、`EmbeddingModel`、`QualitySchemeProperties`

#### 方法清单：

**① 文档加载 — `loadDocuments()`**
- 使用 Spring `ResourcePatternResolver` 扫描 `standard/` 目录下所有 `.pdf` 文件
- 对每个 PDF 用 `PagePdfDocumentReader` 读取，注入元数据：`file_name`、`file_path`、`page_number`、`part_number`（从文件名解析，如"第2部分" → 2）
- 日志：入参目录、找到的文件数、每个文件页数与字符数

**② 数据摄取 — `ingestDocuments(boolean rebuild)`**
- 调用 `loadDocuments()` 加载 PDF
- 用 `TokenTextSplitter`（chunkSize=256, overlap=40）切块
- 调用 `qualitySchemeVectorStore.add(documents)` 写入 Milvus（自动嵌入）
- rebuild=true 时先删除 collection 再重建
- 日志：文档数、切块参数、切块前后文档数、写入耗时、Milvus collection 行数

**③ RAG 问答 — `ragAsk(String question, int topK)`**
- 用 `QuestionAnswerAdvisor.builder(qualitySchemeVectorStore)` 创建 advisor，设置 topK
- 通过 `ChatClient.prompt().user(question).advisors(qaAdvisor).call().content()` 生成回答
- 同时执行 `similaritySearch` 获取来源文档
- 返回 `RagResult(answer, sourceDtos)`
- 日志：问题、topK、答案长度、来源文件列表

**④ 流式 RAG 问答 — `ragAskStream(String question, int topK)`**
- 同上但用 `ChatClient.prompt()...stream()` 返回 `Flux<String>` 用于 SSE
- 日志：问题、topK

**⑤ 向量检索（含元数据过滤） — `retrieve(String question, int topK, String fileName)`**
- 构建 `SearchRequest.builder().query(question).topK(topK)`
- 若 fileName 非空，添加 `FilterExpressionBuilder.eq("file_name", fileName)` 过滤
- 调用 `qualitySchemeVectorStore.similaritySearch(request)`
- 将结果转为 `List<SourceDto>`
- 日志：问题、topK、过滤条件、返回节点数、每个结果的文件名与分数

**⑥ 按部分检索 — `retrieveByPart(String question, int partNumber, int topK)`**
- 从文件名 `part{N}_` 前缀或"第N部分"匹配
- 调用 `retrieve` 并在内存中过滤 `part_number` 元数据
- 日志：部分编号、候选数、命中数

**⑦ 全文总结 — `summarize(String question)`**
- 加载全部文档并切块
- Map 阶段：对每个切块调用 LLM 生成摘要
- Reduce 阶段：合并所有摘要，调用 LLM 生成最终总结
- 日志：文档数、块数、Map 阶段耗时、Reduce 阶段耗时、最终摘要长度

**⑧ 检查项清单 — `getCheckItems()`**
- 返回静态检查项列表

**⑨ 方案生成 — `generateScheme(String requirement, int contextTopK)`**
- 先调用 `retrieve(requirement, contextTopK)` 获取规范上下文
- 组装 prompt（规范上下文 + 检查项清单 + 用户需求）
- 用 `ChatClient.prompt().user(promptText).call().entity(QualityScheme.class)` 获取结构化方案
- 校验生成的 checkCode 合法性，过滤非法项，补齐缺失参数
- 日志：需求、上下文条款数、LLM 原始检查项数、合法项数、被过滤数

**⑩ 意图识别 — `recognizeIntent(String requirement)`**
- 组装意图识别 prompt（检查项清单 + 用户输入）
- 用 `ChatClient.prompt().user(promptText).call().entity(IntentResult.class)` 获取判断
- 异常兜底：LLM 调用失败时默认放行
- 日志：需求、判定结果、理由、耗时

**⑪ 健康检查 — `getHealthInfo()`**
- 返回 provider、model、embed model、Milvus uri/db/collection、standard 目录文件列表、collection 行数
- 日志：各关键信息

**⑫ 重建索引 — `rebuildIndex()`**
- 调用 `ingestDocuments(true)` 强制重建
- 日志：重建开始/完成

### 4.6 QualitySchemeController.java — REST API

**文件**：`src/main/java/com/example/springaidemo/controller/QualitySchemeController.java`

`@RestController` + `@RequestMapping("/api/quality")`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/health` | 健康检查 |
| POST | `/rebuild` | 重建索引 |
| POST | `/ingest` | 首次摄取文档 |
| POST | `/ask` | RAG 问答（返回答案+来源） |
| POST | `/ask-stream` | SSE 流式 RAG 问答 |
| POST | `/retrieve` | 向量检索（含 file_name 过滤） |
| POST | `/retrieve/part` | 按部分编号检索 |
| POST | `/summary` | 全文总结 |
| GET | `/scheme/check-items` | 检查项清单 |
| POST | `/scheme/generate` | 生成质检方案 |

每个端点都有详细日志：入参、处理过程、返回结果摘要。

### 4.7 前端页面 qualityScheme.html

**文件**：`e:\project\agent\springAI\qualityScheme.html`

独立单页应用，复用 test.html 的视觉风格（侧边栏 + 标签页），包含以下面板：

1. **状态面板**：健康检查信息、重建索引按钮
2. **RAG 问答**：问题输入 + topK + 答案展示 + 来源节点列表
3. **向量检索**：问题 + topK + file_name 过滤 + 检索结果列表
4. **按部分检索**：问题 + 部分编号(1-7) + topK + 结果列表
5. **全文总结**：问题输入 + 总结输出
6. **流式问答**：问题 + SSE 流式输出 + 停止按钮
7. **方案编排**：自然语言需求 + context_topK + 方案展示 + 检查项清单查看

所有 API 调用指向 `http://localhost:8080/api/quality/*`。

---

## 五、实现顺序

1. **pom.xml** — 新增 Milvus + PDF reader 依赖
2. **application.yml** — 新增 quality.scheme.* 配置
3. **AiConfig.java** — SimpleVectorStore 加 @Primary
4. **QualitySchemeProperties.java** — 配置属性类
5. **SpringAiDemoApplication.java** — 加 @EnableConfigurationProperties
6. **实体类** — CheckItem、QualityScheme、IntentResult、SourceDto
7. **QualitySchemeConfig.java** — Milvus Bean 配置
8. **QualitySchemeService.java** — 核心业务逻辑（最大的文件）
9. **QualitySchemeController.java** — REST API
10. **qualityScheme.html** — 前端页面
11. **启动验证** — 运行项目，测试各端点

---

## 六、验证步骤

1. **编译验证**：`mvn compile` 无错误
2. **启动验证**：应用启动，Milvus 连接成功，collection 自动创建
3. **摄取验证**：`POST /api/quality/ingest` — 7 份 PDF 读取、切块、嵌入、写入 Milvus
4. **健康检查**：`GET /api/quality/health` — 返回 collection 行数 > 0
5. **RAG 问答**：`POST /api/quality/ask` — 问"检测点编号规则是什么"，返回答案+来源
6. **检索验证**：`POST /api/quality/retrieve` — 按 file_name 过滤检索
7. **按部分检索**：`POST /api/quality/retrieve/part` — part_number=2 检索检测点规范
8. **全文总结**：`POST /api/quality/summary` — 总结时空数据规范核心内容
9. **方案生成**：`POST /api/quality/scheme/generate` — 输入"检测点坐标精度不超过0.5米，编号唯一"，生成结构化方案
10. **前端验证**：浏览器打开 qualityScheme.html，所有面板功能正常

---

## 七、假设与决策

1. **PDF 读取方式**：直接用 Spring AI `PagePdfDocumentReader` 读取 `standard/` 下的 PDF 原文件（LlamaIndex 版先将 PDF 提取为 Markdown 再读取，Spring AI 版直接读 PDF，更简洁）。
2. **Milvus 连接无认证**：按用户要求，`ConnectParam` 不设置 username/password。
3. **嵌入维度 1024**：Qwen3-Embedding-0.6B 输出 1024 维，配置为可调属性。
4. **collection 自动创建**：`initializeSchema=true` 让 MilvusVectorStore 自动创建 collection。
5. **现有 RagController/RagService 不受影响**：SimpleVectorStore 标记 @Primary，保持原有功能。
6. **方案编排包含意图识别**：在 `/api/quality/scheme/generate` 中先做意图识别，非质检需求返回引导提示而非错误。
7. **全文总结用 Map-Reduce**：等价于 LlamaIndex 的 `tree_summarize`，分块摘要再合并。
8. **结构化输出用 ChatClient.entity()**：Spring AI 2.0 的结构化输出能力，等价于 LlamaIndex 的 Pydantic 结构化输出。
