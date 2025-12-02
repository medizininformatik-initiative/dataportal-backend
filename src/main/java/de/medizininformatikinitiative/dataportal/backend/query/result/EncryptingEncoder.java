package de.medizininformatikinitiative.dataportal.backend.query.result;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import lombok.Setter;
import org.bouncycastle.util.Arrays;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;

import javax.crypto.*;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.crypto.Cipher.ENCRYPT_MODE;
import static org.springframework.security.crypto.encrypt.AesBytesEncryptor.CipherAlgorithm.GCM;

/**
 * A {@code PatternLayoutEncoder} that encrypts the bytes the {@code PatternLayoutEncoder} outputs.
 * <p>
 * Encryption is done in the following way:
 * <ul>
 *   <li>a symmetric AES 256 key is generated every {@link #SYMMETRIC_KEY_ROLLOVER_COUNT} log messages</li>
 *   <li>that symmetric key is encoded with the given {@link #setPublicKey(String) public key}</li>
 *   <li>the log message is encoded using AES256-GCM</li>
 *   <li>both the encoded key and the encoded log message are Base64 encoded, joined with a {@code .} byte ({@code 0x2E}) and terminated with a {@code \n} ({@code 0x0A}) byte</li>
 * </ul>
 */
public class EncryptingEncoder extends PatternLayoutEncoder {

  public static final int SYMMETRIC_KEY_ROLLOVER_COUNT = 1000;
  public static final int SYMMETRIC_AES_KEY_SIZE = 256;

  private KeyGenerator keyGenerator;
  private Cipher keyEncryptCipher;
  private Encryptor encryptor;
  private int counter;

  @Setter
  private String publicKey;

  @Override
  public void start() {
    try {
      init();
    } catch (NoSuchAlgorithmException | InvalidKeySpecException |
             NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
             BadPaddingException e) {
      throw new Error("Unable to init encryptor.", e);
    }
    super.start();
  }

  private void init() throws NoSuchAlgorithmException, InvalidKeySpecException,
      NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
    keyGenerator = KeyGenerator.getInstance("AES");
    keyGenerator.init(SYMMETRIC_AES_KEY_SIZE);
    keyEncryptCipher = initKeyEncryptCipher(decodePublicKey());
    encryptor = createEncryptor();
    counter = SYMMETRIC_KEY_ROLLOVER_COUNT;
  }

  private PublicKey decodePublicKey() throws NoSuchAlgorithmException {
    var keyFactory = KeyFactory.getInstance("RSA");
    try {
      return keyFactory.generatePublic(new X509EncodedKeySpec(base64DecodePublicKey()));
    } catch (InvalidKeySpecException e) {
      throw new Error("Invalid public key: " + publicKey, e);
    }
  }

  private byte[] base64DecodePublicKey() {
    try {
      return Base64.getDecoder().decode(publicKey);
    } catch (IllegalArgumentException e) {
      throw new Error("Invalid public key: " + publicKey, e);
    }
  }

  private Cipher initKeyEncryptCipher(PublicKey publicKey)
      throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
    var keyEncryptCipher = Cipher.getInstance("RSA");
    keyEncryptCipher.init(ENCRYPT_MODE, publicKey);
    return keyEncryptCipher;
  }

  @Override
  public byte[] encode(ILoggingEvent event) {
    try {
      return current().encrypt(super.encode(event));
    } catch (IllegalBlockSizeException | BadPaddingException e) {
      return ("Unable to obtain current encryptor: " + e.getMessage()).getBytes(UTF_8);
    }
  }

  private synchronized Encryptor current() throws IllegalBlockSizeException, BadPaddingException {
    if (counter-- == 0) {
      encryptor = createEncryptor();
      counter = SYMMETRIC_KEY_ROLLOVER_COUNT;
    }
    return encryptor;
  }

  private Encryptor createEncryptor() {
    var key = keyGenerator.generateKey();
    var encryptedKey = Base64.getEncoder().encode(encryptKey(key));
    var bytesEncryptor = new AesBytesEncryptor(key, KeyGenerators.secureRandom(16), GCM);
    return new Encryptor(Arrays.append(encryptedKey, (byte) '.'), bytesEncryptor);
  }

  private byte[] encryptKey(SecretKey key) {
    try {
      return keyEncryptCipher.doFinal(key.getEncoded());
    } catch (IllegalBlockSizeException e) {
      throw new Error("Bad size (%d bytes) of internally generated key should be %d bit."
          .formatted(key.getEncoded().length, SYMMETRIC_AES_KEY_SIZE), e);
    } catch (BadPaddingException e) {
      throw new Error("Cipher should be in encryption mode.", e);
    }
  }

  private record Encryptor(byte[] encryptedKey, BytesEncryptor encryptor) {

    private byte[] encrypt(byte[] encodedLog) {
      var encryptedLog = Base64.getEncoder().encode(encryptor.encrypt(encodedLog));
      return Arrays.concatenate(encryptedKey, encryptedLog, new byte[]{'\n'});
    }
  }
}
