package com.example.gulimallware;

import com.atguigu.gulimall.ware.dao.WareSkuDao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GulimallWareApplicationTests {
    @Autowired
    WareSkuDao wareSkuDao;

    @Test
    void contextLoads() {
        wareSkuDao.lockSkuStock(1L, 1L, 3);
    }

}
