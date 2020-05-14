package com.example.rabbitmq.controller;

import com.alibaba.fastjson.JSONObject;
import com.example.rabbitmq.config.IRabbitMQConst;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @className: RabbitMQTestController
 * @description
 * @author: luffy
 * @date: 2020/5/11 23:26
 * @version:V1.0
 */
@RestController
@RequestMapping("/test")
public class RabbitMQTestController {
    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private RedisTemplate<String,String> redisTemplate;

    @GetMapping(value = "ack")
    public void ackTest() {
        rabbitTemplate.convertAndSend("queue1", "hello world!");
    }

    @GetMapping(value = "delay")
    public void delayTest() {
        System.out.println("消息已发出：" + LocalDateTime.now().toString());
        rabbitTemplate.convertAndSend(IRabbitMQConst.DELAY_EXCHANGE, IRabbitMQConst.DELAY_ROUTEKEY, "hello world!", message -> {
            // 设置延迟毫秒值 -- 已消息和队列设置的最小值为准
            message.getMessageProperties().setExpiration(String.valueOf(20000L));
            return message;
        });

    }

    @GetMapping(value = "delay2")
    public void delayTest2() {
        /**
         * 由于队列的先进先出特性，只有当过期的消息到了队列的顶端（队首），才会被真正的丢弃或者进入死信队列。
         * 所以当我们设置了一个较短的消息超时时间，但是因为他之前有队列尚未完结。此消息依旧不会进入死信。
         */
        System.out.println("消息已发出2：" + LocalDateTime.now().toString());
        rabbitTemplate.convertAndSend(IRabbitMQConst.DELAY_EXCHANGE, IRabbitMQConst.DELAY_ROUTEKEY, "hello world!", message -> {
            // 设置延迟毫秒值 -- 已消息和队列设置的最小值为准
            message.getMessageProperties().setExpiration(String.valueOf(2000L));
            return message;
        });
    }

    @GetMapping(value = "retry")
    public void retryTest() {
        String correlationDataId = UUID.randomUUID().toString().replaceAll("-", "");
        MessageConverter converter = rabbitTemplate.getMessageConverter();
        MessageProperties messageProperties = new MessageProperties();
        Message message = converter
                .toMessage("hello world!", messageProperties);
        String messageKey = "correlation:id:" + correlationDataId;
        redisTemplate.opsForValue().set(messageKey, JSONObject.toJSONString(message));
        rabbitTemplate.convertAndSend("workExchange", "work.topic.queue", message, new CorrelationData(correlationDataId));
    }

}
