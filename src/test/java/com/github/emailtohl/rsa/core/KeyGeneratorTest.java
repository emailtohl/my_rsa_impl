package com.github.emailtohl.rsa.core;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KeyGeneratorTest {

	@Test// Thread safety
	public void testGenerateKeys() {
		KeyGenerator keyGenerator = new KeyGenerator();
		for (int i = 0; i < 10; i++) {
			new Thread(() -> {
				KeyPairs keys = keyGenerator.generateKeys(256);
				String s = "hello RSA!";
				byte[] b = s.getBytes();
				BigInteger m = new BigInteger(b);// 注意，明文的数字一定要小于模
				BigInteger c = keyGenerator.powModByMontgomery(m, keys.getPublicKey(), keys.getModule());
				System.out.println("密文是：" + c);
				BigInteger dm = keyGenerator.powModByMontgomery(c, keys.getPrivateKey(), keys.getModule());
				
				assertEquals(m, dm);
				
				b = dm.toByteArray();
				s = new String(b);
				System.out.println("解密后：" + s);
			}).start();
		}
	}

}
