package com.greglturnquist.hackingspringboot.reactive.security;

import org.springframework.data.repository.CrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends CrudRepository<User, String> {
    // name 으로 사용자 조회하는 메서드 하나만 가지고 있는데, 이점이 매우 중요하다.
    // Spring security 는 username 기준으로 하나의 사용자를 찾을 수 있어야 한다.
    Mono<User> findByName(String name);
}
