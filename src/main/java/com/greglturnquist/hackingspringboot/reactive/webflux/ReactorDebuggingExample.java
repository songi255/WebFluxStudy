package com.greglturnquist.hackingspringboot.reactive.webflux;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Random;

public class ReactorDebuggingExample {
    /*
    public static void main(String[] args) {
        Hooks.onOperatorDebug(); // reactor flow 를 stack trace 할 수 있게 활성화한다.

        Mono<Integer> source;
        if (new Random().nextBoolean()) {
            source = Flux.range(1, 10).elementAt(5);
        }else {
            source = Flux.just(1, 2, 3, 4).elementAt(5);
        }

        source
                .subscribeOn(Schedulers.parallel())
                .block();
    }
    */
}
