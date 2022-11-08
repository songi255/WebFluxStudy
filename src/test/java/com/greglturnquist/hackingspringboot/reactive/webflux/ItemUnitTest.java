package com.greglturnquist.hackingspringboot.reactive.webflux;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ItemUnitTest {
    @Test
    void itemBasicsShouldWork(){
        Item sampleItem = new Item("item1", "TV tray", "Alf TV tray", 19.99);

        assertThat(sampleItem.getId()).isEqualTo("item1");
        assertThat(sampleItem.getName()).isEqualTo("TV tray");
        assertThat(sampleItem.getDescription()).isEqualTo("Alf TV tray");
        assertThat(sampleItem.getPrice()).isEqualTo(19.99);

        // 이 외에 tostring, equals 까지도 검사를 한다.

        // Unit Test 는 얼마나 깊게 수행해야 할까? 사람마다 의견이 갈린다. getter와 setter도 모두 test 할 필요가 있을까?
        // 저자는 절대적으로 필요하다고 생각한다고 한다. 전부 domain 객체가 준수해야하는 핵심계약이기 때문이다.
        // domain 객체에 validation 규칙을 포함시킨다면, constructor, getter, setter에 규칙이 작성돼야 한다. 처음엔 없었지만 나중에 생겼다고 해도 이런 변경도 테스트에 반영되어야 한다.
        // 예를 들어, toString 출력값의 각 field값에 따라 code를 쓰기도 한다. 만약 tostring 이 의도와 다르다면 문제가 생길 것이다..


    }
}
