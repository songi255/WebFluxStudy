package rest;

import com.greglturnquist.hackingspringboot.reactive.webflux.Item;
import com.greglturnquist.hackingspringboot.reactive.webflux.ItemRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

}
