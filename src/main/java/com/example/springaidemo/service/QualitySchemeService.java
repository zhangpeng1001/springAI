package com.example.springaidemo.service;

import com.example.springaidemo.config.QualitySchemeProperties;
import com.example.springaidemo.model.qualityscheme.CheckItem;
import com.example.springaidemo.model.qualityscheme.CheckItemDefinition;
import com.example.springaidemo.model.qualityscheme.CheckItemDefinitions;
import com.example.springaidemo.model.qualityscheme.IntentResult;
import com.example.springaidemo.model.qualityscheme.ParamSpec;
import com.example.springaidemo.model.qualityscheme.QualityScheme;
import com.example.springaidemo.model.qualityscheme.SourceDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.DropCollectionParam;
import io.milvus.param.collection.HasCollectionParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 质检规范知识库核心业务服务。
 * <p>
 * 对应 LlamaIndex qualityScheme 模块的全部业务逻辑，包含 8 大核心功能与质检方案编排：
 * <ol>
 *   <li>文档加载 — 从 standard/ 目录读取 7 份 PDF 规范文档</li>
 *   <li>数据摄取 — 切块 + 嵌入 + 写入 Milvus</li>
 *   <li>向量索引 — MilvusVectorStore 管理（由 Spring 容器托管）</li>
 *   <li>索引持久化 — Milvus 自带持久化，进程重启后无需重新嵌入</li>
 *   <li>RAG 问答 — QuestionAnswerAdvisor 自动检索 + 上下文注入 + LLM 生成</li>
 *   <li>来源追踪 — 返回检索到的文档来源（文件名、分数、预览）</li>
 *   <li>元数据过滤 — 按 file_name / part_number 过滤检索</li>
 *   <li>全文总结 — Map-Reduce 策略（分块摘要→合并），等价于 tree_summarize</li>
 *   <li>质检方案编排 — 意图识别 + 检索上下文 + 结构化输出生成方案</li>
 * </ol>
 * <p>
 * 所有方法均提供详细日志：入参、处理过程、返回结果摘要，便于调试与排查。
 *
 * @author spring-ai-demo
 */
@Service
public class QualitySchemeService {

    private static final Logger log = LoggerFactory.getLogger(QualitySchemeService.class);

    // ===== 依赖注入 =====

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final QualitySchemeProperties properties;
    private final MilvusServiceClient milvusServiceClient;
    private final ObjectMapper objectMapper;

    /**
     * 质检规范专用的 Milvus 向量存储。
     * 使用 volatile 保证 rebuild 后多线程可见性。
     */
    private volatile MilvusVectorStore qualitySchemeVectorStore;

    /**
     * 用于从 MilvusServiceClient + EmbeddingModel 创建新的 MilvusVectorStore 的配置。
     */
    private final QualitySchemeProperties.Milvus milvusConfig;

    /**
     * 构造函数注入。
     *
     * @param chatClient           Spring AI ChatClient（OpenAI 兼容）
     * @param embeddingModel       嵌入模型（Qwen3-Embedding-0.6B）
     * @param qualitySchemeVectorStore 质检专用 MilvusVectorStore
     * @param milvusServiceClient  Milvus 服务客户端（用于 collection 管理）
     * @param properties           质检业务配置属性
     */
    public QualitySchemeService(
            ChatClient chatClient,
            EmbeddingModel embeddingModel,
            @Qualifier("qualitySchemeVectorStore") MilvusVectorStore qualitySchemeVectorStore,
            @Qualifier("qualitySchemeMilvusClient") MilvusServiceClient milvusServiceClient,
            QualitySchemeProperties properties) {
        this.chatClient = chatClient;
        this.embeddingModel = embeddingModel;
        this.qualitySchemeVectorStore = qualitySchemeVectorStore;
        this.milvusServiceClient = milvusServiceClient;
        this.properties = properties;
        this.milvusConfig = properties.getMilvus();
        this.objectMapper = new ObjectMapper();
        log.info("QualitySchemeService 初始化完成: collection={}, database={}",
                milvusConfig.getCollectionName(), milvusConfig.getDatabaseName());
    }

    // ========================================================================
    // ① 文档加载 — 对应 LlamaIndex document_loader.py
    // ========================================================================

