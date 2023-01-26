package com.greglturnquist.hackingspringboot.reactive.messaging;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SerializerConfig {
    // 이 Bean 등록 하나만으로 MessageConverter 를 자동활성화할 수 있다.
    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter(){
        return new Jackson2JsonMessageConverter();
    }
}
