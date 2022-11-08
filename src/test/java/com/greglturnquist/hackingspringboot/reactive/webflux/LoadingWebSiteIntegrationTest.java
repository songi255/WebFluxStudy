package com.greglturnquist.hackingspringboot.reactive.webflux;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/* 내장 컨테이너 테스트
    Unit Test 보다 더 넓은 범위의 test 가 필요한 경우가 있다.
        - 예를 들면 Web Controller 가 backend Service 와 바르게 협력하고 있는지?
    이런 종단간 test 는 대체로 값비싼 test 환경을 구성해야 한다.
    복잡한 test case 를 자동화해뒀다고 해도, 사소한 변경에 의해 test 가 깨질수도 있다.
        - 종단간 test 는 이처럼, 비용이 매우 많이 들고 원하는 수준의 확신을 얻기도 어렵다.

    Spring Boot 는 완전한 기능을 갖춘 내장 웹컨테이너를 임의의 포트에 연결해서 구동할 수 있다.
    test case 는 mock 이나 stub 대신 실제 Application 구성요소와 동일환경에서 협력할 수 있따.

    참고로 이 Test Class 는 pom.xml 에서 blockhound-junit-platform 의존관계를 제거한 후 실행해야 성공한다.
*/

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // Spring Boot 가 실제 App을 구동하게 만든다.
// @SpringBootApplication 이 붙은 Class 를 찾아서 내장 컨테이너를 실행한다.
@AutoConfigureWebTestClient // App에 요청을 날리는 WebTestClient 인스턴스를 생성한다.
public class LoadingWebSiteIntegrationTest {
    @Autowired WebTestClient client;

    @Test // 실제 Test method 는 client 를 사용해서 HomeController 의 root 경로를 호출한다.
    void test(){
        client.get().uri("/").exchange()
                .expectStatus().isOk() // 보면 알겠지만, WebTestClient 에 이미 assertion 기능이 포함되어있다.
                .expectHeader().contentType(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .consumeWith(exchangeResult -> {
                    assertThat(exchangeResult.getResponseBody()).contains("<a href=\"/add");
                });
        // 물론 더 다양한 검증도 가능하다. jsoup 로 HTML parsing 해서 pattern 검사도 할 수 있고, JSONPath나 JSONassert 로 JSON 검사도 가능하다.
        // 6장에서는 Spring HATEOAS 에서 제공하는 LinkDiscovere API 같은 library 로 hypermedia 값 까지 검증할 것이다.
    }
}

/* 이렇게 내장 컨테이너를 test 해 보았는데, test 가 상당히 무겁다. 그러므로 첫 test 로는 적절하지 않다.
    다음과 같은 복합적인 test 전략을 가지는 것이 좋다.
        1. null 값 처리를 포함한 domain object test
        2. 가짜협력자를 사용해서 모든 business logic 을 검사하는 service layer test
        3. 내장 웹컨테이너 사용하는 약간의 종단테스트
            - 왜 약간만 수행할까? 시간 외에 다른 이유도 있다.
                - test 는 범위가 넓어질수록 깨지기 쉽다.
                - domain 객체변경은 큰 영향은 없지만, Service 계층의 경우, 거쳐가는 종단 test에 큰 영향을 미친다.
                - 따라서 이런 변경은 test case 또한 변경해야 하고, 이게 많아질수록 관리비용이 증가하기 때문이다.
                - 결론적으로, 종단테스트는 정말 필요한 부분만 쓰는 것이 좋을 것 같다.
*/