    /**
     * 从 standard/ 目录加载所有 PDF 规范文档。
     * <p>
     * 使用 Spring ResourcePatternResolver 扫描目录下的 .pdf 文件，
     * 用 PagePdfDocumentReader 逐页读取，注入元数据（file_name、file_path、part_number）。
     *
     * @return Document 列表，每个 PDF 每页对应一个 Document
     */
    public List<Document> loadDocuments() {
        String standardDir = properties.getStandardDir();
        log.info("开始加载文档目录: standardDir={}", standardDir);

        List<Document> allDocuments = new ArrayList<>();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        try {
            // 扫描目录下所有 PDF 文件
            String pattern = standardDir.endsWith("/") ? standardDir + "*.pdf" : standardDir + "/*.pdf";
            Resource[] pdfResources = resolver.getResources(pattern);
            log.info("找到 PDF 文件数: {}", pdfResources.length);

            for (Resource pdfResource : pdfResources) {
                String fileName = pdfResource.getFilename();
                log.info("读取 PDF: fileName={}", fileName);

                try {
                    // 使用 PagePdfDocumentReader 按页读取 PDF
                    PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(pdfResource);
                    List<Document> docs = pdfReader.get();

                    // 为每个页面注入业务元数据：file_name、file_path、part_number
                    int partNumber = extractPartNumber(fileName);
                    List<Document> enrichedDocs = new ArrayList<>();
                    for (Document doc : docs) {
                        Map<String, Object> metadata = new HashMap<>(doc.getMetadata());
                        metadata.put("file_name", fileName);
                        metadata.put("file_path", pdfResource.getURL().getPath());
                        metadata.put("part_number", partNumber);
                        enrichedDocs.add(Document.builder()
                                .text(doc.getText())
                                .metadata(metadata)
                                .build());
                    }

                    allDocuments.addAll(enrichedDocs);
                    log.info("PDF 读取完成: fileName={}, 页数={}, 字符数={}",
                            fileName, docs.size(),
                            enrichedDocs.stream().mapToInt(d -> d.getText().length()).sum());

                } catch (Exception e) {
                    log.error("读取 PDF 失败: fileName={}, error={}", fileName, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("扫描 PDF 目录失败: standardDir={}, error={}", standardDir, e.getMessage(), e);
        }

        if (allDocuments.isEmpty()) {
            log.error("未加载到任何 PDF 文档: standardDir={}", standardDir);
        } else {
            log.info("文档加载完成: 总页数={}, 文件数={}",
                    allDocuments.size(),
                    allDocuments.stream().map(d -> d.getMetadata().get("file_name")).distinct().count());
        }

        return allDocuments;
    }

    /**
     * 从文件名中提取规范部分编号（1~7）。
     * 文件名形如"实景三维质检大数据支撑库 时空数据规范 第2部分 检测点.pdf"。
     *
     * @param fileName PDF 文件名
     * @return 部分编号，无法识别时返回 0
     */
    private int extractPartNumber(String fileName) {
        Pattern pattern = Pattern.compile("第(\\d+)部分");
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()) {
            int partNum = Integer.parseInt(matcher.group(1));
            log.debug("提取部分编号: fileName={}, partNumber={}", fileName, partNum);
            return partNum;
        }
        log.debug("无法提取部分编号: fileName={}", fileName);
        return 0;
    }

    // ========================================================================
    // ② 数据摄取 — 对应 LlamaIndex document_parser.py + index_persistence.py
    // ========================================================================

    /**
     * 摄取文档到 Milvus 向量存储（加载→切块→嵌入→写入）。
     *
     * @param rebuild 是否强制重建（true=先删除 collection 再重新摄取）
     * @return 摄取结果信息
     */
    public Map<String, Object> ingestDocuments(boolean rebuild) {
        log.info("开始数据摄取: rebuild={}", rebuild);
        long startTime = System.currentTimeMillis();

        Map<String, Object> result = new HashMap<>();

        // rebuild 时先删除旧 collection
        if (rebuild) {
            log.info("rebuild=true，开始删除旧 collection: {}", milvusConfig.getCollectionName());
            try {
                dropCollection();
                log.info("旧 collection 已删除，准备重建");
                // 删除后需要重新创建 MilvusVectorStore（initializeSchema 会创建 collection）
                recreateVectorStore();
            } catch (Exception e) {
                log.warn("删除旧 collection 异常（可能不存在，可忽略）: {}", e.getMessage());
                // 即使删除失败也尝试重新摄取，MilvusVectorStore 会处理
                try {
                    recreateVectorStore();
                } catch (Exception ex) {
                    log.error("重建 MilvusVectorStore 失败: {}", ex.getMessage(), ex);
                }
            }
        }

        // 1. 加载 PDF 文档
        List<Document> documents = loadDocuments();
        if (documents.isEmpty()) {
            log.error("摄取失败：未加载到任何文档");
            result.put("status", "error");
            result.put("message", "未加载到任何 PDF 文档");
            return result;
        }
        log.info("加载完成: 文档数={}", documents.size());

        // 2. 切块（TokenTextSplitter）
        int chunkSize = properties.getChunk().getChunkSize();
        int chunkOverlap = properties.getChunk().getChunkOverlap();
        log.info("开始切块: chunkSize={}, chunkOverlap={}", chunkSize, chunkOverlap);

        TokenTextSplitter splitter = createSplitter();
        List<Document> chunks = splitter.apply(documents);
        log.info("切块完成: 原始文档数={}, 切块后文档数={}", documents.size(), chunks.size());

        // 3. 写入 Milvus（自动嵌入 + 存储）
        log.info("开始写入 Milvus: collection={}", milvusConfig.getCollectionName());
        try {
            qualitySchemeVectorStore.add(chunks);
            log.info("写入 Milvus 完成: chunk数={}", chunks.size());
        } catch (Exception e) {
            log.error("写入 Milvus 失败: {}", e.getMessage(), e);
            result.put("status", "error");
            result.put("message", "写入 Milvus 失败: " + e.getMessage());
            return result;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("数据摄取完成: 切块数={}, 耗时={}ms, collection={}",
                chunks.size(), elapsed, milvusConfig.getCollectionName());

        result.put("status", "success");
        result.put("documentCount", documents.size());
        result.put("chunkCount", chunks.size());
        result.put("elapsedMs", elapsed);
        result.put("collection", milvusConfig.getCollectionName());
        result.put("rebuild", rebuild);
        return result;
    }

    /**
     * 创建文本切块器（TokenTextSplitter）。
     * <p>
     * Spring AI 的 TokenTextSplitter 基于 token 数切块，无 overlap 概念，
     * 这里使用 6 参数构造器：
     * <ul>
     *   <li>chunkSize — 每块目标 token 数（来自配置 quality.scheme.chunk.chunk-size）</li>
     *   <li>minChunkSizeChars — 每块最小字符数，避免过小碎片（默认 350）</li>
     *   <li>minChunkLengthToEmbed — 可嵌入的最小块长度（默认 5）</li>
     *   <li>maxNumChunks — 单文档最大切块数，防止超长文档爆块（默认 10000）</li>
     *   <li>keepSeparator — 是否保留分隔符（true，保留上下文边界）</li>
     *   <li>separators — 分隔符字符列表（换行/制表符，优先在自然边界切分）</li>
     * </ul>
     *
     * @return 配置好的 TokenTextSplitter 实例
     */
    private TokenTextSplitter createSplitter() {
        int chunkSize = properties.getChunk().getChunkSize();
        int chunkOverlap = properties.getChunk().getChunkOverlap();
        log.info("创建 TokenTextSplitter: chunkSize={}, chunkOverlap={}(仅记录，Spring AI 不支持 overlap)", chunkSize, chunkOverlap);
        // 分隔符：在换行/制表符处优先切分，保留语义边界
        List<Character> separators = Arrays.asList('\n', '\r', '\t');
        return new TokenTextSplitter(chunkSize, 350, 5, 10000, true, separators);
    }

    /**
     * 删除 Milvus collection（用于 rebuild）。
     */
    private void dropCollection() {
        String collectionName = milvusConfig.getCollectionName();
        String databaseName = milvusConfig.getDatabaseName();

        // 先检查 collection 是否存在
        R<Boolean> hasResp = milvusServiceClient.hasCollection(
                HasCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withDatabaseName(databaseName)
                        .build());
        if (hasResp.getData() != null && hasResp.getData()) {
            log.info("collection 存在，执行删除: {}", collectionName);
            milvusServiceClient.dropCollection(
                    DropCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withDatabaseName(databaseName)
                            .build());
            log.info("collection 删除成功: {}", collectionName);
        } else {
            log.info("collection 不存在，跳过删除: {}", collectionName);
        }
    }

    /**
     * 重新创建 MilvusVectorStore（用于 rebuild 后重建 collection）。
     */
    private void recreateVectorStore() {
        log.info("重新创建 MilvusVectorStore: collection={}", milvusConfig.getCollectionName());
        MilvusVectorStore newStore = MilvusVectorStore.builder(milvusServiceClient, embeddingModel)
                .collectionName(milvusConfig.getCollectionName())
                .databaseName(milvusConfig.getDatabaseName())
                .embeddingDimension(milvusConfig.getEmbeddingDimension())
                .indexType(IndexType.IVF_FLAT)
                .metricType(MetricType.COSINE)
                .initializeSchema(true)
                .build();
        this.qualitySchemeVectorStore = newStore;
        log.info("MilvusVectorStore 重建成功");
    }

    // ========================================================================
    // ③ RAG 问答 — 对应 LlamaIndex query_engine.py
    // ========================================================================

    /**
     * 使用 RAG 进行问答（检索→上下文注入→LLM 生成→返回答案+来源）。
     *
     * @param question 用户问题
     * @param topK     检索返回的节点数
     * @return 包含答案和来源的结果
     */
    public RagResult ragAsk(String question, int topK) {
        log.info("RAG 问答: question={}, topK={}", truncate(question, 100), topK);

        // 使用 QuestionAnswerAdvisor 自动完成检索和上下文注入
        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(qualitySchemeVectorStore)
                .searchRequest(SearchRequest.builder().topK(topK).build())
                .build();

        // 调用 LLM 生成回答
        String answer = chatClient.prompt()
                .user(question)
                .advisors(qaAdvisor)
                .call()
                .content();

        // 同时执行检索获取来源文档（用于来源追踪）
        List<Document> sourceDocs = qualitySchemeVectorStore.similaritySearch(
                SearchRequest.builder().query(question).topK(topK).build());

        List<SourceDto> sources = toSourceDtos(sourceDocs);

        log.info("RAG 问答完成: 答案长度={}, 来源数={}", answer.length(), sources.size());
        log.debug("答案预览: {}", truncate(answer, 200));

        return new RagResult(answer, sources);
    }

    /**
     * 使用 RAG 进行流式问答（用于 SSE 推送）。
     *
     * @param question 用户问题
     * @param topK     检索返回的节点数
     * @return 流式回答的 Flux
     */
    public reactor.core.publisher.Flux<String> ragAskStream(String question, int topK) {
        log.info("流式 RAG 问答: question={}, topK={}", truncate(question, 100), topK);

        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(qualitySchemeVectorStore)
                .searchRequest(SearchRequest.builder().topK(topK).build())
                .build();

        return chatClient.prompt()
                .user(question)
                .advisors(qaAdvisor)
                .stream()
                .content();
    }

    // ========================================================================
    // ⑤ 向量检索（含元数据过滤）— 对应 LlamaIndex metadata_filter.py
    // ========================================================================

    /**
     * 执行纯向量检索（不调用 LLM），返回 Top-K 节点。
     *
     * @param question 检索问题文本
     * @param topK     返回节点数
     * @param fileName 可选的文件名过滤值，精确匹配 metadata.file_name
     * @return 来源 DTO 列表，按相似度从高到低排序
     */
    public List<SourceDto> retrieve(String question, int topK, String fileName) {
        log.info("向量检索: question={}, topK={}, fileName={}", truncate(question, 80), topK, fileName);

        SearchRequest.Builder requestBuilder = SearchRequest.builder()
                .query(question)
                .topK(topK);

        // 若指定了 fileName，添加元数据过滤
        if (fileName != null && !fileName.trim().isEmpty()) {
            log.info("启用元数据过滤: key=file_name, value={}", fileName);
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            requestBuilder.filterExpression(b.eq("file_name", fileName).build());
        }

        List<Document> docs = qualitySchemeVectorStore.similaritySearch(requestBuilder.build());
        List<SourceDto> sources = toSourceDtos(docs);

        log.info("检索完成: 返回节点数={}", sources.size());
        if (sources.isEmpty()) {
            if (fileName != null) {
                log.warn("过滤后无结果，请检查 file_name={} 是否存在于索引中", fileName);
            } else {
                log.warn("检索无结果，请检查索引是否已构建");
            }
        }

        return sources;
    }

    // ========================================================================
    // ⑥ 按部分检索 — 对应 LlamaIndex metadata_filter.retrieve_by_part
    // ========================================================================

    /**
     * 按规范部分编号（1~7）检索。
     *
     * @param question    检索问题
     * @param partNumber  规范部分编号（1~7）
     * @param topK        返回节点数
     * @return 来源 DTO 列表
     */
    public List<SourceDto> retrieveByPart(String question, int partNumber, int topK) {
        log.info("按部分检索: partNumber={}, question={}", partNumber, truncate(question, 80));

        // 先扩大检索范围，再在内存中按 part_number 过滤
        // Spring AI 的元数据过滤对整数值可能有兼容性问题，这里用内存过滤更可靠
        List<SourceDto> allNodes = retrieve(question, topK * 5, null);
        log.info("扩大检索范围: 候选数={}", allNodes.size());

        // 在内存中按 part_number 过滤（part_number 存储为整数元数据）
        List<SourceDto> filtered = allNodes.stream()
                .filter(s -> {
                    // part_number 在文档元数据中，但 SourceDto 只保留 file_name
                    // 这里通过 file_name 中的"第N部分"来过滤
                    String fn = s.getFileName();
                    if (fn == null) return false;
                    Pattern p = Pattern.compile("第(\\d+)部分");
                    Matcher m = p.matcher(fn);
                    if (m.find()) {
                        return Integer.parseInt(m.group(1)) == partNumber;
                    }
                    return false;
                })
                .limit(topK)
                .collect(Collectors.toList());

        log.info("部分过滤: 候选={}, 命中={}", allNodes.size(), filtered.size());
        return filtered;
    }

    // ========================================================================
    // ⑦ 全文总结 — 对应 LlamaIndex summary_engine.py（Map-Reduce 策略）
    // ========================================================================

    /**
     * 使用 Map-Reduce 策略对全部规范文档进行总结。
     * <p>
     * Map 阶段：对每个切块调用 LLM 生成摘要。
     * Reduce 阶段：合并所有摘要，调用 LLM 生成最终总结。
     * 等价于 LlamaIndex 的 SummaryIndex + tree_summarize。
     *
     * @param question 总结问题（如"请总结时空数据规范的核心内容"）
     * @return 总结文本
     */
    public String summarize(String question) {
        log.info("全文总结: question={}", truncate(question, 80));
        long startTime = System.currentTimeMillis();

        // 1. 加载并切块全部文档
        List<Document> documents = loadDocuments();
        TokenTextSplitter splitter = createSplitter();
        List<Document> chunks = splitter.apply(documents);
        log.info("全文总结切块: 文档数={}, 块数={}", documents.size(), chunks.size());

        // 2. Map 阶段：逐块摘要
        log.info("Map 阶段开始: 对 {} 个块生成摘要", chunks.size());
        List<String> chunkSummaries = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i).getText();
            String fileName = (String) chunks.get(i).getMetadata().getOrDefault("file_name", "未知");
            log.debug("Map 块 #{}/{}, file={}, 长度={}", i + 1, chunks.size(), fileName, chunkText.length());

            String mapPrompt = "请用简洁的语言总结以下规范内容的关键要点（不超过200字）：\n\n" + chunkText;
            try {
                String summary = chatClient.prompt()
                        .user(mapPrompt)
                        .call()
                        .content();
                chunkSummaries.add("[" + fileName + "] " + summary);
            } catch (Exception e) {
                log.warn("Map 块 #{} 摘要失败: {}", i + 1, e.getMessage());
                chunkSummaries.add("[" + fileName + "] " + truncate(chunkText, 200));
            }
        }
        long mapElapsed = System.currentTimeMillis() - startTime;
        log.info("Map 阶段完成: 摘要数={}, 耗时={}ms", chunkSummaries.size(), mapElapsed);

        // 3. Reduce 阶段：合并所有摘要，生成最终总结
        log.info("Reduce 阶段开始: 合并 {} 个摘要", chunkSummaries.size());
        StringBuilder combinedSummaries = new StringBuilder();
        for (String s : chunkSummaries) {
            combinedSummaries.append(s).append("\n\n");
        }

        String reducePrompt = question + "\n\n以下是各部分规范的摘要，请基于这些内容给出综合总结：\n\n" + combinedSummaries;
        String finalSummary = chatClient.prompt()
                .user(reducePrompt)
                .call()
                .content();

        long totalElapsed = System.currentTimeMillis() - startTime;
        log.info("全文总结完成: 最终摘要长度={}, 总耗时={}ms", finalSummary.length(), totalElapsed);
        log.debug("最终摘要预览: {}", truncate(finalSummary, 200));

        return finalSummary;
    }

    // ========================================================================
    // ⑧ 检查项清单 — 对应 LlamaIndex check_items.py
    // ========================================================================

    /**
     * 预定义检查项定义来源：{@link CheckItemDefinitions#DEFINITIONS}（27 项）。
     * <p>
     * 原始数据已迁移至独立文件 CheckItemDefinitions.java，使用 record + ParamSpec 结构，
     * 每个参数含名称、说明、示例，便于 LLM 理解。
     */

    /** 规范化后的检查项列表（含解析后的 paramNames，不含 dataName） */
    private static List<CheckItem> CHECK_ITEMS = null;
    /** checkCode → CheckItem 映射，用于 O(1) 校验 */
    private static Map<String, CheckItem> CHECK_ITEM_BY_CODE = null;

    /**
     * 初始化检查项列表（懒加载）。
     * <p>
     * 从 {@link CheckItemDefinitions#DEFINITIONS} 构建 {@link CheckItem} 列表。
     * dataName 不在 paramNames/checkParam 中（已提升到 CheckItem 外层字段）。
     */
    private synchronized void initCheckItems() {
        if (CHECK_ITEMS != null) return;
        log.info("初始化预定义检查项清单");
        CHECK_ITEMS = new ArrayList<>();
        CHECK_ITEM_BY_CODE = new HashMap<>();

        for (CheckItemDefinition def : CheckItemDefinitions.DEFINITIONS) {
            // 从 CheckItemDefinition 构建 CheckItem（用于 API 输出）
            CheckItem item = new CheckItem(def.checkCode(), def.checkName(), def.checkDesc());
            // checkParam 保持为 JSON 字符串（仅含规则特有参数，不含 dataName）
            try {
                item.setCheckParam(objectMapper.writeValueAsString(def.getParamNames()));
            } catch (Exception e) {
                log.warn("序列化 paramNames 失败: checkCode={}, error={}", def.checkCode(), e.getMessage());
                item.setCheckParam("[]");
            }
            item.setParamNames(def.getParamNames());
            CHECK_ITEMS.add(item);
            CHECK_ITEM_BY_CODE.put(def.checkCode(), item);
        }
        log.info("检查项清单初始化完成: 共 {} 项", CHECK_ITEMS.size());
    }

    /**
     * 返回全部预定义检查项。
     */
    public List<CheckItem> getCheckItems() {
        initCheckItems();
        return CHECK_ITEMS;
    }

    /**
     * 按 checkCode 查询检查项详情。
     */
    public CheckItem getCheckItem(String checkCode) {
        initCheckItems();
        return CHECK_ITEM_BY_CODE.get(checkCode);
    }

    /**
     * 判断 checkCode 是否存在于预定义清单。
     */
    public boolean isValidCheckCode(String checkCode) {
        initCheckItems();
        return CHECK_ITEM_BY_CODE.containsKey(checkCode);
    }

    /**
     * 把检查项清单格式化为供 LLM prompt 使用的结构化文本。
     * <p>
     * 格式：通用说明 + 逐项详述列表。
     * - 通用参数（dataName、fieldNames、dz_data_name）在顶部统一说明
     * - 每项列出规则特有参数（不含 dataName），附带说明 + 示例
     * - 无额外参数的项标注"规则参数：无"
     */
    public String formatCheckItemsForPrompt() {
        initCheckItems();
        StringBuilder sb = new StringBuilder();

        // 通用说明
        sb.append("## 通用说明\n");
        sb.append("每个检查项都有一个 dataName 字段（图层名称），填写被检查的图层名称，如 dltb、检测点、检测线。\n\n");
        sb.append("以下参数在多个检查项中反复出现，含义固定：\n");
        sb.append("- fieldNames：字段名称，多个字段用英文逗号隔开（如：id,name）\n");
        sb.append("- dz_data_name：对照图层名称，用于图层间一致性检查（如：xzq）\n\n");

        // 逐项详述
        sb.append("## 预定义检查项清单（共").append(CheckItemDefinitions.DEFINITIONS.size());
        sb.append("项，生成的 checkCode 必须只能来自此清单）\n");
        sb.append("每个检查项需填写 dataName（图层名称）和下列规则参数：\n\n");

        int index = 1;
        for (CheckItemDefinition def : CheckItemDefinitions.DEFINITIONS) {
            sb.append("### ").append(index).append(". ").append(def.checkCode())
                    .append(" — ").append(def.checkName()).append("\n");
            sb.append("说明：").append(def.checkDesc()).append("\n");

            if (def.params().isEmpty()) {
                sb.append("规则参数：无（仅需 dataName）\n\n");
            } else {
                sb.append("规则参数：\n");
                for (ParamSpec param : def.params()) {
                    sb.append("  - ").append(param.name()).append("：").append(param.desc());
                    sb.append("。示例：").append(param.example()).append("\n");
                }
                sb.append("\n");
            }
            index++;
        }

        return sb.toString();
    }

    // ========================================================================
    // ⑨ 方案生成 — 对应 LlamaIndex scheme_generator.py
    // ========================================================================

    /** 方案生成 Prompt 模板（移植自 LlamaIndex scheme_generator.py） */
    private static final String SCHEME_PROMPT_TEMPLATE = """
            你是实景三维质检方案编排专家。请根据"用户需求"与"时空数据规范上下文"，从"预定义检查项清单"中选择合适的检查项，生成结构化质检方案。

            ## 时空数据规范上下文
            以下是检索到的与用户需求相关的规范条款，供你推断字段名、阈值等参数：
            %s

            ## 预定义检查项清单（生成的 checkCode 必须只能来自此清单）
            %s

            ## 用户需求
            %s

            ## 输出要求
            请输出符合下列规则的方案：
            1. schemeName：简洁名称，体现数据对象与检查重点（如"检测点数据坐标与编号质检方案"）。
            2. description：1-2 句描述方案目标与检查范围。
            3. checkItem：根据用户需求选择最匹配的检查项，遵循：
               - checkCode 必须来自上述清单，且 checkName 与清单一致。
               - dataName 填写用户需求中提到的图层名称（如"检测点"、"检测线"），为顶层字段，不要放在 params 中。
               - params 仅包含该检查项"规则参数"中声明的参数（不含 dataName），键名与参数名完全一致。
               - 参数值推断依据：优先参考"时空数据规范上下文"，上下文不足时结合用户需求合理设定。
               - fieldNames 参数为字段名列表（字符串，逗号隔开）。
               - 不要添加用户未提及的检查项；若用户需求与某检查项无关，则不要选入。
            4. 若用户需求中包含数值阈值（如"不超过0.5米"），请将其填入对应参数（如 threshold 或 min_length / min_area / min_angle）。

            请直接输出结构化结果。
            """;

    /**
     * 根据自然语言需求生成质检方案。
     * <p>
     * 流程：
     * 1. 意图识别：判断输入是否为质检方案要求。
     * 2. 用向量索引检索相关规范条款作为上下文。
     * 3. 调用 LLM 生成结构化方案。
     * 4. 校验 checkCode 合法性，过滤非法项并补齐缺失参数。
     *
     * @param requirement  用户的自然语言质检需求
     * @param contextTopK  检索规范上下文的条款数
     * @return 方案生成结果（包含 IntentResult 和 QualityScheme）
     */
    public SchemeGenerationResult generateScheme(String requirement, int contextTopK) {
        log.info("开始生成质检方案: requirement={}", truncate(requirement, 100));

        // 1. 意图识别
        IntentResult intent = recognizeIntent(requirement);
        log.info("意图识别结果: isQuality={}, reason={}", intent.isQualityRequirement(), intent.getReason());

        if (!intent.isQualityRequirement()) {
            log.info("未识别到质检方案要求，返回引导提示");
            return new SchemeGenerationResult(intent, null, "rejected",
                    "未识别到质检方案要求，请输入具体的质检需求。", intent.getSuggestion());
        }

        // 2. 检索规范上下文
        log.info("检索规范上下文: contextTopK={}", contextTopK);
        List<SourceDto> contextSources = retrieve(requirement, contextTopK, null);
        String context = buildContextFromSources(contextSources);
        log.info("规范上下文构建完成: 条款数={}, 长度={}", contextSources.size(), context.length());

        // 3. 组装 prompt 并调用 LLM 生成结构化方案
        String checkItemsText = formatCheckItemsForPrompt();
        String prompt = String.format(SCHEME_PROMPT_TEMPLATE, context, checkItemsText, requirement);

        log.info("调用 LLM 生成结构化方案");
        QualityScheme rawScheme;
        try {
            rawScheme = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(QualityScheme.class);
        } catch (Exception e) {
            log.error("LLM 生成方案失败: {}", e.getMessage(), e);
            return new SchemeGenerationResult(intent, null, "error",
                    "方案生成失败: " + e.getMessage(), null);
        }

        log.info("LLM 原始生成: schemeName={}, 检查项数={}",
                rawScheme.getSchemeName(),
                rawScheme.getCheckItem() != null ? rawScheme.getCheckItem().size() : 0);

        // 4. 校验 checkCode 合法性，过滤非法项并补齐缺失参数
        List<CheckItem> validItems = new ArrayList<>();
        List<String> invalidCodes = new ArrayList<>();

        if (rawScheme.getCheckItem() != null) {
            for (CheckItem item : rawScheme.getCheckItem()) {
                if (!isValidCheckCode(item.getCheckCode())) {
                    log.warn("非法 checkCode 被过滤: {}", item.getCheckCode());
                    invalidCodes.add(item.getCheckCode());
                    continue;
                }
                // 用清单中的标准 checkName 覆盖
                CheckItem canonical = getCheckItem(item.getCheckCode());
                item.setCheckName(canonical.getCheckName());

                // 补齐缺失的参数
                if (item.getParams() == null) {
                    item.setParams(new HashMap<>());
                }
                for (String paramName : canonical.getParamNames()) {
                    if (!item.getParams().containsKey(paramName)) {
                        log.debug("补齐缺失参数: checkCode={}, param={}", item.getCheckCode(), paramName);
                        item.getParams().put(paramName, null);
                    }
                }
                validItems.add(item);
            }
        }

        if (!invalidCodes.isEmpty()) {
            log.warn("被过滤的非法 checkCode: {}", invalidCodes);
        }

        rawScheme.setCheckItem(validItems);
        log.info("方案生成完成: schemeName={}, 合法检查项={}, 被过滤={}",
                rawScheme.getSchemeName(), validItems.size(), invalidCodes.size());

        return new SchemeGenerationResult(intent, rawScheme, "success", null, null);
    }

    /**
     * 把溯源节点构建为上下文文本。
     */
    private String buildContextFromSources(List<SourceDto> sources) {
        if (sources == null || sources.isEmpty()) {
            log.warn("未检索到相关规范条款，方案生成将缺少业务上下文");
            return "（未检索到相关规范条款）";
        }
        StringBuilder sb = new StringBuilder();
        for (SourceDto s : sources) {
            sb.append("[").append(s.getFileName()).append("] ").append(s.getPreview()).append("\n");
        }
        return sb.toString();
    }

    // ========================================================================
    // ⑩ 意图识别 — 对应 LlamaIndex scheme_intent.py
    // ========================================================================

    /** 意图识别 Prompt 模板（移植自 LlamaIndex scheme_intent.py） */
    private static final String INTENT_PROMPT_TEMPLATE = """
            你是实景三维质检方案的意图识别专家。请判断"用户输入"是否为真实的质检方案要求。

            ## 系统能识别的质检能力域（预定义检查项清单）
            以下检查项覆盖了系统支持的质检能力域（字段检查、几何检查、坐标系检查、图层一致性、值域、时间有效性、编码匹配等）：
            %s

            ## 用户输入
            %s

            ## 判定规则
            1. 若"用户输入"描述了可映射到上述任一检查项能力域的质检需求，即使未出现"质检方案"关键字（例如"检测点坐标精度不超过0.5米，编号唯一"、"检查图层是否使用平面坐标系"、"字段值是否唯一"），则 isQualityRequirement=true。
            2. 若"用户输入"为闲聊、问候、与质检无关的问题（例如"你好"、"今天天气怎么样"、"帮我写首诗"），则 isQualityRequirement=false，并在 suggestion 中给出质检需求示例引导用户重新输入。
            3. suggestion 示例："请输入具体的质检需求，例如：检测点坐标精度不超过0.5米，编号唯一；或：检查图层字段是否完整、是否使用平面坐标系。"

            请直接输出结构化结果。
            """;

    /**
     * 对用户输入做意图识别，判断是否为质检方案要求。
     * <p>
     * 异常兜底：LLM 调用失败时默认放行，保证主流程可用性优先。
     *
     * @param requirement 用户的自然语言输入
     * @return IntentResult 对象
     */
    public IntentResult recognizeIntent(String requirement) {
        log.info("开始意图识别: requirement={}", truncate(requirement, 100));
        long startTime = System.currentTimeMillis();

        String checkItemsText = formatCheckItemsForPrompt();
        String prompt = String.format(INTENT_PROMPT_TEMPLATE, checkItemsText, requirement);

        try {
            log.info("调用 LLM 进行意图识别");
            IntentResult result = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(IntentResult.class);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("意图识别完成: isQuality={}, reason={}, 耗时={}ms",
                    result.isQualityRequirement(), result.getReason(), elapsed);
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.warn("意图识别 LLM 调用异常，默认放行: error={}, 耗时={}ms", e.getMessage(), elapsed);
            IntentResult fallback = new IntentResult();
            fallback.setQualityRequirement(true);
            fallback.setReason("意图识别异常，默认放行: " + e.getMessage());
            fallback.setSuggestion("");
            return fallback;
        }
    }

    // ========================================================================
    // ⑪ 健康检查 — 对应 LlamaIndex web.py /api/health
    // ========================================================================

    /**
     * 获取健康检查信息。
     *
     * @return 包含服务状态、Milvus 配置、文档列表等信息的 Map
     */
    public Map<String, Object> getHealthInfo() {
        log.info("健康检查");
        Map<String, Object> info = new HashMap<>();

        info.put("service", "qualityScheme");
        info.put("milvusUri", milvusConfig.getUri());
        info.put("milvusDatabase", milvusConfig.getDatabaseName());
        info.put("milvusCollection", milvusConfig.getCollectionName());
        info.put("embeddingDimension", milvusConfig.getEmbeddingDimension());
        info.put("metricType", milvusConfig.getMetricType());
        info.put("standardDir", properties.getStandardDir());

        // 扫描标准目录下的文件列表
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            String pattern = properties.getStandardDir().endsWith("/")
                    ? properties.getStandardDir() + "*.pdf"
                    : properties.getStandardDir() + "/*.pdf";
            Resource[] resources = resolver.getResources(pattern);
            List<String> fileNames = Arrays.stream(resources)
                    .map(Resource::getFilename)
                    .sorted()
                    .collect(Collectors.toList());
            info.put("standardFiles", fileNames);
        } catch (Exception e) {
            log.warn("扫描标准目录失败: {}", e.getMessage());
            info.put("standardFiles", List.of());
        }

        // 检查 Milvus collection 是否存在
        try {
            R<Boolean> hasResp = milvusServiceClient.hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName(milvusConfig.getCollectionName())
                            .withDatabaseName(milvusConfig.getDatabaseName())
                            .build());
            boolean collectionExists = hasResp.getData() != null && hasResp.getData();
            info.put("collectionExists", collectionExists);
            log.info("健康检查: collectionExists={}", collectionExists);
        } catch (Exception e) {
            log.warn("检查 collection 存在性失败: {}", e.getMessage());
            info.put("collectionExists", false);
        }

