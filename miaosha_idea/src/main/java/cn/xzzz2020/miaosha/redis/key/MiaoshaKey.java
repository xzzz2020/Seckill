package cn.xzzz2020.miaosha.redis.key;

import cn.xzzz2020.miaosha.redis.key.prefix.KeyPrefix;
import cn.xzzz2020.miaosha.redis.key.prefix.impl.BasePrefix;

public class MiaoshaKey extends BasePrefix {

    //时间不会特别长，防止网页出现变换及时更新
    private static final int TOKEN_EXPIRE = 60 ;
    private static final int PATH_EXPIRE = 60*60*24 ;

    private MiaoshaKey(String prefix) {
        super(prefix);
    }

    MiaoshaKey(int token_expire, String prefix){
        super(token_expire,prefix);
    }


    public static MiaoshaKey getMiaoshaOver(){return new MiaoshaKey(0,"MiaoshaOver");}
    public static MiaoshaKey getMiaoshaPath(){return new MiaoshaKey(PATH_EXPIRE,"MiaoshaPath");}
    public static MiaoshaKey getMiaoshaVerifyCode = new MiaoshaKey(60*5,"MiaoshaVerifyCode");



}
