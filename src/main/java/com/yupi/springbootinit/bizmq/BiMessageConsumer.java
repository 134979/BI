package com.yupi.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.constant.CommonConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.manager.AiManager;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.service.ChartService;
import io.github.briqt.spark4j.SparkClient;
import io.github.briqt.spark4j.constant.SparkApiVersion;
import io.github.briqt.spark4j.exception.SparkException;
import io.github.briqt.spark4j.model.SparkMessage;
import io.github.briqt.spark4j.model.SparkSyncChatResponse;
import io.github.briqt.spark4j.model.request.SparkRequest;
import io.github.briqt.spark4j.model.response.SparkTextUsage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class BiMessageConsumer {

    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private SparkClient sparkClient;

    // 指定程序监听的消息队列和确认机制
    @SneakyThrows
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receiveMessage message = {}", message);
        if (StringUtils.isBlank(message)) {
            // 如果失败，消息拒绝
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息为空");
        }
        long chartId = Long.parseLong(message);
        Chart chart = chartService.getById(chartId);
        if (chart == null) {
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表为空");
        }
        // 先修改图表任务状态为 “执行中”。等执行成功后，修改为 “已完成”、保存执行结果；执行失败后，状态修改为 “失败”，记录任务失败信息。
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus("running");
        //修改redis中图表任务状态为“执行中”。
        Chart updateRedisChart=new Chart();
        updateRedisChart.setId(chart.getId());
        updateRedisChart.setUserId(chart.getUserId());
        updateRedisChart.setGoal(chart.getGoal());
        updateRedisChart.setStatus(chart.getStatus());
        updateRedisChart.setName(chart.getName());
        updateRedisChart.setChartData(chart.getChartData());
        updateRedisChart.setChartType(chart.getChartType());
        Double chartScore = redisTemplate.opsForZSet().score(chart.getUserId().toString(), updateRedisChart);
        Chart redisChart = (Chart) redisTemplate.opsForZSet().rangeByScore(chart.getUserId().toString(), chartScore, chartScore).iterator().next();
        redisChart.setStatus("running");

        boolean b = chartService.updateById(updateChart);
        if (!b) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
            handleRedisChartUpdateError(chart);
            return;
        }
        //更改redis中图表的状态为running
        Long l = redisTemplate.opsForZSet().removeRangeByScore(chart.getUserId().toString(), chartScore, chartScore);
        Boolean redisBool = redisTemplate.opsForZSet().add(chart.getUserId().toString(), redisChart, Instant.now().toEpochMilli());
        if(Boolean.FALSE.equals(redisBool)){
            log.error("更新redis中图表为running失败！"+chart.getId());
        }
        //构造调用讯飞AI请求
//        SparkRequest sparkRequest=SparkRequest.builder()
//// 消息列表
//                .messages(buildXunFeiUserInput(chart))
//// 模型回答的tokens的最大长度,非必传，默认为2048。
//// V1.5取值为[1,4096]
//// V2.0取值为[1,8192]
//// V3.0取值为[1,8192]
//                .maxTokens(2048)
//// 核采样阈值。用于决定结果随机性,取值越高随机性越强即相同的问题得到的不同答案的可能性越高 非必传,取值为[0,1],默认为0.5
//                .temperature(0.2)
//// 指定请求版本，默认使用最新2.0版本
//                .apiVersion(SparkApiVersion.V3_0)
//                .build();
//        String[] splits=null;
//
//        try {
//            // 同步调用
//            SparkSyncChatResponse chatResponse = sparkClient.chatSync(sparkRequest);
//            SparkTextUsage textUsage = chatResponse.getTextUsage();
//            String result = chatResponse.getContent();
//            splits = result.split("【【【【【");
//            if(splits.length<3){
//                System.out.println(splits.length);
//                channel.basicNack(deliveryTag, false, false);
//                handleChartUpdateError(chart.getId(), "AI 生成错误");
//                handleRedisChartUpdateError(redisChart);
//                return;
//            }
//            System.out.println("\n回答：" + chatResponse.getContent());
//        } catch (SparkException e) {
//            System.out.println("发生异常了：" + e.getMessage());
//            channel.basicNack(deliveryTag, false, false);
//            handleChartUpdateError(chart.getId(), "AI 生成错误");
//            handleRedisChartUpdateError(redisChart);
//            return;
//
//        }

        // 调用鱼聪明 AI

        String result = aiManager.doChat(CommonConstant.BI_MODEL_ID, buildUserInput(chart));
        System.out.println(result);
        String[] splits = result.split("【【【【【");
        if (splits.length < 3) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chart.getId(), "AI 生成错误");
            handleRedisChartUpdateError(redisChart);
            return;
        }
        String json=splits[1].trim();
        String genChart=json.substring(json.indexOf('{'), json.lastIndexOf('}') + 1);
        System.out.println(genChart);
