package com.meridian.platform.entitlements.infra;

import java.security.SecureRandom;

/** ENT-nnnnnnnnn / APR-nnnnnnnnn ids, same shape as the fixtures generate. */
public final class Ids {

    private static final SecureRandom RND = new SecureRandom();

    private Ids() {
    }

    public static String next(String prefix) {
        return prefix + "-" + (100_000_000 + RND.nextInt(900_000_000));
    }
}
