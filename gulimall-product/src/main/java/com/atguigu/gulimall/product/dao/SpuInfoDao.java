package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.SpuInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * spu信息
 * 
 * @author mbt
 * @email mbt@gmail.com
 * @date 2023-05-19 20:08:17
 */
@Mapper
public interface SpuInfoDao extends BaseMapper<SpuInfoEntity> {

    void updaSpuStatus(@Param("spuId") Long spuId,@Param("code") int code);
}
