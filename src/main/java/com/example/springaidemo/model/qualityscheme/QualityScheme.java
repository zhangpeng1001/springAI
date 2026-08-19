package com.example.springaidemo.model.qualityscheme;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 质检方案结构化输出实体类。
 * <p>
 * 对应 LlamaIndex qualityScheme/scheme_generator.py 的 QualityScheme Pydantic 模型。
 * 由 LLM 通过结构化输出（ChatClient.entity()）生成，包含方案名称、描述与检查项列表。
 * <p>
 * Spring AI 2.0 的结构化输出会根据该类的字段名和 @JsonProperty 注解生成 JSON Schema，
 * 引导 LLM 输出符合预期的结构化结果。
 *
 * @author spring-ai-demo
 */
public class QualityScheme {

    /** 方案名称，简洁体现数据对象与检查重点 */
    @JsonProperty("schemeName")
    private String schemeName;

    /** 方案描述，1-2 句说明检查目标与范围 */
    @JsonProperty("description")
    private String description;

    /** 检查项列表，每个 checkCode 必须来自预定义清单 */
    @JsonProperty("checkItem")
    private List<CheckItem> checkItem;

    public QualityScheme() {}

    // ===== getter / setter =====

    public String getSchemeName() { return schemeName; }
    public void setSchemeName(String schemeName) { this.schemeName = schemeName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<CheckItem> getCheckItem() { return checkItem; }
    public void setCheckItem(List<CheckItem> checkItem) { this.checkItem = checkItem; }
}
