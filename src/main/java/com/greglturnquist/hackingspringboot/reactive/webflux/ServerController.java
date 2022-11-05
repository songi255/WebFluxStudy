package com.greglturnquist.hackingspringboot.reactive.webflux;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ServerController {
    private final KitchenService kitchen;

    public ServerController(KitchenService kitchen) {
        this.kitchen = kitchen;
    }

    //GetMapping 은 Spring webMVC Annotation이다.
    @GetMapping(value = "/server", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Dish> serveDishes(){
        return this.kitchen.getDishes();
    }
    // 구독은 누가할까? Spring WebFlux가 적절한 옵션과 함께 적절한 타이밍에 구독한다.
    // 그니까.. client가 요청하면 연결이 되면서 WebFlux가 알아서 구독호출하고.. 그렇게 되는거구나.
    // 개발자는 Controller method에서 reactor type을 반환하도록만 작성하면 된다.

    // 이 점이 Spring Boot의 진정한 힘.. View Resolver나 Web method Handler 및 기타 infra structure 관련 Bean을 등록하지 않아도 웹서비스가 만들어진다.

    @GetMapping(value = "/serverd-dishes", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Dish> deliverDishes(){
        return this.kitchen.getDishes()
                .map(dish -> Dish.deliver(dish)); // kitchen이 제공하는 데이터셋을 받아서 Consumer로 조작하여 제공한다.
        // 데이터 변환은 어느계층이든 가능하다. 다만 Spring Boot Project의 Leader 인 Phill Webb은 Web Controller를 최대한 가볍게 가져가는 것을 추천한다.
        // Controller 에는 비즈니스로직을 담지말고, 요청내용을 해석해서 적절한 서비스 메서드에 처리를 위임하고 결과물을 반환만 하는 편이 좋다.
        // 그럼 이게 안좋다는 말인가..
    }


}
