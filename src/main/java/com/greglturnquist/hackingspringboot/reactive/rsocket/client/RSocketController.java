package com.greglturnquist.hackingspringboot.reactive.rsocket.client;

import com.greglturnquist.hackingspringboot.reactive.webflux.Item;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;

import static io.rsocket.metadata.WellKnownMimeType.MESSAGE_RSOCKET_ROUTING;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.*;

@RestController // HTML 을 랜더링하지 않는다.
public class RSocketController {
    // 오류 때문에 임시로 null 했다.
    private final Mono<RSocketRequester> requester; // Mono를 쓰므로, Rsocket 에 연결된 코드는 새 client 가 구독할 떄마다 호출된다.

    // spring boot 는 RSocketRequesterAutoConfiguration 정책 안에서 자동설정으로 RSocketRequester.Builder bean 을 만들어준다.
    // Jackson 을 포함해서 여러가지 encoder / decoder 를 사용할 수 있다.

    // 얘들도 방식이 바뀐 것 같다. 실제 사용에서 따로 사용법을 찾아보도록 하자.


    public RSocketController(RSocketRequester.Builder builder){
        this.requester = builder
                .dataMimeType(APPLICATION_JSON) // data의 mediatype 지정. Spring 상수를 사용했다.
                // routing 정보 같은 meta data 값을 Rsocket 표준인 message/x.rsocket.routing.v0 로 설정했다.
                .metadataMimeType(parseMediaType(MESSAGE_RSOCKET_ROUTING.toString()))
                .connectTcp("localhost", 7000)
                // robustness (견고성) 을 높이기 위해 메시지처리 실패시 Mono 가 5번까지 재시도 할 수 있도록 지정.
                .retry(5)
                // 요청 Mono 를 hot source 로 저장. 가장 최근 신호는 캐시될 수 있으며, 구독자는 사본을 가지고 있을수도 있다.
                // 다수의 client 가 동일한 하나의 data 를 요구할 떄 효율성을 높일 수 있다.
                .cache();
    }

    /* RSocketRequester 는 R Socket 에 무언가를 보낼 때 사용하는 얇은 포장재와 같다.
        - Rsocket 에 messaging 패러다임은 포함되지 않았다. Requester 를 사용하면 Spring 과 연동된다.
            - 도착지 기준 메시지 라우팅 가능
            - traffic 의 encoding/decoding 쉽게 가능
            - Requester 를 사용하지 않으면 server/client 양쪽 모두 R socket 연결에서 data 를 직접 관리해야 한다.

        Mono 로 감싸는 이유는 뭘까?
            - Mono 패러다임은 connection 을 Rsocket 연결 세부정보를 포함하는 lazy construct (지연 구조체) 로 전환한다.
            - 아무도 연결하지 않으면 RSocket 은 열리지 않는다. 누군가 구독해야 세부정보가 여러 구독자에게 공유될 수 있다.

        하나의 RSocket 만으로 모든 구독자에게 서비스 할 수 있다는 점도 중요하다.
            - 구독자마다 1개씩 따로 만들 필요가 없다.
            - 대신 하나의 R Socket Pipe 에 구독자별로 하나씩 연결을 생성한다.

        이렇게 준비과정을 마쳐야 Rsocket 이 Network 를 오가는 Data frame 을 reactive 하게 전송하고 배압처리하는데 집중할 수 있다.
        Rsocket 이든 reactive web 이든, 전부 reactor 위에서 작동하므로, 투박한 편법이 필요하지 않다.
    */

    // 이제, Http 요청을 받아, 각 4가지 방식의 Rsocket 요청으로 전환해보자.
    @PostMapping("/items/request-response")
    Mono<ResponseEntity<?>> addNewItemUsingRSocketRequestResponse(@RequestBody Item item){
        return this.requester
                .flatMap(rSocketRequester -> rSocketRequester
                        .route("newItems.request-response") // 요청을 라우팅했다.
                        .data(item)
                        .retrieveMono(Item.class)) // Mono<Item> 응답을 원한다는 신호를 보낸다.
                .map(savedItem -> ResponseEntity
                        .created(URI.create("/items/request-response"))
                        .body(savedItem)
                );
    }

    @GetMapping(value = "/items/request-stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    // stream 방식 전환을 위해 반환하는 media type 을 application/x-ndjson 으로 변환하고 있다.
    // ndjson 은 newline delimited JSON 의 약자로, 여러개의 JSON 을 줄바꿈으로 구분해서, 여러번에 걸쳐 stream 으로 반환한다는 뜻이다.
    Flux<Item> findItemsUsingRSocketRequestStream() {
        return this.requester //
                .flatMapMany(rSocketRequester -> rSocketRequester // 여러건의 조회결과를 반환할 수 있도록 flatMapMany() 를 썼다.
                        .route("newItems.request-stream") // 라우팅
                        .retrieveFlux(Item.class) // <4>
                        .delayElements(Duration.ofSeconds(1))); // 여러 건의 Item 을 1초에 1건 씩 반환하도록 요청한다.
                        // 걍 stream 응답을 눈으로 쉽게 보기 위한것일 뿐, 필요한 코드는 아니다.
    }

    @PostMapping("/items/fire-and-forget")
    Mono<ResponseEntity<?>> addNewItemUsingRSocketFireAndForget(@RequestBody Item item) {
        return this.requester //
                .flatMap(rSocketRequester -> rSocketRequester //
                        .route("newItems.fire-and-forget")
                        .data(item) //
                        .send()) // 새 Item 정보는 필요하지 않으므로, 그저 send() 후 Mono<Void> 를 받는다.
                .then( // Mono<Void> 라서, map 을 하면 아무일도 안일어난다. 그러나 Created Code 를 반환해야 하므로, Mono 를 새로 만들어 반환한다.
                        Mono.just( //
                                ResponseEntity.created( //
                                        URI.create("/items/fire-and-forget")).build()));
    }

    @GetMapping(value = "/items", produces = TEXT_EVENT_STREAM_VALUE)
    // produces 값은, 응답 결과가 생길 때 마다 stream 에 흘려보낸다는 것을 의미한다.
    // 참고로 cURL 도 stream 결과를 받을 수 있다.
    Flux<Item> liveUpdates() {
        return this.requester //
                .flatMapMany(rSocketRequester -> rSocketRequester //
                        .route("newItems.monitor")
                        .retrieveFlux(Item.class)); // 결과 필터링에 필요한 data 를 data() 를 통해 전달할 수도 있다.
    }
    // 양방향 요청은 어떤 사용사례가 있을까?
}
