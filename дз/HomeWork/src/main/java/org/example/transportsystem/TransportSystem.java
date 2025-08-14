package org.example.transportsystem;


public class TransportSystem {
    public static void main(String[] args) {
    Transport airplane = new Airplane();
    Transport boat = new Boat();
    Transport helicopter = new Helicopter();
    Transport tanker = new Tanker();
    Transport taxi = new Taxi();
    Transport truck = new Truck();

    airplane.display();
    boat.display();
    helicopter.display();
    tanker.display();
    taxi.display();
    truck.display();

        ((HasParts) airplane).hasPart("Wing");
        ((HasProperties) airplane).hasProperty("Can fly");

        ((HasParts) helicopter).hasPart("Rotor");
        ((HasProperties) helicopter).hasProperty("Can hover");

        ((HasParts) boat).hasPart("Hull");
        ((HasProperties) boat).hasProperty("Floats on water");

        ((HasParts) tanker).hasPart("Storage tank");
        ((HasProperties) tanker).hasProperty("Carries liquids");

        ((HasParts) truck).hasPart("Cargo area");
        ((HasProperties) truck).hasProperty("Transports goods");

        ((HasParts) taxi).hasPart("Meter");
        ((HasProperties) taxi).hasProperty("Transports passengers");
    }
}
