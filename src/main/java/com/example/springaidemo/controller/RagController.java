package com.example.springaidemo.controller;

import com.example.springaidemo.common.Result;
import com.example.springaidemo.service.RagService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * RAG（检索增强生成）控制器
 * <p>
 * 本控制器演示如何让 AI 基于知识库文档回答问题。
 * <p>
 * 核心组件：
 * <ul>
 *     <li>{@link QuestionAnswerAdvisor}：RAG 顾问，自动完成"检索→注入上下文→生成"</li>
 *     <li>VectorStore：向量存储，存储文档片段的向量表示</li>
 * </ul>
 * <p>
 * 测试步骤：
 * 1. 应用启动时会自动加载 resources/docs/ 下的知识文档
 * 2. 访问 /rag/ask 接口提问，AI 会基于文档内容回答
 * 3. 访问 /rag/search 接口可以单独查看检索到的文档片段
 *
 * @author spring-ai-demo
 */
@RestController
@RequestMapping("/rag")
public class RagController {

    private final ChatClient chatClient;
    private final RagService ragService;

    public RagController(ChatClient.Builder chatClientBuilder, RagService ragService) {
        this.chatClient = chatClientBuilder.build();
        this.ragService = ragService;
    }

    /**
     * 接口1：基于知识库的问答（RAG 核心功能）
     * <p>
     * 当用户提问时：
     * 1. QuestionAnswerAdvisor 自动将问题向量化并在 VectorStore 中检索相关文档
     * 2. 将检索到的文档作为上下文注入到 Prompt 中
     * 3. 大模型基于上下文文档回答问题
     * <p>
     * 访问示例：GET /rag/ask?question=Spring AI是什么
     *
     * @param question 用户的问题
     * @return AI 基于知识库的回答
     */
    @GetMapping("/ask")
    public Result<String> askWithRag(@RequestParam String question) {
        // 构建 QuestionAnswerAdvisor：
        // - vectorStore：指定从哪个向量存储检索
        // - searchRequest：检索参数（topK=3 表示取最相关的3条）
        QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(ragService.getVectorStore())
                .searchRequest(SearchRequest.builder()
                        .topK(3)  // 检索最相关的 3 个文档片段
                        .build())
                .build();

        // 使用 advisor 后，ChatClient 会自动：
        // 1. 检索相关文档
        // 2. 将文档注入 Prompt 上下文
        // 3. 调用模型生成回答
        String response = chatClient.prompt()
                .user(question)
                .advisors(advisor)
                .call()
                .content();

        return Result.success(response);
    }

    /**
     * 接口2：单独检索相关文档片段（不调用大模型）
     * <p>
     * 演示向量检索功能本身，可以查看检索到了哪些文档片段。
     * 访问示例：GET /rag/search?query=Spring&topK=3
     *
     * @param query 查询文本
     * @param topK  返回条数
     * @return 检索到的文档片段列表
     */
    @GetMapping("/search")
    public Result<List<String>> searchDocuments(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int topK) {
        List<String> docs = ragService.searchDocuments(query, topK);
        return Result.success("检索到 " + docs.size() + " 条相关文档", docs);
    }

    /**
     * 接口3：对比演示 - 不使用 RAG 直接提问
     * <p>
     * 与 /rag/ask 对比，可以看到：
     * - 不用 RAG：AI 凭自己的知识回答（可能不知道私有信息）
     * - 用 RAG：AI 基于知识库文档回答（更准确）
     * 访问示例：GET /rag/no-rag?question=Spring AI是什么
     *
     * @param question 用户的问题
     * @return AI 凭自身知识的回答
     */
    @GetMapping("/no-rag")
    public Result<String> askWithoutRag(@RequestParam String question) {
        String response = chatClient.prompt()
                .user(question)
                .call()
                .content();
        return Result.success(response);
    }

    /**
     * 接口4：重新加载知识库
     * <p>
     * 当文档更新后，可以调用此接口重新加载。
     * 访问示例：GET /rag/reload
     *
     * @return 操作结果
     */
    @GetMapping("/reload")
    public Result<String> reloadKnowledgeBase() {
        ragService.loadDocuments();
        return Result.success("知识库已重新加载");
    }
}
