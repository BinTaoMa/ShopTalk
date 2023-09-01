package com.example.gulimall.gulimallproduct;

import com.atguigu.gulimall.product.GulimallProductApplication;
import com.atguigu.gulimall.product.service.BrandService;


import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.service.impl.SkuInfoServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = GulimallProductApplication.class)
public class GulimallProductApplicationTests {

    @Autowired
    SkuInfoServiceImpl skuInfoService;

    @Autowired(required = false)
    public BrandService brandService;
    @Autowired
    CategoryService categoryService;

//    @Test
//    public void testOss() throws FileNotFoundException {
//        String endpoint = "oss-cn-beijing.aliyuncs.com";
//        // 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
//        String accessKeyId = "LTAI5tJr9ZF2gnJxypvhf73P";
//        String accessKeySecret = "Nkcl6eJbZeifaDUAIHJrgIcNZ1fyMU";
//        // 填写Bucket存储空间名称，例如gulimall-hello。
//        String bucketName = "gulimall-mbt";
//        // 填写存储对象Object完整路径，完整路径中不能包含Bucket名称，例如exampledir/exampleobject.txt。
//        String objectName = "overlord.jpg";
//        // 填写本地文件的完整路径，例如D:\\localpath\\examplefile.txt。
//        // 如果未指定本地路径，则默认从示例程序所属项目对应本地路径中上传文件流。
//        String filePath= "C:\\Users\\ma\\Pictures\\Saved Pictures\\1.jpg";
//
//        // 创建OSSClient实例。
//        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
//
//        try {
//            InputStream inputStream = new FileInputStream(filePath);
//            // 创建PutObject请求。
//            ossClient.putObject(bucketName, objectName, inputStream);
//        } catch (OSSException oe) {
//            System.out.println("Caught an OSSException, which means your request made it to OSS, "
//                    + "but was rejected with an error response for some reason.");
//            System.out.println("Error Message:" + oe.getErrorMessage());
//            System.out.println("Error Code:" + oe.getErrorCode());
//            System.out.println("Request ID:" + oe.getRequestId());
//            System.out.println("Host ID:" + oe.getHostId());
//        } catch (ClientException ce) {
//            System.out.println("Caught an ClientException, which means the client encountered "
//                    + "a serious internal problem while trying to communicate with OSS, "
//                    + "such as not being able to access the network.");
//            System.out.println("Error Message:" + ce.getMessage());
//        } finally {
//            if (ossClient != null) {
//                ossClient.shutdown();
//            }
//        }
//    }
    @Test
    public void t1() throws ExecutionException, InterruptedException {
        System.out.println(skuInfoService.item(3L).toString());

    }


    @Test
    public void testPathCatelog(){
        Long[] catelogPath = categoryService.findCatelogPath(225L);

        log.info("{}", Arrays.asList(catelogPath));

    }

    @Autowired
    StringRedisTemplate redisTemplate;
    @Test
    public void testRedis() {
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        // 保存
        ops.set("hello", "world_" + UUID.randomUUID().toString());
        // 查询
        String hello = ops.get("hello");
        System.out.println(hello);

    }
    @Autowired
    RedissonClient redissonClient;

    @Test
    public void name() {
        System.out.println(redissonClient);
    }

}
