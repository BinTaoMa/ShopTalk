package com.atguigu.gulimallseckill.to;

import com.atguigu.gulimallseckill.vo.SkuInfoVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SeckillSkuRedisTo {
    private Long id;
    /**
     * 活动id
     */
    private Long promotionId;
    /**
     * 活动场次id
     */
    private Long promotionSessionId;
    /**
     * 商品id
     */
    private Long skuId;
    /**
     * 秒杀价格
     */
    private BigDecimal seckillPrice;
    /**
     * 秒杀总量
     */
    private BigDecimal seckillCount;
    /**
     * 每人限购数量
     */
    private BigDecimal seckillLimit;
    /**
     * 排序
     */
    private Integer seckillSort;


    private SkuInfoVo skuInfo;

    private Long startTime;
    /**
     * 每日结束时间
     */
    private Long endTime;

    private String RandomCode;
}
