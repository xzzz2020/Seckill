

>该项目为仅供个人学习使用！！！

> 个人博客地址：[https://blog.csdn.net/qq_43040688](https://blog.csdn.net/qq_43040688) 

> 个人网站地址：[http://www.xzzz2020.cn/](http://www.xzzz2020.cn/)

# 高并发接口优化



## 一、项目简介

该项目主要学习常用的高并发优化技术，并发的瓶颈往往在数据库，采用缓存和消息队列对接口进行优化，减少对数据库的访问，掌握面对高并发场景下的设计思路：

* 以 **Spring Boot** 为主线的技术栈，使用了 **Mybatis+Druid** ，采用**前后端分离**架构
* 整个项目基于商品的 **秒杀接口**，设计登录、商品展示以及订单展示等一系列的功能
* 接口高并发的优化主要利用 **Redis实现页面缓存 + URL缓存 + 对象缓存** ，利用 **前后端分离实现页面静态化** 以及整合 **RabbitMQ实现异步下单** 的优化。`QPS优化至少两倍`
* 用户登录信息使用 **Cookie+Redis实现分布式Session** ，使用 **拦截器+自定义参数解析器** ，获取用户信息。`解决了不同服务器之间出现的缓存不一致或者服务器宕机Session消失的问题`
* 接口安全实现了 **秒杀接口地址隐藏 + 数学公式验证码 + 利用Redis实现接口防刷**。`防止机器人对于核心业务的攻击`
* 利用**Jmeter模拟5000个用户，使用1万个线程**，对商品展示接口和秒杀接口进行压测。`商品展示接口优化前QPS：584.8，优化后QPS：2085.9；秒杀接口优化前QPS：351.8，优化后QPS：2242.7`


## 二、技术栈

**前端**
* `Bootstrap`
* `Ajax`
* `thymeleaf`

**后端**
* `SpringBoot`
* `Mybatis`
* `Druid`
* `Jedis`
* `fastjson`


**数据库**
* `MySQL`
* `Redis`

**中间件**
* `RabbitMQ`

**测试**
* `Jmeter`


## 三、详细实现



### 3.1 分布式Session

常用的有三种分布式Session解决方案：`服务器之间Session共享`、`Session绑定`、`Cookie+缓存`。**本项目使用的就是Cookie+缓存的方式**。下面将介绍这几种方式：



>**服务器之间Session共享**：
>* 使用一台作为用户的登录服务器，当用户登录成功之后，会将session写到当前服务器上，我们通过脚本或者守护进程将session同步到其他服务器上，这时当用户跳转到其他服务器，session一致，也就不用再次登录。
>* **缺陷**：速度慢，同步session有延迟性，可能导致跳转服务器之后，session未同步。而且单向同步时，登录服务器宕机，整个系统都不能正常运行。

>**Session绑定**：
>* 基于nginx的ip-hash策略，可以对客户端和服务器进行绑定，同一个客户端就只能访问该服务器，无论客户端发送多少次请求都被同一个服务器处理
>* **缺陷**：容易造成单点故障，如果有一台服务器宕机，那么该台服务器上的session信息将会丢失前端不能有负载均衡，如果有，session绑定将会出问题


>**Cookie+缓存**
>* 将用户信息保存在`Redis`上，将键值放在`Cookie`中传递给浏览器，浏览器再下一次的访问中就会携带该Cookie。此时利用`拦截器+自定义参数解析器` 解析用户的Cookie，从缓存中获取数据传递给方法。


**部分代码如下：**


>利用Cookie + 加缓存保存用户信息
```java
/**
 * 分布式Session的思路是将数据存放在Redis中
 * 将数据的key放在cookie中发送给用户
 * 用户会携带cookie访问
 * 获取期中的token，从redis中获取
 * 每次访问都会生成一个新的，延长有效期
 */
private void addCookie(HttpServletResponse response, String token, MiaoshaUser user) {
    //生成Cookie
    //生成一个随机字符串token，去掉"-"
    //将token + 加上Redis通用缓存Key，保存在redis中
    redisService.set(MiaoshaUserKey.getByToken(), token, user);
    //生成Cookie，只将token存放在cookie,防止用户获取其他用户信息
    Cookie cookie = new Cookie(COOKIE_NAME_TOKEN, token);
    //将Cookie的时间和Redis缓存时间一直
    cookie.setMaxAge(MiaoshaUserKey.getByToken().expireSeconds());
    //将Cookie存放在根目录
    cookie.setPath("/");
    ////将Cookie返回给浏览器
    response.addCookie(cookie);
}
```


### 3.2  页面缓存 + URL缓存


页面缓存和URL缓存主要差异在于URL缓存**会根据URL的变化，数据会有所不同**，如某个商品的详细信息、视频的详细信息等。



故只介绍商品列表页面缓存技术：


>**页面缓存**
>* 当客户的请求到达后端时，`先去redis中查询缓存，如果缓存中找不到，则进行数据库逻辑操作，然后渲染，存入缓存并返回给前端`
>* 如果在缓存中找到了则直接返回给前端。
>* 存储在Redis缓存中的页面需要设置超时时间，缓存的时间长度根据页面数据变化频繁程度适当调整。目前大多数页面`缓存都是在60~120秒`，少数几乎不变化的可以调整到5分钟!


**部分代码实现**：



>商品列表的Controller层
```java
    @RequestMapping(value = "/to_list", produces = "text/html")
    @ResponseBody
    public String toGoods(Model model, MiaoshaUser user,
                          final HttpServletRequest request,
                          final HttpServletResponse response) {

        //取缓存
        String html;
        html = redisService.get(GoodsKey.getGoodsList(), "", String.class);
        if (html != null) {//如果缓存有这个页面
            return html;
        } else {//如果没有这个页面
            //访问数据库获取商品数据
            List<GoodsVo> goodsList = goodsService.listGoodsVo();
            if (user != null) {
                //如果有用户信息，则保存在Model中
                model.addAttribute("user", user);
            }
            //将商品数据保存在Model中
            model.addAttribute("goodsList", goodsList);

            //手动渲染
            SpringWebContext springWebContext = new SpringWebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap(), context);
            html = viewResolver.getTemplateEngine().process("goods_list", springWebContext);
            if (!StringUtils.isEmpty(html)) {
                //保存到缓存，缓存时间只有60秒，不宜过长
                redisService.set(GoodsKey.getGoodsList(), "", html);
            }
            //返回到浏览器
            return html;
        }

    }
```

**接下来使用Jmeter启动1万个线程进行压测：**

>优化前，可以看的`吞吐量达到584.8`
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200524182716158.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzQzMDQwNjg4,size_16,color_FFFFFF,t_70)

>优化后，可以看的`吞吐量达到2085.9`
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200524182735152.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzQzMDQwNjg4,size_16,color_FFFFFF,t_70)





