package com.greglturnquist.hackingspringboot.reactive.webflux;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service // Component 가 자체 상태를 가지고 있지 않은 Service 임을 나타낸다.
public class CartService {
    private final ItemRepository itemRepository;
    private final CartRepository cartRepository;

    public CartService(ItemRepository itemRepository, CartRepository cartRepository) {
        this.itemRepository = itemRepository;
        this.cartRepository = cartRepository;
    }

    Mono<Cart> addToCart(String cartId, String id){
        return this.cartRepository.findById(cartId)
                .defaultIfEmpty(new Cart(cartId))
                .flatMap(cart -> cart.getCartItems().stream()
                        .filter(cartItem -> cartItem.getItem().getId().equals(id))
                        .findAny()
                        .map(cartItem -> {
                            cartItem.increment();
                            return Mono.just(cart);
                        })
                        .orElseGet(() ->
                            this.itemRepository.findById(id)
                                    .map(CartItem::new)
                                    .doOnNext(cartItem -> cart.getCartItems().add(cartItem))
                                    .map(cartItem -> cart)
                        ))
                .flatMap(this.cartRepository::save);
    }// do on next 는 아마.. map 이 약간 blocking 처럼 작동해서 완료 시그널을 받으면 실행한다 같은 느낌인듯??
}
