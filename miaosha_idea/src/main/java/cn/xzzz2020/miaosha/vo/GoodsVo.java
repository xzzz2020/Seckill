package cn.xzzz2020.miaosha.vo;

import cn.xzzz2020.miaosha.domain.Goods;
import lombok.Data;

import java.util.Date;

/**
 * 合并两个表的数据
 */
@Data
public class GoodsVo extends Goods {
    private Double miaoshaPrice;
    private Integer stockCount;
    private Date startDate;
    private Date endDate;
}
