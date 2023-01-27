package com.greglturnquist.hackingspringboot.reactive.security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.Arrays;

@Configuration
// method 수준 보안은 기본으로 활성화되지는 않고, 아래 어노테이션을 추가해야 한다.
// 당연히, 보안설정 class 에 추가하는 것이 가장 좋다.
@EnableReactiveMethodSecurity
// reactive 버전을 붙여야 한다. 그렇지 않으면 제대로 동작하지 않는다.
public class SecurityConfig {
    // User 객체를 조회해서 Spring Security 의 User 객체로 변환할 수 있는 Bean 을 추가한다.
    // ReactiveUserDetailsService 가 그 연동을 담당한다.
    @Bean
    public ReactiveUserDetailsService userDetailsService(UserRepository repository){
        return username -> repository.findByName(username) // 사용자정의 User 클래스를 Spring Security 의 UserDetail class 로 map 변환한다.
                .map(user -> User.withDefaultPasswordEncoder() // Spring Security 의 User 타입이다. 비번 인코더 지정도 가능한 평문형 API 를 제공한다.
                        .username(user.getName())
                        .password(user.getPassword())
                        .authorities(user.getRoles().toArray(new String[0]))
                        .build()); // 최종적으로 build() 를 통해 UserDetail 을 만든다.
    }
    // 두 User class 의 이름이 같아서 혼동될 수 있는데, 실제로 이 두 타입이 한곳에서 같이 쓰일 일은 거의 없으므로, 굳이 이름을 다르게 하지 않아도 된다.

    // 마지막으로 할 일은, Test 용 사용자 정보를 미리 로딩해두는 것이다. 아래는 이를 위한 Bean 이다.
    @Bean
    CommandLineRunner userLoader(MongoOperations operations) {
        // 자동설정을 통해 MongoOperations Bean 을 사용하려면 pom.xml 에 mongodb-driver-sync 가 있어야 한다.
        // 그렇지 않으면 reactive spring data mongodb starter 는 기본적으로 ReactiveMongoOperations Bean 을 사용한다.
        return args -> {
            operations.save(new com.greglturnquist.hackingspringboot.reactive.security.User(
                    "greg", "password", Arrays.asList("ROLE_USER") // 요 정보로 로그인할 수 있다.
            ));

            // 아래 test 용 user 를 하나 더 추가했다.
            operations.save(new com.greglturnquist.hackingspringboot.reactive.security.User(
                    "manager", "password", Arrays.asList(role(USER), role(INVENTORY))
            ));
        };
    }
    // 문서를 보면, Test 용 사용자 정보를 추가할 수 있는 다른 API 도 볼 수 있다.
    // 하지만 솔직히 mongodb 등에 test 용 사용자정보 저장한 후, repository 를 통해 직접 접근하는 방법이 가장 쉽다.
    // 물론 선택은 개발자 몪이다.

