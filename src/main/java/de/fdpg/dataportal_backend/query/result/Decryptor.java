package de.fdpg.dataportal_backend.query.result;

import org.springframework.security.crypto.encrypt.AesBytesEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.springframework.security.crypto.encrypt.AesBytesEncryptor.CipherAlgorithm.GCM;

/**
 * Decrypts individual log lines using the supplied private key.
 * <p>
 * The {@link #init()} method needs to be called, before using it.
 */
public final class Decryptor {

  private final PrivateKey privateKey;
  private Cipher decryptKeyCipher;

  public Decryptor(PrivateKey privateKey) {
    this.privateKey = requireNonNull(privateKey);
  }

  public void init() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
    decryptKeyCipher = Cipher.getInstance("RSA");
    decryptKeyCipher.init(Cipher.DECRYPT_MODE, privateKey);

  }

  /**
   * Decrypts the {@code encryptedLog} that consists of an encrypted symmetric key, followed by an
   * encrypted message.
   */
  public synchronized String decrypt(String encryptedLog) {
    var dotIndex = encryptedLog.indexOf('.');
    if (dotIndex == -1) {
      return "Unable to find the `.` separator in the encrypted log message: " + encryptedLog;
    }
    var decoder = Base64.getDecoder();
    var encodedKey = encryptedLog.substring(0, dotIndex);
    var encryptedKey = decoder.decode(encodedKey);
    byte[] keyBytes;
    try {
      keyBytes = decryptKeyCipher.doFinal(encryptedKey);
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      return "Unable to decrypt the encrypted symmetric key from: " + encodedKey;
    }
    var key = new SecretKeySpec(keyBytes, "AES");
    var decryptor = new AesBytesEncryptor(key, KeyGenerators.secureRandom(16), GCM);
    var encryptedMsg = decoder.decode(encryptedLog.substring(dotIndex + 1));
    try {
      return new String(decryptor.decrypt(encryptedMsg), UTF_8);
    } catch (IllegalStateException e) {
      return "Unable to decrypt `%s` because it is corrupted.".formatted(encryptedLog.substring(dotIndex + 1));
    }
  }
}
