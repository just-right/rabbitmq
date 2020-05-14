package com.example.rabbitmq.schedule;

import com.alibaba.fastjson.JSONObject;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;

/**
 * @className: ScheduleTask
 * @description
 * @author: luffy
 * @date: 2020/5/13 18:06
 * @version:V1.0
 */
@Component
public class ScheduleTask {

    private static RedisTemplate<String,String> redisTemplate1;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private RedisTemplate<String,String> redisTemplate2;

    @PostConstruct
    private void init() {
        redisTemplate1 = redisTemplate2;
    }


    @Scheduled(fixedRate = 5000)
    public void rabbitMQtTask() {
        //获取消息键值
        Set<String> keys = redisTemplate1.keys("correlation:id:" + "*");

        assert keys != null;
        keys.forEach(key -> {
            String msgInfo = redisTemplate1.opsForValue().get(key);
            Message message = JSONObject.parseObject(msgInfo, Message.class);
            assert message != null;
            Map<String, Object> headersInfo = message.getMessageProperties().getHeaders();
            if (!headersInfo.containsKey("success_send_exchange") || !(boolean) headersInfo.get("success_send_exchange")) {
                messageDeal(key, message, headersInfo, rabbitTemplate);
            }
        });
    }

    public static void messageDeal(String key, Message message, Map<String, Object> headersInfo, RabbitTemplate rabbitTemplate) {
        if (!headersInfo.containsKey("success_send_exchange")) {
            headersInfo.put("success_send_exchange", false);
        }
        int count = headersInfo.containsKey("fail_count_exchange") ? (int) headersInfo.get("fail_count_exchange") : 0;
        if (count >= 3) {
            //删除缓存-失败队列
            redisTemplate1.delete(key);
            rabbitTemplate.convertAndSend("failExchange", "work.fail.queue", message);
            return;
        }
        headersInfo.put("fail_count_exchange", ++count);
        redisTemplate1.opsForValue().set(key, JSONObject.toJSONString(message));
        rabbitTemplate.convertAndSend("retryExchange", "work.retry.queue", message);
    }
}
