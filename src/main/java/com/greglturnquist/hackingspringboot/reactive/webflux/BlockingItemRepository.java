package com.greglturnquist.hackingspringboot.reactive.webflux;

import org.springframework.data.repository.CrudRepository;

public interface BlockingItemRepository extends CrudRepository<Item, String> {
    // reactive repository 의 시작시점 issue 를 해결하기 위해 만든 것.
    // blocking code 를 사용할 위험성이 있으므로 존재해선 안된다.
}
