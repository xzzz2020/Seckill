package cn.xzzz2020.miaosha.web.service;

import cn.xzzz2020.miaosha.web.dao.OrderDao;
import cn.xzzz2020.miaosha.domain.MiaoshaOrder;
import cn.xzzz2020.miaosha.domain.MiaoshaUser;
import cn.xzzz2020.miaosha.domain.OrderInfo;
import cn.xzzz2020.miaosha.redis.RedisService;
import cn.xzzz2020.miaosha.redis.key.OrderKey;
import cn.xzzz2020.miaosha.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

@Service
public class OrderService {

    @Resource
    OrderDao orderDao;

    @Autowired
    RedisService redisService;


    public OrderInfo getOrderById(long orderId) {
        return orderDao.getOrderById(orderId);
    }

    /**
     * 从缓存获取
     * @param userId
     * @param goodsId
     * @return
     */
    public MiaoshaOrder getMiaoshaOrderByUserIdGoodsId(Long userId, long goodsId) {
        return redisService.get(OrderKey.getMiaoshaOrederByUidGid,":"+userId+"_"+goodsId,MiaoshaOrder.class);
    }

    @Transactional
    public OrderInfo creatOrder(MiaoshaUser user, GoodsVo goods) {
        OrderInfo oi = new OrderInfo();
        oi.setCreateDate(new Date());
        oi.setDeliveryAddrId(0L);
        oi.setGoodsCount(1);
        oi.setGoodsId(goods.getId());
        oi.setGoodsName(goods.getGoodsName());
        oi.setGoodsPrice(goods.getMiaoshaPrice());
        oi.setOrderChannel(1);
        oi.setStatus(0);
        oi.setUserId(user.getId());
        orderDao.insetOrder(oi);

        long orderId = oi.getId();


        MiaoshaOrder miaoshaOrder = new MiaoshaOrder();
        miaoshaOrder.setGoodsId(goods.getId());
        miaoshaOrder.setOrderId(orderId);
        miaoshaOrder.setUserId(user.getId());
        orderDao.insetMiaoshaOrder(miaoshaOrder);


        redisService.set(OrderKey.getMiaoshaOrederByUidGid,":"+user.getId()+"_"+goods.getId(),miaoshaOrder);

        return oi;
    }

    @Transactional
    public void deleteOrders() {
        orderDao.deleteMiaoshaOrders();
        orderDao.deleteOrders();
    }


}
