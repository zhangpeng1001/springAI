package com.example.springaidemo.controller;

import com.example.springaidemo.model.qualityscheme.CheckItem;
import com.example.springaidemo.model.qualityscheme.IntentResult;
import com.example.springaidemo.model.qualityscheme.QualityScheme;
import com.example.springaidemo.model.qualityscheme.SourceDto;
import com.example.springaidemo.service.QualitySchemeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 质检规范知识库 REST API 控制器。
 * <p>
 * 对应 LlamaIndex qualityScheme/web.py 的 FastAPI 路由，提供以下端点：
 * <ul>
 *   <li>GET  /api/quality/health — 健康检查</li>
 *   <li>POST /api/quality/ingest — 首次摄取文档到 Milvus</li>
 *   <li>POST /api/quality/rebuild — 重建索引</li>
 *   <li>POST /api/quality/ask — RAG 问答（答案+来源）</li>
 *   <li>POST /api/quality/ask-stream — SSE 流式 RAG 问答</li>
 *   <li>POST /api/quality/retrieve — 向量检索（含 file_name 过滤）</li>
 *   <li>POST /api/quality/retrieve/part — 按部分编号检索</li>
 *   <li>POST /api/quality/summary — 全文总结</li>
 *   <li>GET  /api/quality/scheme/check-items — 检查项清单</li>
 *   <li>POST /api/quality/scheme/generate — 生成质检方案</li>
 * </ul>
 * <p>
 * 所有端点均有详细日志：入参、处理过程、返回结果摘要。
 *
 * @author spring-ai-demo
 */
@RestController
@RequestMapping("/api/quality")
public class QualitySchemeController {

    private static final Logger log = LoggerFactory.getLogger(QualitySchemeController.class);

    private final QualitySchemeService qualitySchemeService;

    public QualitySchemeController(QualitySchemeService qualitySchemeService) {
        this.qualitySchemeService = qualitySchemeService;
    }

    // ========================================================================
    // 健康检查 / 索引管理
    // ========================================================================

    /**
     * 健康检查：返回服务状态、Milvus 配置、文档列表。
     * 对应 LlamaIndex web.py GET /api/health。
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        log.info("收到健康检查请求: GET /api/quality/health");
        Map<String, Object> info = qualitySchemeService.getHealthInfo();
        log.info("健康检查完成: collectionExists={}", info.get("collectionExists"));
        return info;
    }

    /**
     * 首次摄取文档：加载 PDF → 切块 → 嵌入 → 写入 Milvus。
     * 对应 LlamaIndex web.py POST /api/rebuild（首次构建）。
     */
    @PostMapping("/ingest")
    public Map<String, Object> ingest() {
        log.info("收到摄取文档请求: POST /api/quality/ingest");
        Map<String, Object> result = qualitySchemeService.ingestDocuments(false);
        log.info("摄取完成: status={}, chunkCount={}", result.get("status"), result.get("chunkCount"));
        return result;
    }

    /**
     * 重建索引：删除旧 collection 并重新切块、嵌入写入。
     * 对应 LlamaIndex web.py POST /api/rebuild。
     */
    @PostMapping("/rebuild")
    public Map<String, Object> rebuild() {
        log.info("收到重建索引请求: POST /api/quality/rebuild");
        Map<String, Object> result = qualitySchemeService.rebuildIndex();
        log.info("重建完成: status={}", result.get("status"));
        return result;
    }

    // ========================================================================
    // RAG 问答
    // ========================================================================

