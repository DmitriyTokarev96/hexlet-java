package org.example.foodchain;

class Fur extends Animal {
    public Fur() {
        super("Fur");
    }

    @Override
    public void display() {
        System.out.println("I am fur.");
    }
}
