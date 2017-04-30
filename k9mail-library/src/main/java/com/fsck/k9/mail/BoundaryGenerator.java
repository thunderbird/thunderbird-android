package com.fsck.k9.mail;


import java.util.Random;

import android.support.annotation.VisibleForTesting;


public class BoundaryGenerator {
    private static final BoundaryGenerator INSTANCE = new BoundaryGenerator(new Random());
    
    private static final int BOUNDARY_CHARACTER_COUNT = 30;
    private static final char[] BASE36_MAP = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
            'U', 'V', 'W', 'X', 'Y', 'Z'
    };


    private final Random random;


    public static BoundaryGenerator getInstance() {
        return INSTANCE;
    }

    @VisibleForTesting
    BoundaryGenerator(Random random) {
        this.random = random;
    }

    public String generateBoundary() {
        StringBuilder builder = new StringBuilder(4 + BOUNDARY_CHARACTER_COUNT);
        builder.append("----");
        
        for (int i = 0; i < BOUNDARY_CHARACTER_COUNT; i++) {
            builder.append(BASE36_MAP[random.nextInt(36)]);
        }
        
        return builder.toString();
    }
}
