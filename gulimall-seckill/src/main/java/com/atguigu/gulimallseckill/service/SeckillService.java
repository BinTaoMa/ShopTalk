package com.atguigu.gulimallseckill.service;

import com.atguigu.gulimallseckill.to.SeckillSkuRedisTo;

import java.util.List;

/**
 * @author zr
 * @date 2022/1/6 18:14
 */
public interface SeckillService {
    /**
     * 上架三天需要秒杀的商品
     */
    void uploadSeckillSkuLatest3Days();

    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

    String kill(String killId, String key, Integer num) throws InterruptedException;
}

