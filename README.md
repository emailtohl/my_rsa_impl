# 我的RSA算法理解以及Java实现
## 序
我的毕业设计是通过C语言来实现AES算法，从此对密码学具有较大的兴趣，时隔多年后，当自己再重拾编码工作后，第一个锻炼的项目就是完成当年未能实现RSA算法的夙愿。
RSA算法是现代密码技术的基石，有着严密的数学推理，至少到目前为止还没有证明有效的破解方式，我在业余时间我查阅了很多资料，完成了一份自己的Java实现。

本文档既对该算法的来龙去脉进行推导，也给出具体的代码。但还是需要说明的是：

1. 对于像RSA这种最基础的安全组件，在JDK中已经有了实现，不过这并不妨碍自己的学习；

2. 既然是对算法进行学习，那么就尽量抛开JDK中现有实现，实现中除了**生成大素数**太艰深外，其他的如大数幂模运算，欧几里得获取逆元，都可以自己动手实现。

3. 我提交的代码只是算法最核心的部分，并没有提交整个加密解密应用程序，之所以没有完整提交是基于安全考虑，因为安全无小事，自己实现的应用或许在某个环节上存在安全bug，被人破解了那就麻烦了：）

## 第一章 模运算基础
RSA算法完全建立在数论基础上，要理解该算法，需从基础开始。
### 1.1 同余式乘法
若a ≡ b mod m， c ≡ d mod m ，则  ac ≡ bd mod m ，这个定理应用在RSA证明时，用于同余式两边共同u(p-1)次方，证明如下：

a = km + b ， c = hm + d

ac = (km + b)(hm + d) = khm<sup>2</sup> + kmd + bhm + bd

ac = bd + (khm + kd + bh)m，转换成同余式，即：

ac ≡ bd mod m

得证。

### 1.2 模运算的分配率
模运算就像普通运算一样，它是可交换、可结合、可分配的，在计算中经常用到分配率：

> (a + b) mod m = ((a mod m) + (b mod m)) mod m

> ab mod m = (a mod m)(b mod m) mod m

不论是在欧拉定理的证明中还是蒙哥马利幂模算法中都会用到本规律，它们的证明方法是一样的，以乘法为例，证明如下：

(a mod m)(b mod m) mod m = (a - k<sub>1</sub>m)(b - k<sub>2</sub>m) - k<sub>3</sub>m

= ab - (ak<sub>2</sub> + k<sub>1</sub>b - k<sub>1</sub>k<sub>2</sub>m - k<sub>3</sub>)m

= ab mod m

得证。

### 1.3 互素的传递性

1. 如果整数a和b 都与c互素，那么ab也与c互素，因为他们都没有公约数。

2. 如果整数p和q互素，那么(p mod q)后仍然与q互素。

第二点可用反证法：

设 p mod q = r，即：p = r + kq，假设r与q不互素，那么r和q存在公约数d，上面等式可以改写为：p = dr' + kdq' = d(r' + kq')

如此一来d也能整除p，这与p，q互素矛盾。

### 1.4 消去律

该定理将会用于欧拉定理的证明中。

若  ac ≡ bc mod m，且c和m互素，则

a ≡ b mod m

证明如下：

由  ac ≡ bc mod m 可知：ac = km + bc ， 即：

c(a - b) = km

由于c和m互素，因此c | k。设 k = hc ，则

c(a - b) = hcm ， 即

a - b = hm ， 即

a ≡ b mod m

得证。

## 第二章 欧拉定理
### 2.1 欧拉函数
在数论中，将小于n且与n互素的正整数组成集合，该集合的个数就是n的欧拉函数的值，欧拉函数记为φ(n)。

例如φ(8) = 4，这是因为1，3，5，7这4个数和8互素。

### 2.2 欧拉定理
如果两个正整数a和n互素，φ(n)是n的欧拉函数，存在下面的等式：
> a<sup>φ(n)</sup> ≡ 1 (mod n) 