### 3.3 对象级缓存 


相比页面缓存是更细粒度缓存。在实际项目中， 不会大规模使用页面缓存，因为涉及到分页，一般只缓存前面1-2页。对象缓存就是 当用到用户数据的时候，可以从缓存中取出。

**需要注意两个问题**：
* 一旦数据发生更改，一定要将缓存失效
* Service之间相互调用，切忌不能直接调用DAO，因为可能中间调用了缓存



**部分代码实现**：
>保存用户信息
```java
/**
 * 这个是对象级的缓存
 * 从缓存中取出用户信息
 * <p>
 * 和页面缓存最大的区别是：1.时间是永久的 2.当对象发生更新时，需删除或者更新缓存
 * <p>
 * 从这里可以看出，Service之间相互调用，切忌不能直接调用DAO，因为可能中间调用了缓存
 */
public MiaoshaUser getById(long id) {
    //取缓存
    MiaoshaUser user;
    user = redisService.get(MiaoshaUserKey.getById(), ":" + id, MiaoshaUser.class);
    if (user != null) {
        return user;
    } else {
        //取数据库，加入到缓存中
        user = miaoshaUserDao.getById(id);
        redisService.set(MiaoshaUserKey.getById(), ":" + id, user);
        return user;
    }
}
```


### 3.4 核心接口优化



>**核心的业务接口优化主要思路是**：
>* 使用利用Redis保存`商品库存的数量`、`用户的秒杀成功的订单信息`和`商品是否秒杀完的标记`，这样请求更多的访问缓存，减少对数据库的压力
>* 若用户秒杀成功，利用`RabbitMQ实现异步下单，服务器控制访问数据库的压力，让用户暂时等待`，这样可以优化用户的体验，防止出现服务器宕机等问题


**部分代码如下所示**：
>秒杀接口Controller


```java
@RequestMapping(value = "/{path}/do_miaosha", method = RequestMethod.POST)
@ResponseBody
public Result<Integer> do_miaosha(MiaoshaUser user,
                                  @RequestParam("goodsId") long goodsId,
                                  @PathVariable("path") String path) {

    //判断用户是否登录，如果没用登录，则传递提示信息
    if (user == null) {
        return Result.error(CodeMsg.SESSION_ERROR);
    }

    //隐藏了访问接口，需要验证path
    if (StringUtils.isEmpty(path)) {
        return Result.error(CodeMsg.REQUEST_ILLEGAL);
    }
    boolean check = miaoshaService.checkPath(path, user.getId(), goodsId);
    if (!check) {
        return Result.error(CodeMsg.REQUEST_ILLEGAL);
    }
    
    //判断是否秒杀到了
    MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
    //如果能够获取订单，说明该用户已经秒杀到商品
    if (order != null) {
        return Result.error(CodeMsg.REPEATE_MIAO_SHA);
    }
    //判断是否秒杀已经结束
    Boolean over = localOverMap.get(goodsId);
    if (over) {
        return Result.error(CodeMsg.MIAO_SHA_OVER);
    }

    //预减库存
    long stock = redisService.decr(GoodsKey.getMiaoGoodsStock(), ":" + goodsId);
    if (stock < 0) {//如果发现库存不足，则将秒杀结束的标记置成true
        localOverMap.put(goodsId, true);
        return Result.error(CodeMsg.MIAO_SHA_OVER);
    }

    //保存信息
    MiaoshaMessage miaoshaMessage = new MiaoshaMessage();
    miaoshaMessage.setGoodsId(goodsId);
    miaoshaMessage.setUser(user);
    //入队，实现异步下单
    mqSender.sendMiaoshaMessage(miaoshaMessage);
    //返回客户端订单处理中
    return Result.success(0);//排队中
}
```


