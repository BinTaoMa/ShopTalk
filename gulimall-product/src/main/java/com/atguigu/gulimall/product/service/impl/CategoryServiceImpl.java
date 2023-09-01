package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.Catalog2Vo;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        List<CategoryEntity> entities = baseMapper.selectList(null);

        List<CategoryEntity> level1Menus = entities.stream().filter((categoryEntity) -> {
            return categoryEntity.getParentCid() == 0;
        }).map((menu)->{
            menu.setChildren(getChildren(menu, entities));
            return menu;
        }).sorted((menu1, menu2) -> {
            //排序，sort是实体类的排序属性，值越小优先级越高，要判断非空防止空指针异常
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());

        return level1Menus;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 检查菜单

        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);
        return parentPath.toArray(new Long[parentPath.size()]);
    }

    //别忘了加业务注解，引导类开启了业务@EnableTransactionManagement
    @Caching(evict = {
            @CacheEvict(value = {"category"},key ="'getCatalogJson'")
    })
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }



    @Autowired
    StringRedisTemplate redisTemplate;

    @Cacheable(value = "category" ,key = "#root.methodName")
    @Override
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        System.out.println("查询了数据库.....");

        // 一次性获取所有 数据
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        System.out.println("调用了 getCatalogJson  查询了数据库........【三级分类】");
        // 1）、所有1级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

        // 2）、封装数据
        Map<String, List<Catalog2Vo>> collect = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), level1 -> {
            // 查到当前1级分类的2级分类
            List<CategoryEntity> category2level = getParent_cid(selectList, level1.getCatId());
            List<Catalog2Vo> catalog2Vos = null;
            if (category2level != null) {
                catalog2Vos = category2level.stream().map(level12 -> {
                    // 查询当前2级分类的3级分类
                    List<CategoryEntity> category3level = getParent_cid(selectList, level12.getCatId());
                    List<Catalog2Vo.Catalog3Vo> catalog3Vos = null;
                    if (category3level != null) {
                        catalog3Vos = category3level.stream().map(level13 -> {
                            return new Catalog2Vo.Catalog3Vo(level12.getCatId().toString(), level13.getCatId().toString(), level13.getName());
                        }).collect(Collectors.toList());
                    }
                    return new Catalog2Vo(level1.getCatId().toString(), catalog3Vos, level12.getCatId().toString(), level12.getName());
                }).collect(Collectors.toList());
            }
            return catalog2Vos;
        }));