> 即：a<sup>φ(n)</sup> = 1 + kn，或：a<sup>φ(n)</sup> mod n = 1

比如，3和7互素，而7的欧拉函数φ(7)等于6，所以3的6次方（729）减去1，可以被7整除（728 | 7）。

证明分两步：
#### 2.2.1证明以下两个集合相等
> Zn = {x<sub>1</sub>, x<sub>2</sub>, ... , x<sub>φ(n)</sub>}

> S = {ax<sub>1</sub> mod n, ax<sub>2</sub> mod n, ... , ax<sub>φ(n)</sub> mod n}

Zn集合中的元素是小于n，且与n互素的正整数，根据欧拉函数的定义，Zn集合中的元素的个数有φ(n)个。

S集合与Zn集合相对应，其中a也是与n互素的正整数，分别将Zn中的每个元素乘a再模n，它也有φ(n)个元素。

（1）在S集合中，因为a与n互素，并且x<sub>1</sub>, x<sub>2</sub>, ... , x<sub>φ(n)</sub>都与n互素，所以根据**1.3 互素的传递性**ax<sub>1</sub> mod n，ax<sub>2</sub> mod n，……，ax<sub>φ(n)</sub> mod n，它们都与n互素，且都属于Zn集合（这些数都会小于n，且与n互素）。

到此，我们知道了S中的元素个数与Zn元素的个数相等，且S属于Zn的子集，只有证明S集合中的每个元素都互不相等，才能说明Zn = S

（2） 用反证法证明S集合中的每个元素各不相同：

假设ax<sub>i</sub> mod n = ax<sub>j</sub> mod n ，因a，n互素，根据**1.4 消去律**得知：x<sub>i</sub> mod n = x<sub>j</sub> mod n，又，x<sub>i</sub> 和 x<sub>j</sub>都属于集合Zn，它们都小于n，所以x<sub>i</sub> = x<sub>j</sub>，这个结论与事实矛盾，如此就证明了Zn = S 。

#### 2.2.2 证明1 ≡ a<sup>φ(n)</sup> (mod n)
因Zn = S，故分别将Zn集合和S集合中的每个元素相乘，根据**1.1 同余式乘法**他们也应该同余相等：
x<sub>1</sub> x<sub>2</sub> ... x<sub>φ(n)</sub> ≡ a<sup>φ(n)</sup> x<sub>1</sub> x<sub>2</sub> ... x<sub>φ(n)</sub> (mod n)

已知x<sub>1</sub>，x<sub>2</sub>，...，x<sub>φ(n)</sub>与n互素，那么x<sub>1</sub> x<sub>2</sub> ... x<sub>φ(n)</sub>也与n互素，上式通过**1.4 消去律**得到：

> 1 ≡ a<sup>φ(n)</sup> (mod n)

得证。

### 2.3 欧拉等式
两个不同的素数p，q，他们的乘积n = pq ，在小于n的域中，与n互素的集合中的个数有：

{1,2,3,……,n-1} - {p,2p,……,(q-1)p} – {q,2q,……,(p-1)q}

所以φ(n) = (n-1) - (q-1) – (p-1) = (p-1)(q-1) = φ(p) φ(q)

去掉中间步骤，得到重要结论（本该用“中国剩余定理”来证明的）：

φ(n) = φ(p) φ(q)，前提是p和q互素，n = pq 。

## 第三章 RSA算法证明

有了前面的数论基础，就可以推导RSA算法。

### 3.1 RSA算法

下面先介绍RSA算法，该算法需要6个参数，p， q， n， φ(n)， e， d ，其中：

- p，q是随机生成的大素数；
- n = pq ，是加密和解密计算时的模；
- φ(n)是n的欧拉函数值；
- e是加密时中使用的公钥，它与φ(n)互素；
- d是解密时需要的私钥，它是通过e和φ(n)计算出来的，它们的关系是：ed ≡ 1 (mod φ(n))，也可以称d是e关于模φ(n)的**逆元**（与乘法倒数类似）。即：  e*d % φ(n) = 1，其中“%”是模运算符，这个等式还可以写成：ed = 1 + u φ(n)