>消息的发送者
```java
public void sendMiaoshaMessage(MiaoshaMessage miaoshaMessage) {
    //将数据序列化字符串
    String str = SerializableUtil.beanToString(miaoshaMessage);
    //发送消息
    amqpTemplate.convertAndSend(MQConfig.MIAOSHA_QUEUE,str);
}
```



>消息的接收者
```java
@RabbitListener(queues = MQConfig.MIAOSHA_QUEUE)
public void miaoshaReceive(String message) {
    //将消息反序列化
    MiaoshaMessage miaoshaMessage = SerializableUtil.stringToBean(message, MiaoshaMessage.class);
    //获取用户
    MiaoshaUser user = miaoshaMessage.getUser();
    //获取用户id
    long goodsId = miaoshaMessage.getGoodsId();
    //再次判断库存是否足够
    GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
    Integer stockCount = goods.getStockCount();
    //如果库存不足，则直接返回
    if (stockCount <= 0) {
        return;
    }
    //减库存 下订单 写入订单 一个事务中
    miaoshaService.miaosha(user, goods);
}
```


>订单的处理
```java
@Transactional
public OrderInfo miaosha(MiaoshaUser user, GoodsVo goods) {
    //减库存
    boolean success = goodsService.reduceStock(goods);
    if (success){
        //下订单
        return orderService.creatOrder(user,goods);
    }else {
        //如果库存不足，设置商品已经卖完
        setGoodsOver(goods.getId());
    }
    return null;
}

/**
 * 设置商品已经卖完
 */
private void setGoodsOver(Long id) {
    redisService.set(MiaoshaKey.getMiaoshaOver(),":"+id,true);
}
```


**接下来使用Jmeter启动1万个线程，模拟5000个用户进行压测**：

>优化前，可以看的`吞吐量达到351.8`
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200524182806726.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzQzMDQwNjg4,size_16,color_FFFFFF,t_70)

>优化后，可以看的`吞吐量达到2242.7`
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200524182818744.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3FxXzQzMDQwNjg4,size_16,color_FFFFFF,t_70)


### 3.5 接口安全优化
 
 接口的安全优化主要的防止恶意用户的访问，以及减少瞬时用户的并发量
 
 
>**接口隐藏**
* 由于前端的代码在浏览器，所以可以轻易的获取到核心业务的接口
* **解决**：地址是在客户端动态生成的，前端需要先获取地址信息，然后在发送给服务器，服务器会对浏览器的地址进行处理并和真实的地址进行比较

>**数学问题验证码**
>* 验证码主要防止机器人的大量访问，以及将用户的请求分散开，避免集中的下单
>**解决**：服务器生成验证码，通过前端输入进行验证

>**接口防刷**
>* 恶意用户可能会大量的访问服务器，给服务器造成压力
>**解决**：利用缓存，保存一定时间的访问数，如果超过一定限制，则直接拒绝访问








## 四、 项目实践中遇到的问题

### 4.1  秒杀成功商品订单数超过预订数值


**问题分析**：
* 该问题主要因为在高并发下，线程不安全导致的
* 在判断是否秒杀成功时，多个用户通过了判断，然后才减少了库存


**问题解决**：
* MySQL数据库在更新数据时，会自动加锁
* 在SQL语句中减少库存时，判断库存是否大于0，如果不是则执行失败，订单回滚


### 4.2  一个用户的多个请求导致秒杀成功秒杀多次

**问题分析**：
* 由于采用的异步下单，在该用户订单没有完成时，则可能会出现一个用户同时下多个订单


**问题解决**：
* 设计数据库表时，多设计一个秒杀的订单，和普通的订单分离
* 在秒杀订单上，user_id采用唯一索引


### 4.3 使用了缓存依然会大量访问数据库

**问题分析**：
* 由于为了简便，在Service上面统统加了@Transactional注解，会导致所有的方法启用事务
* 此时即使使用了缓存，也依然会访问数据库，最终造成数据库压力过大

**问题解决**：
* 只在需要事务的方法上使用@Transactiona注解，提高性能


## 五、 接下来的优化思路

* `静态资源优化`
* `CDN加速`
* `Nginx水平扩展`
