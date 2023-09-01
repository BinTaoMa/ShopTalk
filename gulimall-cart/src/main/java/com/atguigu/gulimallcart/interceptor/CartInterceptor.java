package com.atguigu.gulimallcart.interceptor;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.atguigu.common.vo.MemberResponseVo;
import com.atguigu.gulimallcart.vo.UserInfoTo;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.util.UUID;

import static com.atguigu.common.constant.AuthServerConstant.LOGIN_USER;
import static com.atguigu.common.constant.CartConstant.TEMP_USER_COOKIE_NAME;
import static com.atguigu.common.constant.CartConstant.TEMP_USER_COOKIE_TIMEOUT;

/**
 * @Description: 在执行目标方法之前，判断用户的登录状态.并封装传递给controller目标请求
 **/
 
public class CartInterceptor implements HandlerInterceptor {
 
//创建ThreadLocal<>对象，同一个线程共享数据
    public static ThreadLocal<UserInfoTo> toThreadLocal = new ThreadLocal<>();
 
    /***
     * 目标方法执行之前
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
 
        UserInfoTo userInfoTo = new UserInfoTo();
 
        HttpSession session = request.getSession();
        //1.从session获得当前登录用户的信息
        MemberResponseVo memberResponseVo = (MemberResponseVo) session.getAttribute(LOGIN_USER);
 
        if (memberResponseVo != null) {
            //2.1 如果用户登录了，给用户传输对象添加id
            userInfoTo.setUserId(memberResponseVo.getId());
        }
//        3.获取cookie
        Cookie[] cookies = request.getCookies();
//        如果cookie不为空，找到和"user-key"同名的cookie，设置userKey，标记临时用户
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                //user-key
                String name = cookie.getName();
                if (name.equals(TEMP_USER_COOKIE_NAME)) {
                    userInfoTo.setUserKey(cookie.getValue());
                    //标记为已是临时用户
                    userInfoTo.setTempUser(true);
                }
            }
        }
 
        //如果没有临时用户一定分配一个临时用户，userKey是临时id。
        if (StringUtils.isEmpty(userInfoTo.getUserKey())) {
            String uuid = UUID.randomUUID().toString();
            userInfoTo.setUserKey(uuid);
        }
 
        //目标方法执行之前，将用户传输信息放到ThreadLocal里，同一个线程共享数据。
        toThreadLocal.set(userInfoTo);
        return true;
    }
 
 
    /**
     * 业务执行之后，分配临时用户来浏览器保存
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
 
        //获取当前用户的值
        UserInfoTo userInfoTo = toThreadLocal.get();
 
        //如果没有临时用户则保存一个临时用户，并延长cookie过期时间，扩大cookie域，实现子域名共享cookie。
        if (!userInfoTo.getTempUser()) {
            //创建一个cookie
            Cookie cookie = new Cookie(TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
            //扩大作用域
//            cookie.setDomain("gulimall.com");
            //设置过期时间
            cookie.setMaxAge(TEMP_USER_COOKIE_TIMEOUT);
            response.addCookie(cookie);
        }
 
    }
 
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
 
    }
}