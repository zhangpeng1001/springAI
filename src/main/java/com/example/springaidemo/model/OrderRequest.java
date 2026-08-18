package com.example.springaidemo.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 创建订单工具的输入参数对象
 * <p>
 * 用于替代原先以 {@code Map<String, Object>} 作为 {@code FunctionToolCallback} 入参类型的写法。
 * <p>
 * 背景：当工具的 {@code inputType} 为 {@code Map.class} 时，Spring AI 生成的参数
 * JSON Schema 形如 {@code {"type":"object"}}，缺少 {@code properties} 与
 * {@code required} 字段约束。LLM 因此无法得知：
 * <ul>
 *     <li>应该传哪些字段（参数名是什么）</li>
 *     <li>每个字段的类型是什么（数字？字符串？）</li>
 *     <li>哪些字段是必填的</li>
 * </ul>
 * 这会导致 LLM 用任意名字传参（如 {@code count}、{@code num}、{@code 数量}），
 * 或干脆省略部分参数，最终 {@code params.get("quantity")} 返回 {@code null}，
 * 调用 {@code .intValue()} 时抛出 {@code NullPointerException}：
 * <pre>
 *   org.springframework.ai.tool.execution.ToolExecutionException:
 *     Cannot invoke "java.lang.Number.intValue()" because the return value
 *     of "java.util.Map.get(Object)" is null
 * </pre>
 * <p>
 * 引入本 record 后，参数 Schema 变为合法的对象结构：
 * {@code {"type":"object",
 *         "properties":{
 *           "product":{"type":"string"},
 *           "quantity":{"type":"integer"}
 *         },
 *         "required":["product","quantity"]}}
 * 模型即可稳定地按 {@code product} / {@code quantity} 字段名返回对象，
 * 并自动转换为强类型字段，杜绝 NPE。
 *
 * @author spring-ai-demo
 */
public record OrderRequest(
        @JsonProperty("product") String product,
        @JsonProperty("quantity") Integer quantity
) {
}