//        String genChart=splits[1].trim();
        String genResult = splits[2].trim();
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(genResult);
        // todo 建议定义状态为枚举值
        updateChartResult.setStatus("succeed");
        boolean updateResult = chartService.updateById(updateChartResult);
        chartScore = redisTemplate.opsForZSet().score(chart.getUserId().toString(), redisChart);
        redisChart = (Chart) redisTemplate.opsForZSet().rangeByScore(chart.getUserId().toString(), chartScore, chartScore).iterator().next();
        //删除redis中的数据
        redisTemplate.opsForZSet().removeRangeByScore(chart.getUserId().toString(), chartScore, chartScore);
        redisChart.setStatus("succeed");
        redisChart.setGenChart(genChart);
        redisChart.setGenResult(genResult);
        if (!updateResult) {
            channel.basicNack(deliveryTag, false, false);
            handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
            handleRedisChartUpdateError(redisChart);
            return;
        }
        Boolean bool=redisTemplate.opsForZSet().add(chart.getUserId().toString(), redisChart, Instant.now().toEpochMilli());
        if(Boolean.FALSE.equals(bool)){
            log.error("更新redis中图表状态失败"+redisChart.getId());
        }
        // 消息确认
        channel.basicAck(deliveryTag, false);
    }

    /**
     * 构建用户输入
     * @param chart
     * @return
     */
    private String buildUserInput(Chart chart) {
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();

        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        userInput.append(csvData).append("\n");
        return userInput.toString();
    }
    private List<SparkMessage> buildXunFeiUserInput(Chart chart){
        String goal = chart.getGoal();
        String chartType = chart.getChartType();
        String csvData = chart.getChartData();
        final String prompt = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
                "分析需求：\n" +
                "{数据分析的需求或者目标}\n" +
                "原始数据：\n" +
                "{csv格式的原始数据，用,作为分隔符}\n" +
                "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
                "【【【【【\n" +
                "{前端 Echarts V5 的 option 配置对象Js的JSON格式的代码,合理地将数据进行可视化。此外，不要生成任何多余的东西，比如注释和】】】】】}\n" +
                "【【【【【\n" +
                "{明确的数据分析结论、越详细越好，不要生成多余的注释}";
//        String question="分析网站用户增长量\n"+"日期,增长量\n" +
//                "1号,20\n" +
//                "2号,40\t\n" +
//                "3号,30\n" +
//                "4号,50\n" +
//                "5号,100";
        // 构造用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据：").append("\n");
        userInput.append(csvData).append("\n");

        // 消息列表，可以在此列表添加历史对话记录
        List<SparkMessage> messages=new ArrayList<>();
        messages.add(SparkMessage.systemContent(prompt));
        messages.add(SparkMessage.userContent(userInput.toString()));
        return messages;
    }

    private void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus("failed");
        updateChartResult.setExecMessage("execMessage");
        boolean updateResult = chartService.updateById(updateChartResult);
        if (!updateResult) {
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
        }
    }
    private void handleRedisChartUpdateError(Chart chart){
        Double chartScore = redisTemplate.opsForZSet().score(chart.getUserId().toString(), chart);
        Chart redisChart = (Chart) redisTemplate.opsForZSet().rangeByScore(chart.getUserId().toString(), chartScore, chartScore).iterator().next();
        redisChart.setStatus("failed");
        Long l = redisTemplate.opsForZSet().removeRangeByScore(chart.getUserId().toString(), chartScore, chartScore);
        Boolean redisBool = redisTemplate.opsForZSet().add(chart.getUserId().toString(), redisChart, Instant.now().toEpochMilli());
        if(Boolean.FALSE.equals(redisBool)){
            log.error("更新redis中图表状态失败"+chart.getId());
        }
    }

}
