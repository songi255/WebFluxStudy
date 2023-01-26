package com.greglturnquist.hackingspringboot.reactive.restdoc;

import com.greglturnquist.hackingspringboot.reactive.webflux.InventoryService;
import com.greglturnquist.hackingspringboot.reactive.webflux.Item;
import com.greglturnquist.hackingspringboot.reactive.webflux.ItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import com.greglturnquist.hackingspringboot.reactive.rest.AffordancesItemController;

import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;
import static reactor.core.publisher.Mono.when;

@WebFluxTest(controllers = AffordancesItemController.class)
@AutoConfigureRestDocs
public class AffordancesItemControllerDocumentationTest {
    @Autowired private WebTestClient webTestClient;
    @MockBean
    InventoryService service;
    @MockBean
    ItemRepository repository;

    @Test
    void findSingleItemAffordances(){
        when(repository.findById("item-1").thenReturn(Mono.just(
                new Item("item-1", "Alf alarm clock", "nothing I really need", 19.99)
        )));

        this.webTestClient.get().uri("/affordances/items/item-1")
                .accept(MediaTypes.HAL_FORMS_JSON) // HAL-FORMS 형식으로 받도록 Accept 헤더에 지정한다.
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(document("single-item-affordances", preprocessResponse(prettyPrint())));
        // adoc 을 확인해보면, affordances 로 인해 "_templates" 항목이 생겼다. 여기서 put 을 쓰려면 어떤 데이터를 보내는지 meta data 가 보인다.
    }
}
