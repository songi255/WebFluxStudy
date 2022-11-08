package com.greglturnquist.hackingspringboot.reactive.webflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.thymeleaf.TemplateEngine;
import reactor.blockhound.BlockHound;

@SpringBootApplication
// autoconfiguration과 component scanning을 포함하는 복합 에너테이션이다.
public class HackingSpringBootApplication {
    public static void main(String[] args) {
        //BlockHound.install(); // Spring 실행 전 BlockHound 호출
        BlockHound.builder()
                .allowBlockingCallsInside(TemplateEngine.class.getCanonicalName(), "process") // thymeleaf의 process 메서드만 허용
                .install();


        SpringApplication.run(HackingSpringBootApplication.class, args);
        // 이 클래스를 App 시작점으로 등록하는 SpringBoot Hook 이다.

        // 실행은 그냥 해도 되고, ./mvnw clean spring-boot:run 해도 된다.
        // 실행 후 $ curl -N -v localhost:8080/server 해보자.
        //   - CLI 도구로 Service를 요청한 것이다.
        //   - -N 옵션은 버퍼링을 사용하지 않고 데이터가 들어오는 대로 처리하겠다는 뜻이다.
        //   - 끊임없이 계속 결과가 출력되는 것을 볼 수 있다.

        // 정리. Controller, Domain Object, Service provide 3가지만 준비되면 서비스가 만들어진다.
    }
}

/* 자동설정
    - 설정내용을 분석해서 다양한 Bean을 자동으로 활성화
    - SpringBoot는 이를 위한 다양한 정책을 갖추고 있다.
        - ClassPath
        - 다양한 설정파일
        - 특정 Bean의 존재여부
        - 기타등등
    - 이와 같이, App의 여러 측면을 살펴보고 유추한 다음, 다양한 컴포넌트를 자동으로 활성화한다.
        - 예를들어, WebFluxConfiguration Bean은 다음에만 활성화된다.
            - reactive container 존재
            - ClassPath에 Spring WebFlux 존재
            - WebFluxConfigurationSupport 타입 bean의 부존재
                - 이 타입 Bean이 없으면 WebFlux와 Netty를 사용하는데 필요한 Bean을 자동생성한다.
                - 커스터마이징하고싶으면 이 타입으로 직접 만들면 된다. 자동생성은 비활성화될 것이다.
    - 참 좋은 기능이다. 원할때만 직접설정. 나머지는 자동설정.
*/


