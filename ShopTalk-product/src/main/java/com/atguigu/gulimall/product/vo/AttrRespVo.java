package com.atguigu.gulimall.product.vo;

import com.atguigu.gulimall.product.vo.AttrVo;
import lombok.Data;

@Data
public class AttrRespVo extends AttrVo {
    private String catelogName;
    private String groupName;
    private Long[] catelogPath;
}