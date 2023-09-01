package com.atguigu.gulimallcart.service.impl;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimallcart.feign.ProductFeignService;
import com.atguigu.gulimallcart.interceptor.CartInterceptor;
import com.atguigu.gulimallcart.service.CartService;
import com.atguigu.gulimallcart.vo.CartItemVo;
import com.atguigu.gulimallcart.vo.CartVo;
import com.atguigu.gulimallcart.vo.SkuInfoVo;
import com.atguigu.gulimallcart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CartServiceImpl implements CartService {
    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    private ThreadPoolExecutor executor;
    @Autowired
    ProductFeignService productFeignService;

    private final String CART_PREFIX = "gulimall:cart:";

    @Override
    public CartItemVo addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        //2.判断Redis是否有该商品的信息
        String productRedisValue = (String) cartOps.get(skuId.toString());

        if (StringUtils.isEmpty(productRedisValue)) {
//2.1如果没有就添加数据
            //添加新的商品到购物车(redis)
            CartItemVo cartItemVo = new CartItemVo();
            //开启第一个异步任务，存商品基本信息
            CompletableFuture<Void> getSkuInfoFuture = CompletableFuture.runAsync(() -> {
                //远程调用商品模块查询当前要添加商品的sku信息
                R productSkuInfo = productFeignService.info(skuId);
                SkuInfoVo skuInfo = productSkuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {});
                //sku信息数据赋值给单个CartItem
                cartItemVo.setSkuId(skuInfo.getSkuId());
                cartItemVo.setTitle(skuInfo.getSkuTitle());
                cartItemVo.setImage(skuInfo.getSkuDefaultImg());
                cartItemVo.setPrice(skuInfo.getPrice());
                cartItemVo.setCount(num);
            }, executor);


            //开启第二个异步任务，存商品属性信息
            CompletableFuture<Void> getSkuAttrValuesFuture = CompletableFuture.runAsync(() -> {
                //2、远程查询skuAttrValues组合信息
                List<String> skuSaleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
                cartItemVo.setSkuAttr(skuSaleAttrValues);
            }, executor);

            //等待所有的异步任务全部完成
            CompletableFuture.allOf(getSkuInfoFuture, getSkuAttrValuesFuture).get();

            String cartItemJson = JSON.toJSONString(cartItemVo);
            cartOps.put(skuId.toString(), cartItemJson);

            return cartItemVo;
        } else {
//2.2 购物车有此商品，修改数量即可
            CartItemVo cartItemVo = JSON.parseObject(productRedisValue, CartItemVo.class);
            cartItemVo.setCount(cartItemVo.getCount() + num);
            //修改redis的数据
            String cartItemJson = JSON.toJSONString(cartItemVo);
            cartOps.put(skuId.toString(),cartItemJson);

            return cartItemVo;
        }

    }
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.toThreadLocal.get();
        String cartKey="";
        //就是登录了
        if(userInfoTo.getUserId()!=null)
        {
            cartKey=CART_PREFIX+userInfoTo.getUserId();
        }else {
            cartKey=CART_PREFIX+userInfoTo.getUserKey();
        }
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);
        return operations;
    }


    @Override
    public CartItemVo getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String str = (String) cartOps.get(skuId.toString());
        CartItemVo cartItem = JSON.parseObject(str, CartItemVo.class);
        return cartItem;
    }
    /**
     * 获取购物车里面的数据
     * @param cartKey
     * @return
     */
    private List<CartItemVo> getCartItems(String cartKey) {
        //获取购物车里面的所有商品
        BoundHashOperations<String, Object, Object> operations = redisTemplate.boundHashOps(cartKey);
        List<Object> values = operations.values();
        if (values != null && values.size() > 0) {
            List<CartItemVo> cartItemVoStream = values.stream().map((obj) -> {
                String str = (String) obj;
                CartItemVo cartItem = JSON.parseObject(str, CartItemVo.class);
                return cartItem;
            }).collect(Collectors.toList());
            return cartItemVoStream;
        }
        return null;

    }
    @Override
    public CartVo getCart() throws ExecutionException, InterruptedException {

        CartVo cart = new CartVo();
        UserInfoTo userInfoTo = CartInterceptor.toThreadLocal.get();

        if (userInfoTo.getUserId()!=null){
            // 1、登录状态
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            // 2、如果临时购物车的数据还没有合并，则合并购物车
            String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<CartItemVo> tempCartItems = getCartItems(tempCartKey);
            if (tempCartItems!=null) {
                // 临时购物车有数据，需要合并
                for (CartItemVo item : tempCartItems) {
                    addToCart(item.getSkuId(),item.getCount());
                }
                // 清除临时购物车的数据
                clearCart(tempCartKey);
            }
            // 3、删除临时购物车
            // 4、获取登录后的购物车数据
            List<CartItemVo> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);

        } else {
            // 2、没登录状态
            String cartKey = CART_PREFIX + userInfoTo.getUserKey();
            // 获取临时购物车的所有项
            List<CartItemVo> cartItems = getCartItems(cartKey);
            cart.setItems(cartItems);
        }
        return cart;
    }

    @Override
    public void clearCart(String cartKey) {
        // 直接删除该键
        redisTemplate.delete(cartKey);
    }

    @Override
    public void checkItem(Long skuId, Integer check) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItemVo cartItem = getCartItem(skuId);
        cartItem.setCheck(check==1?true:false);
        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(),s);
    }

    @Override
    public void countItem(Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItemVo cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        cartOps.put(skuId.toString(),JSON.toJSONString(cartItem));
    }

    /**
     * 删除购物项
     * @param skuId
     */
    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }



    /**
     * 获取用户选择的所有购物项
     * @return
     */
    @Override
    public List<CartItemVo> getUserCartItems() {
        UserInfoTo userInfoTo = CartInterceptor.toThreadLocal.get();
        if (userInfoTo.getUserId() == null) {
            return null;
        } else {
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            // 获取所有用户选择的购物项
            List<CartItemVo> collect1 = getCartItems(cartKey);
            if(collect1!=null){
                List<CartItemVo> collect = collect1.stream()
                        .filter(item -> item.getCheck())
                        .map(item -> {
                            // TODO 1、更新为最新价格
                            R price = productFeignService.getPrice(item.getSkuId());
                            String data = (String) price.get("data");
                            item.setPrice(new BigDecimal(data));
                            return item;
                        })
                        .collect(Collectors.toList());
                return collect;
            }

            return collect1;
        }
    }


}
