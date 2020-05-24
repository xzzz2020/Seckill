package cn.xzzz2020.miaosha.redis.key.prefix;


/**
 * Redis前缀和过期时间的规范
 */
public interface KeyPrefix {

    int expireSeconds();

    String getPrefix();
}
