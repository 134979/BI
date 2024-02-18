package com.yupi.springbootinit.xunfeiAPI;

import io.github.briqt.spark4j.SparkClient;
import io.github.briqt.spark4j.constant.SparkApiVersion;
import io.github.briqt.spark4j.exception.SparkException;
import io.github.briqt.spark4j.model.SparkMessage;
import io.github.briqt.spark4j.model.SparkSyncChatResponse;
import io.github.briqt.spark4j.model.request.SparkRequest;
import io.github.briqt.spark4j.model.response.SparkTextUsage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
public class XunFeiAPITest {
    @Resource
    private SparkClient sparkClient;
    @Test
    public void xunFeiAPITest(){
        final String prompt = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
        "分析需求：\n" +
        "{数据分析的需求或者目标}\n" +
        "原始数据：\n" +
        "{csv格式的原始数据，用,作为分隔符}\n" +
        "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
        "【【【【【\n" +
        "{前端 Echarts V5 的 option 配置对象js代码,合理地将数据进行可视化，不要生成任何多余的内容，比如注释}\n" +
        "【【【【【\n" +
        "{明确的数据分析结论、越详细越好，不要生成多余的注释}";
        String question="分析网站用户增长量\n"+"日期,增长量\n" +
                "1号,20\n" +
                "2号,40\t\n" +
                "3号,30\n" +
                "4号,50\n" +
                "5号,100";

        // 消息列表，可以在此列表添加历史对话记录
        List<SparkMessage> messages=new ArrayList<>();
        messages.add(SparkMessage.systemContent(prompt));
        messages.add(SparkMessage.userContent(question));
// 构造请求
        SparkRequest sparkRequest=SparkRequest.builder()
// 消息列表
                .messages(messages)
// 模型回答的tokens的最大长度,非必传，默认为2048。
// V1.5取值为[1,4096]
// V2.0取值为[1,8192]
// V3.0取值为[1,8192]
                .maxTokens(2048)
// 核采样阈值。用于决定结果随机性,取值越高随机性越强即相同的问题得到的不同答案的可能性越高 非必传,取值为[0,1],默认为0.5
                .temperature(0.2)
// 指定请求版本，默认使用最新2.0版本
                .apiVersion(SparkApiVersion.V3_0)
                .build();

        try {
            // 同步调用
            SparkSyncChatResponse chatResponse = sparkClient.chatSync(sparkRequest);
            SparkTextUsage textUsage = chatResponse.getTextUsage();
            String result = chatResponse.getContent();
            String[] splits = result.split("【【【【【");
            String json =splits[1].trim();
            System.out.println("\n回答：" + chatResponse.getContent());
            Pattern pattern = Pattern.compile("let option = (\\{.*\\});", Pattern.DOTALL);
            System.out.println(json);
            System.out.println("+++++++++++");
            Matcher matcher = pattern.matcher(json);
            System.out.println(matcher);
            System.out.println("+++++++++++");
            if (matcher.find()) {
                System.out.println("+++++++++++");
                String optionJson = matcher.group(1);
                System.out.println(optionJson);
            }
            System.out.println("--------");
        } catch (SparkException e) {
            System.out.println("发生异常了：" + e.getMessage());
        }
    }
}
