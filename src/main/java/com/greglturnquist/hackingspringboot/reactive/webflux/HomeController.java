package com.greglturnquist.hackingspringboot.reactive.webflux;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

@Controller
public class HomeController {
    private ItemRepository itemRepository;
    private CartRepository cartRepository;
    private CartService cartService;
    private InventoryService inventoryService;

    public HomeController(ItemRepository itemRepository, CartRepository cartRepository, CartService cartService, InventoryService inventoryService) {
        this.itemRepository = itemRepository;
        this.cartRepository = cartRepository;
        this.cartService = cartService;
        this.inventoryService = inventoryService;
    }

    @GetMapping
    Mono<Rendering> home(){
        //return Mono.just("home"); // template의 이름을 나타내는 문자열을 Mono에 담아 반환. 메서드 전체의 반환타입이 Mono<String> 이었다.
        // 사실 이렇게 아무 작업 없이 파일이름만 반환하는 경우 굳이 Mono에 담아 반환할 필요는 없다. 이건 그냥예제이기에..

        // Spring Boot의 자동설정에 의해, thymeleaf view resolver 를 통해 home 반환값을 접두어, 접두사와 합친다. 즉,,
        // classpath:templates/home.html 로 바뀐다.
        // 또, 이걸 통해 resource/templates 디렉토리에 작성해야 한다는 걸 알 수 있다.

        // repository 와 연결해서 개선해보자.
        return Mono.just(
                Rendering.view("home.html") // 이번에는 Mono<Rendering>을 반환한다. Rendering은 view/attribute를 포함하는 WebFlux container 이다.
                .modelAttribute("items", this.itemRepository.findAll()) // 이젠 보면 알겠지? template 에서 사용할 attribute 지정이다.
                .modelAttribute("cart", this.cartRepository.findById("My Cart").defaultIfEmpty(new Cart("My Cart")))
                // MongoDB에서 장바구니를 조회해서, 없으면 새로운 Cart를 생성해 반환하는 전형적인 reactor 사용법이다.
                .build()
        ); // findAll() 등의 결과로 Flux 타입이 template에 제공된다. thymeleaf 같은 reactive stream 호환 template engine 과는 아주 잘 맞는다.

    }

    // Mono는 0개 또는 1개의 원소만 담을 수 있는 reactive publisher 이다. project reactore에서 제공한다.
    // 개발 초기 필요성을 고민끝에, 하나의 원소만 비동기적으로 반환하는 경우가 압도적으로 많음을 깨닫고 추가했다.
    // 함수형 프로그래밍으로 무장한 Future라고 생각해도 된다. back pressure와 delay를 지원한다.

    @PostMapping("/add/{id}")
    Mono<String> addToCart(@PathVariable String id){
        return this.cartRepository.findById("My Cart")
                .defaultIfEmpty(new Cart("My Cart"))
                .flatMap(cart -> cart.getCartItems().stream()
                        .filter(cartItem -> cartItem.getItem().getId().equals(id))
                        .findAny()
                        .map(cartItem -> {
                            cartItem.increment();
                            return Mono.just(cart);
                        })
                        .orElseGet(() -> {
                            return this.itemRepository.findById(id)
                                    .map(item -> new CartItem(item))
                                    .map(cartItem -> {
                                        cart.getCartItems().add(cartItem);
                                        return cart;
                                    });
                        }))
                .flatMap(cart -> this.cartRepository.save(cart))
                .thenReturn("redirect:/");
    } // 흐름은 IntelliJ 에서 제공하는 흐름을 따라가는게 훨씬 좋은 듯 하다. 장바구니에 있는지 확인하고, 있으면 수량 추가, 없으면 새로 생성해서
    // 장바구니에 저장한 후 다시 리다이렉트하는 내용이다.

    /* map vs flatmap
        map : A -> B 로 바꾸는 도구
        flatmap : stream<A> -> 다른크기의 stream<B> 로 바꾸는 도구. 즉.. stream을 그대로 잇는다.
        flatmap 은 reactor의 황금망치라고 생각할 수 있다.
            - 앞서, Flux, Mono 는 결국 Container 라고 했다. flatMap은 알고있는대로 unpack 해서 mapping 해주는 함수이다.
            - 즉, Flux<Item> 이 있다면, Flux를 해체해서 Item 으로 다룰 수 있게 해주는 것이다!
            - 심지어 비동기 형식이라고 하는데.. 이건 추가 검색이 필요할 듯 하다. 여튼.. 의도대로 작동하지 않는다면 flatmap을 떠올려보자.
            - 만약 map을 썼다면, Mono<Mono<Item>> 같이 될 확률도 있기 때문에..

    */

    // 전통적인 반복문을 사용하지 않는 이유는, 부수효과때문이다. 즉.. 함수형 프로그래밍을 최대한 활용하자는 것이다.

    // 근데.. 생각해보자. 저 코드가 잘 읽히는가? 상태표시 중간단계가 없기 때문에 장황해졌고, 또 웹컨트롤러가 무거워지는 것도 시간문제다.
    // 분명히 Contoller는 요청처리만 담당하라고 Phil Webb 이 말했다. 즉.. Service로 추출해야 한다.

    /*
    @PostMapping("/add/{id}")
    Mono<String> addToCart(@PathVariable String id){
        return this.cartService.addToCart("My Cart", id)
                .thenReturn("redirect:/");
    }
    아래처럼 간단하게 바꿀 수 있다.
    */

    @GetMapping("/search")
    Mono<Rendering> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam boolean useAnd
    ){
        return Mono.just(Rendering.view("home.html")
                .modelAttribute("items", inventoryService.searchByExample(name, description, useAnd))
                .modelAttribute("cart", this.cartRepository.findById("My Cart").defaultIfEmpty(new Cart("My Cart")))
                .build()
        );
    }
}
