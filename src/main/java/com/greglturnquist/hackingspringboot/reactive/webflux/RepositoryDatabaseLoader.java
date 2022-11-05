package com.greglturnquist.hackingspringboot.reactive.webflux;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

//@Component
// Component Scanning 에서 Class가 Bean 으로 등록되게 해 준다.
public class RepositoryDatabaseLoader {
    //@Bean // method가 반환하는 객체가 Bean으로 등록되게 해 준다.
    // CommandLineRunner 는 App 시작 후 자동실행되는 특수한 Spring Boot Component.
    // App 에서 사용하는 모든 Component 가 등록되고 활성화 된 이후 해당 run() 이 자동실행됨이 보장된다. 다만 CommandLineRunner 사이 순서는 보장되지 않는다.
    /*CommandLineRunner initialize(BlockingItemRepository repository){
        return args -> {
            repository.save(new Item("Alf alarm clock", 19.99));
            repository.save(new Item("Smurf TV tray", 24.99));
        };
    }*/
    // 이 코드는 CommandLineRunner를 통해서 App 초기에만 실행한다. 근데, 이렇게 코드 상에 Blocking code를 남겨놓으면 누군가 모르고 사용할 수 있다.
    // 고로,, 사실 이 class는 아예 만들지를 말고 제거해야 한다. 대신 MongoTemplate를 사용하자.
    // 자동설정 덕에, reactive 버전과 blocking 버전 둘 다 사용할 수 있다.
}
