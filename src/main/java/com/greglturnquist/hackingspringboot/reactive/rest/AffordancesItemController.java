package com.greglturnquist.hackingspringboot.reactive.rest;

import com.greglturnquist.hackingspringboot.reactive.webflux.Item;
import com.greglturnquist.hackingspringboot.reactive.webflux.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import static org.springframework.hateoas.server.reactive.WebFluxLinkBuilder.linkTo;
import static org.springframework.hateoas.server.reactive.WebFluxLinkBuilder.methodOn;

@RestController
public class AffordancesItemController {
    @Autowired
    ItemRepository repository;

    @PutMapping("/affordances/items/{id}")
    // request body 로 Entity Model 을 받는데, Client 가 Item 객체를 보낼수도, HAL 을 보낼수도 있다는 걸 의미한다.
    public Mono<ResponseEntity<?>> updateItem(@RequestBody Mono<EntityModel<Item>> item, @PathVariable String id){
        return item
                .map(EntityModel::getContent)
                .map(content -> new Item(id, content.getName(), content.getDescription(), content.getPrice()))
                .flatMap(this.repository::save) // 몽고 db 에 저장
                .then(findOne(id)) // 방금 저장된 객체 조회
                .map(model -> ResponseEntity.noContent() // 204 noContent 상태코드와 location 헤더에 self URL 을 담아 반환한다.
                        .location(model.getRequiredLink(IanaLinkRelations.SELF).toUri()).build());
    // 단순히 Item 을 받아서 저장하고, 다시 조회해서 URL 추가하여 반환하고 있다.
    }

    @GetMapping("/affordances/items/{id}")
    Mono<EntityModel<Item>> findOne(@PathVariable String id){
        AffordancesItemController controller = methodOn(AffordancesItemController.class);

        Mono<Link> selfLink = linkTo(controller.findOne(id))
                .withSelfRel()
                // 아래 affordance 만 추가되었다. Item update 에 사용되는 updateItem() 에 사용되는 경로를, findOne() 의 self 에 연결한다.
                .andAffordance(controller.updateItem(null, id))
                .toMono();
        // 행동 유도성 관점에서는 실제 Data 를 전부 제공하는게 중요하지는 않다.
        // 하지만 가능하다면 id 필드같은 정보를 제공하는 것이 좋다. 그래야 경로변수를 사용하는 하위링크를 만들 수 있다. (?)

        // 2개 이상의 method 를 연결하는 것도 가능하다. 해당링크로 삭제도 가능하다면 이것도 포함시킬 수 있다.
        // andAffordance 2번 호출하거나, 매개변수에 2개 넣거나 인듯..



        Mono<Link> aggregateLink = linkTo(controller.findAll())
                .withRel(IanaLinkRelations.ITEM)
                .toMono();

        return Mono.zip(repository.findById(id), selfLink, aggregateLink)
                .map(o -> EntityModel.of(o.getT1(), Links.of(o.getT2(), o.getT3())));

    }

    @GetMapping("/affordances/items")
    Mono<CollectionModel<EntityModel<Item>>> findAll(){
        AffordancesItemController controller = methodOn(AffordancesItemController.class);

        Mono<Link> aggregateRoot = linkTo(controller.findAll())
                .withSelfRel()
                .andAffordance(controller.addNewItem(null))
                .toMono();

        return this.repository.findAll()
                .flatMap(item -> findOne(item.getId()))
                .collectList()
                .flatMap(models -> aggregateRoot
                        .map(selfLink -> CollectionModel.of(models, selfLink)));
    }

    @PostMapping("/affordances/items")
    Mono<ResponseEntity<?>> addNewItem(@RequestBody Mono<EntityModel<Item>> item){
        return item
                .map(EntityModel::getContent)
                .flatMap(this.repository::save)
                .map(Item::getId)
                .flatMap(this::findOne)
                .map(newModel -> ResponseEntity.created(newModel.getRequiredLink(IanaLinkRelations.SELF)
                                .toUri()).body(newModel.getContent()));
    }

    // 역시 마찬가지로,, 문서화해버리자.
}
