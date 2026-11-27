package service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class StaffAuthenticationService {

    public static final int PASSWORD_ITERATIONS = 600000;

    private static final int PASSWORD_SALT_BYTES = 16;
    private static final int PASSWORD_HASH_BYTES = 32;
    private static final long SESSION_LIFETIME_MILLIS =
            8L * 60 * 60 * 1000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String staffUsername;
    private final byte[] passwordSalt;
    private final byte[] passwordHash;
    private final Map<String, Long> sessions =
            new ConcurrentHashMap<>();

    public StaffAuthenticationService(
            String staffUsername,
            String encodedPasswordHash
    ) {
        boolean usernameMissing = staffUsername == null
                || staffUsername.trim().isEmpty();
        boolean hashMissing = encodedPasswordHash == null
                || encodedPasswordHash.trim().isEmpty();

        if (usernameMissing && hashMissing) {
            this.staffUsername = null;
            this.passwordSalt = null;
            this.passwordHash = null;
            return;
        }

        if (usernameMissing || hashMissing) {
            throw new IllegalArgumentException(
                    "Both STAFF_USERNAME and STAFF_PASSWORD_HASH are required."
            );
        }

        this.staffUsername = staffUsername.trim();
        byte[][] hashParts = parsePasswordHash(encodedPasswordHash.trim());
        this.passwordSalt = hashParts[0];
        this.passwordHash = hashParts[1];
    }

    public boolean isConfigured() {
        return staffUsername != null;
    }

    public String createSession(String username, char[] password) {
        if (!isConfigured() || password == null) {
            return null;
        }

        byte[] candidateHash = derivePasswordHash(
                password, passwordSalt, PASSWORD_ITERATIONS
        );
        boolean validCredentials =
                (username != null && staffUsername.equals(username.trim()))
                & MessageDigest.isEqual(passwordHash, candidateHash);
        Arrays.fill(candidateHash, (byte) 0);

        if (!validCredentials) {
            return null;
        }

        removeExpiredSessions(System.currentTimeMillis());
        byte[] tokenBytes = new byte[32];
        RANDOM.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);

        sessions.put(
                hashSessionToken(token),
                System.currentTimeMillis() + SESSION_LIFETIME_MILLIS
        );
        Arrays.fill(tokenBytes, (byte) 0);
        return token;
    }

    public boolean isAuthenticated(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) {
            return false;
        }

        String tokenHash = hashSessionToken(token);
        Long expiresAt = sessions.get(tokenHash);

        if (expiresAt == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (expiresAt <= now) {
            sessions.remove(tokenHash, expiresAt);
            return false;
        }

        return true;
    }

    public boolean revokeSession(String token) {
        if (!isAuthenticated(token)) {
            return false;
        }

        return sessions.remove(hashSessionToken(token)) != null;
    }

    public static String createPasswordHash(char[] password) {
        if (password == null || password.length < 12) {
            throw new IllegalArgumentException(
                    "Staff passwords must contain at least 12 characters."
            );
        }

        byte[] salt = new byte[PASSWORD_SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derivedHash = derivePasswordHash(
                password, salt, PASSWORD_ITERATIONS
        );

        String encoded = "pbkdf2-sha256$"
                + PASSWORD_ITERATIONS + "$"
                + Base64.getUrlEncoder().withoutPadding().encodeToString(salt)
                + "$"
                + Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(derivedHash);

        Arrays.fill(salt, (byte) 0);
        Arrays.fill(derivedHash, (byte) 0);
        return encoded;
    }

    private static byte[][] parsePasswordHash(String encodedPasswordHash) {
        String[] parts = encodedPasswordHash.split("\\$", -1);

        if (parts.length != 4
                || !"pbkdf2-sha256".equals(parts[0])
                || !Integer.toString(PASSWORD_ITERATIONS).equals(parts[1])) {
            throw new IllegalArgumentException(
                    "STAFF_PASSWORD_HASH has an unsupported format."
            );
        }

        try {
            byte[] salt = Base64.getUrlDecoder().decode(parts[2]);
            byte[] hash = Base64.getUrlDecoder().decode(parts[3]);

            if (salt.length != PASSWORD_SALT_BYTES
                    || hash.length != PASSWORD_HASH_BYTES) {
                throw new IllegalArgumentException(
                        "STAFF_PASSWORD_HASH has an unsupported format."
                );
            }

            return new byte[][] { salt, hash };
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException(
                    "STAFF_PASSWORD_HASH has an unsupported format."
            );
        }
    }

    private static byte[] derivePasswordHash(
            char[] password,
            byte[] salt,
            int iterations
    ) {
        PBEKeySpec keySpec = new PBEKeySpec(
                password, salt, iterations, PASSWORD_HASH_BYTES * 8
        );

        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(keySpec)
                    .getEncoded();
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException(
                    "PBKDF2 password hashing is unavailable.",
                    error
            );
        } finally {
            keySpec.clearPassword();
        }
    }

    private static String hashSessionToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(digest);
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException(
                    "SHA-256 session hashing is unavailable.",
                    error
            );
        }
    }

    private void removeExpiredSessions(long now) {
        sessions.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
