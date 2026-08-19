package com.example.springaidemo.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 质检规范知识库 Milvus 向量存储配置类。
 * <p>
 * 对应 LlamaIndex qualityScheme/milvus_store.py 的功能：创建指向固定 collection
 * （qualityScheme_springAI）的 MilvusVectorStore。
 * <p>
 * 设计决策：
 * <ul>
 *   <li>使用核心模块 spring-ai-milvus-store 手动配置，不使用 starter 自动配置，
 *       避免与现有 SimpleVectorStore（@Primary）的 Bean 冲突。</li>
 *   <li>MilvusServiceClient 连接 http://milvus-dev1.e-tudou.com:19530，无认证。</li>
 *   <li>MilvusVectorStore 指向 kernel_data_platform.qualityScheme_springAI collection，
 *       使用 COSINE 相似度度量，与 LlamaIndex 版一致。</li>
 *   <li>initializeSchema=true，首次使用时自动创建 collection。</li>
 * </ul>
 *
 * @author spring-ai-demo
 */
@Configuration
public class QualitySchemeConfig {

    private static final Logger log = LoggerFactory.getLogger(QualitySchemeConfig.class);

    /**
     * 创建 Milvus 服务客户端。
     * <p>
     * 连接到用户指定的免认证 Milvus 实例（http://milvus-dev1.e-tudou.com:19530）。
     * 使用 ConnectParam 构建连接参数，不设置 username/password。
     *
     * @param props 质检业务配置属性
     * @return MilvusServiceClient 实例
     */
    @Bean("qualitySchemeMilvusClient")
    public MilvusServiceClient milvusServiceClient(QualitySchemeProperties props) {
        String uri = props.getMilvus().getUri();
        log.info("创建 MilvusServiceClient: uri={}, database={}", uri, props.getMilvus().getDatabaseName());

        // 构建 Milvus 连接参数：仅设置 uri，不设置认证信息（免认证实例）
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withUri(uri)
                .build();

        MilvusServiceClient client = new MilvusServiceClient(connectParam);
        log.info("MilvusServiceClient 创建成功: uri={}", uri);
        return client;
    }

    /**
     * 创建质检规范专用的 Milvus 向量存储 Bean。
     * <p>
     * 指向 collection=qualityScheme_springAI，database=kernel_data_platform。
     * 使用 COSINE 相似度度量，IVF_FLAT 索引类型，自动初始化 schema（创建 collection）。
     *
     * @param client          Milvus 服务客户端
     * @param embeddingModel 嵌入模型（由 Spring AI 自动配置，OpenAI 兼容）
     * @param props          质检业务配置属性
     * @return MilvusVectorStore 实例（实现 VectorStore 接口）
     */
    @Bean("qualitySchemeVectorStore")
    public MilvusVectorStore qualitySchemeVectorStore(
            @Qualifier("qualitySchemeMilvusClient") MilvusServiceClient client,
            EmbeddingModel embeddingModel,
            QualitySchemeProperties props) {

        String collectionName = props.getMilvus().getCollectionName();
        String databaseName = props.getMilvus().getDatabaseName();
        int embeddingDimension = props.getMilvus().getEmbeddingDimension();

        log.info("创建 MilvusVectorStore: collection={}, database={}, dimension={}, metricType=COSINE",
                collectionName, databaseName, embeddingDimension);

        // 使用 Spring AI 2.0 的 Builder 模式构建 MilvusVectorStore
        // initializeSchema=true：首次使用时自动创建 collection（含向量字段、元数据字段）
        // IndexType.IVF_FLAT / MetricType.COSINE 来自 Milvus Java SDK（io.milvus.param）
        MilvusVectorStore vectorStore = MilvusVectorStore.builder(client, embeddingModel)
                .collectionName(collectionName)
                .databaseName(databaseName)
                .embeddingDimension(embeddingDimension)
                .indexType(IndexType.IVF_FLAT)
                .metricType(MetricType.COSINE)
                .initializeSchema(true)
                .build();

        log.info("MilvusVectorStore 创建成功: collection={}, database={}", collectionName, databaseName);
        return vectorStore;
    }
}
