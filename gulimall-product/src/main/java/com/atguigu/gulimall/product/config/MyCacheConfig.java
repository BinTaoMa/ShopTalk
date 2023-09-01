package com.atguigu.gulimall.product.config;
 
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
 
/**
 * @author zr
 * @date 2021/11/14 16:57
 */
@EnableConfigurationProperties(CacheProperties.class)
@EnableCaching
@Configuration
public class MyCacheConfig {
 
//    @Autowired
//    CacheProperties cacheProperties;
 
    /**
     * 需要将配置文件中的配置设置上
     * 1、使配置类生效
     * 1）开启配置类与属性绑定功能EnableConfigurationProperties
     *
     * @ConfigurationProperties(prefix = "spring.cache")  public class CacheProperties
     * 2）注入就可以使用了
     * @Autowired CacheProperties cacheProperties;
     * 3）直接在方法参数上加入属性参数redisCacheConfiguration(CacheProperties redisProperties)
     * 自动从IOC容器中找
     * <p>
     * 2、给config设置上
     */
    @Bean
    RedisCacheConfiguration redisCacheConfiguration(CacheProperties cacheProperties) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();
        config = config.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));
        //指定缓存序列化方式为json
        config = config.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        // 配置文件生效：RedisCacheConfiguration
        CacheProperties.Redis redisProperties = cacheProperties.getRedis();
        //设置配置文件中的各项配置，如过期时间,如果此处以下的代码没有配置,配置文件中的配置不会生效
        if (redisProperties.getTimeToLive() != null) {
            config = config.entryTtl(redisProperties.getTimeToLive());
        }
        if (redisProperties.getKeyPrefix() != null) {
            config = config.prefixKeysWith(redisProperties.getKeyPrefix());
        }
        if (!redisProperties.isCacheNullValues()) {
            config = config.disableCachingNullValues();
        }
        if (!redisProperties.isUseKeyPrefix()) {
            config = config.disableKeyPrefix();
        }
        return config;
    }
}
 