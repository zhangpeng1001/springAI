package com.example.springaidemo;

import com.example.springaidemo.config.QualitySchemeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Spring AI 2.0.0 Demo 项目启动类
 * <p>
 * 本项目基于 Spring AI 2.0.0 GA，演示 Spring AI 核心功能：
 * <ul>
 *     <li>基础聊天（ChatClient）</li>
 *     <li>提示词模板（PromptTemplate）</li>
 *     <li>结构化输出（Structured Output）</li>
 *     <li>多模态（图片理解）</li>
 *     <li>函数调用（Function Calling）</li>
 *     <li>会话记忆（ChatMemory）</li>
 *     <li>RAG 检索增强生成</li>
 *     <li>质检规范知识库问答（qualityScheme，Milvus 向量存储）</li>
 * </ul>
 * <p>
 * 启动前请先配置 API Key（见 application.yml 或设置环境变量 OPENAI_API_KEY）。
 *
 * @author spring-ai-demo
 */
@SpringBootApplication
@EnableConfigurationProperties(QualitySchemeProperties.class)
public class SpringAiDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiDemoApplication.class, args);
    }
}
