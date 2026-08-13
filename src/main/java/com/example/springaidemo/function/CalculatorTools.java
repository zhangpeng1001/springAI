package com.example.springaidemo.function;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 计算器工具 - 演示函数调用的另一个示例
 * <p>
 * 大模型在数学计算上可能不准确，通过函数调用可以让 AI 调用精确的计算器，
 * 保证计算结果的准确性。
 *
 * @author spring-ai-demo
 */
@Component
public class CalculatorTools {

    /**
     * 加法运算
     */
    @Tool(description = "执行两个数字的加法运算")
    public double add(
            @ToolParam(description = "第一个数字") double a,
            @ToolParam(description = "第二个数字") double b) {
        return a + b;
    }

    /**
     * 减法运算
     */
    @Tool(description = "执行两个数字的减法运算")
    public double subtract(
            @ToolParam(description = "被减数") double a,
            @ToolParam(description = "减数") double b) {
        return a - b;
    }

    /**
     * 乘法运算
     */
    @Tool(description = "执行两个数字的乘法运算")
    public double multiply(
            @ToolParam(description = "第一个数字") double a,
            @ToolParam(description = "第二个数字") double b) {
        return a * b;
    }

    /**
     * 除法运算
     */
    @Tool(description = "执行两个数字的除法运算")
    public double divide(
            @ToolParam(description = "被除数") double a,
            @ToolParam(description = "除数，不能为0") double b) {
        if (b == 0) {
            throw new IllegalArgumentException("除数不能为0");
        }
        return a / b;
    }
}
