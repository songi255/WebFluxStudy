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
}
