package com.atguigu.gulimall.member.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 这是一个声明式的远程调用
 */
@Service
@FeignClient("ShopTalk-coupon")
public interface CouponFeignService {
    @RequestMapping("/coupon/coupon/member/list")
    public R mebercoupons();
}
