package com.example.rabbitmq.service;

import com.alibaba.fastjson.JSONObject;
import com.example.rabbitmq.config.IRabbitMQConst;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.utils.SerializationUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @className: ReceiveMsgService
 * @description
 * @author: luffy
 * @date: 2020/5/9 10:48
 * @version:V1.0
 */
@Service
public class ReceiveMsgService {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private RedisTemplate<String,String> redisTemplate;

    //@RabbitHandler
    @RabbitListener(queues = "queue1")
    public void getMsg(String msg, Message message, Channel channel) throws IOException {
        try {
            //false只确认当前一个消息收到，true确认所有consumer获得的消息
            logger.info(msg);
            int a = 1 / 0;
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            //ack返回false，并重新回到队列--注意次数限制 -- 最后一个参数为false则摒弃
            logger.info("消息接收失败，重发！");
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            //拒绝消息
//            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
//
//    @RabbitListener(queues = IRabbitMQConst.DEAD_LETTER_QUEUE)
//    public void getMsg2(String msg, Message message, Channel channel) throws IOException {
//        logger.info("消息已收到：" + LocalDateTime.now().toString());
//        try {
//            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
//        } catch (IOException e) {
//            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
//        }
//    }

    @RabbitListener(queues = "workQueue")
    public void getMsg3(Message message, Channel channel) throws IOException {
        logger.info("消息已收到：" + LocalDateTime.now().toString());
        String correlationDataId = (String) message.getMessageProperties().getHeaders().get("spring_returned_message_correlation");
        String messageKey = "correlation:id:" + correlationDataId;
        try {
            if (!redisTemplate.hasKey(messageKey)) {
                return;
            }
            String msg = (String) SerializationUtils.deserialize(message.getBody());
            logger.info("消费消息:" + msg);
            //模拟异常
            int a = 1 / 0;
            //清除缓存
            redisTemplate.delete(messageKey);
            //更新数据库信息 ----
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            long retryCount = getRetryCount(message.getMessageProperties());
            if (retryCount >= 3) {
                //清除缓存
                redisTemplate.delete(messageKey);
                logger.info("重发超过三次，发送到失败队列！");
                rabbitTemplate.convertAndSend("failExchange", "work.fail.queue", message);
            } else {
                logger.info("第" + (retryCount + 1) + "次重发！");
                //更新缓存
                redisTemplate.opsForValue().set(messageKey, JSONObject.toJSONString(message));
                String msgInfo = redisTemplate.opsForValue().get(messageKey);
                Message tmpMessage = JSONObject.parseObject(msgInfo, Message.class);
                logger.info("获取缓存数据:"+tmpMessage);

                rabbitTemplate.convertAndSend("retryExchange", "work.retry.queue", message);
            }
        } finally {
            //避免死循环
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    @RabbitListener(queues = "failQueue")
    public void getMsg4(Message message, Channel channel) throws IOException {
        String correlationDataId = (String) message.getMessageProperties().getHeaders()
                .get("spring_returned_message_correlation");
        String messageKey = "correlation:id:" + correlationDataId;
        try {
            //幂等处理
            if (!redisTemplate.hasKey(messageKey)) {
                logger.info("不存在该消息");
                return;
            }
            //插入失败记录数据库信息 ----
            logger.info("插入失败记录数据库信息");
        }finally {
        }
    }

    /**
     * 获取消息被重试的次数
     */
    private long getRetryCount(MessageProperties messageProperties) {
        Long retryCount = 0L;
        if (null != messageProperties) {
            List<Map<String, ?>> deaths = messageProperties.getXDeathHeader();
            if (deaths != null && deaths.size() > 0) {
                Map<String, Object> death = (Map<String, Object>) deaths.get(0);
                retryCount = (Long) death.get("count");
            }
        }
        return retryCount;
    }
}
