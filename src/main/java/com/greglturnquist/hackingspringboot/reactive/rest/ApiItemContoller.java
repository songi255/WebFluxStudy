package com.greglturnquist.hackingspringboot.reactive.rest;

import com.greglturnquist.hackingspringboot.reactive.webflux.Item;
import com.greglturnquist.hackingspringboot.reactive.webflux.ItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
// @Controller 와 다르게 화면 HTML 랜더링에 사용되는 값을 반환하지 않고, 데이터객체 체를 반환한다. 직렬화되어 응답본문에 기록된다.
public class ApiItemContoller {
    private final ItemRepository repository;

    public ApiItemContoller(ItemRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    Flux<Item> findAll(){
        return this.repository.findAll(); // 예제니까 모든 Item 을 반환했지만, 실제로는 Spring Data 의 paging 을 이용해서 조회한도를 정하는 편이 좋다.
    }

    @GetMapping("/api/items/{id}")
    Mono<Item> findOne(@PathVariable String id){ // 변수이름이 다르다면 @PathVariable("id") String itemID 같이 명시해주면 된다.
        return this.repository.findById(id);
    }

    // Post 는 idempotent(멱등) 하지 않다. 즉, 여러번 연산해도 결과가 달라지지 않는 멱등함이 없다는 뜻.
    // GET, PUT, DELETE 등은 멱등하다.
    @PostMapping("/api/items")
    Mono<ResponseEntity<?>> addNewItem(@RequestBody Mono<Item> item){
        return item.flatMap(s -> this.repository.save(s)) // Mono 에서 한번 꺼내야 하므로 flatMap
                .map(savedItem -> ResponseEntity
                        .created(URI.create("/api/items/" + savedItem.getId()))
                        .body(savedItem)
                );
    }

    @PutMapping("/api/items/{id}")
    public Mono<ResponseEntity<?>> updateItem(@RequestBody Mono<Item> item, @PathVariable String id){
        // 응답본문 ID 와 요청 ID 가 다를 수 있는데, 요청 ID 를 따라야 한다.
        return item.
                map(content -> new Item(id, content.getName(), content.getDescription(), content.getPrice()))
                .flatMap(this.repository::save)
                .map(ResponseEntity::ok);
    }
}
