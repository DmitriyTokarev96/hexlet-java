package org.example.transportsystem;

abstract class Transport {
    protected String name;

    public Transport(String name) {
        this.name = name;
    }

    public abstract void display();
}
