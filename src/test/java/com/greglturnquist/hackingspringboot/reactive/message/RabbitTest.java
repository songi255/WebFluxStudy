package com.greglturnquist.hackingspringboot.reactive.message;

import com.greglturnquist.hackingspringboot.reactive.webflux.Item;
import com.greglturnquist.hackingspringboot.reactive.webflux.ItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@AutoConfigureWebTestClient // WebTestClient 자동설정
@Testcontainers // JUnit5 에서 제공하는 @. testcontainer 를 test 에 사용할 수 있게 해준다.
@ContextConfiguration // 지정한 Class 를 Test 실행 전 먼저 App Context 에 Loading 해 준다.
public class RabbitTest {
    // Test 에 사용할 Container 생성. RabbitMQ instance 를 관리한다.
    @Container static RabbitMQContainer container = new RabbitMQContainer("rabbitmq:3.7.25-management-alpine");
    // Test 는 수명주기가 달라 권장하는 생성자주입 대신 필드주입을 해도 괜찮다.
    @Autowired
    WebTestClient webTestClient;
    @Autowired
    ItemRepository repository;

    // @DynamicPropertySource 는 lambda:Supplier 를 사용해서, 환경설정 내용을 Environment 에 동적추가한다.
    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry){
        // test container 에서 실행한 rabbitmq 의 host와 port 를 가져온다.
        // 이 rabbitmq 연결정보를 spring 환경설정 정보에 저장하게 되므로 Spring AMQP 에서 사용할 수 있게 된다.
        registry.add("spring.rabbitmq.host", container::getContainerIpAddress);
        registry.add("spring.rabbitmq.port", container::getAmqpPort);
    }

    // 컨테이너를 사용하는 test 이므로 Thread.sleep() 같은 걸 사용해야 한다.


    @Test
    void verifyMessagingThroughAmqp() throws InterruptedException{
        this.webTestClient.post().uri("/items")
                .bodyValue(new Item("Alf alarm clock", "nothing important", 19.99))
                .exchange()
                .expectStatus().isCreated()
                .expectBody();

        Thread.sleep(1500L); // broker 를 거쳐 저장소에 저장되기 기다림

        this.webTestClient.post().uri("/items")
                .bodyValue(new Item("Smurf TV tray", "nothing important", 29.99))
                .exchange()
                .expectStatus().isCreated()
                .expectBody();

        Thread.sleep(2000L);

        // 2개 message 를 통해 잘 저장되었는지 확인
        this.repository.findAll()
                .as(StepVerifier::create)
                .expectNextMatches(item -> {
                    assertThat(item.getName()).isEqualTo("Alf alarm clock");
                    assertThat(item.getDescription()).isEqualTo("nothing important");
                    assertThat(item.getPrice()).isEqualTo(19.99);
                    return true;
                })
                .expectNextMatches(item -> {
                    assertThat(item.getName()).isEqualTo("Smurf TV tray");
                    assertThat(item.getDescription()).isEqualTo("nothing important");
                    assertThat(item.getPrice()).isEqualTo(29.99);
                    return true;
                })
                .verifyComplete();
    }



}
