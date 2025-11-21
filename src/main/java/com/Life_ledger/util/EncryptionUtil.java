package com.Life_ledger.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class EncryptionUtil {

    private static final String AES = "AES";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 128;
    private static final int T_LEN = 128; // GCM tag length

    // Use a fixed key for now (can move to .env later)
    private static final String secretKey = "MySuperSecretKey"; // must be 16 bytes
    private static final String initVector = "RandomInitVector"; // 16 bytes

    public String encrypt(String plainText) {
        try {
            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), AES);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(T_LEN, initVector.getBytes());

            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error while encrypting: " + e.getMessage(), e);
        }
    }

    public String decrypt(String cipherText) {
        try {
            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(), AES);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(T_LEN, initVector.getBytes());

            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

            byte[] decoded = Base64.getDecoder().decode(cipherText);
            return new String(cipher.doFinal(decoded));
        } catch (Exception e) {
            throw new RuntimeException("Error while decrypting: " + e.getMessage(), e);
        }
    }
}
