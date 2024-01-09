package com.yupi.springbootinit.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 用于创建测试程序用到的交换机和队列（只用在程序启动前执行一次）
 */
public class BiInitMain {

    public static void main(String[] args) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("192.168.88.129");
            factory.setPort(5672);
            factory.setVirtualHost("/");
            factory.setUsername("itcast");
            factory.setPassword("123321");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            //创建交换机
            String EXCHANGE_NAME =  BiMqConstant.BI_EXCHANGE_NAME;
            channel.exchangeDeclare(EXCHANGE_NAME, "direct",true);
            //创建死信交换机
            String TTLEXCHANGE_NAME=BiMqConstant.BI_TTL_EXCHANGE_NAME;
            channel.exchangeDeclare(TTLEXCHANGE_NAME,"fanout",true);
            //创建死信队列
            String ttlQueueName=BiMqConstant.BI_TTL_QUEUE_NAME;
            channel.queueDeclare(ttlQueueName,true,false,false,null);
            channel.queueBind(ttlQueueName,TTLEXCHANGE_NAME,"");
            // 创建队列，随机分配一个队列名称
            String queueName = BiMqConstant.BI_QUEUE_NAME;
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("x-dead-letter-exchange", TTLEXCHANGE_NAME); // 死信消息发送到的交换机
            arguments.put("x-dead-letter-routing-key", ""); // 死信消息的路由键
            channel.queueDeclare(queueName, true, false, false, arguments);
            channel.queueBind(queueName, EXCHANGE_NAME,  BiMqConstant.BI_ROUTING_KEY);
        } catch (Exception e) {

        }

    }
}
