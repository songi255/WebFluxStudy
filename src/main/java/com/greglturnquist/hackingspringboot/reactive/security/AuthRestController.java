package com.greglturnquist.hackingspringboot.reactive.security;

import com.greglturnquist.hackingspringboot.reactive.webflux.Item;
import com.greglturnquist.hackingspringboot.reactive.webflux.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
public class AuthRestController {
    @Autowired
    ItemRepository repository;

    private static final SimpleGrantedAuthority ROLE_INVENTORY = new SimpleGrantedAuthority("ROLE_" + "INVENTORY");

    // 메서드 보안을 이용해서, 인가되지 않은 link 는 제공하지 않는 예제.
    @GetMapping("/api/items/{id}")
    Mono<EntityModel<Item>> findOne(@PathVariable String id, Authentication auth){
        AuthRestController controller = methodOn(AuthRestController.class);

        Mono<Link> selfLink = Mono.just(linkTo(controller.findOne(id, auth)).withSelfRel());

        Mono<Link> aggregateLink = Mono.just(linkTo(controller.findAll(auth)).withRel(IanaLinkRelations.ITEM));

        Mono<Links> allLinks; // 반환할 링크정보를 담는다. HATEOAS 의 Link 데이터 모음집인 Links 타입을 사용한다.

        // zip 은 다시한번 눈여겨보자. 여러개를 반환해야 하는데, 각 결과가 언제 종료될 지 알 수 없는 경우 사용하는, 함수형 프로그래밍에서 굉장히 친숙한 개념이다.
        if (auth.getAuthorities().contains(ROLE_INVENTORY)){
            Mono<Link> deleteLink = Mono.just(linkTo(controller.deleteItem(id)).withRel("delete"));
            allLinks = Mono.zip(selfLink, aggregateLink, deleteLink).map(links -> Links.of(links.getT1(), links.getT2(), links.getT3()));
        }else{
            allLinks = Mono.zip(selfLink, aggregateLink).map(links -> Links.of(links.getT1(), links.getT2()));
        }

        // return this.repository.findById(id); 예제 참고해서 수정 ㄱ
        return null;
    }

    // 임시
    public Mono<Item> deleteItem(String id){return null;};
    public Mono<Item> findAll(Authentication auth){return null;};
}
