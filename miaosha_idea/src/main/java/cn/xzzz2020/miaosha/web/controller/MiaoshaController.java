package cn.xzzz2020.miaosha.web.controller;


import cn.xzzz2020.miaosha.domain.MiaoshaOrder;
import cn.xzzz2020.miaosha.domain.MiaoshaUser;
import cn.xzzz2020.miaosha.exception.GlobalException;
import cn.xzzz2020.miaosha.rabbitmq.MQSender;
import cn.xzzz2020.miaosha.rabbitmq.MiaoshaMessage;
import cn.xzzz2020.miaosha.redis.RedisService;
import cn.xzzz2020.miaosha.redis.key.GoodsKey;
import cn.xzzz2020.miaosha.redis.key.MiaoshaKey;
import cn.xzzz2020.miaosha.redis.key.MiaoshaUserKey;
import cn.xzzz2020.miaosha.redis.key.OrderKey;
import cn.xzzz2020.miaosha.redis.key.prefix.AccessKey;
import cn.xzzz2020.miaosha.result.CodeMsg;
import cn.xzzz2020.miaosha.result.Result;
import cn.xzzz2020.miaosha.util.MD5Util;
import cn.xzzz2020.miaosha.util.UUIDUtil;
import cn.xzzz2020.miaosha.web.access.AccessLimit;
import cn.xzzz2020.miaosha.web.service.GoodsService;
import cn.xzzz2020.miaosha.web.service.MiaoshaService;
import cn.xzzz2020.miaosha.web.service.MiaoshaUserService;
import cn.xzzz2020.miaosha.web.service.OrderService;
import cn.xzzz2020.miaosha.vo.GoodsVo;
import org.apache.http.HttpResponse;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;


@RequestMapping("/miaosha")
@Controller
public class MiaoshaController implements InitializingBean {


    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    MiaoshaService miaoshaService;

    @Autowired
    RedisService redisService;

    @Autowired
    MQSender mqSender;

    private HashMap<Long, Boolean> localOverMap = new HashMap<>();

    /**
     * get和post区别？
     * <p>
     * 1. get是幂等的，即只是从服务端获取数据，无论获取多少次，都不会改变数据
     * 2. Post是会更新服务端的数据
     */

    /*
        一、解决高并发情况下，导致的商品库存出现负数
        修改SQL语句，在减少库存的时候再一次判断是否库存足够
        MySQL在修改的时候，会自动加锁
        update miaosha_goods set stock_count = stock_count-1 where goods_id = #{goodsId} and stock_count >0
     */
    /*
        二、解决一个用户重复请求的导致生成多个订单的情况
        1.生成的订单表由两个，一个是正常的订单表，另一个是秒杀订单表
        只需要在秒杀订单表的user_id属性上建立唯一索引即可
        2.秒杀的时候利用验证码，防止出现重复秒杀的请求
     */

    /*
        未优化：351.8
        优化后：1793.8
     */
    @RequestMapping(value = "/{path}/do_miaosha", method = RequestMethod.POST)
    @ResponseBody
    public Result<Integer> do_miaosha(MiaoshaUser user,
                                      @RequestParam("goodsId") long goodsId,
                                      @PathVariable("path") String path) {

        //判断用户是否登录，如果没用登录，则传递提示信息
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        //隐藏了访问接口，需要验证path
        if (StringUtils.isEmpty(path)) {
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }
        boolean check = miaoshaService.checkPath(path, user.getId(), goodsId);
        if (!check) {
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }

        //优化后

        //判断是否秒杀到了
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
        //如果能够获取订单，说明该用户已经秒杀到商品
        if (order != null) {
            return Result.error(CodeMsg.REPEATE_MIAO_SHA);
        }
        //判断是否秒杀已经结束
        Boolean over = localOverMap.get(goodsId);
        if (over) {
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }

        //预减库存
        long stock = redisService.decr(GoodsKey.getMiaoGoodsStock(), ":" + goodsId);
        if (stock < 0) {//如果发现库存不足，则将秒杀结束的标记置成true
            localOverMap.put(goodsId, true);
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }

        //保存信息
        MiaoshaMessage miaoshaMessage = new MiaoshaMessage();
        miaoshaMessage.setGoodsId(goodsId);
        miaoshaMessage.setUser(user);
        //入队，实现异步下单
        mqSender.sendMiaoshaMessage(miaoshaMessage);
        //返回客户端订单处理种
        return Result.success(0);//排队中


        //未优化前

        /*
        //判断库存
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        Integer stockCount = goods.getStockCount();
        if (stockCount <= 0) {
            model.addAttribute("errmsg", CodeMsg.MIAO_SHA_OVER.getMsg());
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }

        //判断是否秒杀到了
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
        if (order != null) {
            model.addAttribute("errmsg", CodeMsg.REPEATE_MIAO_SHA.getMsg());
            return Result.error(CodeMsg.REPEATE_MIAO_SHA);
        }

        //减库存 下订单 写入订单 一个事务中
        OrderInfo orderInfo = miaoshaService.miaosha(user, goods);
        if(orderInfo==null){
            model.addAttribute("errmsg", CodeMsg.MIAO_SHA_OVER.getMsg());
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }
        model.addAttribute("orderInfo", orderInfo);
        model.addAttribute("goods", goods);
        return Result.success(orderInfo);
         */


    }

