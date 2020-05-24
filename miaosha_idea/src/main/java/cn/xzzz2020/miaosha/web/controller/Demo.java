//package cn.xzzz2020.miaosha.web.controller;
//
//import cn.xzzz2020.miaosha.rabbitmq.MQSender;
//import cn.xzzz2020.miaosha.result.CodeMsg;
//import cn.xzzz2020.miaosha.result.Result;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.ResponseBody;
//
//@Controller
//@RequestMapping("/demo")
//public class Demo {
//
//
//    @Autowired
//    MQSender mqSender;
//
//    @ResponseBody
//    @RequestMapping("/mq")
//    public Result mq(){
//        mqSender.send("hello dada~");
//        return Result.success(CodeMsg.SUCCESS);
//    }
//
//
//
//    @ResponseBody
//    @RequestMapping("/mq/topic")
//    public Result mqTopic(){
//        mqSender.sendTopic("hello dada~");
//        return Result.success(CodeMsg.SUCCESS);
//    }
//
//    @ResponseBody
//    @RequestMapping("/mq/fanout")
//    public Result mqFanout(){
//        mqSender.sendFanout("hello dada~");
//        return Result.success(CodeMsg.SUCCESS);
//    }
//
//    @ResponseBody
//    @RequestMapping("/mq/header")
//    public Result mqHeader(){
//        mqSender.sendHeader("hello dada~");
//        return Result.success(CodeMsg.SUCCESS);
//    }
//
//
//}
