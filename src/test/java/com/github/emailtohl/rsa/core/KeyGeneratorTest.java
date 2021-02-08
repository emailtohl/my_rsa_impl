package com.github.emailtohl.rsa.core;

import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class KeyGeneratorTest {

  @Test// Thread safety
  public void testGenerateKeys() {
    for (int i = 0; i < 10; i++) {
      new Thread(() -> {
        Keys keys = KeyGenerator.generateKeys(256);
        String s = "hello RSA!";
        BigInteger m = new BigInteger(s.getBytes());// 注意，明文的数字一定要小于模
        BigInteger c = KeyGenerator.powModByMontgomery(m, keys.e, keys.n);
        System.out.println("密文是：" + c);
        BigInteger dm = KeyGenerator.powModByMontgomery(c, keys.d, keys.n);

        assertEquals(m, dm);

        s = new String(dm.toByteArray());
        System.out.println("解密后：" + s);
      }).start();
    }
  }

}
