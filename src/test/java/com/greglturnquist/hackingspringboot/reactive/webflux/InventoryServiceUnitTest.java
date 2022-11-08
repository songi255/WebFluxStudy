package com.greglturnquist.hackingspringboot.reactive.webflux;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class) // test handler 를 지정할 수 있는 JUnit5의 API 이다. SpringExtension 은 Spring 특화된 test 기능을 사용할 수 있다.
public class InventoryServiceUnitTest { // Class 이름을 이렇게 대상클래스 + 범위까지 표현하면 굉장히 알기 쉽다.
    // test 대상 class 를 CUT 라고 한다. (class under test)
    // 여기서 CUT 는 domian 객체가 아니지만, Spring Boot와 project reactor 에서는 비동기, 논블로킹코드도 JUnit 을 통해 test 할 수 있다.

    InventoryService inventoryService; // CUT. 아무런 annotaion 이 없으며, test 시 초기화된다.

    @MockBean private ItemRepository itemRepository; // mockito 를 사용해서 가짜 객체를 만들어 context 에 bean 으로 추가한다.
    /* 다음 코드를 직접 작성하는것과 같다. 하지만 @MockBean 이 협력자를 더 눈에 띄게 잘 드러나게 한다.
    @BeforeEach
    void setup(){
        itemRepository = mock(ItemRepository.class);
    }
    */

    @MockBean private CartRepository cartRepository;
    // 협력자가 reactive 하다면 test 용 가짜협력자도 reactive 해야 한다.
    // 모든 것을 Mono, Flux 로 감싸는 것은 귀찮지만, 이를 피하려면 별도의 reactor용 mockito API 를 사용해야 한다.
    // 하지만, 이 mockito API 를 사용하면 BlockHound 가 잘못 사용된 blocking code를 검출하기가 매우 어려워질 수 있다.


    @BeforeEach // JUnit5의 annotaion. 모든 test method 실행 전 준비내용을 담고있는 메서드를 실행한다.
    // 모든 메서드보다 가장 먼저 1회 실행되어야 하는게 필요하다면 @BeforeAll 을 사용한다.
    void setUp(){
        // test 데이터 정의
        Item sampleItem = new Item("item1", "TV tray", "Alf TV tray", 19.99);
        CartItem sampleCartItem = new CartItem(sampleItem);
        Cart sampleCart = new Cart("My Cart", Collections.singletonList(sampleCartItem));

        // 협력자와의 상호작용 정의
        when(cartRepository.findById(anyString())).thenReturn(Mono.empty());
        when(itemRepository.findById(anyString())).thenReturn(Mono.just(sampleItem));
        when(cartRepository.save(any(Cart.class))).thenReturn(Mono.just(sampleCart));

        inventoryService = new InventoryService(itemRepository, cartRepository);
    }

    @Test
    void addItemToEmptyCartShouldProduceOneCartItem(){
        inventoryService.addItemToCart("My Cart", "item1")
                .as(StepVerifier::create) // Reactor test Module 로 연결한다.
                .expectNextMatches(cart -> {
                    assertThat(cart.getCartItems()).extracting(CartItem::getQuantity)
                            .containsExactlyInAnyOrder(1);
                    assertThat(cart.getCartItems()).extracting(CartItem::getItem)
                            .containsExactly(new Item("item1", "TV tray", "Alf TV tray", 19.99));
                    return true; // 이 지점까지 통과했다면 true 를 반환한다.
                })
                .verifyComplete(); // reactive stream 의 complete signal 이 발생하고 성공적 완료되었음을 검증한다.
    }
    /* 정리하면, reactive code 를 test 할 때는 기능만을 검사하는 것이 아니고, reactive stream signal 도 함께 검사해야 한다.
        - onSubscribe(), onNext(), onError(), onComplete() 를 말한다.
        - 예제는 onNext()와 onComplete() 를 모두 검사하는데, 이 두가지가 모두 발생하면 successful path 라고 한다.
        StepVerifier가 구독을 담당한다.
        여기서는 top-level 방식의 패러다임을 사용했다.
            - reactor 기반 함수를 top-level 에서 먼저 호출하고, 바로 다음에 as 를 이어서 호출하는 것이다.
            - 다른방법으로도 작성할 수 있다. 아래에 바로 적겠다.
    */
    @Test
    void alternativeWayToTest(){
        StepVerifier.create(inventoryService.addItemToCart("My Cart", "item1"))
                .expectNextMatches(cart -> {
                    assertThat(cart.getCartItems()).extracting(CartItem::getQuantity)
                            .containsExactlyInAnyOrder(1);
                    assertThat(cart.getCartItems()).extracting(CartItem::getItem)
                            .containsExactly(new Item("item1", "TV tray", "Alf TV tray", 19.99));
                    return true;
                })
                .verifyComplete();
    }
    // 다만, 단순히 바깥에 명시적으로 드러난 행이 아니라, 메소드 인자까지 뒤져봐야 무엇이 test 되는 지 알 수 있으므로 별로 좋아보이지 않다고 한다.

    // subscribe 는 체크하지 않아도 될까? 자명하기 때문에 상관없다. 만약 doOnSubscribes()로 뭔갈 작설했다면, expectSubscription()... 을 사용해서 test 해야 한다.



}

