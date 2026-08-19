package com.example.springaidemo.model.qualityscheme;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 意图识别结构化输出实体类。
 * <p>
 * 对应 LlamaIndex qualityScheme/scheme_intent.py 的 IntentResult Pydantic 模型。
 * 用于在方案生成前判断用户输入是否为真实的质检方案要求。
 * <p>
 * 判定规则：
 * <ul>
 *   <li>True：用户输入描述了可映射到预定义检查项能力域的质检需求</li>
 *   <li>False：闲聊、问候或与质检无关的问题</li>
 * </ul>
 *
 * @author spring-ai-demo
 */
public class IntentResult {

    /**
     * 用户输入是否为真实的质检方案要求。
     * True 表示质检需求；False 表示闲聊/无关问题。
     */
    @JsonProperty("isQualityRequirement")
    private boolean isQualityRequirement;

    /** 判定理由，简短说明依据，用于日志与调试 */
    @JsonProperty("reason")
    private String reason;

    /**
     * 当判定为 False 时，给用户的引导提示，包含质检需求示例；
     * 判定为 True 时可为空字符串。
     */
    @JsonProperty("suggestion")
    private String suggestion;

    public IntentResult() {}

    // ===== getter / setter =====

    public boolean isQualityRequirement() { return isQualityRequirement; }
    public void setQualityRequirement(boolean qualityRequirement) { isQualityRequirement = qualityRequirement; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
}
