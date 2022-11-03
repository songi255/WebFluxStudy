package com.greglturnquist.hackingspringboot.reactive.webflux;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Service
// Spring Bean 임을 나타낸다.
public class KitchenService {
    Flux<Dish> getDishes(){
        return Flux.<Dish> generate(sink -> sink.next(randomDish())) // 이번에는 Flux.generate를 사용해서, 끊임없이 계속 데이터가 공급된다.
                .delayElements(Duration.ofMillis(250)); // 제공속도를 제어한다.
        // Flux.just 는 고정 목록의 요리를 반환하고 끝이었다.

        // sink 는 Flux의 핸들러. 데이터를 감싸고 있다. Flux에 포함 될 원소를 동적으로 발행할 수 있게 해준다.
        // generate 의 파라미터는 Consumer<SynchronousSink<T>>이다. sink는 생성된 데이터가 빠져나가는 배출구라고 생각하면 된다.
    }

    private Dish randomDish(){
        return menu.get(picker.nextInt(menu.size()));
    }

    private List<Dish> menu = Arrays.asList(
            new Dish("Sesame chicken"),
            new Dish("Lo mein noodles, plain"),
            new Dish("Sweet & sour beef")
    );

    private Random picker = new Random();
}
