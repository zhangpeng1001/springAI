package com.example.springaidemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 质检规范知识库（qualityScheme）配置属性绑定类。
 * <p>
 * 对应 application.yml 中 {@code quality.scheme.*} 配置项，包含：
 * <ul>
 *   <li>standardDir：标准规范 PDF 文档目录</li>
 *   <li>milvus：Milvus 向量数据库连接配置（uri / database / collection / 维度 / 索引类型 / 相似度度量）</li>
 *   <li>chunk：文档切块参数（chunkSize / chunkOverlap）</li>
 *   <li>retrieve：检索默认参数（defaultTopK）</li>
 *   <li>scheme：方案生成参数（contextTopK）</li>
 * </ul>
 * <p>
 * 对应 LlamaIndex qualityScheme/config.py 的 QualitySchemeConfig。
 *
 * @author spring-ai-demo
 */
@ConfigurationProperties(prefix = "quality.scheme")
public class QualitySchemeProperties {

    /** 标准规范 PDF 文档目录路径 */
    private String standardDir;

    /** Milvus 向量数据库连接配置 */
    private Milvus milvus = new Milvus();

    /** 文档切块参数 */
    private Chunk chunk = new Chunk();

    /** 检索默认参数 */
    private Retrieve retrieve = new Retrieve();

    /** 方案生成参数 */
    private Scheme scheme = new Scheme();

    // ===== getter / setter =====

    public String getStandardDir() {
        return standardDir;
    }

    public void setStandardDir(String standardDir) {
        this.standardDir = standardDir;
    }

    public Milvus getMilvus() {
        return milvus;
    }

    public void setMilvus(Milvus milvus) {
        this.milvus = milvus;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public void setChunk(Chunk chunk) {
        this.chunk = chunk;
    }

    public Retrieve getRetrieve() {
        return retrieve;
    }

    public void setRetrieve(Retrieve retrieve) {
        this.retrieve = retrieve;
    }

    public Scheme getScheme() {
        return scheme;
    }

    public void setScheme(Scheme scheme) {
        this.scheme = scheme;
    }

    /**
     * Milvus 向量数据库连接配置。
     */
    public static class Milvus {
        /** Milvus 连接地址，例如 http://milvus-dev1.e-tudou.com:19530 */
        private String uri = "http://localhost:19530";
        /** 数据库名称 */
        private String databaseName = "default";
        /** Collection 名称 */
        private String collectionName = "qualityScheme_springAI";
        /** 嵌入向量维度 */
        private int embeddingDimension = 1024;
        /** 索引类型 */
        private String indexType = "IVF_FLAT";
        /** 相似度度量类型 */
        private String metricType = "COSINE";

        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = uri; }
        public String getDatabaseName() { return databaseName; }
        public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
        public String getCollectionName() { return collectionName; }
        public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
        public int getEmbeddingDimension() { return embeddingDimension; }
        public void setEmbeddingDimension(int embeddingDimension) { this.embeddingDimension = embeddingDimension; }
        public String getIndexType() { return indexType; }
        public void setIndexType(String indexType) { this.indexType = indexType; }
        public String getMetricType() { return metricType; }
        public void setMetricType(String metricType) { this.metricType = metricType; }
    }

    /**
     * 文档切块参数。
     */
    public static class Chunk {
        /** 每块最大 token 数 */
        private int chunkSize = 256;
        /** 相邻块重叠 token 数 */
        private int chunkOverlap = 40;

        public int getChunkSize() { return chunkSize; }
        public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
        public int getChunkOverlap() { return chunkOverlap; }
        public void setChunkOverlap(int chunkOverlap) { this.chunkOverlap = chunkOverlap; }
    }

    /**
     * 检索默认参数。
     */
    public static class Retrieve {
        /** 默认返回的 Top-K 节点数 */
        private int defaultTopK = 3;

        public int getDefaultTopK() { return defaultTopK; }
        public void setDefaultTopK(int defaultTopK) { this.defaultTopK = defaultTopK; }
    }

    /**
     * 方案生成参数。
     */
    public static class Scheme {
        /** 方案生成时检索规范上下文的条款数 */
        private int contextTopK = 5;

        public int getContextTopK() { return contextTopK; }
        public void setContextTopK(int contextTopK) { this.contextTopK = contextTopK; }
    }
}
