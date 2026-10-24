package com.zelora.app.util;

import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

// auth
public final class PasswordUtil {
    private static final int ITERATIONS = 100000;
    private static final int LEGACY_ITERATIONS = 10000;
    private static final int KEY_BITS = 256;

    private PasswordUtil() {}

    public static String newSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    public static String hash(String password, String saltB64) {
        return ITERATIONS + "$" + derive(password, saltB64, ITERATIONS);
    }

    public static boolean matches(String password, String saltB64, String stored) {
        if (password == null || stored == null) return false;
        int iterations = LEGACY_ITERATIONS;
        String expected = stored;
        int sep = stored.indexOf('$');
        if (sep > 0) {
            try {
                iterations = Integer.parseInt(stored.substring(0, sep));
            } catch (NumberFormatException e) {
                return false;
            }
            expected = stored.substring(sep + 1);
        }
        String actual = derive(password, saltB64, iterations);
        return MessageDigest.isEqual(actual.getBytes(), expected.getBytes());
    }

    private static String derive(String password, String saltB64, int iterations) {
        try {
            byte[] salt = Base64.decode(saltB64, Base64.NO_WRAP);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
            byte[] out = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
                    .generateSecret(spec).getEncoded();
            return Base64.encodeToString(out, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing failed", e);
        }
    }
}
