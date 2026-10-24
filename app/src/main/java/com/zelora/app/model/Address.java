package com.zelora.app.model;

public class Address {
    public final int id;
    public final String recipient;
    public final String line;

    public Address(int id, String recipient, String line) {
        this.id = id;
        this.recipient = recipient;
        this.line = line;
    }
}
