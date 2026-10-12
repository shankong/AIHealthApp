package com.shankong.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration 
@EnableWebSecurity
@EnableMethodSecurity 
public class SecurityConfig {
    public static final String[] PUBLIC_PATHS = {
        "/",
        "/api/test",
        "/api/user/login"
    };
    // 配置安全相关的 Bean
    @Bean 
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 关闭 CSRF 保护 (API服务通常不需要)
            .csrf(AbstractHttpConfigurer::disable)
            // 关闭会话管理(JWT不需要)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            //配置请求授权规则
            .authorizeHttpRequests(auth -> auth
                // 公共路径放行
                .requestMatchers(PUBLIC_PATHS).permitAll()
                // 其他路径需要认证
                .anyRequest().authenticated()
            );
        return http.build();
    }
}