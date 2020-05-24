package cn.xzzz2020.miaosha.redis.key.prefix.impl;


import cn.xzzz2020.miaosha.redis.key.prefix.KeyPrefix;

/**
 * 部分实现接口
 * 可以有自己的成员变量
 * 不可以被实例化
 */
public abstract class BasePrefix implements KeyPrefix {


    private final int expireSeconds;

    private final String prefix;

    public BasePrefix(String prefix){
        this(0,prefix);
    }

    public BasePrefix(int expireSeconds, String prefix) {
        this.expireSeconds = expireSeconds;
        this.prefix = prefix;
    }

    @Override
    public int expireSeconds() {//默认0是永不过期
        return expireSeconds;
    }

    /**
     * 利用类名区别Redis的key
     * @return
     */
    @Override
    public String getPrefix() {
        String className = getClass().getSimpleName();
        return className +":"+ prefix;
    }
}