设m是数字化的明文，**注意，这里的m一定要比n小！**

c是加密后的值，加密和解密的过程实际上进行幂模运算，过程如下：

> 加密：c = m<sup>e</sup> % n

> 解密：m = c<sup>d</sup> % n

### 3.2 RSA算法证明

目标是证明：Decrypt(c) = c<sup>d</sup> % n = m

Decrypt(c) = c<sup>d</sup> % n = (m<sup>e</sup>)<sup>d</sup> % n = m<sup>ed</sup> % n

又， ed = 1 + u φ(n) ，所以原等式改为：

Decrypt(c) = m<sup>1 + u φ(n)</sup> % n = (m % n)((m<sup>φ(n)</sup>)<sup>u</sup> % n)

下面分两种情况讨论：

#### 3.2.1当m与n互素时

根据欧拉定理：m<sup>φ(n)</sup> ≡ 1 (mod n)，即： m<sup>φ(n)</sup> % n = 1

所以Decrypt(c) = (m % n)((m<sup>φ(n)</sup>)<sup>u</sup> % n) = m \* 1 = m，得证！

#### 3.2.2如果m与n不互素
这时说明m和n有公因子，而n的因子只有p和q，设m与n的公因子是p，m可以写成：m = kp

可用反证法证明m一定和q互素：

如果m与q不互素，由于q本身是素数，所以q只能是k的因子，那么m可写成：

> m = pk = phq = hpq = hn

这样就得到m &gt; n的结论，与前提m &lt; n是矛盾的，所以m一定与q互素，这时m和q存在欧拉定理的等式：

> m<sup>φ(q)</sup> ≡ 1 (mod q)

因φ(q) = q-1，同余等式等价于：

> m<sup>q-1</sup> ≡ 1 (mod q)

根据同余式乘法定理，两边同时进行u(p-1)次方仍然同余相等：

> m<sup>u(p-1)(q-1)</sup> ≡ 1<sup>u(p-1)</sup> (mod q)

根据前面已证明的**2.3 欧拉函数的等式**： (p-1)(q-1) = φ(p)φ(q) = φ(n)，同余等式改为：

> m<sup>uφ(n)</sup> ≡ 1 (mod q)

将同余等式改成方程式：

> m<sup>uφ(n)</sup> = 1 + vq

方程式两边同时乘上m：

> m<sup>uφ(n) + 1</sup> = m + vmq

前面已假设m = pk，所以vmq = vkpq：

> m<sup>uφ(n) + 1</sup> = m + vkpq

因n = pq，再设vk = w，方程式改为：

> m<sup>uφ(n) + 1</sup> = m + wn

> m<sup>uφ(n) + 1</sup> % n = m

又，uφ(n) + 1 = ed

> m<sup>ed</sup> % n = m

> c<sup>d</sup> % n = m

得证！

## 第四章 求得逆元

### 4.1 获取RSA算法所需参数

重新回顾RSA算法需要的6个参数：

p， q， n， φ(n)， e， d ，其中：

- p，q是随机生成的大素数；
- n = pq ，是加密和解密计算时的模；
- φ(n)是n的欧拉函数值；
- e是加密时中使用的公钥，它与φ(n)互素；
- d是解密时需要的私钥，它是通过e和φ(n)计算出来的，它们的关系是：ed ≡ 1 (mod φ(n))，即：ed = 1 + u φ(n)

（1）对于p，q的选择，比较困难，小素数（比如1亿）的测试，还可以用蛮力法进行测试：

> 首先由java生成随机数p，过滤掉偶数，然后依次用2到sqrt(p)之间的数去整除p，如果没有一个数能被整除，说明p是素数。

但是对于成百上千位的大素数来说，这种方式的性能是不可接受的，对于大素数的筛选方法有Miller检验，不过它理论我还没弄懂，所以并没有实现该方法，这里使用JDK，BigInteger自带的：

