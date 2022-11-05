package com.greglturnquist.hackingspringboot.reactive.webflux;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ItemRepository extends ReactiveCrudRepository<Item, String>, ReactiveQueryByExampleExecutor<Item> {
    /* 업계에서는 NoSQL data store를 표준화하는 방법을 찾기위해 다양한 시도를 했지만, 아직 성공사례가 없다.
        이유는 모든 NoSQL 엔진이 각기 다르고, 저마다 특징과 장단점, 상충되는 부분이 존재하기 때문이다. 성급한 일반화는 고유한 특성을 잃어 실패하게 된다.

        spring은 어떻게 해결할까? spring 의 강력한 패러다임 중 하나는 template pattern 이다. (디자인 패턴의 template method pattern이 아니다. 혼동하지 말자)
        typesafe way로 연산을 처리하고, 다루기 복잡하고 귀찮은 것들을 추상화해서 DB 등 협력대상과 상호작용이 바르게 동작하도록 보장한다.
            - 가장 간단한 예시는 JdbcTemplate를 사용하면 DB 연결을 직접 여닫지 않아도 된다.

        Spring Data module 에는 repository 계층이 있다. template 에 포함된 풍부한 연산을 사용하다보면, 그 많은 API를 모두 익히는 것이
        결국 새로운 버전의 mongoQL 쿼리를 typesafe way 로 작성하는 거나 다른게 없다는 느낌을 받게 될 것이다.
        결론적으로, CRUD 같은 단순하고 공통적인 연산은 추상화해서 표준화된 방식으로 접근하자 -> 이것이 repository 이다.

        이렇게, repository Interface 를 적절하게 상속해서 만든다. <조회타입, Key> 이다. 이렇게만 하면 끝이다... 대박..
        이제 상속받은 save, saveall, findeById, count, delete 등등... 바로 사용가능하다!
            - 눈여겨볼 것은 모든 method의 return 이 Flux, Mono 이다. 구독하다가 MongoDB가 데이터를 제공할 준비가 되면 받을 수 있다는 뜻이다.
            - 모든 reactor type은 Publisher reactive stream type을 구현한다. 예를 들면 RxJava 코드가 받아서 사용할 수도 있다.
                - parameter로 받을 수도 있으므로, reactive stream 명세를 준수하여 호환성을 보장한다.

        여기서 문제. repo.save() 해도 리액티브라서 구독하기 전까지 아무런 동작이 없다.
            - repo.save().subscribe() 하면 될까? 시작과정에서 문제의 소지가 있다.
                - Netty가 시작되면, subscriber가, App launch thread 가 event loop 를 deadlock 에 빠뜨리게 할 위험이 존재한다. (왜?)
        따라서 App 시작시점에 어떤 작업을 하려면 그 순간만큼은 blocking version을 사용하는 것이 좋다. (시간이 지난 지금은 해결되지 않았을까?)
    */

    // spring repository의 환상적인 기능을 보자. 아래와 같이 메소드 이름규칙을 따라서 메소드를 정의하면 걍 쿼리가 생긴다;; (필요한 쿼리의 80% 정도는 가능하다;;)
    // MongoDB 든, JPA 든 .. 아무 상관없다.
    Flux<Item> findByNameContaining(String partialName);
    // 애초에 IntelliJ 에서 자동완성이 된다... 예시를 몇개 적겠다..
    // findByNameAndDescription : Name과 Description 이 일치하는 것
    // findTop10ByName : 첫 10개만 질의
    // findFirst10ByName : 첫 10개만 질의
    // findByNameIgnoreCase
    // findByNameAndDescriptionAllIgnoreCase : Name, desc 모두 대소문자 무시
    // OrderByDescriptionASC
    // findByReleaseDateBefore/After : date 이전인 데이터
    // GreaterThan/GreaterThanEqual//LessThan
    // Between(int from, int to)
    // In/NotIn(Collection unitss)
    // NotNull/IsNotNull/Null/IsNull
    // Like(String f) : f 를 포함
    // NotLike/IsNotLike
    // StartingWith/EndingWith
    // NotContaining
    // Regex
    // Near(Point p, Distance max) : Point 타입을 기준으로 작성됨.
    // Near(Point p, Distance min, Distance max)
    // Within(Curcle c / Box b) : Point 기준.
    // isTrue/False/Exists
    // 등등.. 위에 건 mongoDB용.. 어마무시하다 ㄹㅇ.. find 말고 delete 에도 적용할 수 있다.

    // 지원하는 반환타입도 Flux, Mono 외에 정말 많다. 그냥 쌩으로 반환도 되고, Collection, Stream, Optional, Future 에 담는다던가,
    // @Async completableFuture 같은거나, Slice, Page, GeoResult 등등.. 정말 다양한 반환타입을 지원한다. 자세한건 문서에서..

    // ~~~ repository query keywords 로 검색하자. store 별로 다르다. JPA, Cassandra.. 등등....


    // 이제 이걸로 물가능한 나머지 쿼리는 어떻게 할까? 걍 직접 짜주면 된다.
    @Query("{ 'name' :  ?0, 'age':  ?1 }")
    Flux<Item> findItemsForCustomerMonthlyReport(String name, int age);

    @Query(sort = "{ 'age' :  -1 }")
    Flux<Item> findSortedStuffForWeeklyReport();


    // 이제 만약, 여러 필드에 대해 필터링 기능이 필요하다거나 하면 어떻게 해야할까? 2^가짓수 조합을 전부 적어야 할까? Example Query를 이용하면 된다!!
    // 우선 ReactiveQueryByExampleExecutor 를 상속해야한다.
    // .. 끝이다. 이후 Service에서 Example 객체를 사용하여 query 한다. inventoryService 에 적어놓겠다.









}
