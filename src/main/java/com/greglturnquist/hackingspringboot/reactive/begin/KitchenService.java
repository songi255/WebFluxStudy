package com.greglturnquist.hackingspringboot.reactive.begin;

import reactor.core.publisher.Flux;


public class KitchenService {
    /*  reactive stream은 demand control 에 기반한다. project reactor는 핵심타입인 Flux<T>를 사용해서 demand control 을 구현한다.
        reactor는 non blocking 으로 동작한다.
       
        Future와 비슷한데, Future 는 이미 시작되었음을 나타내는 반면, Flux는 시작할 수 있음을 나타낸다.
        그렇다면 Flux만 제공하는 기능은 뭘까?
            - 하나 이상의 값 포함가능
            - 각 값이 제공될 때 어떤 일이 발생하는지 지정가능
            - 성공 실패 두 경로 별 처리가능
            - 결과 polling 불필요
            - 함수형 프로그래밍 지원
        물론 Future도 업데이트가 되긴 했으나 아직 back pressure와 demand control을 구현하는데 쓸 정도는 아니다.

       */
    Flux<Dish> getDishes(){
        return Flux.just(
                new Dish("Sesame chicken"),
                new Dish("Lo mein noodles, plain"),
                new Dish("Sweet & sour beef")
        );
    }
}