```java
BigInteger probablePrime(int bitLength, Random rnd);
```

需要注意的是，该方法返回的可能是素数的，还需要在RSA中进行测试，看看是否能解密还原。

（2）找到p，q后，就得到n = pq，由于p，q互素，φ(n) = φ(pq) = φ(p)φ(q) = (p - 1)(q - 1)，如此也计算出了φ(n)。

（3）对于公钥e，来说选择就比较容易，只要与φ(n)互素即可。现在最重要的是找到私钥d，目前只知道d与e，φ(n)之间存在以下关系：

> ed = 1 + u φ(n)

在数论中这种关系，称d是e关于φ(n)的**逆元**。可通过**4.3 扩展欧几里得算法**实现，这就需要知道一些新的理论知识……

### 4.2 欧几里德算法
欧几里德算法又称辗转相除法，用于计算两个整数a,b的最大公约数。算法基于下面这个等式：

> gcd(a,b) = gcd(b,a mod b)

证明：
设gcd(a,b) = g，那么可写成：a = m*g，b = n*g，这里的m和n一定是互素的，否则g就不是a和b的最大公约数，下面证明的思路也是围绕这个原理进行的。

a和b存在关系式：r = a - kb = mg - kng = (m - kn)g

现在比较这两个式子：

> r = (m - kn)g

> b = ng

可以看出，g是r和b的公约数，另外，因m与n互素，所以(m - kn)与n互素，如此就说明g也是r和b的最大公约数，所以证明了：
gcd(a,b) = gcd(b,r) = gcd(b,a mod b) 。

### 4.3 扩展欧几里得算法裴蜀定理
在欧几里得算法中，最大公约数通过辗转相除的等式关联起来：

> gcd(a,b) = gcd(b,a%b) = ……

而扩展欧几里得算法中需要使用到**裴蜀定理**：

> gcd(a,b)是a，b的最大公约数，存在整数x和y使得a*x + b*y = gcd(a,b)

如此一来辗转相除等式就可以改写成：

> ax<sub>1</sub> + by<sub>1</sub> = gcd(a,b) = gcd(b, a % b) = b x<sub>2</sub> + (a % b)y<sub>2</sub> = ……

一直辗转递归下去，直到a’%b’ = 0时，可得到最大公约数为a’， 同时得到此时的x'，通过递归返回求得最初a和b时候的x，即可求得**逆元**。

为什么x和y一定会存在？下面将将分三步来证明裴蜀定理：
#### 4.3.1 第一步
设ax + by是一个正整数的集合，该集合存在最小的正整数，设为d，先证明ax + by集合中任意整数都是d的倍数

可以用反证法证明d能整除ax + by集合中的所有值：

当x = x<sub>0</sub>，y = y<sub>0</sub>时，有等式：ax<sub>0</sub> + by<sub>0</sub> = d

当x = m，y = n时，有等式：am + bn = e

当然e ≥ d，假设d不能整除e，对e做带余除法，必定存在 p，r 使 e = pd + r，显然，r &lt; d
但是通过下面的推导却会得出r &gt; d矛盾的结果：

> r = e - pd = (am + bn) - p(ax<sub>0</sub> + by<sub>0</sub>) = (m - px<sub>0</sub>)a + (n - py<sub>0</sub>)b

观察上面等式的首尾，说明存在整数 m - px<sub>0</sub>， n-p<sub>y0</sub> 使 ax + by = r &lt; d，这与d的最小性矛盾。

所以d整除e，即d能整除ax + by集合中的任意值。

#### 4.3.2 第二步
证明d是a和b的公约数

令x = 1，y = 0，则ax + by = a，所以d能整除a

同理d也能整除b，这就说明d是a，b的公约数。

#### 4.3.3 第三步
实际上，d就是a和b的最大公约数，证明如下：

对于a和b的任意公约数d’，存在a = kd’，b = ld’，有等式成立：

