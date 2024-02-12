package com.atguigu.gulimall.order.interceptor;

import com.alibaba.nacos.common.packagescan.resource.AntPathMatcher;
import com.atguigu.common.vo.MemberResponseVo;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

import static com.atguigu.common.constant.AuthServerConstant.LOGIN_USER;

@Component
public class LoginUserInterceptor implements HandlerInterceptor {
    //登录的ThreadLocal，从session中获得的登录信息会放到这里面
//以后都通过这个ThreadLocal获取登录信息
    public static ThreadLocal<MemberResponseVo> loginUser = new ThreadLocal<MemberResponseVo>();

    /**
     * 用户登录拦截器
     *
     * @param request
     * @param response
     * @param handler
     * @return 用户登录：放行
     * 用户未登录：跳转到登录页面
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//直接放行的页面
        String uri = request.getRequestURI();
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        boolean match = antPathMatcher.match("/order/order/status/**", uri);
//        boolean match1 = antPathMatcher.match("/payed/notify", uri);
        if (match ) {
            return true;
        }
//不直接放行的页面，检测登录状态



        //获取登录的用户信息
        MemberResponseVo attribute = (MemberResponseVo) request.getSession().getAttribute(LOGIN_USER);

        if (attribute != null) {
            //把登录后用户的信息放在ThreadLocal里面进行保存
            loginUser.set(attribute);

            return true;
        } else {
            //未登录，返回登录页面
            response.setContentType("text/html;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.println("<script>alert('请先进行登录，再进行后续操作！');location.href='http://auth.gulimall.com/login.html'</script>");
            // session.setAttribute("msg", "请先进行登录");
            // response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
    }
}