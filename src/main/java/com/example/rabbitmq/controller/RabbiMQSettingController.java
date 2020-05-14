package com.example.rabbitmq.controller;

import org.springframework.amqp.core.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @className: RabbiMQSettingController
 * @description
 * @author: luffy
 * @date: 2020/5/9 13:16
 * @version:V1.0
 */
@RestController
@RequestMapping("setting")
public class RabbiMQSettingController {
    @Resource
    private AmqpAdmin amqpAdmin;

    @PostMapping(value = "addQueue")
    public String addQueue(@RequestParam(value = "name") String queueName) {
        return amqpAdmin.declareQueue(new Queue(queueName));
    }

    @PostMapping(value = "addExchange")
    public boolean addExchange(@RequestParam(value = "name") String exchangeName, @RequestParam(value = "type") Integer exchangeType) {
        Exchange exchange = null;
        switch (exchangeType) {
            case 1:
                exchange = new DirectExchange(exchangeName);
                break;
            case 2:
                exchange = new FanoutExchange(exchangeName);
                break;
            case 3:
                exchange = new TopicExchange(exchangeName);
                break;
            default:
                break;
        }
        amqpAdmin.declareExchange(exchange);
        return true;
    }

    @PostMapping(value = "addFanoutBinding")
    public boolean addFanoutBinding(@RequestParam(value = "queueName") String queueName, @RequestParam(value = "fanoutExchangeName") String fanoutExchangeName) {
        amqpAdmin.declareBinding(new Binding(queueName,
                Binding.DestinationType.QUEUE, fanoutExchangeName, null, null));
        return true;
    }

    @PostMapping(value = "addTopicBinding")
    public boolean addTopicBinding(@RequestParam(value = "queueName") String queueName, @RequestParam(value = "topicExchangeName") String topicExchangeName, @RequestParam(value = "routingKey") String routingKey) {
        amqpAdmin.declareBinding(new Binding(queueName,
                Binding.DestinationType.QUEUE, topicExchangeName, routingKey, null));
        return true;
    }
}