//            redisTemplate.opsForValue().set("catelogJSON",s);


        return collect;
    }

    public Map<String, List<Catalog2Vo>> getCatalogJson2() {
        //1.加入缓存逻辑
        String catelogJSON = redisTemplate.opsForValue().get("catelogJSON");
        if (StringUtils.isEmpty(catelogJSON)){
            //2.缓存中没有,查询数据库
            System.out.println("缓存未命中.....查询数据库");
            Map<String, List<Catalog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithRedissonLock();
            //3.查到的数据再放入缓存,将对象转为json放在缓存中
        }
        System.out.println("缓存命中.....直接返回");
        Type type=new TypeReference<Map<String, List<Catalog2Vo>>>(){}.getType();
        Map<String, List<Catalog2Vo>> result = JSON.parseObject(catelogJSON,type);
        return result;
    }
    @Autowired
    RedissonClient redissonClient;

    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDbWithRedissonLock() {

        //1、占分布式锁。去redis占坑
        //（锁的粒度，越细越快）例如具体缓存的是某个数据，11号商品，锁名就设product-11-lock，不锁其他商品
        //RLock catalogJsonLock = redissonClient.getLock("catalogJson-lock");
        //创建读锁
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("catalogJson-lock");

        RLock rLock = readWriteLock.readLock();

        Map<String, List<Catalog2Vo>> dataFromDb = null;
        try {
            rLock.lock();
            //加锁成功...执行业务
            dataFromDb = getDataFromDb();
        } finally {
            rLock.unlock();
        }
        //先去redis查询下保证当前的锁是自己的
        //获取值对比，对比成功删除=原子性 lua脚本解锁
        // String lockValue = stringRedisTemplate.opsForValue().get("lock");
        // if (uuid.equals(lockValue)) {
        //     //删除我自己的锁
        //     stringRedisTemplate.delete("lock");
        // }

        return dataFromDb;

    }
    //分布式锁
    public Map<String, List<Catalog2Vo>> getCatalogJsonFromDbWithRedisLock() {
        // 1、分布式锁。去redis占坑，同时设置过期时间

        //每个线程设置随机的UUID，也可以成为token
        String uuid = UUID.randomUUID().toString();

        //只有键key不存在的时候才会设置key的值。保证分布式情况下一个锁能进线程
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
//setIfAbsent()如果返回true代表此线程拿到锁；
//        如果返回false代表没拿到锁，就sleep一会递归重试，一直到某一层获取到锁并层层返回redis或数据库结果。
        if (lock) {
            // 加锁成功....执行业务【内部会判断一次redis是否有值】
            System.out.println("获取分布式锁成功....");
            Map<String, List<Catalog2Vo>> dataFromDB = null;
            try {
                dataFromDB = getDataFromDb();
            } finally {
                // 2、查询UUID是否是自己，是自己的lock就删除
                // 查询+删除 必须是原子操作：lua脚本解锁
                String luaScript = "if redis.call('get',KEYS[1]) == ARGV[1]\n" +
                        "then\n" +
                        "    return redis.call('del',KEYS[1])\n" +
                        "else\n" +
                        "    return 0\n" +
                        "end";
                // 删除锁
                Long lock1 = redisTemplate.execute(
                        new DefaultRedisScript<Long>(luaScript, Long.class),
                        Arrays.asList("lock"), uuid);    //把key和value传给lua脚本
            }
            return dataFromDB;
        } else {
            System.out.println("获取分布式锁失败....等待重试...");
            // 加锁失败....重试
            // 休眠100ms重试
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getCatalogJsonFromDbWithRedisLock();// 自旋的方式
        }
    }
    //从数据库查询并封装分类的数据
    public Map<String, List<Catalog2Vo>> getDataFromDb() {
        //只要是同一把锁,就能锁住需要这个锁的所有线程
        //synchronized (this):SpringBoot所有组件在容器中都是单例的
        //TODO 本地锁:synchronized,JUC(Lock)

//        synchronized (this) {
            //得到锁后,我们应该再去缓存确定一次,如果没有才需要继续查询
            String catalogJSON = redisTemplate.opsForValue().get("catelogJSON");
            if (!StringUtils.isEmpty(catalogJSON)) {
                Type type=new TypeReference<Map<String, List<Catalog2Vo>>>(){}.getType();
                Map<String, List<Catalog2Vo>> result = JSON.parseObject(catalogJSON, type);
                return result;
            }
            System.out.println("查询了数据库.....");

            // 一次性获取所有 数据
            List<CategoryEntity> selectList = baseMapper.selectList(null);
            System.out.println("调用了 getCatalogJson  查询了数据库........【三级分类】");
            // 1）、所有1级分类
            List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

            // 2）、封装数据
            Map<String, List<Catalog2Vo>> collect = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), level1 -> {
                // 查到当前1级分类的2级分类
                List<CategoryEntity> category2level = getParent_cid(selectList, level1.getCatId());
                List<Catalog2Vo> catalog2Vos = null;
                if (category2level != null) {
                    catalog2Vos = category2level.stream().map(level12 -> {
                        // 查询当前2级分类的3级分类
                        List<CategoryEntity> category3level = getParent_cid(selectList, level12.getCatId());
                        List<Catalog2Vo.Catalog3Vo> catalog3Vos = null;
                        if (category3level != null) {
                            catalog3Vos = category3level.stream().map(level13 -> {
                                return new Catalog2Vo.Catalog3Vo(level12.getCatId().toString(), level13.getCatId().toString(), level13.getName());
                            }).collect(Collectors.toList());
                        }
                        return new Catalog2Vo(level1.getCatId().toString(), catalog3Vos, level12.getCatId().toString(), level12.getName());
                    }).collect(Collectors.toList());
                }
                return catalog2Vos;
            }));
//            redisTemplate.opsForValue().set("catelogJSON",s);
            String s = JSON.toJSONString(collect);
            redisTemplate.opsForValue().set("catelogJSON",s,1, TimeUnit.DAYS);
            System.out.println("储存完毕");

            return collect;
        }
//    }

    /**
     * 查询出父ID为 parent_cid的List集合
     */
    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parent_cid) {
        return selectList.stream().filter(item -> item.getParentCid() == parent_cid).collect(Collectors.toList());
        //return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", level.getCatId()));
    }


    //递归查找父节点id
    public List<Long> findParentPath(Long catelogId,List<Long> paths){
        //1、收集当前节点id
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0){
            findParentPath(byId.getParentCid(), paths);
        }
        paths.add(catelogId);
        return paths;
    }


    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all){
        List<CategoryEntity> children = all.stream()
                .filter(CategoryEntity -> CategoryEntity.getParentCid().equals(root.getCatId()))
                .map(categoryEntity -> {
                    //递归查找
                    categoryEntity.setChildren(getChildren(categoryEntity, all));
                    return categoryEntity;
                })
                .sorted((menu1, menu2) -> {
                    return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
                })
                .collect(Collectors.toList());
        return children;
    }


}