package com.github.emailtohl.rsa.core;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * 存储RSA密钥对最关键的信息：n，e，d
 */
class Keys implements Serializable {
  final BigInteger n, e, d;

  Keys(BigInteger n, BigInteger e, BigInteger d) {
    this.n = n;
    this.e = e;
    this.d = d;
  }

  @Override
  public String toString() {
    return "Keys{" +
        "n=" + n +
        ", e=" + e +
        ", d=" + d +
        '}';
  }
}
