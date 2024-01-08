package com.yupi.springbootinit.config;

import io.github.briqt.spark4j.SparkClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class SparkClientConfig {
//    private String appid = "5bfa843f";
//    private String apiSecret = "NzMyODc0MTU2MjgwOTQyYTNkODNkODlj";
//    private String apiKey = "e10ca63b2580570f5abc4b4b5aaeb936";
    @Value("${xunfei.client.appid}")
    private String appid;
    @Value("${xunfei.client.apiSecret}")
    private String apiSecret;
    @Value("${xunfei.client.apiKey}")
    private String apiKey;


    @Bean
    public SparkClient sparkClient() {
        SparkClient sparkClient = new SparkClient();
        sparkClient.apiKey = apiKey;
        sparkClient.apiSecret = apiSecret;
        sparkClient.appid = appid;
        return sparkClient;

    }
}
