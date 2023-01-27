package com.greglturnquist.hackingspringboot.reactive.security;

import com.greglturnquist.hackingspringboot.reactive.webflux.Cart;
import com.greglturnquist.hackingspringboot.reactive.webflux.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

@Controller
public class AuthController {
    @Autowired
    InventoryService inventoryService;

    // 보안관리기능을 추가한다는 것은, 현재 사용자의 세부정보에 접근할 수 있다는 점에서 또 다른 중요함이 있다.
    // 사용자 별 장바구니 보여주기 예시이다.
    @GetMapping
    Mono<Rendering> home(Authentication auth){
        // 이 매개변수는 Spring Security 가 subscriber context 에서 Authentication 정보를 추출해서 주입해준다.
        return Mono.just(Rendering.view("home.html")
                .modelAttribute("items", this.inventoryService.getInventory())
                .modelAttribute("cart", this.inventoryService.getCart(cartName(auth))
                        .defaultIfEmpty(new Cart(cartName(auth))))
                // auth 를 model 에 추가하면, 사용자 context 정보를 보여줄 수 있는 확실한 장점이 추가된다.
                // ${auth.authorities/name} 등 사용하면 된다. 생략한다.
                .modelAttribute("auth", auth)
                .build());
    }

    private static String cartName(Authentication auth){
        return auth.getName() + "'s Cart";
    }

    // method 수준 보안을 적용해보자. 아래는 그냥 적었다. 의미는 없는 메소드이다.
    @PreAuthorize("hasRole('" + "INVENTORY" + "')")
    // SpEL (Spring Expression Language) 표현식을 사용해서 권한을 확인한다.
    @GetMapping("/any")
    Mono<String> any(Authentication auth){ // auth 맥락이 필요하다면 이렇게 주입받으면 된다.
        return Mono.just("any");
    }

    // @PostAuthorize 도 있는데, method 호출 후 보안규칙을 적용한다.
    //   - 중요 결정사항이 포함 된 핵심내용이 반환되는 경우 사용하면 좋다 (?)
    //   - SpEL 표현식에 단순히 return Object 를 사용해서 반환값을 참조하면 된다.
    //   - 하지만 DB 를 수정하고 반환값으로 제어하는 것은 비용이 든다.

    // @PostFilter 의 경우, 결과목록 반환받아 필터링을 하고싶을 때 쓰면 좋다.
    //   - 이렇게 하면, 사용자가 볼 수 없는 데이터는 반환목록에서 제외할 수 있다.
    //   - 근데 편하긴 하지만, 결국 쓸모없는 데이터까지 조회하는거 자체가 비효율적이다.

    // 그래서 Spring Security 는 Auth 객체를 이용해서 볼 수 있는 데이터만 조회하는 기능을 지원한다.
    //   - Spring Security + Spring Data 의 통합은 @Query 를 사용할 때만 적용된다.


    // OAuth2 를 사용한 home 컨트롤러
    @GetMapping
    Mono<Rendering> home2(
            @RegisteredOAuth2AuthorizedClient OAuth2AuthorizedClient authorizedClient, // OAuth client 정보
            @AuthenticationPrincipal OAuth2User oAuth2User // 로그인 한 사용자정보
    ){
        return Mono.just(Rendering.view("home2.html")
                .modelAttribute("items", this.inventoryService.getInventory())
                .modelAttribute("cart", this.inventoryService.getCart(cartName2(oAuth2User))
                        .defaultIfEmpty(new Cart(cartName2(oAuth2User))))

                // 인증 상세정보 조회는 조금 복잡하다.
                .modelAttribute("userName", oAuth2User.getName())
                .modelAttribute("authorities", oAuth2User.getAuthorities())
                .modelAttribute("clientName", authorizedClient.getClientRegistration().getClientName())
                .modelAttribute("userAttributes", oAuth2User.getAttributes())
                .build());


    }

    private static String cartName2(OAuth2User oAuth2User){
        return oAuth2User.getName() + "'s Cart";
    }

    // oauth2 적용한 template 를 참고해보자.
}
