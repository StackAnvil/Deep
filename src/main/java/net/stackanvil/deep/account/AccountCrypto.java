package net.stackanvil.deep.account;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Password-based encryption for stored account sessions: PBKDF2-HMAC-SHA256 for the key and AES-GCM for the data.
 */
public final class AccountCrypto {

    public static final int DEFAULT_ITERATIONS = 310_000;
    static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private AccountCrypto() {
    }

    public static byte[] newSalt(final SecureRandom random) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        return salt;
    }

    public static SecretKey deriveKey(final char[] password, final byte[] salt, final int iterations) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            byte[] encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            try {
                return new SecretKeySpec(encoded, "AES");
            } finally {
                Arrays.fill(encoded, (byte) 0);
            }
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * Encrypts data with a fresh random IV.
     *
     * @return Base64 of the IV followed by the ciphertext and tag
     */
    public static String encrypt(final SecretKey key, final byte[] plaintext, final SecureRandom random) throws GeneralSecurityException {
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
        byte[] ciphertext = cipher.doFinal(plaintext);
        return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array());
    }

    /**
     * Decrypts data from {@link #encrypt}. A wrong key fails the GCM tag check and throws.
     */
    public static byte[] decrypt(final SecretKey key, final String data) throws GeneralSecurityException {
        byte[] bytes = Base64.getDecoder().decode(data);
        if (bytes.length <= IV_BYTES) {
            throw new GeneralSecurityException("Encrypted data is too short");
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES));
        return cipher.doFinal(bytes, IV_BYTES, bytes.length - IV_BYTES);
    }
}
