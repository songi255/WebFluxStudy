package com.greglturnquist.hackingspringboot.reactive.begin;

public class Dish {
    private String description;
    private boolean delivered;

    public Dish(String title){
        this.description = title;
        this.delivered = false;
    }

    public Dish(String title, boolean delivered) {
        this.description = title;
        this.delivered = delivered;
    }


    public static Dish deliver(Dish dish) {
        dish.delivered = true;
        return dish;
    }

    @Override
    public String toString() {
        return "Dish{" +
                "description='" + description + '\'' +
                ", delivered=" + delivered +
                '}';
    }
}
