package cn.xzzz2020.miaosha.web.access;

import cn.xzzz2020.miaosha.domain.MiaoshaUser;

public class UserContext {

    private static ThreadLocal<MiaoshaUser> userThreadLocal = new ThreadLocal<>();

    public  static void setUser(MiaoshaUser user){
        userThreadLocal.set(user);
    }

    public  static MiaoshaUser getUser(){
        return userThreadLocal.get();
    }
}
