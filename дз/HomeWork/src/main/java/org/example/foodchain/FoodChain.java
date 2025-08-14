package org.example.foodchain;

public class FoodChain {
    public static void main(String[] args) {
        Animal cat = new Cat();
        Animal bear = new Bear();
        Animal fish = new Fish();
        Animal whale = new Whale();
        Animal water = new Water();

        cat.display();
        bear.display();
        fish.display();
        whale.display();
        water.display();
    }
}
