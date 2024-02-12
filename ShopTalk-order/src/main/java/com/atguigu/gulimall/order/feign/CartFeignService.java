package com.atguigu.gulimall.order.feign;

import com.atguigu.gulimall.order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient("ShopTalk-cart")
public interface CartFeignService {
 
    @GetMapping("/currentUserCartItems")
    List<OrderItemVo> getCurrentUserCartItems();
}