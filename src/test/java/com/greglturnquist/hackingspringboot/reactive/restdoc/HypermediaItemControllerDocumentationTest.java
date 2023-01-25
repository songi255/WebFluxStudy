package com.greglturnquist.hackingspringboot.reactive.restdoc;

import com.greglturnquist.hackingspringboot.reactive.webflux.InventoryService;
import com.greglturnquist.hackingspringboot.reactive.webflux.Item;
import com.greglturnquist.hackingspringboot.reactive.webflux.ItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import rest.HypermediaItemController;

import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static reactor.core.publisher.Mono.when;

@WebFluxTest(controllers = HypermediaItemController.class)
@AutoConfigureRestDocs
public class HypermediaItemControllerDocumentationTest {
    @Autowired private WebTestClient webTestClient;
    @MockBean
    InventoryService service;
    @MockBean
    ItemRepository repository;
    
    @Test
    void findOneItem(){
        when(repository.findById("item-1")).thenReturn(Mono.just(
                new Item("item-1", "Alf alarm clock", "nothing I really need", 19.99)
        ));
        
        this.webTestClient.get().uri("/hypermedia/items/item-1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(document("findOne-hypermedia", preprocessResponse(prettyPrint()),
                        links(
                                linkWithRel("self").description("이 `Item`에 대한 공식 링크"),
                                linkWithRel("item").description("`Item` 목록링크")
                        ))); // restdoc 문서화
        // 응답 json 에는 _links 에 "self" 와 "item" 의 hyperlink 가 실려있다. 이를 토대로 문서를 생성한다.
        // Item 객체 자체정보 외에도, link 정보도 links.adoc 파일이 생겨서 거기 저장된다. 얘도 index 에 포함시키자.
    }
}
