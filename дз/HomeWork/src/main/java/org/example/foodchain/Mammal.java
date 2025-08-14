package org.example.foodchain;

class Mammal extends Animal implements Eater, Edible {
    public Mammal(String name) {
        super(name);
    }

    @Override
    public void display() {
        System.out.println("I am a " + name);
    }

    @Override
    public void eats() {
        System.out.println(name + " eats other animals.");
    }

    @Override
    public void isEaten() {
        System.out.println(name + " is eaten by another animal.");
    }
}
