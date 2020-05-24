package cn.xzzz2020.miaosha.rabbitmq;

import cn.xzzz2020.miaosha.domain.MiaoshaUser;
import lombok.Data;

@Data
public class MiaoshaMessage {

    private MiaoshaUser user;
    private long goodsId;

}
