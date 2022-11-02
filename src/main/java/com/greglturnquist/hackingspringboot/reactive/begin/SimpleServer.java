package com.greglturnquist.hackingspringboot.reactive.begin;

import reactor.core.publisher.Flux;

public class SimpleServer {
    /* 아주 단순한 형태의 reactive consumer 이다. 다른 reactive service를 호출하고 결과를 반환한다.

    */
    private final KitchenService kitchen;

    public SimpleServer(KitchenService kitchen) {
        this.kitchen = kitchen;
    }

    Flux<Dish> doingMyJob(){
        return this.kitchen.getDishes()
                .map(dish -> Dish.deliver(dish));
    }
}
