package org.example.transportsystem;

class Airplane extends Transport implements HasParts, HasProperties{
    public Airplane() {
        super("Airplane");
    }
    @Override

    public void display() {
        System.out.println("I am a " + name);
    }

    @Override
    public void hasPart(String part) {
        System.out.println(name + " has part: " + part);
    }

    @Override
    public void hasProperty(String property) {
        System.out.println(name + " has property: " + property);
    }
}
