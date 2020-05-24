package cn.xzzz2020.miaosha.web.service;


import cn.xzzz2020.miaosha.web.dao.MiaoshaUserDao;
import cn.xzzz2020.miaosha.domain.MiaoshaUser;
import cn.xzzz2020.miaosha.exception.GlobalException;
import cn.xzzz2020.miaosha.redis.key.MiaoshaUserKey;
import cn.xzzz2020.miaosha.redis.RedisService;
import cn.xzzz2020.miaosha.result.CodeMsg;
import cn.xzzz2020.miaosha.vo.LoginVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import cn.xzzz2020.miaosha.util.MD5Util;
import cn.xzzz2020.miaosha.util.UUIDUtil;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;


@Service

public class MiaoshaUserService {

    @Resource
    private MiaoshaUserDao miaoshaUserDao;

    @Autowired
    RedisService redisService;

    public static final String COOKIE_NAME_TOKEN = "token";


    /**
     * 这个是对象级的缓存
     * 从缓存中取出用户信息
     * <p>
     * 和页面缓存最大的区别是：1.时间是永久的 2.当对象发生更新时，需删除或者更新缓存
     * <p>
     * 从这里可以看出，Service之间相互调用，切忌不能直接调用DAO，因为可能中间调用了缓存
     */
    public MiaoshaUser getById(long id) {
        //取缓存
        MiaoshaUser user;
        user = redisService.get(MiaoshaUserKey.getById(), ":" + id, MiaoshaUser.class);
        if (user != null) {
            return user;
        } else {
            //取数据库，加入到缓存中
            user = miaoshaUserDao.getById(id);
            redisService.set(MiaoshaUserKey.getById(), ":" + id, user);
            return user;
        }
    }

    /**
     * 更新密码
     * <p>
     * 1. 更新数据库
     * 2. 需要更新或删除缓存
     * <p>
     * 顺序不能颠倒的原因？
     * 如果先删除缓存，另一个用户读取了数据，会将旧的数据写入缓存，然后再更新数据库（此时出现了数据不一致）
     * 如果先更新数据库，再删除缓存，另一个用户读取数据，会将最新的数据读入缓存
     *
     * @param token
     * @param id
     * @param password
     * @return
     */
    public boolean updatePassword(String token, long id, String password) {

        //更新数据库
        MiaoshaUser user = getById(id);
        if (user == null) {
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
        }
        MiaoshaUser newUser = new MiaoshaUser();
        newUser.setId(id);
        newUser.setPassword(MD5Util.fromPassToDB(password, user.getSalt()));

        miaoshaUserDao.updatePassword(newUser);
        user.setPassword(newUser.getPassword());
        //更新成功，需要删除或者更新缓存
        redisService.delete(MiaoshaUserKey.getById(), ":" + id);
        redisService.set(MiaoshaUserKey.getByToken(), token, user);
        return true;
    }


    /**
     * 登录功能
     *
     * @param loginVo
     * @return
     */
    public String login(HttpServletResponse response, LoginVo loginVo) {
        if (loginVo == null) {
            throw new GlobalException(CodeMsg.SERVER_ERROR);
        }
        String mobile = loginVo.getMobile();
        String password = loginVo.getPassword();

        //判断手机号是否存在
        MiaoshaUser user = getById(Long.parseLong(mobile));
        if (user == null) {
            throw new GlobalException(CodeMsg.MOBILE_NOT_EXIST);
        }
        //验证密码
        String dbPassword = user.getPassword();
        String salt = user.getSalt();
        String dbPass = MD5Util.fromPassToDB(password, salt);
        if (!dbPassword.equals(dbPass)) {
            throw new GlobalException(CodeMsg.PASSWORD_ERROR);
        }
        //登录成功

        String token = UUIDUtil.uuid();
        addCookie(response, token, user);
        return token;
    }


    public MiaoshaUser getByToken(HttpServletResponse response, String token) {
        //参数校验
        if (StringUtils.isEmpty(token)) {
            return null;
        }
        MiaoshaUser user;
        user = redisService.get(MiaoshaUserKey.getByToken(), token, MiaoshaUser.class);

        if (user != null) {
            addCookie(response, token, user);
        } else {
            //System.out.println("aaaa");
        }
        return user;
    }

    /**
     * 分布式Session的思路是将数据存放在Redis中
     * 将数据的key放在cookie中发送给用户
     * 用户会携带cookie访问
     * 获取期中的token，从redis中获取
     * 每次访问都会生成一个新的，延长有效期
     */
    private void addCookie(HttpServletResponse response, String token, MiaoshaUser user) {
        //生成Cookie
        //生成一个随机字符串token，去掉"-"
        //将token + 加上Redis通用缓存Key，保存在redis中
        redisService.set(MiaoshaUserKey.getByToken(), token, user);
        //生成Cookie，只将token存放在cookie,防止用户获取其他用户信息
        Cookie cookie = new Cookie(COOKIE_NAME_TOKEN, token);
        //将Cookie的时间和Redis缓存时间一直
        cookie.setMaxAge(MiaoshaUserKey.getByToken().expireSeconds());
        //将Cookie存放在根目录
        cookie.setPath("/");
        ////将Cookie返回给浏览器
        response.addCookie(cookie);
    }
}
