package com.atguigu.gulimall.authserver.LoginController;

import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.constant.AuthServerConstant;
import com.atguigu.common.exception.BizCodeEnume;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.authserver.feign.MemberFeignService;
import com.atguigu.gulimall.authserver.feign.ThirdPartFeignService;
import com.atguigu.common.vo.MemberResponseVo;
import com.atguigu.gulimall.authserver.vo.UserLoginVo;
import com.atguigu.gulimall.authserver.vo.UserRegisterVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
public class LoginController {
    @Autowired
    private ThirdPartFeignService thirdPartFeignService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping(value = "/sms/sendCode")
    public R sendCode(@RequestParam("phone") String phone) {

        //1、接口防刷
        String redisCode = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
        if (!StringUtils.isEmpty(redisCode)) {
            //活动存入redis的时间，用当前时间减去存入redis的时间，通过比较redis存的时间和现在时间是否超过一分钟，防止一分钟内不断刷验证码
            long currentTime = Long.parseLong(redisCode.split("_")[1]);
            if (System.currentTimeMillis() - currentTime < 60000) {
                //60s内不能再发
                return R.error(BizCodeEnume.SMS_CODE_EXCEPTION.getCode(),BizCodeEnume.SMS_CODE_EXCEPTION.getMsg());
            }
        }

        //2、验证码的再次效验 redis.存key-phone,value-code
//        String code = UUID.randomUUID().toString().substring(0, 5);
//        String redisValue = code+"_"+System.currentTimeMillis();
        int code = (int) ((Math.random() * 9 + 1) * 100000);// 验证码只可以是数字
        String codeNum = String.valueOf(code);
//redis存的值为六位数字+当前系统时间，防止一分钟内不断刷验证码
        String redisStorage = codeNum + "_" + System.currentTimeMillis();

        //存入redis并指定过期时间10min，十分钟内验证码有效
        stringRedisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone,
                redisStorage,10, TimeUnit.MINUTES);

        thirdPartFeignService.sendCode(phone, codeNum);

        return R.ok();
    }

    @Autowired
    MemberFeignService memberFeignService;

    /**
     *
     * TODO: 重定向携带数据：利用session原理，将数据放在session中。
     * TODO:只要跳转到下一个页面取出这个数据以后，session里面的数据就会删掉
     * TODO：分布下session问题
     * RedirectAttributes：重定向也可以保留数据，不会丢失。这里用于存错误消息
     * 用户注册
     * @return
     */
    @PostMapping(value = "/register")
    public String register(@Valid UserRegisterVo vos, BindingResult result, RedirectAttributes attributes) {

        //如果有错误回到注册页面
        if (result.hasErrors()) {
            Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
            attributes.addFlashAttribute("errors", errors);

            //效验出错回到注册页面
            return "到注册";
            // 1、return "reg"; 请求转发【使用Model共享数据】【异常：，405 POST not support】
            // 2、"redirect:http:/reg.html"重定向【使用RedirectAttributes共享数据】【bug：会以ip+port来重定向】
            // 3、到注册重定向【使用RedirectAttributes共享数据】
        }

        //1、效验验证码
        String code = vos.getCode();

        //获取存入Redis里的验证码
        String redisCode = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vos.getPhone());
        if (!StringUtils.isEmpty(redisCode)) {
            // 判断验证码是否正确【有BUG，如果字符串存储有问题，没有解析出code，数据为空，导致验证码永远错误】
            if (code.equals(redisCode.split("_")[0])) {
                //删除验证码（不可重复使用）;令牌机制
                stringRedisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX+vos.getPhone());
                //验证码通过，真正注册，调用远程服务进行注册【会员服务】
                R register = memberFeignService.register(vos);
                if (register.getCode1() == 0) {
                    //成功，重定向到登录页面
                    return "到登录";
                } else {
                    //失败
                    Map<String, String> errors = new HashMap<>();
                    errors.put("msg", register.getData("msg",new TypeReference<String>(){}));
                    attributes.addFlashAttribute("errors",errors);
                    return "到注册";
                }
            } else {
                //验证码错误
                Map<String, String> errors = new HashMap<>();
                errors.put("code","验证码错误");
                attributes.addFlashAttribute("errors",errors);
                return "到注册";
            }
        } else {
            // redis中验证码过期
            Map<String, String> errors = new HashMap<>();
            errors.put("code","验证码过期");
            attributes.addFlashAttribute("errors",errors);
            return "到注册";
        }
    }



    /**
     *
     * @param session 将登录用户信息写入session并重定向到主页，右上角显示你好，Xxx。
     *  整合了SpringSession，扩大了session作用域到"gulimall.com"，解决了session共享问题，
     * @return 路由到的页面，不加@ResponseBody，防止返回值是字符串
     */
    @PostMapping(value = "/login")
    public String login(UserLoginVo vo, RedirectAttributes attributes, HttpSession session) {
        //远程登录
        R login = memberFeignService.login(vo);

        if (login.getCode1() == 0) {
//登陆成功，将登录者信息放到session中，重定向到首页。
            MemberResponseVo data = login.getData("data", new TypeReference<MemberResponseVo>() {});
            session.setAttribute(AuthServerConstant.LOGIN_USER, data);
            return "登录成功";
        } else {
//登陆失败，将错误消息添加到attributes，重定向到登录页。
            Map<String,String> errors = new HashMap<>();
            errors.put("msg",login.getData("msg",new TypeReference<String>(){}));
            attributes.addFlashAttribute("errors",errors);
            System.out.println(login.getData("msg",new TypeReference<String>(){}));
            return "登录失败";
        }
    }
}
 