> d = kd’x + ld’y = d’(kx + ly)

所以a，b的任意公约数d’都能整除d，即d就是a和b的最大公约数（最大公约数等于各公约数的积）

特例：当a，b互素时，d = 1，这就说明了对于互素的a和b，一定存在x，y满足ax + by = 1 。

### 4.4 计算逆元

前面已证明对于互素的a和b，一定存在x，y满足：

> ax + by = 1

再来对比：

> ed = 1 + u φ(n)  => - φ(n)u + ed = 1

等式中的a即是- φ(n)，e即是b，要求得逆元d，可在求得a和b最大公约数时获取。

> ax<sub>0</sub> + by<sub>0</sub> = gcd(a, b) = gcd(b, a % b) = bax<sub>1</sub> + (a % b)y<sub>1</sub> = ……

一直辗转递归下去，直到a'%b' = 0 时，可得到最大公约数为a'，观察此时的等式：

> a'*x' + (a'%b')*y' = a' 

这是递归的最后一次，要使上面等式成立，令此时的x'=1，y'=0。 此时随着递归的逐层返回，求得最初a和b时候的x0和y0，即是a和b各自的逆元。

算法的进一步描述：

对于不完全为 0 的非负整数 a和b，gcd(a，b)表示 a，b 的最大公约数，必然存在整数对 x，y ，使得

> gcd(a，b) = ax + by

设
> ax<sub>0</sub> + by<sub>0</sub> = gcd(a, b)

> bx<sub>1</sub> + (a % b)y<sub>1</sub> = gcd(b, a % b)

根据朴素的欧几里德原理有

> gcd(a, b) = gcd(b, a % b)

所以：

> ax<sub>0</sub> + by<sub>0</sub> = bx<sub>1</sub> + (a % b)y<sub>1</sub>

这里的a % b在程序中可以写成 a - (a/b)*b   （a/b的整形值是忽略小数的整数值，例如System.out.println(20/7)，显示的结果是2）
所以等式可以改写成：

> ax<sub>0</sub> + by<sub>0</sub> = bx<sub>1</sub> + (a-(a/b)b)y<sub>1</sub> = ay<sub>1</sub> + bx<sub>1</sub> - (a/b)by<sub>1</sub> = a(y<sub>1</sub>) + b(x<sub>1</sub>-(a/b)y<sub>1</sub>)

对比等式左右两边：

> x<sub>0</sub> = y<sub>1</sub>

> y<sub>0</sub> = x<sub>1</sub>-(a/b)*y<sub>1</sub>

如此就得到递归等式，只要迭代计算到y<sub>0</sub>即得到逆元。

## 第五章 代码实现
长篇理论终于翻篇了，下面在Java代码中实现RSA算法。
### 5.1 获取大素数p，q
大素数校验理论较为艰深，我的代码中并未实现，直接调用JDK中的：

> BigInteger probablePrime(int bitLength, Random rnd);

### 5.2 计算密钥d
通过拓展欧几里得算法计算：

```java
private BigInteger x, y;// 定义x，y成员变量，用于保存下面generateD()计算时的中间值

private void generateD() {
	BigInteger gcd = extendEuclid(e, fn);// 在调用扩展欧几里得算法时，计算出x，y值
	System.out.println("gcd = " + gcd);// 先打印查看最大公约数是否为1，保证无异常
	System.out.println("x = " + x);
	System.out.println("y = " + y);
	System.out.println("e * x (mod φ(n)) = " + (e.multiply(x).remainder(fn)));
	/*
	 * 调用了扩展欧几里得方法后，x,y被计算出来了，但是需要注意的是x可能是负数，虽然x虽然满足： (e * x) + (φ(n) * y) = 1
	 * 但e * x (mod φ(n)) != 1，这就不满足RSA的前提条件了,这时需对x稍作处理，让其在0 ~ φ(n)范围内，即加上一个φ(n)的倍数即可
	 * 先检查一下算出的x是否小于0，如果小于0，正好相差一个模数，加上一个φ(n)的倍数即可
	 */
	while (x.compareTo(zero) == -1)
		x = x.add(fn);
	d = x;
	System.out.println("d = " + d);
	System.out.println("e * d (mod φ(n)) = " + (e.multiply(d).remainder(fn)));
}

private BigInteger extendEuclid(BigInteger a, BigInteger b) {
	if (b.compareTo(zero) == 0) {
		x = BigInteger.valueOf(1);
		y = BigInteger.valueOf(0);
		return a;
	}
	BigInteger gcd = extendEuclid(b, a.remainder(b));
	BigInteger temp = x;
	x = y;
	y = temp.subtract(a.divide(b).multiply(y));
	return gcd;
}
```

