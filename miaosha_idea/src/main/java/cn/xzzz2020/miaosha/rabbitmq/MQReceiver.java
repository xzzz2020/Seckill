package cn.xzzz2020.miaosha.rabbitmq;

import cn.xzzz2020.miaosha.domain.MiaoshaUser;
import cn.xzzz2020.miaosha.domain.OrderInfo;
import cn.xzzz2020.miaosha.result.CodeMsg;
import cn.xzzz2020.miaosha.result.Result;
import cn.xzzz2020.miaosha.web.service.GoodsService;
import cn.xzzz2020.miaosha.web.service.MiaoshaService;
import cn.xzzz2020.miaosha.web.service.OrderService;
import cn.xzzz2020.miaosha.util.*;
import cn.xzzz2020.miaosha.rabbitmq.config.MQConfig;
import cn.xzzz2020.miaosha.vo.GoodsVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 接收者
 */
@Service
public class MQReceiver {

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    MiaoshaService miaoshaService;
    private static Logger logger = LoggerFactory.getLogger(MQReceiver.class);

    @RabbitListener(queues = MQConfig.MIAOSHA_QUEUE)
    public void miaoshaReceive(String message) {
        //将消息反序列化
        MiaoshaMessage miaoshaMessage = SerializableUtil.stringToBean(message, MiaoshaMessage.class);
        //获取用户
        MiaoshaUser user = miaoshaMessage.getUser();
        //获取用户id
        long goodsId = miaoshaMessage.getGoodsId();
        //再次判断库存是否足够
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        Integer stockCount = goods.getStockCount();
        //如果库存不足，则直接返回
        if (stockCount <= 0) {
            return;
        }
        //减库存 下订单 写入订单 一个事务中
        miaoshaService.miaosha(user, goods);
    }

//

//
//    @RabbitListener(queues = MQConfig.QUEUE_DIRECT)
//    public void receive(String message){
//        logger.info("receive message : "+ message);
//
//    }
//
//
//
//    @RabbitListener(queues = MQConfig.QUEUE_TOPIC_1)
//    public void receiveTopic1(String message){
//        logger.info("receive message : "+ message);
//
//    }
//
//    @RabbitListener(queues = MQConfig.QUEUE_TOPIC_2)
//    public void receiveTopic2(String message){
//        logger.info("receive message : "+ message);
//
//    }
//
//
//    @RabbitListener(queues = MQConfig.QUEUE_Header)
//    public void receiveHeader(byte [] message){
//        logger.info("receive message : "+ new String(message));
//
//    }
}
