package com.atguigu.gulimallcart.controller;

import com.atguigu.gulimallcart.interceptor.CartInterceptor;
import com.atguigu.gulimallcart.service.CartService;
import com.atguigu.gulimallcart.vo.CartItemVo;
import com.atguigu.gulimallcart.vo.CartVo;
import com.atguigu.gulimallcart.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
public class CartController {

    @Autowired
    CartService cartService;

    @GetMapping("/currentUserCartItems")
    public List<CartItemVo> getCurrentUserCartItems(){
        return cartService.getUserCartItems();
    }


    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId) {
        cartService.deleteItem(skuId);
        return "删除完了重定向";
    }

    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num) {
        cartService.countItem(skuId,num);
        return "修改完了重定向";
    }

    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("check") Integer check) {
        cartService.checkItem(skuId,check);
        return "勾选完了重定向";
    }
    /**
     * 去往用户购物车页面
     *  浏览器有一个cookie：user-key 用来标识用户身份，一个月后过期
     *  如果第一次使用京东的购物车功能，都会给一个临时用户身份；浏览器以后保存，每次访问都会带上这个cookie；
     * 登录：Session有
     * 没登录：按照cookie里面的user-key来做。
     *  第一次：如果没有临时用户，帮忙创建一个临时用户。
     * @return
     */
    @GetMapping(value = "/cart")
    public String cartListPage(Model model) throws ExecutionException, InterruptedException {
        //快速得到用户信息：id,user-key
         UserInfoTo userInfoTo = CartInterceptor.toThreadLocal.get();
 
        CartVo cartVo = cartService.getCart();
        model.addAttribute("cart",cartVo);
        return "cartList";
    }



    /**
     * 添加商品到购物车
     * @param skuId 商品的skuid
     * @param num   添加的商品数量
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num,
                            RedirectAttributes ra) throws ExecutionException, InterruptedException {

        CartItemVo cartItem = cartService.addToCart(skuId,num);
        ra.addAttribute("skuId", skuId);
        return "success";
    }




}