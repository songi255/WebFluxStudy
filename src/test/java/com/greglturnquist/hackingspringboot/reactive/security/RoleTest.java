package com.greglturnquist.hackingspringboot.reactive.security;

import com.greglturnquist.hackingspringboot.reactive.webflux.Item;
import com.greglturnquist.hackingspringboot.reactive.webflux.ItemRepository;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.HypermediaWebTestClientConfigurer;
import org.springframework.hateoas.server.core.TypeReferences;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest()
@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL) // webTestClient 가 하이퍼미디어 문서를 응답으로 받아 처리할 수 있도록 설정한다.
// 사실 HATEOAS 가 classpath 에 있다면 Spring Boot 가 자동으로 적용해준다. 여기선 직접 명시했다.
@AutoConfigureWebTestClient
public class RoleTest {
    @Autowired
    WebTestClient webTestClient;

    @Autowired
    ItemRepository repository;

    @Autowired
    HypermediaWebTestClientConfigurer webTestClientConfigurer; // HATEOAS 의 것이다.

    @BeforeEach
    void setUp(){
        this.webTestClient = this.webTestClient.mutateWith(webTestClientConfigurer);
        // configurer 를 web Test Client 에 적용했다.
    }

    @Test
    void verifyLoginPageBlocksAccess() {
        this.webTestClient.get().uri("/") //
                .exchange() //
                .expectStatus().isUnauthorized();
    }

    @Test
    @WithMockUser(username = "ada")
    void verifyLoginPageWorks() {
        this.webTestClient.get().uri("/") //
                .exchange() //
                .expectStatus().isOk();
    }

    // 역할이 없는 사용자가 item 추가를 시도
    @Test
    @WithMockUser(username = "alice", roles = { "SOME_OTHER_ROLE" }) // 가짜 사용자 설정
    void addingInventoryWithoutProperRoleFails() {
        this.webTestClient.post().uri("/")
                .exchange() // <3>
                .expectStatus().isForbidden();
        // 403 Forbidden 은 사용자가 authenticated(인증) 은 됬지만, not authorized (인가받지 못함) 을 의미한다.
    }

    // inventory 역할이 item 추가 시도 (올바른 케이스)
    @Test
    @WithMockUser(username = "bob", roles = { "INVENTORY" })
    void addingInventoryWithProperRoleSucceeds() {
        this.webTestClient //
                .post().uri("/") //
                .contentType(MediaType.APPLICATION_JSON) //
                .bodyValue("{" + //
                        "\"name\": \"iPhone 11\", " + //
                        "\"description\": \"upgrade\", " + //
                        "\"price\": 999.99" + //
                        "}") //
                .exchange() //
                .expectStatus().isOk(); //

        this.repository.findByName("iPhone 11") // <5>
                .as(StepVerifier::create) // 응답 검증을 위해 감싸기
                .expectNextMatches(item -> {
                    assertThat(item.getDescription()).isEqualTo("upgrade");
                    assertThat(item.getPrice()).isEqualTo(999.99);
                    return true;
                }) //
                .verifyComplete(); // 완료신호도 확인
    }
    // 이렇게, 보안규칙은 하나 만들 떄 마다 실패/성공 의 최소한 2개 case 는 작성해야 한다. post 를 test 했으니, delete 도 test 해야한다.
    // 보안관점에서 중요한 2가지 원칙이 있다.
    //   - 권한없는 사용자가 인가받지 않은 기능을 사용하지 못하게 해야한다.
    //   - 위 원칙을 위배할 수 있는 어떠한 단서도 사용자에게 보여주지 않아야 한다.
    //      - Hyper Media 관점에서는 링크자체도 제공하지 않아야 함을 의미한다.

    @Test
    @WithMockUser(username = "carol", roles = { "SOME_OTHER_ROLE" })
    void deletingInventoryWithoutProperRoleFails() {
        this.webTestClient.delete().uri("/some-item") //
                .exchange() //
                .expectStatus().isForbidden();
    }

    @Test
    @WithMockUser(username = "dan", roles = { "INVENTORY" })
    void deletingInventoryWithProperRoleSucceeds() {
        String id = this.repository.findByName("Alf alarm clock") //
                .map(Item::getId) //
                .block();

        this.webTestClient //
                .delete().uri("/" + id) //
                .exchange() //
                .expectStatus().isOk();

        this.repository.findByName("Alf alarm clock") //
                .as(StepVerifier::create) //
                .expectNextCount(0) //
                .verifyComplete();
    }

    // 하이퍼미디어를 사용하는 test case
    @Test
    @WithMockUser(username = "alice", roles = {"INVENTORY"})
    void navigateToItemWithInventoryAuthority(){
        // api 에 get 요청
        RepresentationModel<?> root = this.webTestClient.get().uri("/api")
                .exchange()
                .expectBody(RepresentationModel.class)
                .returnResult().getResponseBody();

        // Item 의 aggregate root link 에 get 요청
        CollectionModel<EntityModel<Item>> items = this.webTestClient.get()
                .uri(root.getRequiredLink(IanaLinkRelations.ITEM).toUri())
                .exchange()
                .expectBody(new TypeReferences.CollectionModelType<EntityModel<Item>>() {})
                .returnResult().getResponseBody();

        // assertThat(items.getLinks()).hasSize(2); 패키지가 다른가봄.
        assertThat(items.hasLink(IanaLinkRelations.SELF)).isTrue();
        assertThat(items.hasLink("add")).isTrue();

        // 첫번때 Item 의 EntityModel 획득
        EntityModel<Item> first = items.getContent().iterator().next();

        // item 의 SELF 링크를 통해 첫번째 Item 정보 획득
        EntityModel<Item> item = this.webTestClient.get()
                .uri(first.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .exchange()
                .expectBody(new TypeReferences.EntityModelType<Item>() {})
                .returnResult().getResponseBody();

        //assertThat(item.getLinks()).hasSize(3);
        assertThat(item.hasLink(IanaLinkRelations.SELF)).isTrue();
        assertThat(item.hasLink(IanaLinkRelations.ITEM)).isTrue();
        assertThat(item.hasLink("delete")).isTrue();
    }
}
