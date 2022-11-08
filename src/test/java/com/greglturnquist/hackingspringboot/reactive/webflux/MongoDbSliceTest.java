package com.greglturnquist.hackingspringboot.reactive.webflux;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest // Spring Boot 기능 중, mongoDB test 관련 기능을 활성화한다.
// 그 외에 @Component 붙은 다른 Bean 정의를 무시한다. 소요시간이 절반 가까이 줄어들었다...
// Spring framework 초기부터 계속 진행되어 온 것이 바로 test 성능개선. Spring Application Context 는 test 성능을 위해 재사용될 수 있다.
// 바로 이 점이 단위테스트, 통합테스트, 슬라이스 테스트 사이 trade-off 를 강조하는 이유이다.
public class MongoDbSliceTest {
    /* Unit test 와 통합 test 사이 중간정도인 test는 없을까? slice test 라고 존재한다!
        SpringBoot는 다양한 test 지원기능들이 준비되어있다(@ 생략).
            - AutoConfigureRestDocs, DataJdbcTest, DataJpaTest, DataLdapTest, DataMongoTest, DataNeo4JTest, DataRedisTest,
            - JdbcTest, JooqTest, JsonTest, RestClientTest, WebFluxTest, WebMvcTest......
            - 여기서 나온 모든 ...Test 들은 JUnit5 의 @ExtendWith(SpringExtension.class) 를 포함하고 있으므로, 직접 추가하지 않아도 된다.
    */

    @Autowired ItemRepository repository;

    @Test
    void itemRepositorySaveItems(){
        Item sampleItem = new Item("name", "description", 1.99);

        repository.save(sampleItem)
                .as(StepVerifier::create)
                .expectNextMatches(item -> {
                    assertThat(item.getId()).isNotNull();
                    assertThat(item.getName()).isEqualTo("name");
                    assertThat(item.getDescription()).isEqualTo("description");
                    assertThat(item.getPrice()).isEqualTo(1.99);

                    return true;
                })
                .verifyComplete();
    }

}
