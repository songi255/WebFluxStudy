package com.greglturnquist.hackingspringboot.reactive.messaging;

import com.greglturnquist.hackingspringboot.reactive.webflux.Item;
import com.greglturnquist.hackingspringboot.reactive.webflux.ItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.management.ValueExp;

/* AMQP Consumer. 메시지를 받으면 실행된다.
    Consumer 는 여러가지 방법으로 만들 수 있다.
        - 가장 단순한 AmqpTemplate.receive(queueName) 이 가장 좋다고는 할 수 없다.
            - 특히, 부하가 많은 상황에서는 적합하지 않다.
    더 많은 message 를 polling 이나 callback 등으로 처리할수도 있다.
    하지만 결국 @RabbitListener 를 사용하는 것이 가장 유연하고 편리하다.
*/
@Service
public class SpringAmqpItemService {
    private static final Logger log = LoggerFactory.getLogger(SpringAmqpItemService.class);

    private final ItemRepository repository;

    public SpringAmqpItemService(ItemRepository repository) {
        this.repository = repository;
    }

    // RabbitListener 는 Spring AMQP 가 가능한 가장 효율적인 Cache & Pooling 메커니즘을 적용하고, Background 에 Listener 를 등록한다.
    // 직렬화는 기본적으로 Java Serialization 과 연동된다. 메시지를 Serializable 하게 구현하면 가능은 하지만 피해야 한다.
    //      - 알다시피, 역직렬화가 Java에 포함된 여러 보안검사를 우회하기 때문에, 다양한 보안공격에 활용되온, 필요악이다.
    // -> 그럼 이젠 알자나.. -> Jackson 쓰자. 성능저하된다는 확실한 벤치마크가 나오지 않는 한 Jackson 을 사용할 것을 추천한다.
    @RabbitListener( // 얘가 붙으면 메시지 받을 수 있다.
            ackMode = "MANUAL",
            bindings = @QueueBinding( // queue 를 exchange 에 binding 하는 방법을 지정.
                    value = @Queue, // 임의의 지속성 없는 익명큐 생성. (특정 큐 바인딩하고 싶으면 인자로 큐이름 지정. durable, exclusive, autoDelete 등도 있음)
                    exchange = @Exchange("hacking-spring-boot"), // 큐와 연결될 exchange.
                    key = "new-item-spring-amqp" // routing key
            )
    )
    // Spring AMQP 는 reactor 타입도 처리할 수 있으므로, 구독도 AMQP에 위임하면 된다.
    public Mono<Void> processNewItemViaSpringAmqp(Item item){ // message 에 들어있던 item 이 전달된다.
        log.debug("Consuming => " + item);
        return this.repository.save(item).then();
    }

    /* 익명큐 vs named queue
        동일 메시지를 여러 consumer 가 사용해야 하는 상황에서는, 용도에 맞게 설정하는게 중요하다.
            - 만약 2개의 consumer 가 동일 큐 를 사용하도록 설정되면 메시지는 둘 중 1명만 받는다. (복불복)
            - 각자 다른 큐를 사용하면 메시지가 복제된다.
    */

}
