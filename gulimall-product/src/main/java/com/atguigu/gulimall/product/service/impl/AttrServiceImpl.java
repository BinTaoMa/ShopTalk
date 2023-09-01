package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.constant.ProductConstant;
import com.atguigu.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.AttrGroupRelationVo;
import com.atguigu.gulimall.product.vo.AttrRespVo;
import com.atguigu.gulimall.product.vo.AttrVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.AttrDao;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    AttrAttrgroupRelationDao attrAttrgroupRelationDao;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveAttr(AttrVo attr) {
        //AttrEntity attrEntity = new AttrEntity();
        //保存attrEntity
        //利用attr的属性给attrEntity的属性赋值，前提是他们俩的属性名一直
        //BeanUtils.copyProperties(attr, attrEntity);

        //1、保存基本数据
        this.save(attr);

        //2、保存关联关系
        if(attr.getAttrType()==ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()&&attr.getAttrGroupId()!=null){
            AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = new AttrAttrgroupRelationEntity();
            attrAttrgroupRelationEntity.setAttrId(attr.getAttrId());
            attrAttrgroupRelationEntity.setAttrGroupId(attr.getAttrGroupId());
            attrAttrgroupRelationDao.insert(attrAttrgroupRelationEntity);
        }

    }

    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    private AttrGroupDao attrGroupDao;


    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params,Long catelogId,String attrType) {
        //模糊查询

        String key = (String) params.get("key");
        LambdaQueryWrapper<AttrEntity> wrapper = new LambdaQueryWrapper<>();
        //值添加这一行。判断是规格参数还是销售属性,[0-销售属性，1-基本属性，2-既是销售属性又是基本属性]
        wrapper.eq(AttrEntity::getAttrType,"base".equalsIgnoreCase(attrType)? ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() :ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());
        if(StringUtils.isNotEmpty(key)){
            wrapper.and(obj->obj.eq(AttrEntity::getAttrId,key).or().like(AttrEntity::getAttrName,key));
        }
        //catelogId查询
        if(catelogId!=0) wrapper.eq(AttrEntity::getCatelogId,catelogId);
        //查询
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                wrapper
        );
        PageUtils pageUtils = new PageUtils(page);
        //分页数据中加入当前属性的“所属分类”和“所属分组”
        List<AttrEntity> attrEntities = page.getRecords();
        List<AttrRespVo> attrRespVos = attrEntities.stream().map(item->{
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(item,attrRespVo);
            //查询“所属分类”和“所属分组”的name
            Long catelogId2 = attrRespVo.getCatelogId();
            Long attrGroupId=null;
            AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = attrAttrgroupRelationDao.selectOne(
                    new LambdaQueryWrapper<AttrAttrgroupRelationEntity>().eq(AttrAttrgroupRelationEntity::getAttrId,attrRespVo.getAttrId())
            );
            if(attrAttrgroupRelationEntity!=null) attrGroupId = attrAttrgroupRelationEntity.getAttrGroupId();
            CategoryEntity categoryEntity = categoryDao.selectById(catelogId2);
            //没查到的对象就不能getName了，必须防止空指针异常，习惯习惯，坑点
            if(categoryEntity!=null) attrRespVo.setCatelogName(categoryEntity.getName());
            AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrGroupId);
            if(attrGroupEntity!=null) attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
            return attrRespVo;
        }).collect(Collectors.toList());
        pageUtils.setList(attrRespVos);
        //返回分页工具对象
        return pageUtils;
    }

    @Autowired
    private CategoryService categoryService;

    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrRespVo attrRespVo = new AttrRespVo();
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity, attrRespVo);

        //设置所属分组
        AttrAttrgroupRelationEntity attrAttrgroupRelation = attrAttrgroupRelationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId));
        if (attrAttrgroupRelation != null){
            attrRespVo.setAttrGroupId(attrAttrgroupRelation.getAttrGroupId());
        }
        //设置所属分类路径
        Long[] catelogPath = categoryService.findCatelogPath(attrEntity.getCatelogId());
        attrRespVo.setCatelogPath(catelogPath);

        return attrRespVo;
    }

    //保存时，要修改两张表，要加业务注解，引导类也开启了业务
    @Override
    @Transactional
    public void updateAttr(AttrVo attrVo) {
        //先修改属性
        this.updateById(attrVo);
        Long attrGroupId = attrVo.getAttrGroupId();
        Long attrId = attrVo.getAttrId();
        //再判断修改关联关系。如果是销售属性或者分组id是空时，不用修改
        if(attrVo.getAttrType()==ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode()||attrGroupId==null){
            //todo。待优化，如果是将规格属性改为销售属性，查询删除原关联关系
            return;
        }
        //查询关联表中是否已有此属性
        AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = attrAttrgroupRelationDao.selectOne(
                new LambdaQueryWrapper<AttrAttrgroupRelationEntity>().eq(AttrAttrgroupRelationEntity::getAttrId, attrId)
        );
        if(attrAttrgroupRelationEntity!=null){
//            已存在，修改
            attrAttrgroupRelationEntity.setAttrGroupId(attrGroupId);
            attrAttrgroupRelationDao.updateById(attrAttrgroupRelationEntity);
        }else {
            //不存在，新增关联关系
            attrAttrgroupRelationEntity=new AttrAttrgroupRelationEntity();
            attrAttrgroupRelationEntity.setAttrGroupId(attrGroupId);
            attrAttrgroupRelationEntity.setAttrId(attrId);
            attrAttrgroupRelationDao.insert(attrAttrgroupRelationEntity);
        }
    }

    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {
        List<AttrAttrgroupRelationEntity> relationEntities = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrgroupId));
        List<Long> attrIds = relationEntities.stream().map((entity) -> {
            return entity.getAttrId();
        }).collect(Collectors.toList());

        List<AttrEntity> attrEntities = this.baseMapper.selectBatchIds(attrIds);

        return attrEntities;
    }

    @Override
    public void deleteRelation(AttrGroupRelationVo[] vos) {
        List<AttrGroupRelationVo> relationVos = Arrays.asList(vos);
        List<AttrAttrgroupRelationEntity> entities = relationVos.stream().map((relationVo) -> {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(relationVo, relationEntity);
            return relationEntity;
        }).collect(Collectors.toList());
        //根据attrId，attrGroupId批量删除关联关系
        System.out.println(entities.get(0));
        attrAttrgroupRelationDao.deleteBatchRelation(entities);
    }

    /*
     *获取当前分组没有关联的所有属性
     *@param:[params, attrgroupId]
     *@return:com.xmh.common.utils.PageUtils
     *@date: 2021/8/9 20:35
     */
    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId) {
        //1、当前分组只能关联自己所属分类里面的所有属性
        //先查询出当前分组所属的分类
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        Long catelogId = attrGroupEntity.getCatelogId();
        //2、当前分组只能关联别的分组没有引用的属性
        //2.1当前分类下的所有分组
        List<AttrGroupEntity> attrGroupEntities = attrGroupDao.selectList(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        List<Long> attrGroupIds = attrGroupEntities.stream().map(attrGroupEntity1 -> {
            return attrGroupEntity1.getAttrGroupId();
        }).collect(Collectors.toList());
        //2.2这些分组关联的属性
        List<AttrAttrgroupRelationEntity> relationEntities = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id", attrGroupIds));
        List<Long> attrIds = relationEntities.stream().map((relationEntity) -> {
            return relationEntity.getAttrId();
        }).collect(Collectors.toList());
        // 2.3从当前分类的所有属性中移除这些属性
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>().eq("catelog_id", catelogId).eq("attr_type", ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode());
        if (attrIds != null && attrIds.size() > 0){
            wrapper.notIn("attr_id", attrIds);
        }
        //模糊查询
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)){
            wrapper.and((w)->{
                w.eq("attr_id", key).or().like("attr_name", key);
            });
        }
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper);

        return new PageUtils(page);
    }

    @Override
    public List<Long> selectSearchAttrIds(List<Long> attrIds) {

        return baseMapper.selectSearchAttrIds(attrIds);
    }


}