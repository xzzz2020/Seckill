package cn.xzzz2020.miaosha.domain;

import lombok.Data;

import java.util.Date;
@Data
public class MiaoshaGoods {
	private Long id;
	private Long goodsId;
	private Double goodsPrice;
	private Integer stockCount;
	private Date startDate;
	private Date endDate;

}
