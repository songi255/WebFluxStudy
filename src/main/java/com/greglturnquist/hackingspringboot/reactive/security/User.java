package com.greglturnquist.hackingspringboot.reactive.security;

import org.springframework.data.annotation.Id;

import java.util.List;

/* 보안 운영팀에서는 시스템에 접근할 사용자를 분리된 외부의 도구로 관리하길 원할 가능성이 높다.
    Spring security 와 reactive repository 를 연결해야 한다.

    보안팀의 사용자 관리도구가 mongo db 라고 가정하겠다. 다르다고 해도, 식별정보 / 비번 / 역할 이 저장되는 근본개념은 같다.

    spring security 는 식별정보를 하드코딩해서 저장할 수 있는 여러 방법을 제공한다.
    하지만 역시 db 를 연결하는게 더 쉽다.
*/

public class User {
    private @Id String id;
    private String name;
    private String password;
    private List<String> roles;

    // Spring 에서는 인자 없는 기본생성자가 반드시 필요할 때가 있다.
    private User(){};

    // key 를 포함한 모든 필드를 받는 생성자는 test 에서 편리하게 사용된다.
    public User(String id, String name, String password, List<String> roles) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.roles = roles;
    }

    // key 제외한 생성자는 data 저장용이다.
    public User(String name, String password, List<String> roles) {
        this.name = name;
        this.password = password;
        this.roles = roles;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
