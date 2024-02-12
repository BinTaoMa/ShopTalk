package com.atguigu.gulimall.order.listener;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@RabbitListener(queues = "order.release.order.queue")
@Service
public class OrderClassListener {
 
    @Autowired
    OrderService orderService;
 
    @RabbitHandler
    public void listener(OrderEntity entity, Channel channel, Message message) throws IOException {
        System.out.println("收到过期的订单信息：准备关闭订单！" + entity.getOrderSn());
        try {
            orderService.closeOrder(entity);
//肯定，让broker将移除此消息，以后不用重试。
//参数deliveryTag号是消息的唯一标识，参数false代表不批量确认此deliveryTag编号之前的所有消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e){
//拒绝；参数deliveryTag号是消息的唯一标识；参数true代表重新回队列，如果false则代表丢弃
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}