package com.yupi.springbootinit.config;

import io.github.briqt.spark4j.SparkClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SparkClientConfig {
//    private String appid = "5bfa843f";
//    private String apiSecret = "NzMyODc0MTU2MjgwOTQyYTNkODNkODlj";
//    private String apiKey = "e10ca63b2580570f5abc4b4b5aaeb936";

    private String appid = "df7cf2dd";
    private String apiSecret = "Y2RiNmUyNTJmMTkxZTI4ODcwZWIxMWRm";
    private String apiKey = "c81ba7828b5afa5df1e15bc52c79e172";


    @Bean
    public SparkClient sparkClient() {
        SparkClient sparkClient = new SparkClient();
        sparkClient.apiKey = apiKey;
        sparkClient.apiSecret = apiSecret;
        sparkClient.appid = appid;
        return sparkClient;

    }
}
