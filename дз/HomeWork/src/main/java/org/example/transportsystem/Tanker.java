package org.example.transportsystem;

class Tanker extends Transport implements HasParts, HasProperties {
    public Tanker() {
        super("Tanker");
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
