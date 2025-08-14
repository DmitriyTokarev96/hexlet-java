package org.example.foodchain;

class Water extends Animal {
    public Water() {
        super("Water");
    }

    @Override
    public void display() {
        System.out.println("I am water.");
    }
}
