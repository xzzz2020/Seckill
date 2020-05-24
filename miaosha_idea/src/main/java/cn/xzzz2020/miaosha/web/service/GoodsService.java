package cn.xzzz2020.miaosha.web.service;

import cn.xzzz2020.miaosha.web.dao.GoodsDao;
import cn.xzzz2020.miaosha.domain.MiaoshaGoods;
import cn.xzzz2020.miaosha.vo.GoodsVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service

public class GoodsService {

    @Resource
    GoodsDao goodsDao;

    public List<GoodsVo> listGoodsVo(){
        return goodsDao.listGoodsVo();
    }

    public GoodsVo getGoodsVoByGoodsId(long goodsId) {
        return goodsDao.getGoodsVoByGoodsId(goodsId);
    }

    /**
     * 减少库存
     * @param goods
     */
    public boolean reduceStock(GoodsVo goods) {
        MiaoshaGoods mg =new MiaoshaGoods();
        mg.setGoodsId(goods.getId());
        int i = goodsDao.reduceStock(mg);
        return i>0;
    }

    public void resetStock(List<GoodsVo> goodsList) {
        for(GoodsVo goods : goodsList ) {
            MiaoshaGoods g = new MiaoshaGoods();
            g.setGoodsId(goods.getId());
            g.setStockCount(goods.getStockCount());
            goodsDao.resetStock(g);
        }
    }
}
