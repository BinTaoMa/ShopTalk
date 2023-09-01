package com.atguigu.gulimallseckill.control;

import com.atguigu.common.utils.R;
import com.atguigu.gulimallseckill.service.SeckillService;
import com.atguigu.gulimallseckill.to.SeckillSkuRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;

/**
 * @author zr
 * @date 2022/1/8 18:07
 */
@RestController
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    /**
     * 当前时间可以参与秒杀的商品信息
     */
    @GetMapping(value = "/getCurrentSeckillSkus")
    @ResponseBody
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {

        //获取到当前可以参加秒杀商品的信息
        List<SeckillSkuRedisTo> vos = seckillService.getCurrentSeckillSkus();
        System.out.println(vos);

        return vos;
    }

    /**
     * 商品进行秒杀(秒杀开始)
     * 查看表 oms_order_item
     */
    @GetMapping(value = "/kill")
    public String seckill(@RequestParam("killId") String killId,
                          @RequestParam("key") String key,
                          @RequestParam("num") Integer num,
                          Model model) {

        String orderSn = null;
        try {
            //1、判断是否登录
            orderSn = seckillService.kill(killId,key,num);
            model.addAttribute("orderSn",orderSn);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "success";
    }


}

