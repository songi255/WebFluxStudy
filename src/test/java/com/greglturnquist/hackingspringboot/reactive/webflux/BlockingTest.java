package com.greglturnquist.hackingspringboot.reactive.webflux;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class BlockingTest {
    // test 관점에서 BlockHound 는 정확히 어떤 것을 검출하는 것일까?
    // 주요한 몇가지는 다음과 같다. java.lang.Thread#sleep(), 여러 Socket 및 Network 연산, file method 일부
    // 전체목록은 BlockHound 클래스 안의 Builder 클래스의 blockingMethods hashmap 에서 확인할 수 있다.



    @Test
    void threadSleepIsABlockingCall(){
        Mono.delay(Duration.ofSeconds(1))
                .flatMap(tick -> {
                    try {
                        Thread.sleep(10);
                        return Mono.just(true);
                    }catch (InterruptedException e){
                        return Mono.error(e);
                    }
                })
                .as(StepVerifier::create)
                /* test 를 성공시키려면 complete 대신 다음을 작성한다.
                .verifyErrorMatches(throwable -> {
                    assertThat(throwable.getMessage()).contains("Blocking call! java.lang.Thread.sleep")
                })*/
                .verifyComplete();
    }

    @BeforeEach
    void setUp(){
        // 모두 적지 않고 일부만 적은 test method..

        // test data
        Item sampleItem = new Item("item1", "TV tray", "Alf TV tray", 19.99);
        CartItem sampleCartItem = new CartItem(sampleItem);
        Cart sampleCart = new Cart("My Cart", Collections.singletonList(sampleCartItem));

        // 협력자와 가짜 상호작용 정의
        //when(cartRepository.findById(anyString())).thenReturn(Mono.<Cart> empty().hide());
        // 가짜 객체 생성과정에서 해당 코드는 무슨 의미일까?
        // Mono.empty() 는 MonoEmpty 클래스의 singleton 을 반환한다. reactor는 이런 인스턴스를 감지하고 runtime에서 최적화하는데,
        // reactor는 필요하지 않다면 blocking 호출을 알아서 삭제한다. 즉, 장바구니가 없어도 동작하길 바랬지만, 그렇지 않고 blocking code 가 제거된다.
        // 그래서 MonoEmpty 를 숨겨서 최적화루틴에 걸리지 않게 해야한다. 애초에 Docs 에도 hide() 의 주 목적은 진단을 적확하게 하기 위함이라고 되어있다.
        // 이후 test 를 수행하면 된다.
    }

}
