package com.yupi.springbootinit.manager;

import javax.annotation.Resource;

import io.github.briqt.spark4j.SparkClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Cos 操作测试
 *

 */
@SpringBootTest
class CosManagerTest {

    @Resource
    private SparkClient sparkClient;

    @Resource
    private CosManager cosManager;

    @Test
    void putObject() {
//        cosManager.putObject("test", "test.json");

    }
}