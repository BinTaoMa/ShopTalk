package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.AttrEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品属性
 * 
 * @author mbt
 * @email mbt@gmail.com
 * @date 2023-05-20 20:30:32
 */
@Mapper
public interface AttrDao extends BaseMapper<AttrEntity> {
	
}
