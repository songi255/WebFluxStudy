package com.greglturnquist.hackingspringboot.reactive.messaging;

import com.greglturnquist.hackingspringboot.reactive.webflux.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;

@RestController
public class SpringAmqpItemController {
    private static final Logger log = LoggerFactory.getLogger(SpringAmqpItemController.class);

    // RabbitMQ 를 사용하므로, RabbitTemplate 가 주입된다.
    private final AmqpTemplate template;

    public SpringAmqpItemController(AmqpTemplate template) {
        this.template = template;
    }

    @PostMapping("/items")
    Mono<ResponseEntity<?>> addNewItemUsingSpringAmqp(@RequestBody Mono<Item> item){
        return item
                // AmqpTemplate 는 Blocking API 를 호출한다. -> subscribeOn() 을 이용해서,
                // bounded elastic scheduler 에서 관리하는 별도 Thread 에서 실행되게 한다.
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(content -> {
                    return Mono
                            .fromCallable(() -> {
                                // fromCallable 을 통해, Lambda 호출을 Callable 로 감쌌다.
                                this.template.convertAndSend(
                                        // hacking-spring-boot exchange 로 
                                        // routing key : new-items-spring-amqp 와 함께
                                        // Item data (content) 전송
                                        "hacking-spring-boot", "new-items-spring-amqp", content
                                );
                                // 추가된 item 의 URI 전송
                                return ResponseEntity.created(URI.create("/items")).build();
                            });
                });
    }
    /* 다시 한번 보면, RabbitMQ 가 Blocking 이 있다.
        결국 비동기과정으로 돌아가더라도 Blocking 이다. 아무리 짧다지만 이게 쌓이고 쌓이면 나중에 무시 못 할 부담이 될 수 있다.

        project reactor 에서는 그래서 이를 해결하기 위한 방법을 만들어뒀다 (scheduler 로 감싸기)
            - reactor 는 보던바 같이, 작업 절차를 기술한다.
            - 여기서 scheduler 를 통해, 개별 단계가 실행될 스레드를 지정할 수 있다.

        코루틴방식으로, 반응할 준비가 되있을떄만 개별 수행단계를 실행하게 하면 싱글스레드로 처리가 가능하다.
        하나의 작업단계 완료되면, Thread 는 reactor 의 작업 코디네이터에 반환되고, 다음에 어떤 작업을 할 지 결정된다.

        Reactor 는 다음과 같은 여러방식으로 Thread 를 사용할 수 있다.
            - Schedulers.immediate() : 현재 스레드
            - Schedulers.single() : 재사용가능한 1개의 스레드.
                - 현재 수행중인 reactor flow 뿐만아니라, 호출되는 모든 작업이 동일한 1개의 스레드에서 실행된다.
            - Schedulers.newSingle() : 새로 생성한 전용스레드
            - Schedulers.boundedElastic() : 작업량에 따라 Thread 수가 늘어나거나 줄어드는 신축성있는 thread pool
            - Schedulers.parallel() : 병렬에 적합하도록 최적화된 고정크기 worker thread pool
            - Schedulers.fromExecutorService() : ExecutorService 인스턴스를 감싸서 재사용
            single(), newSingle(), parallel() 은 Nonblocking thread 를 생성한다.
                - 여기서 blocking 코드가 호출되면 IllegalStateException 이 발생한다.

        그럼 Scheduler 는 어떻게 변경할까? 2가지 방법이 있다.
            - publishOn()
                - 호출시점 이후로는 지정한 Scheduler 사용.
                - 이를 이용해서 여러번 바꿀 수도 있다.
            - subscribeOn()
                - flow 전체에 사용되는 Scheduler 지정. (위치가 중요하지는 않은 것이다!)
                - 전체에 영향을 미치므로 범위가 더 넓다.




    */


}
