package cn.xzzz2020.miaosha.web.controller;

import cn.xzzz2020.miaosha.domain.MiaoshaUser;
import cn.xzzz2020.miaosha.domain.OrderInfo;
import cn.xzzz2020.miaosha.result.CodeMsg;
import cn.xzzz2020.miaosha.result.Result;
import cn.xzzz2020.miaosha.web.service.GoodsService;
import cn.xzzz2020.miaosha.web.service.OrderService;
import cn.xzzz2020.miaosha.vo.GoodsVo;
import cn.xzzz2020.miaosha.vo.OrderDetailVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@ResponseBody
@RequestMapping(value = "/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private GoodsService goodsService;


    /*
    订单详情静态化
     */
    @RequestMapping(value = "/detail")
    public Result<OrderDetailVo> orderDetail(@RequestParam("orderId") long orderId,
                                             MiaoshaUser user){
        if (user==null){
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        OrderInfo orderInfo = orderService.getOrderById(orderId);
        if (orderInfo==null){
            return Result.error(CodeMsg.OREDER_NOT_EXIST);
        }

        long goodsId = orderInfo.getGoodsId();
        GoodsVo goodsVo = goodsService.getGoodsVoByGoodsId(goodsId);
        OrderDetailVo orderDetailVo = new OrderDetailVo();
        orderDetailVo.setGoodsVo(goodsVo);
        orderDetailVo.setOrderInfo(orderInfo);
        return Result.success(orderDetailVo);
    }
}
