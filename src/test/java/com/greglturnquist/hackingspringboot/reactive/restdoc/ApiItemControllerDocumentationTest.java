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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.greglturnquist.hackingspringboot.reactive.rest.ApiItemContoller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;


// 이 Test File 을 실행하면 Docs 가 자동생성된다.
@WebFluxTest(controllers = ApiItemContoller.class) // Controller Test 에 필요한 내용만 자동설정되게 한다.
// 이렇게 controller 지정 시, 이 controller 만 집중적으로 test 한다.
@AutoConfigureRestDocs // REST Doc 자동설정
public class ApiItemControllerDocumentationTest {
    @Autowired private WebTestClient webTestClient;
    @MockBean
    InventoryService service;
    @MockBean
    ItemRepository repository;
    // Mock 내용이 API 문서에 반영되므로, 여러 API 에 걸쳐 일관성 있는 내용이 표시되도록 Test Data 를 구성하는 것이 좋다.

    // findAll 에 대한 Docs 생성
    @Test
    void findingAllItems(){
        when(repository.findAll()).thenReturn(
                Flux.just(new Item("item-1", "Alf alarm clock", "nothing I really need", 19.99))
        );

        this.webTestClient.get().uri("/api/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody() // 본문에 여러가지 더 단언 가능하지만, 지금은 자동생성이 목표이므로, 간단한 설명을 위해 생략한다.
                .consumeWith(document("findAll", preprocessResponse(prettyPrint())));
        // REST DOC 의 document() 가 바로 문서생성의 핵심이다.
        // 첫번쨰 인자인 findAll 이름의 directory 를 생성하고, 그 안에 여러 .adoc 파일이 생성된다.
        // 두번째 인자인 preprocessResponse(~~) 는 요청결과로 반환 될 JSON 문자열을 보기 편하게(prettyPrint()) 출력한다.
    }

    /* REST Doc 은 API 문서를 다듬을 수 있도록 preprocessRequest/Response(요청/응답 전처리기) 를 제공한다.
        전처리기 종류는 아래와 같다.
            - prettyPrint() : 요청/응답 메시지에 indent 등 적용
            - removeHeaders(String... headerNames) : Spring 의 HttpHeaders 유틸클래스에 표준 Header 이름이 상수로 있으므로, 쓰면 편리하다.
            - removeMatchingHeaders(String... headerNamePatterns) : 표시안할 Header 를 Regex 로 표현
            - maskLinks() : href 항목을 ... 로 대체
                - HAL(Hypertext Application Language) 적용 시 API 문서에 하드코딩된 URI 대신 Link 를 통해 API 사용을 독려하기 위함
            - maskLinks(String mask) : href 대체문자 명시
            - replacePatterns(Pattern pattern, String replacement) : 매칭되는 문자열을 replace 로 교체
            - modifyParameters() : fluent API (평문형 API) 를 사용해서 요청 Parameter 추가, 변경, 제거
            - modifyUris() : 평문형 API 를 사용해서 local 환경 test 시 API 문서에 표시되는 URI 지정
        여기서 제공되는 기능이 부족하다면 직접 OperationPreprocessor 를 구현해서 전처리기로 사용할 수 있다.
            - OperationPreprocessorAdapter 추상클래스를 상속해서 필요한 부분만 오버라이드 하는 것이 더 편하다.
    
    
    */
    
    // 새 Item 추가에 대한 Docs
    @Test
    void postNewItem(){
        when(repository.save(any())).thenReturn(
                Mono.just(new Item("1", "Alf alarm clock", "nothing important", 19.99))
        );

        this.webTestClient.post().uri("/api/items")
                .bodyValue(new Item("Alf alarm clock", "nothing important", 19.99))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .consumeWith(document("post-new-items", preprocessResponse(prettyPrint())));
    }

    // 이제 단순히 Test 실행이 아닌 문서생성도 해야하므로, maven 의 prepare-package 단계로 실행한다.
    //  - ./mvnw clean prepare-package
    // 실행이 성공했다면 다음 내용을 포함하는 snippet(문서조각) 들이 생성된다.
    //  - cURL, HTTPie 형식에 맞는 요청명령
    //  - HTTP 형식에 맞는 요청 및 응답메시지
    //  - JSON 형식 요청/응답 본문
    // snippets 들은 target/generated-snippets 하위에 설정한 이름으로 생성된다.
    // 이제 이 snippets 들을 index.adoc 에 보기 좋게 추가하면 된다.
    // 완료했다면 localhost:8080/docs/index.html 에 접속해서 화면을 볼 수 있다.
}
