package de.medizininformatikinitiative.dataportal.backend.query.result;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.assertj.core.api.Assertions.assertThat;

class EncryptingEncoderTest {

  private static final String PRIVATE_KEY = "MIIG/QIBADANBgkqhkiG9w0BAQEFAASCBucwggbjAgEAAoIBgQDWVY59fMT+ZQQ+K00vGwMy2MREkZWFLyA/Vaa4c++M/FQ5DKh3gRslxsmWQGouqloizwG7LeBxsm8g8RF0bU3UFJnw04o61G5uKgp/fQnCct1kBp7e8kDNELeHjY48zd+iFkJczFuuO74ozRlQissRcoQMwdBZ968JALF6LG9dVil9eWQkgEZpbPhwOAQ6KLCFTzsAzMZnCMAge+vihMX+4eDr4Lcx3HSbxYu3KBOISLEl6Axv+3FH7iA1VivOmbkHANRTtDHLedNQNhkuVMZ4/0wEVZb5bzsgfOA/44WvdkowSQA9TRHWWpcVkJhyBkcYDkd7ZbvTWKyeqQohBPx484fjdk5ejdLfg+rDmhDhDaQ+5unXCBlbDRW0SJkO2XCpQn6vcKKBTs0AUzkmcL+ySIgEp61tag2bUk8R8Y5OuCMcPa6jzaNI7y1Xjks0fURpHb7lRZIVnIO9NrItmzGxWVEVckqBAkMjfjhHqWEZstduGoK4/N5BoevVcYIoC10CAwEAAQKCAYB20JLeyY3CoGkLtaFh3Y2WYEfc4v+KByfPNEHpuosvEqn1viidprpP5LTXT/oMyG6TIUIKa1IidRHZpwlQC0+7o3f64qmcFyrocvHC3qPMYTSNQIZNfOmjRAMZZ8VTu6SfC8vZ+YdRPNkbcSb5WRddIqhFEiPX28/yI6o+2ecaTKtPXT3AZkOiKcBHJSZivy+rltJUjS/m6hjnaQrIVWYU905iM/4Z6+XQfGAJNnQtdm6NXueWJEMV5tF//7blHp1JqO4gfx8WpUTMT/fPscHL5wjmOIXWaWEjWJ+lZ4drCZCdGrcJhqfD2aC1EZpaixSQTZzskc1/U92tGa+ih9lnq0hkU6gr4/6LOZqUnjQoB6hYyYbSHnkMB5LJvrF/3PEmS14TtBFhenUCfmwZNxwtT4xYP5sBFGMFv9hkfnceEtwQ7N6OWTT7/his3XgY5GG4DCfxEZH8oML7474bYbewREcFTlefwrZWmkmWfdPbcyCOptLqZkr5Tt/1f2RuW9kCgcEA64woU91S7dm/KEqAmJHXuXydOcQYTj3vYTbZOw4IT08YBg2u+RNTtQDCacVb/ooF4/YIw/OjKaOnoIejuNw60hAVhjLVE/342GakhOhLSpMUE1kvPiC7BbvDZUmycWcvt2uT6BJna1ujgZjkb9mkpDWS2ygtxjiA033IxJzEGYIxXKFqD69hc266h+iUjCJRyj/T8inoDxLuGr+JAnV8TQWbbfpPkfcb8MdqeVJlco/+4zXYY+NZYyCksMcJIAr/AoHBAOjx3J/XxoO8xqzlk9Lto1pNcp6IG8HYeHzIxo6gm1yRCYB4wxghb+ypTCWcpgyYmiKIlgWLTu1fTHAFUk8FV1LGwL0KHJOMcCb94/yXsbqkjskvZJNtM6uMb6B8PEv4qnqNa1eNjC4mE6iA7QFRLimD352DI9FrqzwT1/gFH+r7nL2lGAIA1epIB4OZq9iB/sejjv9/0ODuk/P123Oda95fd9vxCGfuZxKT2hNX6SPEmw5it24ppVeivWxnbsX1owKBwENrHGfUo1Xcyy/3ExOYOsymdEICdIqAg7Gph0e13n8EvnWNGRXFiGH4U6z+hjQ2wTTcSOn9JChY5TO3Xw8cSeGyJNcCWaadPMqDpnc8HcC8lDRthG4d5Cnh8i1diKuYwzmWmwEDs4Iw+n2vi0LQYqV1iBEeUOu5ZHYkPIC59g7vCr3enYLbyeLGQLGBynLJp+thlYJsqDUYT/pr9AU2J1vMTQ6PZJL8zYx/J2SORuche+0Ajm0Yt4792uWWMnBvdQKBwQCKrpuDyimUeonpm1BTjkjnVR59BVlJIcAxwjJ77WAxTuPSSZMUxatlwTDlX4p4C04QazKtoE9gAJF4S6LCCtL/I/bRVLjImx6WCCd4VTNpg9jCK+X741KUuiom6G/ZZvTPu2wBlvKy8tZXRlJTq2oJK0qw8scbQbeTL9ku/pYPBrc9LJHLd4XjUfivP4jQgCwX3Ocgc47+qusIngGFpl326O1p0ukHPya8J6v4Qik5sy4A9YJxIngeYXPWmwmW73MCgcBzxKY/PNTV5MYlpcVWi2Sv2OlhlEUVuH63mzIxFrYNCzHh468CEGG1kGNXqURC1+OLWMUuuI/bRH083OjRmXAsR+IMqlfleSLyy0VA8cBVwlN5s8yJCo0Qx1VhVMfYFVYCQUqV4L7GZoycXhGXodSWA2qY4yYeL//X90qF+M+rEBLsoQyaBAsqH8OOTjTv/vhZ+K/JZp1LQZEBY9XXwYwKfK0jbNkM4xBAMCbZYgUBo/7Oh1z96uy3xZWwss/cDAA=";
  private static final String PUBLIC_KEY = "MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEA1lWOfXzE/mUEPitNLxsDMtjERJGVhS8gP1WmuHPvjPxUOQyod4EbJcbJlkBqLqpaIs8Buy3gcbJvIPERdG1N1BSZ8NOKOtRubioKf30JwnLdZAae3vJAzRC3h42OPM3fohZCXMxbrju+KM0ZUIrLEXKEDMHQWfevCQCxeixvXVYpfXlkJIBGaWz4cDgEOiiwhU87AMzGZwjAIHvr4oTF/uHg6+C3Mdx0m8WLtygTiEixJegMb/txR+4gNVYrzpm5BwDUU7Qxy3nTUDYZLlTGeP9MBFWW+W87IHzgP+OFr3ZKMEkAPU0R1lqXFZCYcgZHGA5He2W701isnqkKIQT8ePOH43ZOXo3S34Pqw5oQ4Q2kPubp1wgZWw0VtEiZDtlwqUJ+r3CigU7NAFM5JnC/skiIBKetbWoNm1JPEfGOTrgjHD2uo82jSO8tV45LNH1EaR2+5UWSFZyDvTayLZsxsVlRFXJKgQJDI344R6lhGbLXbhqCuPzeQaHr1XGCKAtdAgMBAAE=";
  private static final String MSG = "msg-164034";

