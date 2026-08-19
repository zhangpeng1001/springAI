package com.example.springaidemo.model.qualityscheme;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 质检检查项实体类。
 * <p>
 * 对应 LlamaIndex qualityScheme/check_items.py 中的预定义检查项。
 * 共 28 项，涵盖字段检查、几何检查、坐标系检查、图层一致性等质检能力域。
 * <p>
 * 字段含义：
 * <ul>
 *   <li>checkCode：检查项唯一编码，方案中引用它</li>
 *   <li>checkName：检查项中文名称，便于人读</li>
 *   <li>checkDesc：检查项说明</li>
 *   <li>checkObjType：检查对象类型（当前均为 VECTOR 矢量数据）</li>
 *   <li>checkParam：该检查项需要的参数名列表（JSON 字符串形式）</li>
 *   <li>paramNames：解析后的参数名列表</li>
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

    /** 检查对象类型（VECTOR） */
    @JsonProperty("checkObjType")
    private String checkObjType;

    /** 参数名 JSON 字符串，例如 ["data_name","fieldNames"] */
    @JsonProperty("checkParam")
    private String checkParam;

    /** 检查请求 URL（仅记录，本系统不调用） */
    @JsonProperty("checkRequestUrl")
    private String checkRequestUrl;

    /** 解析后的参数名列表 */
    @JsonProperty("paramNames")
    private List<String> paramNames;

    // 用于方案生成时 LLM 输出的参数键值对（仅在 scheme 生成时使用）
    /** 检查项参数，键名匹配 paramNames，值为具体取值 */
    @JsonProperty("params")
    private java.util.Map<String, Object> params;

    public CheckItem() {}

    public CheckItem(String checkCode, String checkName, String checkDesc,
                    String checkObjType, String checkParam, String checkRequestUrl) {
        this.checkCode = checkCode;
        this.checkName = checkName;
        this.checkDesc = checkDesc;
        this.checkObjType = checkObjType;
        this.checkParam = checkParam;
        this.checkRequestUrl = checkRequestUrl;
    }

    // ===== getter / setter =====

    public String getCheckCode() { return checkCode; }
    public void setCheckCode(String checkCode) { this.checkCode = checkCode; }
    public String getCheckName() { return checkName; }
    public void setCheckName(String checkName) { this.checkName = checkName; }
    public String getCheckDesc() { return checkDesc; }
    public void setCheckDesc(String checkDesc) { this.checkDesc = checkDesc; }
    public String getCheckObjType() { return checkObjType; }
    public void setCheckObjType(String checkObjType) { this.checkObjType = checkObjType; }
    public String getCheckParam() { return checkParam; }
    public void setCheckParam(String checkParam) { this.checkParam = checkParam; }
    public String getCheckRequestUrl() { return checkRequestUrl; }
    public void setCheckRequestUrl(String checkRequestUrl) { this.checkRequestUrl = checkRequestUrl; }
    public List<String> getParamNames() { return paramNames; }
    public void setParamNames(List<String> paramNames) { this.paramNames = paramNames; }
    public java.util.Map<String, Object> getParams() { return params; }
    public void setParams(java.util.Map<String, Object> params) { this.params = params; }
}
