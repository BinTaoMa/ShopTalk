package com.atguigu.gulimall.ware.listener;

import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.to.mq.StockLockedTo;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;


@Slf4j
@Service
@RabbitListener(queues = "stock.release.stock.queue")
public class StockReleaseListener {
 
    @Autowired
    WareSkuService wareSkuService;
 
    @RabbitHandler
//消息内容为库存锁定单传输对象，里面包括库存锁定单id和库存锁定详情单对象
    public void handleStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
 
        System.out.println("收到锁库存成功的消息，准备解锁库存");
        try {
            wareSkuService.unlockStock(to);
//消费者确认消息接收成功
            System.out.println("解锁");
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e){
            System.out.println("aaaa"+e);
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }

    @RabbitHandler
    public void handleOrderCloseRelease(OrderTo orderTo, Message message, Channel channel) throws IOException {

        System.out.println("订单关闭准备解锁库存");
        try {
            wareSkuService.unlockStock(orderTo);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }
    }


}