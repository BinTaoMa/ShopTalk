package com.atguigu.gulimall.order.controller;

import com.atguigu.common.exception.NoStockException;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

@RestController
public class OrderWebController {
 
    @Autowired
    OrderService orderService;
 
//去结算确认页
    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = orderService.confirmOrder();
        model.addAttribute("OrderConfirmData",confirmVo);
        return "confirm";
    }
    /**
     * 下单功能
     * @param vo
     * @return
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model, RedirectAttributes redirectAttributes){
        // 1、创建订单、验令牌、验价格、验库存
        try {
            SubmitOrderResponseVo responseVo = orderService.submitOrder(vo);
            if (responseVo.getCode() == 0) {
                // 下单成功来到支付选择页
                model.addAttribute("submitOrderResp",responseVo);
                return "pay";
            } else {
                // 下单失败回到订单确认页重新确认订单信息
                String msg = "下单失败: ";
                switch ( responseVo.getCode()){
                    case 1: msg+="订单信息过期，请刷新再次提交";break;
                    case 2: msg+="订单商品价格发生变化，请确认后再次提交";break;
                    case 3: msg+="商品库存不足";break;
                }
                System.out.println(msg);
                redirectAttributes.addAttribute("msg",msg);
                return "下单失败";
            }
        } catch (Exception e){
            if (e instanceof NoStockException) {
                String message = e.getMessage();
                redirectAttributes.addFlashAttribute("msg", message);
            }
            return "报错" ;
        }

    }
}