package cn.xzzz2020.miaosha.rabbitmq.config;


import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


@Configuration
public class MQConfig {


    public static final String MIAOSHA_QUEUE = "miaosha.queue";


    public static final String QUEUE_DIRECT = "queueDirect";
    public static final String QUEUE_Header = "queueHeader";
    public static final String QUEUE_TOPIC_1 = "queueTopic1";
    public static final String QUEUE_TOPIC_2 = "queueTopic2";
    public static final String TOPIC_EXCHANGE = "TopicExchange";
    public static final String FANOUT_EXCHANGE = "FanoutExchange";
    public static final String HEADER_EXCHANGE = "HeaderExchange";



    @Bean
    public Queue miaoshaQueue() {
        return new Queue(MIAOSHA_QUEUE, true);
    }



    /*
    direct 模式 Exchanger
     */
    @Bean
    public Queue queueDirect() {
        return new Queue(QUEUE_DIRECT, true);
    }


    /*
           topic 模式 Exchanger
     */
    @Bean
    public Queue queueTopic1() {
        return new Queue(QUEUE_TOPIC_1, true);
    }




    /*
        topic 模式 Exchanger
     */
    @Bean
    public Queue queueTopic2() {
        return new Queue(QUEUE_TOPIC_2, true);
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE);
    }

    @Bean
    public Binding Binding1() {
        return BindingBuilder.bind(queueTopic1()).to(topicExchange()).with("topic.key1");
    }

    @Bean
    public Binding Binding2() {
        return BindingBuilder.bind(queueTopic2()).to(topicExchange()).with("topic.#");
    }


    /*
        fanout 模式 Exchanger
     */
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(FANOUT_EXCHANGE);
    }

    @Bean
    public Binding fanoutBinding1() {
        return BindingBuilder.bind(queueTopic1()).to(fanoutExchange());
    }


    @Bean
    public Binding fanoutBinding2() {
        return BindingBuilder.bind(queueTopic2()).to(fanoutExchange());
    }



    /*
          Header 模式 Exchanger
    */
    @Bean
    public HeadersExchange headersExchange() {
        return new HeadersExchange(HEADER_EXCHANGE);
    }


    @Bean
    public Queue queueHeader() {
        return new Queue(QUEUE_Header, true);
    }

    @Bean
    public Binding headerBinding1() {

        Map<String,Object> map = new HashMap<>();
        map.put("header1","value1");
        map.put("header2","value2");

        return BindingBuilder.bind(queueHeader()).to(headersExchange()).whereAll(map).match();
    }

}
