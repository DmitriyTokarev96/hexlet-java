package org.example.transportsystem;

class Boat extends Transport implements HasParts, HasProperties{
    public Boat() {
        super("Boat");
    }

    @Override
    public void hasPart(String part) {
        System.out.println(name + " has part: " + part);
    }

    @Override
    public void hasProperty(String property) {
        System.out.println(name + " has property: " + property);
    }

    @Override
    public void display() {
        System.out.println("I am a " + name);
    }
}
