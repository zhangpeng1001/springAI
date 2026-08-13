package com.example.springaidemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring AI Demo 项目启动类
 * <p>
 * 本项目是一个 Spring AI 学习用 Demo，演示了 Spring AI 的核心功能：
 * <ul>
 *     <li>基础聊天（ChatClient）</li>
 *     <li>提示词模板（PromptTemplate）</li>
 *     <li>结构化输出（Structured Output）</li>
 *     <li>多模态（图片理解）</li>
 *     <li>函数调用（Function Calling）</li>
 *     <li>会话记忆（ChatMemory）</li>
 *     <li>RAG 检索增强生成（Retrieval Augmented Generation）</li>
 * </ul>
 * <p>
 * 启动前请先配置 API Key（见 application.yml 或设置环境变量 OPENAI_API_KEY）。
 *
 * @author spring-ai-demo
 */
@SpringBootApplication
public class SpringAiDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiDemoApplication.class, args);
    }
}
