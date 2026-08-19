package com.example.springaidemo.model.qualityscheme;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 质检检查项实体类。
 * <p>
 * 既用于预定义检查项清单（由 {@link CheckItemDefinitions} 构建），
 * 也用于 LLM 生成的质检方案中的检查项实例。
 * 共 27 项，涵盖字段检查、几何检查、坐标系检查、图层一致性等质检能力域。
 * <p>
 * 字段含义：
 * <ul>
 *   <li>checkCode：检查项唯一编码，方案中引用它</li>
 *   <li>checkName：检查项中文名称，便于人读</li>
 *   <li>checkDesc：检查项说明</li>
 *   <li>dataName：图层名称（每个检查项都有，从 params 中提升到外层，与 checkCode 同级）</li>
 *   <li>checkParam：规则参数名列表（JSON 字符串形式，不含 dataName）</li>
 *   <li>paramNames：解析后的规则参数名列表（不含 dataName）</li>
 *   <li>params：检查项参数键值对（LLM 生成方案时填写，键名匹配 paramNames）</li>
 * </ul>
 *
 * @author spring-ai-demo
 */
public class CheckItem {

    /** 检查项唯一编码 */
    @JsonProperty("checkCode")
    private String checkCode;

    /** 检查项中文名称 */
    @JsonProperty("checkName")
    private String checkName;

    /** 检查项说明 */
    @JsonProperty("checkDesc")
    private String checkDesc;

    /** 图层名称（每个检查项都有，从 params 中提升到外层，与 checkCode 同级） */
    @JsonProperty("dataName")
    private String dataName;

    /** 规则参数名 JSON 字符串，例如 ["fieldNames","fieldLengths"]（不含 dataName） */
    @JsonProperty("checkParam")
    private String checkParam;

    /** 解析后的规则参数名列表（不含 dataName） */
    @JsonProperty("paramNames")
    private List<String> paramNames;

    /** 检查项参数，键名匹配 paramNames，值为具体取值（LLM 生成方案时填写） */
    @JsonProperty("params")
    private java.util.Map<String, Object> params;

    public CheckItem() {}

    public CheckItem(String checkCode, String checkName, String checkDesc) {
        this.checkCode = checkCode;
        this.checkName = checkName;
        this.checkDesc = checkDesc;
    }

    // ===== getter / setter =====

    public String getCheckCode() { return checkCode; }
    public void setCheckCode(String checkCode) { this.checkCode = checkCode; }
    public String getCheckName() { return checkName; }
    public void setCheckName(String checkName) { this.checkName = checkName; }
    public String getCheckDesc() { return checkDesc; }
    public void setCheckDesc(String checkDesc) { this.checkDesc = checkDesc; }
    public String getDataName() { return dataName; }
    public void setDataName(String dataName) { this.dataName = dataName; }
    public String getCheckParam() { return checkParam; }
    public void setCheckParam(String checkParam) { this.checkParam = checkParam; }
    public List<String> getParamNames() { return paramNames; }
    public void setParamNames(List<String> paramNames) { this.paramNames = paramNames; }
    public java.util.Map<String, Object> getParams() { return params; }
    public void setParams(java.util.Map<String, Object> params) { this.params = params; }
}