### 5.3 大数的幂模运算
加密解密时有非常大量的幂模运算，JDK中也提供相应的方法：

> BigInteger modPow(BigInteger exponent, BigInteger m);

不过这里，我们可以自己实现，幂模运算也遵循乘法分配率，所以对于大数的幂模运算，可以先将底数做模运算后，再做指数运算，这样可以将数值运算保持在较小的数域范围内。

蒙哥马利算法计算大数的幂模运算的思路是不断将指数进行除2分解，直到指数分解到1为止，当然每次分解指数时，同时也在计算底数的平方。

以m<sup>9</sup> mod n可以这样分解为例，可以这样分解：

> m<sup>9</sup> mod n = ((m<sup>2</sup> mod n)<sup>9/2</sup> m ) mod n

每次运算，指数减半，同时计算底数的平方，然后再将中间值做模运算，如此底数一直保持在小于模数的范围内，不仅计算不会溢出，同时大指数也会迅速地减小。

从上面式子也看到了，如果指数是奇数，那么还需要乘上一个剩余的底数m，指数不断地对半分，直到为1，这时候就可以直接返回底数了，采用递归方法来实现。

代码如下：

```java
private BigInteger powModByMontgomery(BigInteger bottomNumber, BigInteger exponent, BigInteger module) {
	if (exponent.compareTo(BigInteger.valueOf(1)) == 0) {// 如果指数为1，那么直接返回底数
		return bottomNumber.remainder(module);
	} else {
		/*下面判断exponent的奇偶性，只要判断它最后一个bit位即可，1是奇数，0是偶数
		getLowestSetBit()方法可以返回最右端的一个1的索引，例如84的二进制是01010100，最右边1的索引就是2*/
		if (exponent.getLowestSetBit() == 0)
		/*如果指数是奇数，那么底数做平方，指数减半后，还应该乘以剩下的一个底数
		下面对指数的除2处理是通过移位运算完成的，指数除2取整，相当于做右移一位操作，“exponent >>= 1”操作相当于“exponent /= 2”，但是效率会更高*/
			return (bottomNumber.multiply(powModByMontgomery(bottomNumber.multiply(bottomNumber).remainder(module), exponent.shiftRight(1),module)).remainder(module));
		else
			return (powModByMontgomery(bottomNumber.multiply(bottomNumber).remainder(module), exponent.shiftRight(1),module));
	}
}
```
### 5.4 加密与解密
终于，所有的准备工作已完成，剩下的就是简单的加密和解密。下面的一段测试代码将直接说明加密与解密的过程：

```java
// 随机生成的 BigInteger，它是在 0 到 (2^numBits - 1)（包括）范围内均匀分布的值。
BigInteger mm = new BigInteger(mArrayLength,new SecureRandom());
System.out.println("test m = " + mm);
//BigInteger cc = mm.modPow(e, n);// 这是JDK中的幂模运算实现
BigInteger cc = powModByMontgomery(mm,e,n);
System.out.println("test c = " + cc);
//BigInteger dm = cc.modPow(d, n);
BigInteger dm = powModByMontgomery(cc,d,n);
System.out.println("test dm = " + dm);
if(mm.compareTo(dm) != 0)
	return false;
else 
	System.out.println("Test passed ");
```

