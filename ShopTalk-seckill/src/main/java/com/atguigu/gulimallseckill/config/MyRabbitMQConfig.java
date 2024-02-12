package com.atguigu.gulimallseckill.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zr
 * @date 2022/1/10 15:27
 */
@Configuration
public class MyRabbitMQConfig {
    /**
     * 以json序列化的格式发送消息
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