    /**
     * RAG 问答：检索 → 上下文注入 → LLM 生成 → 返回答案+来源。
     * 对应 LlamaIndex web.py POST /api/quickstart。
     *
     * @param req 请求体（question + topK）
     */
    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody QuestionRequest req) {
        log.info("收到 RAG 问答请求: question={}, topK={}", truncate(req.question, 80), req.topK);

        QualitySchemeService.RagResult result = qualitySchemeService.ragAsk(req.question, req.topK);

        Map<String, Object> response = new HashMap<>();
        response.put("answer", result.answer());
        response.put("sources", result.sources());

        log.info("RAG 问答返回: 答案长度={}, 来源数={}",
                result.answer().length(), result.sources().size());
        return response;
    }

    /**
     * SSE 流式 RAG 问答：逐 token 推送生成结果。
     * 对应 LlamaIndex web.py POST /api/stream。
     *
     * @param req 请求体（question + topK）
     */
    @PostMapping(value = "/ask-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> askStream(@RequestBody QuestionRequest req) {
        log.info("收到流式 RAG 问答请求: question={}, topK={}", truncate(req.question, 80), req.topK);
        return qualitySchemeService.ragAskStream(req.question, req.topK);
    }

    // ========================================================================
    // 向量检索（不调用 LLM）
    // ========================================================================

    /**
     * 纯向量检索：展示 Top-K 节点，支持 file_name 过滤。
     * 对应 LlamaIndex web.py POST /api/retrieve。
     *
     * @param req 请求体（question + topK + fileName）
     */
    @PostMapping("/retrieve")
    public Map<String, Object> retrieve(@RequestBody RetrieveRequest req) {
        log.info("收到向量检索请求: question={}, topK={}, fileName={}",
                truncate(req.question, 80), req.topK, req.fileName);

        List<SourceDto> sources = qualitySchemeService.retrieve(req.question, req.topK, req.fileName);

        Map<String, Object> response = new HashMap<>();
        response.put("sources", sources);

        log.info("向量检索返回: 节点数={}", sources.size());
        return response;
    }

    /**
     * 按规范部分编号（1~7）检索。
     * 对应 LlamaIndex web.py POST /api/retrieve/part。
     *
     * @param req 请求体（question + partNumber + topK）
     */
    @PostMapping("/retrieve/part")
    public Map<String, Object> retrieveByPart(@RequestBody RetrieveByPartRequest req) {
        log.info("收到按部分检索请求: partNumber={}, question={}",
                req.partNumber, truncate(req.question, 80));

        List<SourceDto> sources = qualitySchemeService.retrieveByPart(req.question, req.partNumber, req.topK);

        Map<String, Object> response = new HashMap<>();
        response.put("partNumber", req.partNumber);
        response.put("sources", sources);

        log.info("按部分检索返回: partNumber={}, 节点数={}", req.partNumber, sources.size());
        return response;
    }

    // ========================================================================
    // 全文总结
    // ========================================================================

    /**
     * 全文总结：遍历全部规范做归纳（Map-Reduce 策略）。
     * 对应 LlamaIndex web.py POST /api/summary。
     *
     * @param req 请求体（question）
     */
    @PostMapping("/summary")
    public Map<String, Object> summary(@RequestBody QuestionRequest req) {
        log.info("收到全文总结请求: question={}", truncate(req.question, 80));

        String answer = qualitySchemeService.summarize(req.question);

        Map<String, Object> response = new HashMap<>();
        response.put("answer", answer);

        log.info("全文总结返回: 摘要长度={}", answer.length());
        return response;
    }

    // ========================================================================
    // 质检方案编排
    // ========================================================================

    /**
     * 返回预定义检查项清单（28 项）。
     * 对应 LlamaIndex scheme_api.py GET /api/scheme/check-items。
     */
    @GetMapping("/scheme/check-items")
    public Map<String, Object> getCheckItems() {
        log.info("收到检查项清单请求: GET /api/quality/scheme/check-items");
        List<CheckItem> items = qualitySchemeService.getCheckItems();

        Map<String, Object> response = new HashMap<>();
        response.put("data", items);
        response.put("total", items.size());

        log.info("检查项清单返回: 共 {} 项", items.size());
        return response;
    }

    /**
     * 根据自然语言需求生成质检方案。
     * 对应 LlamaIndex scheme_api.py POST /api/scheme/generate。
     * <p>
     * 流程：意图识别 → 检索规范上下文 → LLM 生成结构化方案 → 校验 checkCode。
     *
     * @param req 请求体（requirement + contextTopK）
     */
    @PostMapping("/scheme/generate")
    public Map<String, Object> generateScheme(@RequestBody SchemeRequest req) {
        log.info("收到方案生成请求: requirement={}, contextTopK={}",
                truncate(req.requirement, 100), req.contextTopK);

        if (req.requirement == null || req.requirement.trim().isEmpty()) {
            log.warn("需求描述为空");
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "需求描述不能为空");
            return error;
        }

        QualitySchemeService.SchemeGenerationResult result =
                qualitySchemeService.generateScheme(req.requirement, req.contextTopK);

        Map<String, Object> response = new HashMap<>();
        response.put("status", result.status());

        if ("rejected".equals(result.status())) {
            // 非质检要求：返回引导提示
            response.put("message", result.message());
            response.put("suggestion", result.suggestion());
            log.info("方案生成被拒绝（意图识别未命中）: suggestion={}", truncate(result.suggestion(), 50));
        } else if ("error".equals(result.status())) {
            response.put("message", result.message());
            log.warn("方案生成失败: {}", result.message());
        } else {
            // 成功：返回方案
            response.put("schemeName", result.scheme().getSchemeName());
            response.put("description", result.scheme().getDescription());
            response.put("checkItem", result.scheme().getCheckItem());
            log.info("方案生成成功: schemeName={}, 检查项数={}",
                    result.scheme().getSchemeName(),
                    result.scheme().getCheckItem() != null ? result.scheme().getCheckItem().size() : 0);
        }

        return response;
    }

    // ========================================================================
    // 请求 DTO
    // ========================================================================

    /** 通用问题请求（RAG 问答、流式问答、全文总结） */
    public static class QuestionRequest {
        /** 用户问题 */
        public String question;
        /** 检索返回的节点数 */
        public int topK = 3;
    }

    /** 检索请求（支持 file_name 过滤） */
    public static class RetrieveRequest {
        /** 检索问题 */
        public String question;
        /** 返回节点数 */
        public int topK = 3;
        /** 可选的文件名过滤值 */
        public String fileName;
    }

    /** 按部分检索请求 */
    public static class RetrieveByPartRequest {
        /** 检索问题 */
        public String question;
        /** 规范部分编号（1~7） */
        public int partNumber;
        /** 返回节点数 */
        public int topK = 3;
    }

    /** 方案生成请求 */
    public static class SchemeRequest {
        /** 自然语言描述的质检需求 */
        public String requirement;
        /** 检索规范上下文的条款数 */
        public int contextTopK = 5;
    }

    // ========================================================================
    // 工具方法
    // ========================================================================

    /**
     * 截断字符串到指定长度（用于日志）。
     */
    private String truncate(String text, int maxLen) {
        if (text == null) return "null";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
