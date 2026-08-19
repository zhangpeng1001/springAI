package com.example.springaidemo.model.qualityscheme;

import java.util.List;

/**
 * 预定义检查项定义（含完整参数规格）。
 * <p>
 * 与 {@link CheckItem} 的区别：
 * <ul>
 *   <li>CheckItemDefinition：预定义检查项的静态定义，包含参数规格（ParamSpec），
 *       用于生成 LLM Prompt 和校验 LLM 输出</li>
 *   <li>CheckItem：LLM 生成的检查项实例，包含 dataName 和 params（具体取值）</li>
 * </ul>
 * <p>
 * 设计要点：{@code dataName}（图层名称）不在 params 中，因为每个检查项都有图层名称，
 * 它是检查项的固有属性，与 checkCode 同级。LLM 输出时 dataName 也是 CheckItem 的顶层字段。
 *
 * @param checkCode 检查项编码
 * @param checkName 中文名称
 * @param checkDesc 检查项说明
 * @param params    规则特有参数列表（不含 dataName）
 *
 * @author spring-ai-demo
 */
public record CheckItemDefinition(
        String checkCode,
        String checkName,
        String checkDesc,
        List<ParamSpec> params
) {
    /**
     * 获取参数名列表（用于校验 LLM 输出，不含 dataName）。
     *
     * @return 参数名列表
     */
    public List<String> getParamNames() {
        return params.stream().map(ParamSpec::name).toList();
    }
}
