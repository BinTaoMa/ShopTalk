package com.atguigu.gulimallcart.vo;


import lombok.Data;

@Data
public class UserInfoTo {
    private Long userId;
    private String userKey; 
    private Boolean tempUser = false;   // 判断是否有临时用户
}