        return info;
    }

    // ========================================================================
    // ⑫ 重建索引 — 对应 LlamaIndex web.py /api/rebuild
    // ========================================================================

    /**
     * 重建索引（删除旧 collection 并重新切块、嵌入写入）。
     *
     * @return 重建结果信息
     */
    public Map<String, Object> rebuildIndex() {
        log.info("收到重建索引请求");
        Map<String, Object> result = ingestDocuments(true);
        if ("success".equals(result.get("status"))) {
            result.put("message", "质检规范 Milvus 索引已重建");
        }
        return result;
    }

    // ========================================================================
    // 工具方法
    // ========================================================================

    /**
     * 将检索到的 Document 列表转换为来源 DTO 列表。
     * 对应 LlamaIndex source_tracker.py 的 sources_to_dict。
     */
    private List<SourceDto> toSourceDtos(List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return new ArrayList<>();
        }

        List<SourceDto> sources = new ArrayList<>();
        int position = 1;
        for (Document doc : docs) {
            String fileName = (String) doc.getMetadata().getOrDefault("file_name", "未知文件");
            String filePath = (String) doc.getMetadata().get("file_path");
            Double score = null; // Spring AI 的 similaritySearch 不直接返回 score
            String preview = doc.getText().replace("\n", " ");
            if (preview.length() > 200) {
                preview = preview.substring(0, 200);
            }
            String docId = doc.getId();

            sources.add(new SourceDto(position, fileName, filePath, score, preview, docId));
            log.debug("来源 #{}: file={}, docId={}", position, fileName, docId);
            position++;
        }
        return sources;
    }

    /**
     * 截断字符串到指定长度（用于日志）。
     */
    private String truncate(String text, int maxLen) {
        if (text == null) return "null";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    // ========================================================================
    // DTO 类
    // ========================================================================

    /**
     * RAG 问答结果 DTO。
     */
    public record RagResult(String answer, List<SourceDto> sources) {}

    /**
     * 方案生成结果 DTO。
     */
    public record SchemeGenerationResult(
            IntentResult intent,
            QualityScheme scheme,
            String status,
            String message,
            String suggestion
    ) {}
}
