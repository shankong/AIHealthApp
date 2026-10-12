package com.shankong.backend.util;

import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.context.ApplicationContext;
import com.shankong.backend.config.JwtConfig;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.util.Date;

@Component 
public class JWTTokenUtil implements ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override 
    public void setApplicationContext(ApplicationContext applicationContext) {
        // 初始化上下文
        JWTTokenUtil.applicationContext = applicationContext;
    }

    private static JwtConfig getJwtConfig() {
        return applicationContext.getBean(JwtConfig.class);
    }
    
    // 生成token
    // userId 的类型跟着主键走：user.id 现在是 32 位 UUID 字符串，不再是 Long。
    // JWT 的 claim 本来就允许字符串，所以这里不用做任何转换。
    public static String generateToken(String userId, String username, Integer userType) {
        try{
            //  获取jwt配置
        JwtConfig jwtConfig = getJwtConfig();
        // 生成签名算法
        Algorithm algorithm = Algorithm.HMAC256(jwtConfig.getSecret());
        // 生成过期时间
        Date expiration = new Date(System.currentTimeMillis() + jwtConfig.getExpiration());

        String token = JWT.create()
            .withIssuer(jwtConfig.getIssuer()) // 签发者
            .withClaim("userId", userId)
            .withClaim("username", username)
            .withClaim("userType", userType)
            .withExpiresAt(expiration) // 过期时间
            .withIssuedAt(new Date()) // 签发时间
            .sign(algorithm); // 签名
        return token;
        }catch (Exception e) {
            throw new RuntimeException("生成token失败: " + e.getMessage(), e);
        }

    }
}
