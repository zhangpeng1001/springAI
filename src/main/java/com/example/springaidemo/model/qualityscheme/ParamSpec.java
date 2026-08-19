package com.example.springaidemo.model.qualityscheme;

/**
 * 检查项参数规格定义：名称 + 说明 + 示例。
 * <p>
 * 用于在 {@link CheckItemDefinition} 中描述每个规则特有参数的含义，
 * 供 LLM 生成质检方案时参考，使大模型能清晰理解每个参数应填什么值。
 *
 * @param name    参数名（保持原始命名，如 "fieldLengths"、"min_length"）
 * @param desc    参数说明，描述该参数的含义与填写要求
 * @param example 示例值，给出一个具体的参考取值
 *
 * @author spring-ai-demo
 */
public record ParamSpec(
        String name,
        String desc,
        String example
) {}
