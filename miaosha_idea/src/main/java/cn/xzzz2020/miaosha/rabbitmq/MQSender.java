package cn.xzzz2020.miaosha.rabbitmq;

import cn.xzzz2020.miaosha.rabbitmq.config.MQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cn.xzzz2020.miaosha.util.SerializableUtil;

/**
 * 发送者
 */
@Service
public class MQSender {

    @Autowired
    AmqpTemplate amqpTemplate;
    private static Logger logger = LoggerFactory.getLogger(MQSender.class);

    public void sendMiaoshaMessage(MiaoshaMessage miaoshaMessage) {
        //将数据序列化字符串
        String str = SerializableUtil.beanToString(miaoshaMessage);
        //发送消息
        amqpTemplate.convertAndSend(MQConfig.MIAOSHA_QUEUE,str);
    }



//
//    public void send(Object message){
//        String str = SerializableUtil.beanToString(message);
//        logger.info("send message : "+ message);
//        amqpTemplate.convertAndSend(MQConfig.QUEUE_DIRECT,str);
//    }
//
//
//    public void sendTopic(Object message){
//        String str = SerializableUtil.beanToString(message);
//        logger.info("send message : "+ message);
//        amqpTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE,"topic.key1",str+"1");
//        amqpTemplate.convertAndSend(MQConfig.TOPIC_EXCHANGE,"topic.key2",str+"2");
//    }
//
//    public void sendFanout(Object message){
//        String str = SerializableUtil.beanToString(message);
//        logger.info("send message : "+ message);
//        amqpTemplate.convertAndSend(MQConfig.FANOUT_EXCHANGE,"",str+"1");
//    }
//
//    public void sendHeader(Object message){
//        String str = SerializableUtil.beanToString(message);
//        logger.info("send message : "+ message);
//        MessageProperties messageProperties  =new MessageProperties() ;
//        /*
//        map.put("header1","value1");
//        map.put("header2","value2");
//         */
//        messageProperties.setHeader("header1","value1");
//        messageProperties.setHeader("header2","value2");
//
//        Message obj = new Message(str.getBytes(),messageProperties);
//        amqpTemplate.convertAndSend(MQConfig.HEADER_EXCHANGE,"",obj);
//    }


}