    /* 여기까지 했을 때, 정확히 어떤 기능이 활성화된 것일까?
        Spring Boot 는 @EnableWebFluxSecurity 를 적용할지 말지 결정한다. 적용된다면 다음 기능이 활성화된다.
            - HTTP BASIC 을 활성화 -> cURL 같은 도구로도 계정명 / 비밀번호 값을 전송할 수 있다.
            - HTTP FORM 을 활성화 -> 로그인되지 않은 사용자는 browser 기본 login 팝업창 대신, Spring Security 가 제공하는 로그인페이지로 redirect된다.
            - 인증 완료시 모든 자원에 접근가능하다.

        Spring Boot 는 개발자가 뭘 하려는 지 추측하고, 그에 따라 필요한 Bean 을 자동설정으로 등록한다.
            - 그래서 classpath 에 webflux 가 있으면 reactor Netty instance 를 실행하고 몇가지 view resolver 를 자동연동한다.
        하지만 Security 는 설정방법이 너무 다양하고 App 마다 천차만별이라, classpath 에 있다는 것 만으로 추측은 불가능하다.
            - Boot 1.X 까지는 어느정도 추측해서 선택지를 제공했지만, 잘 추측되지 않았다.
            - 그래서 2.X 부터는 Boot 는 단순히 Security 를 활성화하는 역할만 한다.
            - 하지만 @EnableWebFluxSecurity 를 직접 명시할 때 생성되는 WebFilterChainProxy Bean 을 찾으면, Boot 는 시큐리티설정 권한을 개발자에게 모두 넘긴다.
            - 물론 지금본것처럼 기본값이 사용되는걸 허용은 한다. 하지만 여러 정책을 지정하면서, 결국에는 개발자에게 운전석을 넘겨준다.

        개발자들은 App 설정할 때 요구사항이 굉장히 다양하다. 화면의 section 단위로, page 단위로 제어 등등... 로그인 방식도 사이트마다 다 다르다.
        하지만 어떤 작업은 반드시 절차대로 진행되어야 하고, 순서가 깨지면 쓸모가 없어진다.

        Spring Security 만큼 DI 를 잘 사용하는 project 도 많지 않을것이다.
        개발자가 만든 custom filter 를 끼워넣을 수 있는 다양한 주입점을 제공한다.

        WebFlux 에는 Servlet 이 사용되지 않아, javax.servlet.Filter hook 을 사용할 수 없다.
        하지만 Filtering 은 web app 에서는 매우 쓸모가 많은 패러다임이기에, 다른버전의 API 인 WebFilter 를 제공한다.
        WebFlux 뿐 아니라, Security 에서도 WebFilter 를 만들어 제공함으로써 WebFlux 를 지원한다.

        Spring Security 는 중요한 Filter 를 되도록 모두 등록하려고 노력하며, custom filter 도 등록하게 해준다.
        기본제공 filter 를 custom filter 로 대체하는 경우에는 상당한 주의가 필요하다.
            - app customizing 의 일반적인 경로를 한참 벗어나는 방식이다.

        이는 충분한 보안조치가 아니다. Role 별로 권한이 달라야 한다. 볼 수 없는 link 는 제공되기조차 말아야한다.
        이를 위해 custom 정책을 적용해보자.
    */

    static final String USER = "USER";
    static final String INVENTORY = "INVENTORY";

    @Bean
    SecurityWebFilterChain myCustomSecurityPolicy(ServerHttpSecurity http){
        return http
                .authorizeExchange(exchanges -> exchanges
                        // 만약 method 수준 보안방식을 사용한다면 pathMatchers 는 모두 제거해야 한다.
                        // 유일하게 csrf 만 disable 하게 된다.
                        .pathMatchers(HttpMethod.POST, "/").hasRole(INVENTORY)
                        .pathMatchers(HttpMethod.DELETE, "/**").hasRole(INVENTORY)
                        .anyExchange().authenticated() // 그 외 나머지 모든 규칙은 이 지점에서 더이상 전진하지 못하고, 인증을 거쳐야만 통과할 수 있다.
                        .and()
                        .httpBasic() // HTTP BASIC 인증을 허용한다. 이 경우 HTTP 연결이 SSL 같은 것으로 보호되야 한다.
                        // HTTP BASIC 에서는 계정명:비번 형태로 Base64로 인코딩되어 전송되며, 이는 쉽게 복원해서 비밀번호를 알 수 있다.
                        .and()
                        .formLogin()) // 로그인 정보를 HTTP FORM 으로 전송하는 것을 허용한다.
                .csrf().disable()
                .build();
    }

    // 이제 ROLE_INVENTORY 역할을 가진 test user 를 만들자.
    static String role(String auth){
        return "ROLE_" + auth;
        /* role 과 authority (역할과 권한)
            어떤 기능 수행에 적절한 권한을 가지고 있는지 확인하기 위해, 간단하면서 가장 널리 사용되는 구현이, 바로 사용자가 가진 role 의 목록을 확인하는 것이다.
            특정 URL 에는 ADMIN 역할의 사용자만 접근할 수 있게 했다면, ROLE_ADMIN 을 권한 이라고 부른다.

            하지만 접두어로 ROLE_ 을 붙이는 것이 일상적인 패러다임이 됨에 따라, Spring Security 에서는 단순히 role 만 검사하는 API 가 많이 있다.
            다시 정리. ADMIN 은 role 이고 / ROLE_ADMIN 은 authority 이다.
        */
    }

}
