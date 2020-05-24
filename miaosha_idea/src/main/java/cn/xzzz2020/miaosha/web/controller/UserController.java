package cn.xzzz2020.miaosha.web.controller;


import cn.xzzz2020.miaosha.domain.MiaoshaUser;
import cn.xzzz2020.miaosha.redis.RedisService;
import cn.xzzz2020.miaosha.redis.key.MiaoshaUserKey;
import cn.xzzz2020.miaosha.result.Result;
import cn.xzzz2020.miaosha.web.service.GoodsService;
import cn.xzzz2020.miaosha.web.service.MiaoshaUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;


@RequestMapping("/user")
@Controller
public class UserController {

    @Autowired
    private MiaoshaUserService miaoshaUserService;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    RedisService redisService;

    @ResponseBody
    @RequestMapping("/info")
    public Result<MiaoshaUser> info(MiaoshaUser user) throws Exception {
//        if (user==null){
//            //System.out.println(1);
//        }
        return Result.success(user);
    }


    @ResponseBody
    @RequestMapping("/aaa")
    public Result<MiaoshaUser> info(HttpServletResponse response,@RequestParam(value = MiaoshaUserService.COOKIE_NAME_TOKEN) String token)
            throws Exception {


        MiaoshaUser user = redisService.get(MiaoshaUserKey.getByToken(), token, MiaoshaUser.class);

//        MiaoshaUser byToken = miaoshaUserService.getByToken(response, token);
        return Result.success(user);
    }



}

