# 04 - 多模态

## 概述

Spring AI 2.0 支持多模态（Multimodal）功能，允许在对话中处理图片等非文本内容。通过 `Media` 类可以将图片、音频等媒体文件发送给支持多模态的 AI 模型。

## 核心概念

### Media 类

`Media` 是 Spring AI 中表示媒体内容的类，支持：
- 图片（JPEG, PNG 等）
- 音频
- 视频

### 构建 Media 对象

```java
// 从 URL 加载图片
Media image = Media.builder()
        .mimeType(MimeType.valueOf("image/jpeg"))
        .data(URI.create("https://example.com/image.jpg"))
        .build();

// 从字节数组加载
byte[] imageBytes = Files.readAllBytes(Paths.get("image.jpg"));
Media localImage = Media.builder()
        .mimeType(MimeType.valueOf("image/jpeg"))
        .data(imageBytes)
        .build();
```

## API 接口

### 1. 图片分析

让 AI 分析图片内容并回答问题。

**请求：**
```
GET /api/multimodal/analyze?imageUrl=https://example.com/cat.jpg&question=这是什么动物
```

**代码示例：**
```java
@GetMapping("/analyze")
public String analyzeImage(
        @RequestParam String imageUrl,
        @RequestParam(defaultValue = "请描述这张图片的内容") String question) {

    Media imageMedia = Media.builder()
            .mimeType(MimeType.valueOf("image/jpeg"))
            .data(URI.create(imageUrl))
            .build();

    return chatClient.prompt()
            .user(u -> u.text(question).media(imageMedia))
            .call()
            .content();
}
```

### 2. 图片描述

自动生成图片描述。

**请求：**
```
GET /api/multimodal/describe?imageUrl=https://example.com/scene.jpg
```

**代码示例：**
```java
@GetMapping("/describe")
public String describeImage(@RequestParam String imageUrl) {
    Media imageMedia = Media.builder()
            .mimeType(MimeType.valueOf("image/jpeg"))
            .data(URI.create(imageUrl))
            .build();

    return chatClient.prompt()
            .user(u -> u.text("请详细描述这张图片的内容，包括：1.图片中有什么 2.场景是什么 3.可能的用途").media(imageMedia))
            .call()
            .content();
}
```

### 3. 图片对比

对比分析两张图片的异同。

**请求：**
```
GET /api/multimodal/compare?imageUrl1=url1&imageUrl2=url2
```

**代码示例：**
```java
@GetMapping("/compare")
public String compareImages(
        @RequestParam String imageUrl1,
        @RequestParam String imageUrl2,
        @RequestParam(defaultValue = "请对比这两张图片的异同") String question) {

    Media image1 = Media.builder()
            .mimeType(MimeType.valueOf("image/jpeg"))
            .data(URI.create(imageUrl1))
            .build();
    Media image2 = Media.builder()
            .mimeType(MimeType.valueOf("image/jpeg"))
            .data(URI.create(imageUrl2))
            .build();

    return chatClient.prompt()
            .user(u -> u.text(question)
                    .media(image1)
                    .media(image2))
            .call()
            .content();
}
```

### 4. OCR 识别

识别图片中的文字内容。

**请求：**
```
GET /api/multimodal/ocr?imageUrl=https://example.com/document.jpg
```

**代码示例：**
```java
@GetMapping("/ocr")
public String ocrRecognize(@RequestParam String imageUrl) {
    Media imageMedia = Media.builder()
            .mimeType(MimeType.valueOf("image/jpeg"))
            .data(URI.create(imageUrl))
            .build();

    return chatClient.prompt()
            .user(u -> u.text("请识别图片中的所有文字内容，并以文本形式返回。如果是表格，请保留表格结构。").media(imageMedia))
            .call()
            .content();
}
```

## 支持的 MIME 类型

```java
public class MultimodalController {
    @GetMapping("/supported-types")
    public List<String> getSupportedTypes() {
        return List.of(
                "image/jpeg",
                "image/png",
                "image/gif",
                "image/webp"
        );
    }
}
```

## 最佳实践

1. **图片大小**：控制图片大小在模型支持的范围内
2. **URL 可访问**：确保图片 URL 可以被 AI 服务访问
3. **描述性提问**：提出具体的问题以获得更好的结果
4. **批量处理**：需要时可以传入多张图片
5. **错误处理**：处理加载图片失败的情况

## 注意事项

- 多模态功能需要使用支持视觉的 AI 模型（如 GPT-4o）
- 不同模型支持的图片格式可能不同
- 图片 URL 必须是公开可访问的
- 大图片可能需要压缩处理