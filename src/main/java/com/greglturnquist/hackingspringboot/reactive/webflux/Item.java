package com.greglturnquist.hackingspringboot.reactive.webflux;

import org.springframework.data.annotation.Id;

public class Item {
    // 도메인객체. Entity modeling 이나 domain-driven-design 관련 내용은 따로 알아보자.

    // MongoDB의 ObjectId 값으로 사용된다. 이 값은 모든 MongoDB Collection 에서 _id 필드로 사용된다.
    // Spring data commons 의 @Id 를 사용했는데, 모든 DB 특화 솔루션을 가지지만 일부 개념 (기본키같은)은 commons에 이미 포함되어있다.
    // 예를 들면, JPA에서는 javax.persistence.Id 를 사용해서 지정할 수 있지만, JPA 미지원 타 DB에서는 commons 것을 사용한다.
    private @Id String id;
    private String name;
    private double price;
    private String description;

    private Item(){}

    Item(String name, double price) {
        this.name = name;
        this.price = price;
    }

    public Item(String name, String description, double price) {
        this.name = name;
        this.price = price;
        this.description = description;
    }

    public Item(String id, String name, String description, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
    }

    // boiler plate

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Item{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                '}';
    }
}
