package com.example.springaidemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 全局 CORS 跨域配置
 * <p>
 * 允许前端测试页面（无论以 file:// 方式直接打开，还是运行在任意端口）
 * 调用本后端服务的所有 REST API。
 *
 * @author spring-ai-demo
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许所有来源（开发/演示环境使用，生产环境请限制具体域名）
        config.addAllowedOriginPattern("*");
        // 允许携带认证信息（Cookie/Authorization 等）
        config.setAllowCredentials(true);
        // 允许所有 HTTP 方法
        config.addAllowedMethod("*");
        // 允许所有请求头
        config.addAllowedHeader("*");
        // 暴露响应头（前端需要读取这些头时）
        config.addExposedHeader("*");
        // 预检请求缓存时间（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径生效
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
