package com.greglturnquist.hackingspringboot.reactive.rsocket.server;

import com.greglturnquist.hackingspringboot.reactive.webflux.Item;
import com.greglturnquist.hackingspringboot.reactive.webflux.ItemRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Controller
public class RSocketService {
    private final ItemRepository repository;

    private final Sinks.Many<Item> itemSink;
    public RSocketService(ItemRepository repository) {
        this.repository = repository;

        this.itemSink = Sinks.many().multicast().onBackpressureBuffer(); // EmitterProcessor 에 해당하는 최신 코드이다.
        // sink 는, Processor 에 새 Item 을 추가하기 위한 진입점이라고 생각하자.
        // Emitter Processor 는 단지 특별한버전의 Flux 일 뿐이라는 것을 꼭 기억해두자.
    }

    /* 들어가기에 먼저 요구사항을 정의해보자. 역동적으로, 새로운 Item 저장 시, 구독중인 모두에게 자동으로 정보를 제공해보자!
        Item 을 계속 추가할 수 있는 Flux 와, 이 Flux 에 구독을 통해 stream traffic 을 받아가도록 하자.

        deprecated 되었지만 참고하자.
            - 가장최근 메시지만 보내야하면 EmitProcessor
            - 최근 N개 보관 후 새 구독자에게 N개 모두 보내야 한다면 ReplayProcessor
            - 단 하나의 consumer 만 대상이라면 UnicastProcessor
    */

    // 요청 - 응답
    @MessageMapping("newItems.request-response") // Rsocket message 라우팅
    public Mono<Item> processNewItemsViaRSocketRequestResponse(Item item){
        return this.repository.save(item)
                .doOnNext(savedItem -> this.itemSink.tryEmitNext(savedItem));
        // 결국, 새 Item 저장 후, sink 를 구독하고 있는 모두에게 해당 Mono<Item> 을 적절한 배압신호를 사용해서 반환한다.
    }

    // 요청 - 스트림
    @MessageMapping("newItems.request-stream")
    public Flux<Item> findItemsViaRSocketRequestStream(){
        return this.repository.findAll()
                .doOnNext(this.itemSink::tryEmitNext);
        // 수신 받을 Client 에서는 회신받은 Flux 에 여러 연산과 배압을 적용해서, 최종 client 에게 데이터를 제공할 수 있다.
    }

    // 실행 후 망각
    @MessageMapping("newItems.fire-and-forget")
    public Mono<Void> processNewItemsViaRSocketFireAndForget(Item item){
        return this.repository.save(item)
                .doOnNext(savedItem -> this.itemSink.tryEmitNext(savedItem))
                .then(); // then() 을 사용하면 Mono 안의 내용을 사용하지 않고 버릴 수 있다. 제어신호만 사용하게 된다.
        // 유일하게 다른점은 반환타입이다. reactive 프로그래밍에서는 적어도 신호를 받을 수 있는 수단은 반환해야 하므로, Mono<Void> 가 딱 맞다.
    }
    // RSocket Protocol 덕에 reactive stream chain 이 Network 를 넘어서도 동작한다.
    // 처리과정중에 무언가 잘못되면 Mono.error() 가 전달되는데, 이는 실행 후 망각도 동일하다. 다른점은 잘 완료됬을때의 수행되는 일이다.

    // 양방향 채널
    @MessageMapping("newItems.monitor")
    public Flux<Item> monitorNewItems(){
        return this.itemSink.asFlux();
        // client 가 요청에 data (쿼리, 필터링 도 가능) 를 담아 보낼 수도 있기에, Flux 를 반환한다.
        // 단순히 Flux 를 반환한다. 구독자들은 복사본을 받게 된다. (지금은 emit 설정이기 떄문에.)
    }
    // web page 에 연결할수도 있고, audit (감사) 시스템에 연결할 수도 있다. 활용처는 무궁무진하다.



    // 위 패러다임들은 결국 배압신호에 의해 작동한다는 것을 알아야 한다.
}
