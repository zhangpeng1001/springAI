# 08 - 配置详解

## 概述

Spring AI 2.0 的配置相比 1.x 版本有一些变化。本指南详细说明各配置项的含义和用法。

## application.yml 完整配置

```yaml
server:
  port: 8080

spring:
  application:
    name: spring-ai-demo

  ai:
    # 通用配置
    providers:
      # OpenAI 配置
      openai:
        api-key: ${OPENAI_API_KEY:default-key}
        base-url: https://api.openai.com
        
        # 聊天模型配置
        chat:
          options:
            model: gpt-4o
            temperature: 0.7
            max-tokens: 2000
            top-p: 0.9
            stream-timeout: 10s
          # 是否启用
          enabled: true
        
        # Embedding 配置
        embedding:
          options:
            model: text-embedding-3-small
            dimensions: 1536
          enabled: true

# 日志配置
logging:
  level:
    org.springframework.ai: DEBUG
```

## 常用配置项

### 模型选择

```yaml
spring:
  ai:
    openai:
      chat:
        options:
          # 模型名称
          model: gpt-4o
          # 支持的模型示例：
          # - gpt-4o (多模态)
          # - gpt-4o-mini (快速)
          # - gpt-3.5-turbo (文本)
          # - o1 (推理)
```

### 采样参数

```yaml
spring:
  ai:
    openai:
      chat:
        options:
          # 温度：0-2，越高越随机
          temperature: 0.7
          # Top-P：核采样
          top-p: 0.9
          # 最大 Token 数
          max-tokens: 2000
          # 频率惩罚
          frequency-penalty: 0.0
          # 存在惩罚
          presence-penalty: 0.0
```

### 超时配置

```yaml
spring:
  ai:
    openai:
      chat:
        options:
          # 流式响应超时
          stream-timeout: 30s
      # HTTP 客户端超时
      client:
        connect-timeout: 10s
        read-timeout: 60s
```

### 代理配置

```yaml
spring:
  ai:
    openai:
      # 代理 URL
      proxy:
        host: proxy.example.com
        port: 8080
        username: admin
        password: password
```

## 环境变量配置

支持通过环境变量覆盖配置：

```bash
# Linux/Mac
export OPENAI_API_KEY=your-key
export SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL=gpt-4o

# Windows
set OPENAI_API_KEY=your-key
set SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL=gpt-4o
```

## 多模型配置

```yaml
spring:
  ai:
    # 同时配置多个提供商
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o
    
    # 阿里云百炼
    alibaba:
      api-key: ${DASHSCOPE_API_KEY}
      base-url: https://dashscope.aliyuncs.com
      chat:
        options:
          model: qwen-max
    
    # Anthropic
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      chat:
        options:
          model: claude-3-opus
```

## Java 配置类

### 自定义 ChatClient

```java
@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                // 可以添加全局 Advisor
                .defaultAdvisors()
                .build();
    }

    // 创建支持不同用途的 ChatClient
    @Bean("creativeClient")
    public ChatClient creativeClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .build();
    }

    @Bean("preciseClient")
    public ChatClient preciseClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .build();
    }
}
```

### 自定义 Embedding

```java
@Configuration
public class EmbeddingConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        // 可以选择不同的向量存储实现
        return SimpleVectorStore.builder(embeddingModel)
                .build();
    }
}
```

## 常见配置场景

### 1. 使用国内 API

```yaml
spring:
  ai:
    openai:
      base-url: https://api.deepseek.com
      api-key: ${DEEPSEEK_API_KEY}
      chat:
        options:
          model: deepseek-chat
```

### 2. 开发环境配置

```yaml
spring:
  ai:
    openai:
      chat:
        options:
          model: gpt-4o-mini  # 使用更便宜的模型
          temperature: 0.8    # 更有创造性
          max-tokens: 1000    # 限制输出长度
```

### 3. 生产环境配置

```yaml
spring:
  ai:
    openai:
      chat:
        options:
          model: gpt-4o
          temperature: 0.3   # 更确定性
          max-tokens: 4000
          stream-timeout: 60s
      client:
        connect-timeout: 5s
        read-timeout: 120s
```

## 配置优先级

1. 命令行参数（`--spring.ai.openai.api-key=xxx`）
2. 环境变量（`SPRING_AI_OPENAI_API_KEY`）
3. application.yml
4. application.properties

## 注意事项

- API Key 不要硬编码在代码中
- 使用环境变量或配置中心管理敏感信息
- 不同环境使用不同的配置文件
- 生产环境要配置合理的超时和重试策略