package cn.xzzz2020.miaosha.vo;


import cn.xzzz2020.miaosha.domain.MiaoshaUser;
import lombok.Data;

@Data
public class GoodsDetailVo {

    private int miaoshaStatus = 0;
    private long remainSeconds = 0;
    private GoodsVo goods;
    private MiaoshaUser miaoshaUser;
}
