package com.greglturnquist.hackingspringboot.reactive.webflux;

public class CartItem {
    // 이건 MongoDB에서 사용될 POJO 객체이다. mongodb annotation을 사용하는 등 권장사례를 따르기를 추천한다.
    // 모든 Data Store 에 범용적으로 적용할 수 있는 유일한 해법은 없다. 참고로 동일한 도메인 객체가 여러 DB engine에 저장되도록 설계하는 것은 바람직하지 않다.


    private Item item;
    private int quantity;

    private CartItem() {}

    public CartItem(Item item) {
        this.item = item;
        this.quantity = 1;
    }

    // BT

    public Item getItem() {
        return item;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "CartItem{" +
                "item=" + item +
                ", quantity=" + quantity +
                '}';
    }

    public void increment() {
        this.quantity++;
    }
}
