package com.example.springaidemo.model;

import java.util.List;

/**
 * 演员参演电影 - 结构化输出示例模型
 * <p>
 * 该类用于演示 Spring AI 的结构化输出功能：
 * AI 返回的文本会被自动解析并填充到这个 Java 对象中。
 * <p>
 * 工作原理：
 * 1. Spring AI 根据 Java 类的结构生成 JSON Schema
 * 2. 将 Schema 作为约束发送给大模型（通过 response_format 参数）
 * 3. 大模型返回符合 Schema 的 JSON
 * 4. Spring AI 自动反序列化为 Java 对象
 * <p>
 * 注意：字段名会自动映射为 JSON 的 key，建议使用清晰的命名。
 *
 * @author spring-ai-demo
 */
public class ActorFilms {

    /** 演员姓名 */
    private String actor;

    /** 参演的电影列表 */
    private List<String> movies;

    /** 生平简介 */
    private String biography;

    // 默认构造器（反序列化需要）
    public ActorFilms() {
    }

    public ActorFilms(String actor, List<String> movies, String biography) {
        this.actor = actor;
        this.movies = movies;
        this.biography = biography;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public List<String> getMovies() {
        return movies;
    }

    public void setMovies(List<String> movies) {
        this.movies = movies;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    @Override
    public String toString() {
        return "ActorFilms{" +
                "actor='" + actor + '\'' +
                ", movies=" + movies +
                ", biography='" + biography + '\'' +
                '}';
    }
}
