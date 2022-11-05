package com.greglturnquist.hackingspringboot.reactive.webflux;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Component;

@Component
public class TemplateDatabaseLoader {
    @Bean
    CommandLineRunner initialize(MongoOperations mongo){
        return args -> {
            mongo.save(new Item("Alf alarm clock", 19.99));
            mongo.save(new Item("Smurf TV tray", 24.99));
        };
    }
    // blocking repo 사용하지 않고 blocking 방식으로 Data를 로딩하였다. blocking repository를 아예 만들지 않았으므로 실수의 여지가 훨씬 줄어든다.

    // MongoOperation 은 뭘까? Spring team 은 수년 전 JdbcTemplate 일부를 추출해 JdbcOperations interface 를 만들었다.
    // interface를 사용하면 contract(계약) 과 세부 구현내용을 분리할 수 있다. 이 패턴은 거의 모든 spring portfolio의 template 들이 사용하고 있다.
    // 결론은, App과 MongoDB의 결합도를 낮추려면 MongoOperations를 사용하면 좋다.
}
