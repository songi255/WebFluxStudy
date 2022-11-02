package com.greglturnquist.hackingspringboot.reactive.begin;

import reactor.core.publisher.Flux;

public class PoliteServer {
    private final KitchenService Kitchen;

    public PoliteServer(KitchenService kitchen) {
        Kitchen = kitchen;
    }

    Flux<Dish> doingMyJob(){
        return this.Kitchen.getDishes()
                // reactive stream 의 onNext() 시그널을 받으면 doOnNext()가 실행된다
                .doOnNext(dish -> System.out.printf("Thank you for " + dish + "!"))
                // onError() 시그널 대응
                .doOnError(error -> System.out.println("So sorry about " + error.getMessage()))
                // onComplete() 시그널
                .doOnComplete(() -> System.out.println("Thanks for all your hard work!"))
                .map(Dish::deliver);
        // 시그널들은 두번이상 받을 수 있다. 그러나 한번에 처리하는 것이 콜백이 늘어나지 않으므로 성능면에서 더 유리하다.
    }
    // 이렇게 필요한 모든 흐름과 핸들러를 정의했지만, subscribe 하기 전에는 아무런 연산도 일어나지 않는다.
    // subscribe가 핵심이다. reactor의 일부이면서 reactive stream의 일부이기도 하다.
}
