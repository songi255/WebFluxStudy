package com.greglturnquist.hackingspringboot.reactive.webflux;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class InventoryService {
    private final ItemRepository itemRepository;
    private final CartRepository cartRepository;
/*
    public InventoryService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }
*/
    public InventoryService(ItemRepository itemRepository, CartRepository cartRepository) {
        this.itemRepository = itemRepository;
        this.cartRepository = cartRepository;
    }

    Flux<Item> searchByExample(String name, String description, boolean useAnd){
        Item item = new Item(name, description, 0.0);

        ExampleMatcher matcher = (useAnd ? ExampleMatcher.matchingAll() : ExampleMatcher.matchingAny())
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase()
                .withIgnorePaths("price");

        Example<Item> probe = Example.of(item, matcher);

        return itemRepository.findAll(probe); // 이렇게, Example 과 API를 조합하여 호출한다.
        // 참고로, API는 findOne, findAll, findAll(Example<S> var1, Sort var2), count, exists 5개가 끝인 듯 하다... 조합해서 잘 사용하면 된다.
    }

    public Mono<Cart> addItemToCart(String cart, String id) {
        return null; // 일단 걍 했다.
    }

    // Spring Data MongoDB 가 domain object 를 mongoDB document로 저장할 때 _class 라는 attribute 가 포함된다.
    // _class 에는 개발자가 작성한 코드와 DB 저장내용 변환에 필요한 meta data 가 있다.
    // Example 쿼리는 strictly-type 방식으로 구현해서, _class 와 정보가 합치되는 mongoDB document 에 대해서만 적용된다.
    // 이 type 검사를 우회해서 모든 collection에 대해 쿼리를 수행하려면 UntypedExampleMatcher 를 사용해야 한다.


    // 평문형 연산 (fluent operation)
    // 여러 연산을 메소드 이름으로 연결하는 느낌이다.
    // ReactiveFluentMongoOperations 를 구현한 repository 에 사용할 수 있다.
    /*
    Flux<Item> searchByFluentExample(String name, String description){
        return itemRepository.query(Item.class)
                //.matching(Query.query(where("TV tray").is(name).and("Smurf").is(description)))
                //.matching(Query.query(byExample(Example.of(item, matcher)))) // Example 을 사용할 수도 있다.
                .all();
    }
    */

    public Flux<Item> getInventory() {
        return Flux.<Item>just( new Item("id1", "name1", "desc1", 1.99),
                new Item("id2", "name2", "desc2", 9.99));
        // 걍 임시로 만듦
    }

    public Mono<Cart> getCart(String name){
        return Mono.<Cart>just(new Cart("My Cart")); // 역시 걍 임시로 만듦
    }
}
