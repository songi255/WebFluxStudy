package rest;

import com.greglturnquist.hackingspringboot.reactive.webflux.Item;
import com.greglturnquist.hackingspringboot.reactive.webflux.ItemRepository;
import org.springframework.hateoas.*;
import org.springframework.hateoas.mediatype.alps.Alps;
import org.springframework.hateoas.mediatype.alps.Type;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

// static 자동 import 할때, 색인이 좀 느려서 안뜨기도 하네. 조심하자.
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mediatype.alps.Alps.alps;
import static org.springframework.hateoas.mediatype.alps.Alps.descriptor;
import static org.springframework.hateoas.server.reactive.WebFluxLinkBuilder.linkTo;
import static org.springframework.hateoas.server.reactive.WebFluxLinkBuilder.methodOn;

@RestController
public class HypermediaItemController {
    private final ItemRepository repository;

    public HypermediaItemController(ItemRepository repository) {
        this.repository = repository;
    }

    // HATEOAS 의 Class 들이 덕지덕지 쓰였다.
    @GetMapping("/hypermedia/items/{id}")
    Mono<EntityModel<Item>> findOne(@PathVariable String id){
        // HATEOAS 의 methodOn() 으로 controller 의 proxy 를 생성한다.
        HypermediaItemController controller = methodOn(HypermediaItemController.class);

        // WebFluxBuilder 의 linkTo() 를 이용해서 controller 의 findOne() 에 대한 link 를 생성한다.
        // 현재 메소드가 findOne() 이므로, self 라는 이름의 Link 를 추가하고 Mono 에 담아 반환한다.
        Mono<Link> selfLink = linkTo(controller.findOne(id)).withSelfRel().toMono();

        // findAll() 에 대한 aggregate root (단순히 어떤 Entity 의 목록을 볼 수 있는 링크를 의미한다. (DDD 의 애그리것과는 다름)) 의 link 를 생성한다.
        // IANA(Internet Assigned Numbers Authority : 인터넷 할당번호 관리기관) 표준에 따라 link 이름을 item 으로 명명한다.
        Mono<Link> aggregateLink = linkTo(repository.findAll()).withRel(IanaLinkRelations.ITEM).toMono();

        // 여러개의 비동기 명령을 했기 때문에, 결과를 하나로 합치기 위해 zip 한다.
        //  - 여기서는 type 안정성이 보장되는 Reactor Tuple 타입에 넣고 Mono 로 감싸서 반환했다.
        // 이후 map을 통해 Tuple 에 담긴 여러 결과를 꺼내서, EntityModel 로 만들고 다시 Mono 로 감싸서 반환한다.
        return Mono.zip(repository.findById(id), selfLink, aggregateLink)
                .map(o -> EntityModel.of(o.getT1(), Links.of(o.getT2(), o.getT3())));
    }
    /* Hypermedia Link 를 만들때는 Domain 객체와 Link 를 조합해야 한다. 이를 쉽게 하기 위해 HATEOAS 는 다음과 같은 vendor-netural(벤더중립적) Model을 제공한다.
        상속관계를 표시했다.
        RepresentationModel : < Link 정보를 포함하는 Domain 객체 > 의 기본타입
            - EntityModel : Domain 객체를 감싸고 Link 를 추가할 수 있는 Model
            - CollectionModel : Domain 객체컬렉션을 감싸고 Link 추가 가능한 Model
                - PagedModel : Paging 관련 Metadata 포함 모델.

        이 4가지 모델과 Link, Links 객체를 기반으로 하이퍼미디어를 제공한다.
        web method 가 4개 중 하나를 그대로 반환 or Reactor 에 감싸 반환하면 -> HATEOAS 직렬화가 동작, 하이퍼미디어 생성한다.

        REST 에서는 상호작용대상을 resource 라 부르는데, web mothod 가 반환하는 것이 바로 이것이다.
        HATEOAS 는 resource 에 하이퍼미디어를 추가해주는것이다.

        이제 test 를 해보자.
    */


    // 아래는 Alps 사용한 custom profile 사용 예시이다.
    @GetMapping(value = "/hypermedia/items/profile", produces = MediaTypes.ALPS_JSON_VALUE)
    public Alps profile(){
        return alps()
                .descriptor(Collections.singletonList(descriptor()
                        .id(Item.class.getSimpleName() + "-repr")
                        .descriptor(Arrays.stream(Item.class.getDeclaredFields())
                                .map(filed -> descriptor()
                                        .name(filed.getName())
                                        .type(Type.SEMANTIC)
                                        .build())
                                .collect(Collectors.toList()))
                        .build()))
                .build();
    }
    // REST Docs 로 ALPS JSON 메타데이터 문서화하는 것은 실습과제로 남겨둔다.

}
