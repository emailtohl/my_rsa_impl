package com.github.emailtohl.rsa.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.security.SecureRandom;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.ZERO;

/**
 * 由于前端大多数时候使用的JavaScript，该语言没有相关安全库，所以使用自己实现的RSA算法来对接前端生成的原始值。
 * <p>
 * RSA算法需要6个参数，p,q,n,φ(n),e,d ，其中：
 * p，q是随机生成的大素数，n = p * q ，是加密和解密计算时的模。
 * φ(n)（用fn表示）是n的欧拉函数值。
 * 在数论中，对正整数n的欧拉函数是小于n的数中与n互素的数的个数（包括1）， 例如φ(8) = 4，这是因为1,3,5,7这4个数和8互素。
 * e是加密时中使用的公钥，它与φ(n)互素。
 * d是解密时需要的私钥，它是通过e和φ(n)计算出来的，它们的关系是：e * d ≡ 1 (mod φ(n))，即：
 * e*d % φ(n) = 1，其中“%”是模运算符，这个等式还可以写成：e*d = 1 + u*φ(n)
 * 设m是数字化的明文，注意，这里的m一定要比n小！
 * c是加密后的值，加密和解密的过程实际上进行幂模运算，过程如下：
 * 加密：c = m^e % n
 * 解密：m = c^d % n
 *
 * @author HeLei
 */
public final class KeyGenerator {
	private static final Logger LOG = LogManager.getLogger();

	private KeyGenerator() {
	}

	/**
	 * 生成RSA密钥，其中n和e组成公钥，n和d组成私钥
	 *
	 * @param bitLength 密钥位数
	 * @return 密钥，n,e,d三个元素
	 */
	public static Keys generateKeys(int bitLength) {
		BigInteger p, q, n, fn, e, d;
		do {
			p = generateP(bitLength);
			q = generateQ(bitLength, p);
			n = generateN(p, q);
			fn = generateFn(p, q);
			e = generateE(fn);
			Euclid euclid = new Euclid(fn, e);
			d = euclid.getD();
		} while (!test(e, d, n));// 用本方法进行加密和解密，以确保生成的RSA参数正确性
		return new Keys(n, e, d);
	}

	/**
	 * 用JDK自带的方法获取大素数，但需注意的是，获取的值，只是“有可能”是素数，所以还需要测试
	 *
	 * @param bitLength 密钥的bit位长度，越长越安全，但是性能会越低
	 * @return p的值
	 */
	private static BigInteger generateP(int bitLength) {
		return BigInteger.probablePrime(bitLength, new SecureRandom());
	}

	/**
	 * 同上
	 *
	 * @param bitLength 密钥的bit位长度，越长越安全，但是性能会越低
	 * @param p         不能让q与p相等
	 * @return q的值
	 */
	private static BigInteger generateQ(int bitLength, BigInteger p) {
		BigInteger q;
		do {
			q = BigInteger.probablePrime(bitLength, new SecureRandom());
		} while (q.equals(p));
		return q;
	}

	/**
	 * 通过两个素数求得模N
	 *
	 * @param p p的值
	 * @param q q的值
	 * @return p和q的乘积
	 */
	private static BigInteger generateN(BigInteger p, BigInteger q) {
		return p.multiply(q);
	}

	/**
	 * p，q如果是素数的话，φ(p * q) = φ(p) * φ(q)
	 *
	 * @param p p的值
	 * @param q q的值
	 * @return 返回n（p和q乘积）的欧拉函数值
	 */
	private static BigInteger generateFn(BigInteger p, BigInteger q) {
		// 若n是奇数，则φ(n) = n - 1
		BigInteger fp = p.subtract(ONE);
		BigInteger fq = q.subtract(ONE);
		return fp.multiply(fq);
	}

	/**
	 * 对于e来说，选取一个素数即可，一般选65537
	 *
	 * @param fn n的欧拉函数值
	 * @return 公钥e
	 */
	private static BigInteger generateE(BigInteger fn) {
		BigInteger e = BigInteger.valueOf(65537);// 65537的二进制只有两个1，据说加密速度会更快
		if (ZERO.equals(fn.remainder(e))) {
			e = BigInteger.valueOf(257);
			if (ZERO.equals(fn.remainder(e))) {
				e = BigInteger.valueOf(17);
				if (ZERO.equals(fn.remainder(e))) {
					e = BigInteger.valueOf(3);
					if (ZERO.equals(fn.remainder(e))) {
						LOG.error("Create a RSA parameter failure, re execution");// 如果模n与上面的素数都不互素的话，创建RSA参数失败
						throw (new RuntimeException("Not selected to the correct public key !"));// 抛出异常，重新执行
					}
				}
			}
		}
		return e;
	}

	/**
	 * 计算逆元需要使用成员变量，为了避免使用同步锁，故创建内部类
	 *
	 * @author helei
	 */
	private static class Euclid {
		private BigInteger fn, e, x, y, d;// x，y成员变量，用于保存generateD()计算时的中间值

