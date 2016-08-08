package com.fsck.k9.mail;


import java.util.Locale;
import java.util.Random;


public class BoundaryGenerator {
    private static final BoundaryGenerator INSTANCE = new BoundaryGenerator();


    private Random random;


    public static BoundaryGenerator getInstance() {
        return INSTANCE;
    }

    private BoundaryGenerator() {
        this.random = new Random();
    }

    public String generateBoundary() {
        StringBuilder sb = new StringBuilder();
        sb.append("----");
        for (int i = 0; i < 30; i++) {
            sb.append(Integer.toString(random.nextInt(36), 36));
        }
        return sb.toString().toUpperCase(Locale.US);
    }
}
