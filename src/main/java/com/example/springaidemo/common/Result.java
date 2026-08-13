package com.example.springaidemo.common;

/**
 * 统一返回结果封装类
 * <p>
 * 用于规范化所有 REST 接口的返回格式，方便前端统一处理。
 * <pre>
 * 成功返回示例：
 * { "success": true, "message": "成功", "data": "..." }
 *
 * 失败返回示例：
 * { "success": false, "message": "API 调用失败", "data": null }
 * </pre>
 *
 * @param <T> data 字段的数据类型
 * @author spring-ai-demo
 */
public class Result<T> {

    /** 是否成功 */
    private boolean success;

    /** 提示信息 */
    private String message;

    /** 返回数据 */
    private T data;

    public Result() {
    }

    public Result(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    /** 快速构建成功结果 */
    public static <T> Result<T> success(T data) {
        return new Result<>(true, "成功", data);
    }

    /** 快速构建成功结果（自定义消息） */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(true, message, data);
    }

    /** 快速构建失败结果 */
    public static <T> Result<T> error(String message) {
        return new Result<>(false, message, null);
    }

    // getter / setter
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
