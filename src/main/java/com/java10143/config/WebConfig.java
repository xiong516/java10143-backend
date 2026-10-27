package com.java10143.config;

import com.java10143.interceptor.AdminInterceptor;
import com.java10143.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域 + 拦截器注册
 * 跨域是为了：HBuilderX 里跑的 H5、微信小程序、以及单独打开的网页前端都能直接调这个后端
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AdminInterceptor adminInterceptor;
    private final com.java10143.interceptor.OptionalAuthInterceptor optionalAuthInterceptor;
    private final com.java10143.interceptor.RateLimitInterceptor rateLimitInterceptor;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.addAllowedOriginPattern("*");
        cfg.addAllowedHeader("*");
        cfg.addAllowedMethod("*");
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return new CorsFilter(source);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 限流放最前面：被刷时连登录查询都不用做，直接 429 挡住（order 越小越先执行）
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/img/**", "/media/**")
                .order(-1);
        // 可选登录：/home/recommend 有 token 就解析用户（个性化推荐），没有就当访客放行
        registry.addInterceptor(optionalAuthInterceptor)
                .addPathPatterns("/home/recommend")
                .order(0);
        // 先校验登录（/member/**、/pay/**、/admin/** 都要带 token）
        // 注意：/member/ai/chat 也必须登录（2026-10-17 用户要求），只有 /member/ai/status 保持公开
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/member/**", "/pay/**", "/admin/**")
                .excludePathPatterns("/pay/mock", "/pay/result", "/member/ai/status")
                .order(1);
        // 再校验管理员身份（只护 /admin/**）
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**")
                .order(2);
    }
}
