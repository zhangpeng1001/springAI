# 多模态功能

## 概述

多模态（Multimodal）功能允许 AI 同时处理文本和图片输入，实现"看图理解"、"OCR"、"图表分析"等能力。

## 前提条件

需要使用支持多模态的模型：
- GPT-4o / GPT-4o-mini
- Claude 3+
- Gemini Pro Vision
- 其他支持 vision 的模型

## API 列表

### 1. 图片分析

**GET** `/api/multimodal/analyze`

分析图片内容并回答相关问题。

**参数：**
- `imageUrl` (String, 必填) - 图片 URL
- `question` (String, 可选) - 关于图片的问题，默认"请描述这张图片的内容"

**示例：**
```bash
curl "http://localhost:8080/api/multimodal/analyze?imageUrl=https://example.com/cat.jpg"
```

---

### 2. 图片描述

**GET** `/api/multimodal/describe`

自动生成图片的详细描述。

**参数：**
- `imageUrl` (String, 必填) - 图片 URL

---

### 3. 图片对比

**GET** `/api/multimodal/compare`

对比两张图片的异同。

**参数：**
- `imageUrl1` (String, 必填) - 第一张图片 URL
- `imageUrl2` (String, 必填) - 第二张图片 URL
- `question` (String, 可选) - 对比问题

## 核心代码解析

```java
// Spring AI 2.0 中使用 Media.from() 创建图片媒体对象
Media imageMedia = Media.from(new URL(imageUrl));

// 通过 user() 的 lambda 方式同时传递文本和图片
String result = chatClient.prompt()
    .user(u -> u.text("描述这张图片").media(imageMedia))
    .call()
    .content();
```

### 多图片处理

```java
Media image1 = Media.from(new URL(url1));
Media image2 = Media.from(new URL(url2));

// 链式 media() 调用添加多张图片
String result = chatClient.prompt()
    .user(u -> u.text("对比这两张图").media(image1).media(image2))
    .call()
    .content();
```

## 支持的图片格式

- JPEG (image/jpeg)
- PNG (image/png)
- GIF (image/gif)
- WebP (image/webp)

## 常见应用场景

1. **图片描述**：自动生成图片说明文字
2. **OCR 识别**：从图片中提取文字
3. **图表分析**：分析数据图表
4. **产品识别**：识别图片中的产品
5. **安全审核**：内容安全审核
6. **医疗影像**：辅助医学影像分析

## Spring AI 2.0 要点

- 使用 `Media.from(URL)` 替代旧的 `Media.builder().url(URI)` 
- `user()` 方法支持 lambda 风格的参数传递
- 支持 `Resource` 和 `byte[]` 作为图片源
- 需要模型端支持 vision 能力
