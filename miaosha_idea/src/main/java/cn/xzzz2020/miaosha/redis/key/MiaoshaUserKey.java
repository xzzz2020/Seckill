package cn.xzzz2020.miaosha.redis.key;

import cn.xzzz2020.miaosha.redis.key.prefix.impl.BasePrefix;

public class MiaoshaUserKey extends BasePrefix {

    private static final int token_expire = 3600 * 2;

    private MiaoshaUserKey(String prefix) {
        super(prefix);
    }

    MiaoshaUserKey(int token_expire,String prefis){
        super(token_expire,prefis);
    }


    public static MiaoshaUserKey getByToken(){return new MiaoshaUserKey(token_expire,"token");}
    public static MiaoshaUserKey getById(){return new MiaoshaUserKey(0,"user:id");}



}
