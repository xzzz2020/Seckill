package cn.xzzz2020.miaosha.redis.key;

import cn.xzzz2020.miaosha.redis.key.prefix.impl.BasePrefix;

public class OrderKey extends BasePrefix {



    public OrderKey(int expireSeconds, String prefix) {
        super(expireSeconds, prefix);
    }

    public static OrderKey getMiaoshaOrederByUidGid = new OrderKey(0,"miaosha:order:gidAnduid");
}
