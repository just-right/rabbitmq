package com.example.rabbitmq.config;

/**
 * @className: IRabbitMQConst
 * @description
 * @author: luffy
 * @date: 2020/5/11 23:40
 * @version:V1.0
 */
public interface IRabbitMQConst {
    String DELAY_QUEUE = "delay.queue";
    String DEAD_LETTER_EXCHANGE_ARGUMENT = "x-dead-letter-exchange";
    String DEAD_LETTER_EXCHANGE = "dead.letter.exchange";
    String DEAD_LETTER_ROUTEKEY_ARGUMENT = "x-dead-letter-routing-key";
    String DEAD_LETTER_ROUTEKEY = "dead.letter.routeKey";
    String MESSAGE_TTL_ARGUMENT = "x-message-ttl";
    Long EXPIRE_TIME = 10000L;
    String DELAY_ROUTEKEY = "delay.*";
    String DELAY_EXCHANGE = "delay.exchange";
    String DEAD_LETTER_QUEUE = "dead.letter.queue";
}
