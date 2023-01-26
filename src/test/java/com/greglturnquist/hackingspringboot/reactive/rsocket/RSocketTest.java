package com.greglturnquist.hackingspringboot.reactive.rsocket;

import com.greglturnquist.hackingspringboot.reactive.webflux.Item;
import com.greglturnquist.hackingspringboot.reactive.webflux.ItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.util.function.Predicate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/* Rsocket 과 webflux 모두 reactor 라 test 도 편리하다.
    이건 server를 켜둔 채 client 도 실행하는 통합테스트다.
    cURL 로도 가능하지만, spring 의 reactive 도구를 활용하는 편이 훨씬 효율적이다.
*/

@SpringBootTest
@AutoConfigureWebTestClient
public class RSocketTest {
    @Autowired
    WebTestClient webTestClient;
    @Autowired
    ItemRepository repository;

    @Test
    void verifyRemoteOperationsThroughRSocketRequestResponse() throws InterruptedException{
        this.repository.deleteAll()
                .as(StepVerifier::create)
                .verifyComplete();

        // post 를 받으면 rsocket 으로 server 에 요청 후 db 에 저장하게 될 것..
        this.webTestClient.post().uri("/items/request-response")
                .bodyValue(new Item("Alf alarm clock", "nothing important", 19.99))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Item.class)
                .value(item -> {
                    assertThat(item.getId()).isNotNull();
                    assertThat(item.getName()).isEqualTo("Alf alarm clock");
                    assertThat(item.getDescription()).isEqualTo("nothing important");
                    assertThat(item.getPrice()).isEqualTo(19.99);
                });

        Thread.sleep(500); // 저장되기까지 잠시 기다렸다가..

        // 몽고 db 에 잘 저장 되었는지 확인
        this.repository.findAll()
                .as(StepVerifier::create)
                .expectNextMatches(item -> {
                    assertThat(item.getId()).isNotNull();
                    assertThat(item.getName()).isEqualTo("Alf alarm clock");
                    assertThat(item.getDescription()).isEqualTo("nothing important");
                    assertThat(item.getPrice()).isEqualTo(19.99);
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void verifyRemoteOperationsThroughRSocketRequestStream() throws InterruptedException {
        this.repository.deleteAll().block(); // 기존데이터 삭제. 검증대상은 아니므로 StepVerifier 대신 block() 만 써도 무방하다.

        // Create 3 new "item"s
        List<Item> items = IntStream.rangeClosed(1, 3)
                .mapToObj(i -> new Item("name - " + i, "description - " + i, i))
                .collect(Collectors.toList());

        this.repository.saveAll(items).blockLast(); // 여기도 마찬가지. 테스트 대상이 아니다.


        // Get stream
        this.webTestClient.get().uri("/items/request-stream")
                .accept(MediaType.APPLICATION_NDJSON) // Accept 헤더에 형식지정으로, Rsocket Client 에 JSON Stream 받는다는 걸 알린다.
                .exchange() //
                .expectStatus().isOk()
                .returnResult(Item.class) // 응답으로 넘어온 데이터를 Item 으로 받아 StepVerifier 로 검증할 수 있도록 chaining flow 에서 빠져나온다.
                .getResponseBody() // 응답 본문을 flux 로 변환
                .as(StepVerifier::create)
                .expectNextMatches(itemPredicate("1")) // item 값 검증
                .expectNextMatches(itemPredicate("2"))
                .expectNextMatches(itemPredicate("3"))
                .verifyComplete();

        // request - stream 방식 test 는 기존과 다르다.
        // 일단 returnResult(), getResponseBody() 를 통해 일단 flow 에서 빠져나온 다음 StepVerifier 를 사용해서 검증하는 용법을 잘 기억해두자.
    }

    private Predicate<Item> itemPredicate(String num) {
        return item -> {
            assertThat(item.getName()).startsWith("name");
            assertThat(item.getName()).endsWith(num);
            assertThat(item.getDescription()).startsWith("description");
            assertThat(item.getDescription()).endsWith(num);
            assertThat(item.getPrice()).isPositive();
            return true;
        };
    }

    @Test
    void verifyRemoteOperationsThroughRSocketFireAndForget() throws InterruptedException {

        // Clean out the database
        this.repository.deleteAll()
                .as(StepVerifier::create)
                .verifyComplete();

        // Create a new "item"
        this.webTestClient.post().uri("/items/fire-and-forget")
                .bodyValue(new Item("Alf alarm clock", "nothing important", 19.99)) //
                .exchange() //
                .expectStatus().isCreated()
                .expectBody().isEmpty();

        Thread.sleep(500); //

        // Verify the "item" has been added to MongoDB
        this.repository.findAll()
                .as(StepVerifier::create) //
                .expectNextMatches(item -> {
                    assertThat(item.getId()).isNotNull();
                    assertThat(item.getName()).isEqualTo("Alf alarm clock");
                    assertThat(item.getDescription()).isEqualTo("nothing important");
                    assertThat(item.getPrice()).isEqualTo(19.99);
                    return true;
                }) //
                .verifyComplete();
    }

    // 터미널에서 curl -v localhost:8080/items 하고, 이 test 를 실행하면 결과값을 받을 수 있다.
    // 혹은 이렇게 받을 수 있게 해놓고, 다른 terminal 에서 요청을 보내서 확인할 수도 있다.
    //  - 요청-응답 : curl -X POST -H "Content-Type:application/json" localhost:8080/items/
    //  - 실행 후 망각 : curl -X POST -H "Content-Type:application/json" -i localhost:8080/items/
    //  - 요청-스트림 : curl -H "Accept:application/x-ndjson" localhost:8080/items/request-stream


}
