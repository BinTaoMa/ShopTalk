package com.atguigu.gulimall.product.web;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.service.SkuInfoService;
import com.atguigu.gulimall.product.vo.SkuItemVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutionException;

@RestController
public class ItemController {
 
    /**
     * 展示当前sku的详情
     * @param skuId
     * @return
     */
    @Autowired
    SkuInfoService skuInfoService;

    @GetMapping("/html/{skuId}")
    public R skuItem(@PathVariable("skuId") Long skuId) throws ExecutionException, InterruptedException {

        System.out.println("准备查询" + skuId + "详情");

        SkuItemVo vos = skuInfoService.item(skuId);

//        model.addAttribute("item",vos);


        return R.ok().put("data",vos);


    }
}