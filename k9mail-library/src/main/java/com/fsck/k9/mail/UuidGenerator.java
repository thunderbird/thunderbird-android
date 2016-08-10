package com.fsck.k9.mail;


import java.util.UUID;


public class UuidGenerator {
    private static final UuidGenerator INSTANCE = new UuidGenerator();

    public static UuidGenerator getInstance() {
        return INSTANCE;
    }

    private UuidGenerator() { }

    public UUID generateUUID() {
        return UUID.randomUUID();
    }
}