  private Decryptor decryptor;

  private EncryptingEncoder encoder;

  private static String changeMessagePart(String msg) {
    var dotIndex = msg.indexOf('.');
    var decodedBytes = Base64.getDecoder().decode(msg.substring(dotIndex + 1));
    decodedBytes[0]++;
    return msg.substring(0, dotIndex + 1) + Base64.getEncoder().encodeToString(decodedBytes);
  }

  private static LoggingEvent loggingEvent(String message) {
    var event = new LoggingEvent();
    event.setMessage(message);
    return event;
  }

  private static String removeNewline(String encryptedMsg) {
    return encryptedMsg.substring(0, encryptedMsg.length() - 1);
  }

  @BeforeEach
  void setUp() throws Exception {
    var keyFactory = KeyFactory.getInstance("RSA");
    var privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(
        PRIVATE_KEY)));
    decryptor = new Decryptor(privateKey);
    decryptor.init();

    encoder = new EncryptingEncoder();
    encoder.setPublicKey(PUBLIC_KEY);
    encoder.setPattern("%msg");
    encoder.setContext(new LoggerContext());
    encoder.start();
  }

  @Test
  void encode_key_length() {
    var encryptedMsg = new String(encoder.encode(loggingEvent(MSG)), US_ASCII);

    var dotIndex = encryptedMsg.indexOf('.');
    assertThat(dotIndex)
        .withFailMessage("Expected encrypted and encoded key length to be 512 but was %d.",
            dotIndex)
        .isEqualTo(512);
  }

  @Test
  void encode_decrypt() {
    var encryptedMsg = new String(encoder.encode(loggingEvent(MSG)), US_ASCII);

    var msg = decryptor.decrypt(removeNewline(encryptedMsg));

    assertThat(msg).isEqualTo(MSG);
  }

  @Test
  @DisplayName("Decryption of a manipulated message fails.")
  void encode_decrypt_fails() {
    var encryptedMsg = new String(encoder.encode(loggingEvent(MSG)), US_ASCII);
    var changedMsg = changeMessagePart(removeNewline(encryptedMsg));

    assertThat(decryptor.decrypt(changedMsg))
        .isEqualTo("Unable to decrypt `%s` because it is corrupted."
            .formatted(changedMsg.substring(changedMsg.indexOf('.') + 1)));
  }

  @Test
  @DisplayName("Doesn't change the key in the first 1000 encodings.")
  void encode_rollover_1000() {
    var encryptedKeys = IntStream.range(0, 1000)
        .mapToObj(i -> new String(encoder.encode(loggingEvent(MSG)), US_ASCII))
        .map(encryptedMsg -> encryptedMsg.substring(0, encryptedMsg.indexOf('.')))
        .collect(Collectors.toSet());

    assertThat(encryptedKeys).hasSize(1);
  }

  @Test
  @DisplayName("Does change the key after 1000 encodings.")
  void encode_rollover_1001() {
    var encryptedKeys = IntStream.range(0, 1001)
        .mapToObj(i -> new String(encoder.encode(loggingEvent(MSG)), US_ASCII))
        .map(encryptedMsg -> encryptedMsg.substring(0, encryptedMsg.indexOf('.')))
        .collect(Collectors.toSet());

    assertThat(encryptedKeys).hasSize(2);
  }

  @Test
  void encode_performance() {
    for (int i = 0; i < 1_000_000; i++) {
      encoder.encode(loggingEvent("2023-05-15T16:10:19.803;110059;site-name-142848;110103"));
    }
  }

  @Test
  @DisplayName("Equal messages are encrypted differently, because an initialization vector is used.")
  void encode_initialization_vector() {
    var encryptedKeys = IntStream.range(0, 2)
        .mapToObj(i -> new String(encoder.encode(loggingEvent(MSG)), US_ASCII))
        .map(encryptedMsg -> removeNewline(encryptedMsg.substring(encryptedMsg.indexOf('.') + 1)))
        .collect(Collectors.toSet());

    assertThat(encryptedKeys).hasSize(2);
  }
}
