package com.atguigu.gulimall.order.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class MyMQConfig {
 
 
    /**
     * Spring中注入Bean之后，容器中的Binding、Queue、Exchange 都会自动创建（前提是RabbitMQ中没有）
     * RabbitMQ 只要有，@Bean属性发生变化也不会覆盖
     * @return
     * Queue(String name, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments)
     */
    @Bean
    public Queue orderDelayQueue(){
        HashMap<String, Object> arguments = new HashMap<>();
        /**
         * x-dead-letter-exchange ：order-event-exchange 设置死信路由
         * x-dead-letter-routing-key : order.release.order 设置死信路由键
         * x-message-ttl ：60000
         */
        arguments.put("x-dead-letter-exchange","order-event-exchange");
        arguments.put("x-dead-letter-routing-key","order.release.order");
        arguments.put("x-message-ttl",30000);
 
        Queue queue = new Queue("order.delay.queue", true, false, false,arguments);
        return queue;
    }
 
    @Bean
    public Queue orderReleaseOrderQueue(){
        return new Queue("order.release.order.queue", true, false, false);
    }
 
    @Bean
    public Exchange orderEventExchange(){
        // TopicExchange(String name, boolean durable, boolean autoDelete, Map<String, Object> arguments)
        return new TopicExchange("order-event-exchange",true,false);
    }
 
    @Bean
    public Binding orderCreateOrder(){
        // Binding(String destination, Binding.DestinationType destinationType, String exchange, String routingKey, Map<String, Object> arguments)
        return new Binding("order.delay.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.create.order",
                null);
    }
 
    @Bean
    public Binding orderReleaseOrder(){
        // Binding(String destination, Binding.DestinationType destinationType, String exchange, String routingKey, Map<String, Object> arguments)
        return new Binding("order.release.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.order",
                null);
    }
 
    /**
     * 订单释放直接和库存释放进行绑定
     * @return
     */
    @Bean
    public Binding orderReleaseOtherBingding(){
        // Binding(String destination, Binding.DestinationType destinationType, String exchange, String routingKey, Map<String, Object> arguments)
        return new Binding("stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.release.other.#",
                null);
    }
    /**
     * 商品秒杀队列
     * 作用：削峰，创建订单
     */
    @Bean
    public Queue orderSecKillOrderQueue() {
        Queue queue = new Queue("order.seckill.order.queue", true, false, false);
        return queue;
    }

    @Bean
    public Binding orderSecKillOrderQueueBinding() {
        //String destination, DestinationType destinationType, String exchange, String routingKey,
        // 			Map<String, Object> arguments
        Binding binding = new Binding(
                "order.seckill.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.seckill.order",
                null);

        return binding;
    }


}