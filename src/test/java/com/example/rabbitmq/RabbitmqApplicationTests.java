package com.example.rabbitmq;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.time.LocalDateTime;


@SpringBootTest
class RabbitmqApplicationTests {

    @Resource
    private RabbitTemplate rabbitTemplate;

    //Direct
//    @Test
//    void contextLoads() {
//        rabbitTemplate.convertAndSend("queue1", "hello world!");
//
//    }

    //Fanout
//    @Test
//    void contextLoads2() {
//        rabbitTemplate.convertAndSend("fanoutExchange", "", "just right!");
//    }

    //Topic
//    @Test
//    void contextLoads3() {
//        rabbitTemplate.convertAndSend("topicExchange", "topic.call", "topic.call!");
//    }

    //Topic
//    @Test
//    void contextLoads4() {
//        rabbitTemplate.convertAndSend("topicExchange", "topic.match", "topic.match!");
//    }

    //延迟队列
//    @Test
//    void contextLoads5() {
//        System.out.println(LocalDateTime.now());
//        rabbitTemplate.convertAndSend("delay.exchange", "delay.routeKey","hello world!");
//    }

}
