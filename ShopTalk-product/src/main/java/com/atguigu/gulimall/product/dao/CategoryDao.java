package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author mbt
 * @email mbt@gmail.com
 * @date 2023-05-20 20:30:33
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