    /**
     * @param model
     * @param user
     * @param goodsId
     * @return 如果秒杀成功，返回订单id，如果秒杀失败返回-1，如果还未处理0
     */
    @RequestMapping(value = "/result", method = RequestMethod.GET)
    @ResponseBody
    public Result<Long> miaoshaResult(Model model, MiaoshaUser user, @RequestParam("goodsId") long goodsId) {
        if (user != null) {
            model.addAttribute("user", user);
        } else {
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        long result = miaoshaService.getMiaoshaResult(user.getId(), goodsId);

        return Result.success(result);
    }


    @AccessLimit(seconds = 5,maxCount = 5,needLogin = true)
    @RequestMapping(value = "/path", method = RequestMethod.POST)
    @ResponseBody
    public Result<String> miaoshaPath(Model model, MiaoshaUser user,
                                      HttpServletRequest request,
                                      @RequestParam("goodsId") long goodsId,
                                      @RequestParam(value="verifyCode", defaultValue="0")int verifyCode) {
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }



        boolean check  = miaoshaService.checkVerifyCode(user,goodsId,verifyCode);
        if (!check){
            return Result.error(CodeMsg.VERIFY_CODE_ERROR);
        }
        String path;
        path = MD5Util.md5(UUIDUtil.uuid()) + "123456";
        redisService.set(MiaoshaKey.getMiaoshaPath(), ":" + user.getId() + "_" + goodsId, path);
        return Result.success(path);
    }

    /**
     * 验证码
     *
     * @param user
     * @param goodsId
     * @return
     */

    @RequestMapping(value = "/verifyCode", method = RequestMethod.GET)
    @ResponseBody
    public Result<String> miaoshaVerifyCode(HttpServletResponse response, MiaoshaUser user,
                                            @RequestParam("goodsId") long goodsId) {
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        BufferedImage image = miaoshaService.creatVerifyCode(user,goodsId);
        try {
            ServletOutputStream os = response.getOutputStream();
            ImageIO.write(image,"JPEG",os);
            os.flush();
            os.close();
            return null;
        } catch (IOException e) {
            throw new GlobalException(CodeMsg.SERVER_ERROR);
        }
    }


    @RequestMapping(value = "/reset")
    @ResponseBody
    public Result<Boolean> reset() {
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        for (GoodsVo goods : goodsList) {
            goods.setStockCount(10);
            redisService.set(GoodsKey.getMiaoGoodsStock(), ":" + goods.getId(), 10);
            localOverMap.put(goods.getId(), false);
        }
        redisService.delete(OrderKey.getMiaoshaOrederByUidGid);
        redisService.delete(MiaoshaKey.getMiaoshaOver());
        miaoshaService.reset(goodsList);
        return Result.success(true);
    }


    /**
     * 系统进行初始化
     * 将库存信息放入缓存
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> goodsVos = goodsService.listGoodsVo();
        if (goodsVos != null && goodsVos.size() > 0) {
            for (GoodsVo goodsVo : goodsVos) {
                redisService.set(GoodsKey.getMiaoGoodsStock(), ":" + goodsVo.getId(), goodsVo.getStockCount());
                localOverMap.put(goodsVo.getId(), false);
            }
        }
    }
}
