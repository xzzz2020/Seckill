package cn.xzzz2020.miaosha.web.controller;


import cn.xzzz2020.miaosha.domain.MiaoshaUser;
import cn.xzzz2020.miaosha.redis.key.GoodsKey;
import cn.xzzz2020.miaosha.redis.RedisService;
import cn.xzzz2020.miaosha.result.Result;
import cn.xzzz2020.miaosha.web.access.AccessLimit;
import cn.xzzz2020.miaosha.web.service.GoodsService;
import cn.xzzz2020.miaosha.vo.GoodsDetailVo;
import cn.xzzz2020.miaosha.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.spring4.context.SpringWebContext;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 商品列表展示页面：
 * <p>
 * 主要有两个页面：1.展示商品  2.展示商品详细信息
 * <p>
 * 页面层进行了两个高并发的优化：1.页面缓存 2.URL缓存
 * 需要注意的是，这两个缓存的有效期时间都很短，一般在一分钟，这是为了防止页面出现变化
 * <p>
 * 一、页面缓存
 * 主要是页面先从缓存中取，如果取出直接展示到客户端
 * 如果没有取到，则去访问数据库（mysql），获取数据
 * 接着对页面进行渲染，保存在缓存中（Redis）
 * 最后返回到客户端
 * <p>
 * 二、URL缓存
 * 与页面缓存的区别在于，每个商品都有个商品详情页，所以对缓存需要加上GoodsId区分
 */

@RequestMapping("/goods")
@Controller
public class GoodsController {


    @Autowired
    private GoodsService goodsService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ThymeleafViewResolver viewResolver;

    @Autowired
    private ApplicationContext context;

    /*
        未优化：584.8
        优化后：2085.9
     */

    @RequestMapping(value = "/to_list", produces = "text/html")
    @ResponseBody
    public String toGoods(Model model, MiaoshaUser user,
                          final HttpServletRequest request,
                          final HttpServletResponse response) {

        //取缓存
        String html;
        html = redisService.get(GoodsKey.getGoodsList(), "", String.class);
        if (html != null) {//如果缓存有这个页面
            return html;
        } else {//如果没有这个页面
            //访问数据库获取商品数据
            List<GoodsVo> goodsList = goodsService.listGoodsVo();
            if (user != null) {
                //如果有用户信息，则保存在Model中
                model.addAttribute("user", user);
            }
            //将商品数据保存在Model中
            model.addAttribute("goodsList", goodsList);

            //手动渲染
            SpringWebContext springWebContext = new SpringWebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap(), context);
            html = viewResolver.getTemplateEngine().process("goods_list", springWebContext);
            if (!StringUtils.isEmpty(html)) {
                //保存到缓存
                redisService.set(GoodsKey.getGoodsList(), "", html);
            }
            //返回到浏览器
            return html;
        }
//        return "goods_list";
    }


    @RequestMapping(value = "/to_detail2/{goodsId}", produces = "text/html")
    @ResponseBody
    public String toDetail2(Model model, MiaoshaUser user,
                            @PathVariable("goodsId") long goodsId,
                            final HttpServletRequest request,
                            final HttpServletResponse response) {

        //取缓存
        String html;
        html = redisService.get(GoodsKey.getGoodsDetail(), "", String.class);
        if (html != null) {
            return html;
        } else {
            GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
            if (user != null) {
                model.addAttribute("user", user);
            }
            model.addAttribute("goods", goods);

            long startTime = goods.getStartDate().getTime();
            long endTime = goods.getEndDate().getTime();
            long nowTime = System.currentTimeMillis();
            int miaoshaStatus = 0;

            long remainSeconds = 0;

            if (nowTime < startTime) {//秒杀没开始，倒计时
                remainSeconds = (startTime - nowTime) / 1000;
            } else if (nowTime > endTime) {//秒杀结束
                miaoshaStatus = 2;
                remainSeconds = -1;
            } else {//秒杀进行中
                miaoshaStatus = 1;
            }
            model.addAttribute("miaoshaStatus", miaoshaStatus);
            model.addAttribute("remainSeconds", remainSeconds);
            //手动渲染
            SpringWebContext springWebContext = new SpringWebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap(), context);
            html = viewResolver.getTemplateEngine().process("goods_detail", springWebContext);
            if (!StringUtils.isEmpty(html)) {
                //保存到缓存
                redisService.set(GoodsKey.getGoodsDetail(), ":" + goodsId, html);
            }
            return html;
        }
        //return "goods_detail";
    }


    /**
     * 升级为商品详情静态化
     *
     * @param user
     * @param goodsId
     * @return
     */
    @RequestMapping(value = "/to_detail/{goodsId}")
    @ResponseBody
    public Result<GoodsDetailVo> toDetail(MiaoshaUser user, @PathVariable("goodsId") long goodsId) {


        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);


        long startTime = goods.getStartDate().getTime();
        long endTime = goods.getEndDate().getTime();
        long nowTime = System.currentTimeMillis();
        int miaoshaStatus = 0;

        long remainSeconds = 0;

        if (nowTime < startTime) {//秒杀没开始，倒计时
            remainSeconds = (startTime - nowTime) / 1000;
        } else if (nowTime > endTime) {//秒杀结束
            miaoshaStatus = 2;
            remainSeconds = -1;
        } else {//秒杀进行中
            miaoshaStatus = 1;
        }


        GoodsDetailVo goodsDetailVo  = new GoodsDetailVo();
        goodsDetailVo.setGoods(goods);
        goodsDetailVo.setMiaoshaStatus(miaoshaStatus);
        goodsDetailVo.setMiaoshaUser(user);
        goodsDetailVo.setRemainSeconds(remainSeconds);

        return Result.success(goodsDetailVo);

    }


}

