package com.greglturnquist.hackingspringboot.reactive.webflux;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface CartRepository extends ReactiveCrudRepository<Cart, String> {
    // Cart. 즉 장바구니용 리포지토리. Item이 Cart 객체를 다루는 Context에서 필요한가? 와 같은 질문을 통해 Context 경계를 구분하는 개념은 DDD에 잘 설명되어있다.
    // DDD를 조금 더 공부해 보자.
}
