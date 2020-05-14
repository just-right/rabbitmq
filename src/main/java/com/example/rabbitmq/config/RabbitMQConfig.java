package com.example.rabbitmq.config;

import com.alibaba.fastjson.JSONObject;
import com.example.rabbitmq.schedule.ScheduleTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

/**
 * @className: RabbitMQConfig
 * @description
 * @author: luffy
 * @date: 2020/5/9 10:44
 * @version:V1.0
 */
@Configuration
public class RabbitMQConfig {
    //自定义序列化
    @Bean
    public MessageConverter getMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(ConnectionFactory connectionFactory){
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }
    @Bean
    public RabbitTemplate getRabbitTemplate(ConnectionFactory connectionFactor,RedisTemplate<String,String> redisTemplate) {
        Logger logger = LoggerFactory.getLogger(RabbitTemplate.class);
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactor);
        // 消息发送失败返回到队列中, yml需要配置 publisher-returns: true
        rabbitTemplate.setMandatory(true);

        // 消息发送失败返回到队列中, yml需要配置 publisher-returns: true
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            String correlationId = message.getMessageProperties().getCorrelationId();
            logger.info("消息：{} 发送失败, 应答码：{} 原因：{} 交换机: {}  路由键: {}", correlationId, replyCode, replyText, exchange, routingKey);
        });

        // 消息确认-发送到Exchange, yml需要配置 publisher-confirms: true
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (correlationData != null) {
                String messageKey = "correlation:id:" + correlationData.getId();
                if (redisTemplate.hasKey(messageKey)) {
                    //获取缓存数据
                    String msgInfo = redisTemplate.opsForValue().get(messageKey);
                    Message message = JSONObject.parseObject(msgInfo, Message.class);
                    assert message != null;
                    Map<String, Object> headersInfo = message.getMessageProperties().getHeaders();
                    if (!headersInfo.containsKey("success_send_exchange") || !(boolean) headersInfo.get("success_send_exchange")) {
                        if (ack) {
                            headersInfo.put("success_send_exchange",true);
                            //更新缓存
                            redisTemplate.opsForValue().set(messageKey,JSONObject.toJSONString(message));
                            //更新失败记录数据库信息 ----
                            logger.info("消息发送到exchange成功");
                        }else{
                            ScheduleTask.messageDeal( messageKey, message, headersInfo, rabbitTemplate);
                        }
                    }
                }
            }
        });

        return rabbitTemplate;
    }


    @Bean
    public Queue queue1() {
        return new Queue("queue1");
    }

    @Bean
    public Queue queue2() {
        return new Queue("queue2");
    }

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange("directExchange");
    }

    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange("fanoutExchange");
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange("topicExchange");
    }


    //queue1和queue2绑定fanoutExchange
    @Bean
    public Binding bindingExchangeAndQueue1(Queue queue1, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(queue1).to(fanoutExchange);
    }

    @Bean
    public Binding bindingExchangeAndQueue2(Queue queue2, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(queue2).to(fanoutExchange);
    }

    //queue1和queue2绑定topicExchange
    @Bean
    public Binding bindingExchangeAndQueue3(@Qualifier("queue1") Queue queue1, TopicExchange topicExchange) {
        return BindingBuilder.bind(queue1).to(topicExchange).with("topic.match");
    }

    //#匹配0个或多个 *匹配一个
    @Bean
    public Binding bindingExchangeAndQueue4(@Qualifier("queue2") Queue queue2, TopicExchange topicExchange) {
        return BindingBuilder.bind(queue2).to(topicExchange).with("topic.#");
    }

    //延迟队列
    @Bean
    public Queue delayQueue() {
        return QueueBuilder.durable(IRabbitMQConst.DELAY_QUEUE)
                // DLX，dead letter发送到的exchange ,设置死信队列交换器到处理交换器
                .withArgument(IRabbitMQConst.DEAD_LETTER_EXCHANGE_ARGUMENT, IRabbitMQConst.DEAD_LETTER_EXCHANGE)
                // dead letter携带的routing key，配置处理队列的路由key
                .withArgument(IRabbitMQConst.DEAD_LETTER_ROUTEKEY_ARGUMENT, IRabbitMQConst.DEAD_LETTER_ROUTEKEY)
                // 设置过期时间
                .withArgument(IRabbitMQConst.MESSAGE_TTL_ARGUMENT, IRabbitMQConst.EXPIRE_TIME)
                .build();
    }

    //延迟交换器-主题
    @Bean
    public TopicExchange delayExchange() {
        return new TopicExchange(IRabbitMQConst.DELAY_EXCHANGE);
    }

    //延迟绑定
    @Bean
    public Binding delayBinding(Queue delayQueue, TopicExchange delayExchange) {
        return BindingBuilder.bind(delayQueue).to(delayExchange).with(IRabbitMQConst.DELAY_ROUTEKEY);
    }

    //死信队列
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(IRabbitMQConst.DEAD_LETTER_QUEUE).build();
    }

    //死信交换器-主题
    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(IRabbitMQConst.DEAD_LETTER_EXCHANGE);
    }

    //死信绑定
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(IRabbitMQConst.DEAD_LETTER_ROUTEKEY);
    }

    //工作队列
    @Bean
    public Queue workQueue() {
        return new Queue("workQueue");
    }

    //工作交换器
    @Bean
    public TopicExchange workExchange() {
        return new TopicExchange("workExchange");
    }

    //工作绑定
    @Bean
    public Binding topicQueueBinding(Queue workQueue, TopicExchange workExchange) {
        return BindingBuilder.bind(workQueue).to(workExchange).with("work.topic.*");
    }

    //重发队列
    @Bean
    public Queue retryQueue() {
        return QueueBuilder.durable("retryQueue")
                // DLX，dead letter发送到的exchange ,设置死信队列交换器到处理交换器
                .withArgument(IRabbitMQConst.DEAD_LETTER_EXCHANGE_ARGUMENT, "workExchange")
                // dead letter携带的routing key，配置处理队列的路由key
                .withArgument(IRabbitMQConst.DEAD_LETTER_ROUTEKEY_ARGUMENT, "work.topic.retry")
                // 设置过期时间
                .withArgument(IRabbitMQConst.MESSAGE_TTL_ARGUMENT, 3000L)
                .build();
    }

    //重发交换器
    @Bean
    public TopicExchange retryExchange() {
        return ExchangeBuilder.topicExchange("retryExchange").durable(true).build();
    }

    //重发绑定
    @Bean
    public Binding retryDirectBinding(@Qualifier("retryQueue") Queue retryQueue,
                                      @Qualifier("retryExchange") TopicExchange retryExchange) {
        return BindingBuilder.bind(retryQueue).to(retryExchange).with("work.retry.*");
    }

    //失败队列
    @Bean
    public Queue failQueue() {
        return QueueBuilder.durable("failQueue").build();
    }

    //失败交换器
    @Bean
    public TopicExchange failExchange() {
        return ExchangeBuilder.topicExchange("failExchange").durable(true).build();
    }

    //失败绑定
    @Bean
    public Binding failDirectBinding(@Qualifier("failQueue") Queue failQueue,
                                     @Qualifier("failExchange") TopicExchange failExchange) {
        return BindingBuilder.bind(failQueue).to(failExchange).with("work.fail.*");
    }




}
