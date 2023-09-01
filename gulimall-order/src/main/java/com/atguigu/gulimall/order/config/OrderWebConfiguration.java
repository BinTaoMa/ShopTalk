package com.atguigu.gulimall.order.config;


import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Data time:2022/4/11 22:21
 * StudentID:2019112118
 * Author:hgw
 * Description:
 */
@Configuration
public class OrderWebConfiguration implements WebMvcConfigurer {
 
    @Autowired
    LoginUserInterceptor interceptor;
 
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/**");
    }
}