package cn.xzzz2020.miaosha.redis.key;

import cn.xzzz2020.miaosha.redis.key.prefix.impl.BasePrefix;

public class GoodsKey extends BasePrefix {

    //时间不会特别长，防止网页出现变换及时更新
    private static final int token_expire = 60 ;

    private GoodsKey(String prefix) {
        super(prefix);
    }

    GoodsKey(int token_expire, String prefix){
        super(token_expire,prefix);
    }


    public static GoodsKey getGoodsList(){return new GoodsKey(token_expire,"goods:list");}
    public static GoodsKey getGoodsDetail(){return new GoodsKey(token_expire,"goods:detail");}
    public static GoodsKey getMiaoGoodsStock(){return new GoodsKey(0,"MiaoshaGoodsStock");}



}
