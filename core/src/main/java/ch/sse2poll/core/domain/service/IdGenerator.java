package ch.sse2poll.core.domain.service;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Simple ULID-like generator. Not a full ULID spec implementation,
 * but stable enough for uniqueness in this context.
 */
public final class IdGenerator {
    private static final SecureRandom RNG = new SecureRandom();

    private IdGenerator() {}

    public static String ulid() {
        long time = Instant.now().toEpochMilli();
        long rnd1 = RNG.nextLong();
        long rnd2 = RNG.nextLong();
        return toBase32(time) + toBase32(rnd1) + toBase32(rnd2);
    }

    private static final char[] ALPH = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private static String toBase32(long v) {
        char[] buf = new char[13];
        for (int i = 12; i >= 0; i--) {
            int idx = (int)(v & 31);
            buf[i] = ALPH[idx & 31];
            v >>>= 5;
        }
        return new String(buf);
    }
}