		Euclid(BigInteger fn, BigInteger e) {
			this.fn = fn;
			this.e = e;
			generateD();
		}

		private void generateD() {
			BigInteger gcd = extendEuclid(fn, e);// 在调用扩展欧几里得算法时，计算出x，y值
			LOG.debug("gcd = " + gcd);// 先打印查看最大公约数是否为1，保证无异常
			LOG.debug("x = " + x);
			LOG.debug("y = " + y);
			/*
			 * 调用了扩展欧几里得方法后，x,y满足： (φ(n) * x) + (e * y) = 1
			 * 这里不能完全保证y的正负性，如果y是负数，则需转换为同余正数，例如：-3 ≡ 2 (mod 5)
			 */
			while (y.compareTo(ZERO) < 0)
				y = y.add(fn);

			d = y;
			LOG.debug("d = " + d);
			LOG.debug("e * d (mod φ(n)) = " + (e.multiply(d).remainder(fn)));
		}

		/**
		 * 下面的方法使用了扩展欧几里得算法获取逆元，用于RSA中获取密钥d所用
		 * 注意：通过扩展欧几里得算法得到的x,y可能为负数，最后要把它加上模的倍数，直到成正数
		 *
		 * @param a 欧几里得算法中的迭代参数a
		 * @param b 欧几里得算法中的迭代参数b
		 * @return 最大公约数
		 */
		private BigInteger extendEuclid(BigInteger a, BigInteger b) {
			if (a.compareTo(b) < 0) {
				return extendEuclid(b, a);
			}
			if (ZERO.equals(b)) {
				x = ONE;
				y = ZERO;
				return a;
			}
			BigInteger gcd = extendEuclid(b, a.remainder(b));
			BigInteger temp = x;
			x = y;
			y = temp.subtract(a.divide(b).multiply(y));
			return gcd;
		}

		public BigInteger getD() {
			return d;
		}
	}

	/**
	 * 由于获取的p，q“有可能”是素数，所以在使用前，先做测试，为保证万一，验证3次
	 *
	 * @param e 公钥
	 * @param d 私钥
	 * @param n 运算范围的模n
	 * @return 测试公钥和私钥是否找对了
	 */
	private static boolean test(BigInteger e, BigInteger d, BigInteger n) {
		// 做一次检查，确保RSA的参数的正确性
		// 随机生成的 BigInteger，它是在 0 到 (2^numBits - 1)（包括）范围内均匀分布的值。
		BigInteger mm = new BigInteger((n.bitLength() - 1) / 8, new SecureRandom());
		LOG.debug("test m = " + mm);
		//BigInteger cc = mm.modPow(e, n);
		BigInteger cc = powModByMontgomery(mm, e, n);
		LOG.debug("test c = " + cc);
		//BigInteger dm = cc.modPow(d, n);
		BigInteger dm = powModByMontgomery(cc, d, n);
		LOG.debug("test dm = " + dm);
		if (mm.compareTo(dm) != 0) {
			LOG.debug("Test failed");
			return false;
		} else {
			LOG.debug("Test passed ");
		}
		return true;
	}

	/**
	 * 幂模运算也遵循乘法分配率，所以对于大数的幂模运算，可以先将底数做模运算后，再做指数运算，这样可以将数值运算保持在较小的数域范围内。
	 * 蒙哥马利算法计算大数的幂模运算的思路是不断将指数进行除2分解，直到指数分解到1为止，当然每次分解指数时，同时也在计算底数的平方。
	 *
	 * @param bottomNumber 底数
	 * @param exponent     指数
	 * @param module       模
	 * @return 幂模运算后的值
	 */
	static BigInteger powModByMontgomery(BigInteger bottomNumber, BigInteger exponent, BigInteger module) {
		if (ONE.equals(exponent)) {// 如果指数为1，那么直接返回底数
			return bottomNumber.remainder(module);
		}
		/*下面判断exponent的奇偶性，只要判断它最后一个bit位即可，1是奇数，0是偶数
			getLowestSetBit()方法可以返回最右端的一个1的索引，例如84的二进制是01010100，最右边1的索引就是2*/
		if (exponent.getLowestSetBit() == 0) {
			/*如果指数是奇数，那么底数做平方，指数减半后，还应该乘以剩下的一个底数
			下面对指数的除2处理是通过移位运算完成的，指数除2取整，相当于做右移一位操作，“exponent >>= 1”操作相当于“exponent /= 2”，但是效率会更高*/
			return bottomNumber.multiply(powModByMontgomery(squareAndMod(bottomNumber, module), exponent.shiftRight(1), module)).remainder(module);
		}
		return powModByMontgomery(squareAndMod(bottomNumber, module), exponent.shiftRight(1), module);
	}

	/**
	 * 对底数进行平方然后去模
	 *
	 * @param bottomNumber 底数
	 * @param module       模
	 * @return 对底数进行平方然后去模的值
	 */
	private static BigInteger squareAndMod(BigInteger bottomNumber, BigInteger module) {
		return bottomNumber.multiply(bottomNumber).remainder(module);
	}
}
