package com.example.springaidemo.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 天气查询工具的输入参数对象
 * <p>
 * 用于替代原先直接以 {@code String} 作为 {@code FunctionToolCallback} 入参类型的写法。
 * <p>
 * 背景：当工具的 {@code inputType} 为 {@code String.class} 时，Spring AI 生成的参数
 * JSON Schema 形如 {@code {"type":"string"}}。但主流 LLM 在发起工具调用时，参数
 * 一律按 JSON 对象返回（如 {@code {"city":"北京"}}，token 为 {@code START_OBJECT}），
 * Jackson 无法将一个对象反序列化为 {@code String}，从而抛出
 * {@code MismatchedInputException: Cannot deserialize value of type
 * `java.lang.String` from Object value}。
 * <p>
 * 引入本 record 后，参数 Schema 变为合法的对象结构
 * {@code {"type":"object","properties":{"city":{"type":"string"}},"required":["city"]}}，
 * 模型返回的对象即可被正确绑定到 {@link #city()} 字段。
 *
 * @author spring-ai-demo
 */
public record WeatherRequest(
        @JsonProperty("city") String city
) {
}
