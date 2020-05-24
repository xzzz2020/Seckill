package cn.xzzz2020.miaosha.domain;

import lombok.Data;

@Data
public class Goods {

	private Long id;
	private String goodsName;
	private String goodsTitle;
	private String goodsImg;
	private String goodsDetail;
	private Double goodsPrice;
	private Integer goodsStock;

}
