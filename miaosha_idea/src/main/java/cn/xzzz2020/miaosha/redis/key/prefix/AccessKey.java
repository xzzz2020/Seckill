package cn.xzzz2020.miaosha.redis.key.prefix;

import cn.xzzz2020.miaosha.redis.key.prefix.impl.BasePrefix;

public class AccessKey extends BasePrefix {


    private AccessKey(String prefix) {
        super(prefix);
    }

    AccessKey(int token_expire, String prefis) {
        super(token_expire, prefis);
    }


    public static AccessKey accessTime(int expireTime) {
        return new AccessKey(expireTime, "access");

    }


}
