package com.example.springaidemo.model.qualityscheme;

/**
 * 来源追踪 DTO。
 * <p>
 * 对应 LlamaIndex qualityScheme/source_tracker.py 的 sources_to_dict 输出结构。
 * 用于向前端返回 RAG 问答或向量检索的溯源信息，包含文件名、相似度分数、内容预览等。
 *
 * @author spring-ai-demo
 */
public class SourceDto {

    /** 排名序号（从 1 开始） */
    private int position;

    /** 来源文件名 */
    private String fileName;

    /** 来源文件完整路径（若有） */
    private String filePath;

    /** 相似度分数（保留 4 位小数），可能为 null */
    private Double score;

    /** 内容预览（前 200 字符） */
    private String preview;

    /** 文档/节点 ID，便于前端定位 */
    private String documentId;

    public SourceDto() {}

    public SourceDto(int position, String fileName, String filePath, Double score, String preview, String documentId) {
        this.position = position;
        this.fileName = fileName;
        this.filePath = filePath;
        this.score = score;
        this.preview = preview;
        this.documentId = documentId;
    }

    // ===== getter / setter =====

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
}
