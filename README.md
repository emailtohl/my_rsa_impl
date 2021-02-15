# 我的RSA算法理解以及Java实现
## 序
我的毕业设计是通过C语言来实现AES算法，从此对密码学具有较大的兴趣，时隔多年后，当自己再重拾编码工作后，第一个锻炼的项目就是完成当年未能实现RSA算法的夙愿。
RSA算法是现代密码技术的基石，有着严密的数学推理，至少到目前为止还没有证明有效的破解方式，我在业余时间我查阅了很多资料，完成了一份自己的Java实现。

本文档既对该算法的来龙去脉进行推导，也给出具体的代码。但还是需要说明的是：

1. 对于像RSA这种最基础的安全组件，在JDK中已经有了实现，不过这并不妨碍自己的学习；

2. 既然是对算法进行学习，那么就尽量抛开JDK中现有实现，实现中除了**生成大素数**太艰深外，其他的如大数幂模运算，欧几里得获取逆元，都可以自己动手实现。

3. 本文档着重描述算法原理，实现在KeyGenerator，而应用部分的示例可参考pad项目的RSACipher。

4. 文档编排方式与`意图导向编程`风格一样，以展示RSA的算法原理为主干脉络，过程中的一些理论基础以及细枝末节仅在后续章节补充。

## 第一章 RSA算法原理
想象一下时钟，以12为模，超出了12点后，数字就会重新调整到12点以内，如12调整为0，13调整为1，17调整为5，10000调整为4，也就是说不管数字有多大，模12后结果也只能在12范围内。

对一个数字m，进行幂运算后，通常会很大，但再进行模运算，其结果会回到模范围内，如 m<sup>e</sup> mod n = c，c &lt; n，如果再对c做幂模运算，结果是否能还原为之前的m呢？

### 1.1 一个简单的演算示例
以下用`n`表示模，`明文`用`m`表示，m的幂用`e`表示，假设n = 323，m = 10，e = 7，将`明文`m进行幂模运算：

m<sup>e</sup> mod n = 10<sup>7</sup> mod 323 = 243

这就是RSA算法的加密了，它将`明文`10通过幂模运算映射成了`密文`243 用`c`表示，之前的`n`和`e`被称之为`公钥`。

从模运算的特性来看，不能简单通过逆向运算得到原始的明文。但是能找到一个`d = 247`，再结合`n`进行幂模运算：

c<sup>d</sup> mod n = 243<sup>247</sup> mod 323 = 10

这就是RSA算法的解密了，`d`和`n`被称之为`私钥`，总结成数学公式：

> 加密：c = m<sup>e</sup> mod n

> 解密：m = c<sup>d</sup> mod n

可以看出`公钥`和`私钥`各不相同，这就是`非对称加密算法`最大的特性，可解决通信过程中泄露密钥的问题。

另外，解密时指数特别大，JDK已提供BigInteger#divideAndRemainder支持，我们也可以自行实现`5.2 大数的幂模运算`。

### 1.2 解密公式的证明

既然通过模运算可以还原明文`m`，那么是否一定能找到`d`使得解密公式成立？需要满足三个条件：

1. `n`是两个素数`p`和`q`的乘积，用公式表示: n = pq
2. m在n的范围内，即m &lt; n
3. `d`满足 ed = 1 + u φ(n)，其中`φ(n)`被称之为`欧拉函数`，表示在[1, n)范围内与n互素的数字的个数

证明如下：

c<sup>d</sup> mod n = (m<sup>e</sup>)<sup>d</sup> mod n = m<sup>ed</sup> mod n

根据`条件3`:

m<sup>ed</sup> mod n = m<sup>1 + u φ(n)</sup> mod n = (m(m<sup>φ(n)</sup>)<sup>u</sup>) mod n

根据`2.1 同余式乘法`，等式演变为：

(m(m<sup>φ(n)</sup>)<sup>u</sup>) mod n = ((m mod n) * (m<sup>φ(n)</sup> mod n)<sup>u</sup>) mod n

根据`条件2`，m mod n = m，下面分m与n是否互素分开讨论。

#### 1.2.1 当**m与n互素**

根据`3.2 欧拉定理` m<sup>φ(n)</sup> mod n = 1，故等式演变为：

((m mod n) * (m<sup>φ(n)</sup> mod n)<sup>u</sup>) mod n = (m * 1<sup>u</sup>) mod n = m

如此就证明了 **m = c<sup>d</sup> mod n** 正确性，能将密文`c`解密成明文`m`。

#### 1.2.2 当**m和n不互素**

这时候m和n存在公因子，根据`条件1`n是两个不能再次分解的素数因子：p和q的乘积，所以要么p是m的因子，要么q是m的因子，不妨设p是m的因子，下面将先用反证法证明m与q互素，再应用`3.2 欧拉定理`将 m<sup>φ(q)</sup> ≡ 1 (mod q) 推导出 m = c<sup>d</sup> mod n

##### 1.2.3.1 若p是m的因子则m一定与q互素

p是m的因子，可写成：

m = pk

**假设m与q不互素**，因q是素数，故m只能是q的倍数，且只能从k中进行因式分解，等式可改为：

m = pk = phq = hpq = hn

这样就得到m &gt; n的结论，与`条件1` m &lt; n是矛盾的，所以**m一定与q互素**

##### 1.2.3.2 推导出c<sup>d</sup> mod n = m

**m一定与q互素**，则m和q就存在`3.2 欧拉定理`的同余式：

m<sup>φ(q)</sup> ≡ 1 (mod q)

根据`2.1 同余式乘法`，两边同时进行uφ(p)次方仍然同余相等：

m<sup>uφ(p)φ(q)</sup> ≡ 1<sup>uφ(p)φ(q)</sup> (mod q)

由于p和q是素数，根据`3.3 欧拉等式`, φ(p)φ(q) = φ(pq) = φ(n)，故同余式改为：

m<sup>uφ(n)</sup> ≡ 1 (mod q)

现将同余等式改成方程式：

m<sup>uφ(n)</sup> = 1 + vq

方程式两边同时乘上m：

m<sup>uφ(n) + 1</sup> = m + vmq

前面已假设m = pk，所以vmq = vkpq，因n = pq，再设vk = w，方程式改为：

m<sup>uφ(n) + 1</sup> = m + wn

改成模等式：

m<sup>uφ(n) + 1</sup> mod n = m

根据`条件3` uφ(n) + 1 = ed

m<sup>ed</sup> mod n = m

如此就推导出：

c<sup>d</sup> mod n = m

至此，不论m是否与n互素，均能证明 c<sup>d</sup> mod n = m 成立。

### 1.3 三个条件的补充说明
`1.2 解密公式的证明`证明了RSA解密公式的正确性，但是需要满足三个条件

* 对于`条件1`，可在生成密钥时，保证p和q是素数
* 对于`条件2`，要求明文数字m在n的范围内，即 m &lt; n
* 对于`条件3`，则是**获取私钥d的关键**，可通过`4.3 扩展欧几里得算法`推算出d，关键在于**φ(n)**，根据`3.3 欧拉等式` φ(n) = φ(p)φ(q) = (p-1)(q-1)，只要p，q不泄露，要从n中分解出p，q来却很困难，这就保证了私钥的安全。

实际上，还有一个条件没有提到，那就是**e必须是素数**，这关系到d是否存在，有关论述在`4.2 计算d`。

以上即RSA的算法描述，其中用到了很多数论原理，如欧拉函数、欧拉定理、同余式乘法等，具体相关原理和证明见后续章节。

## 第二章 模运算基础

RSA算法建立在数论基础上，下面从基础开始，逐步证明同余式乘法、欧拉定理。

### 2.1 同余式乘法
若a ≡ b mod m， c ≡ d mod m ，则  ac ≡ bd mod m ，这个定理应用在RSA证明时，用于`1.2.3.2`同余式两边共同u(p-1)次方的场景中，证明如下：

a = km + b ， c = hm + d

ac = (km + b)(hm + d) = khm<sup>2</sup> + kmd + bhm + bd

ac = bd + (khm + kd + bh)m，转换成同余式，即：

ac ≡ bd mod m

得证。

### 2.2 模运算的分配率
模运算就像普通运算一样，它是可交换、可结合、可分配的，在计算中经常用到分配率：

(a + b) mod m = ((a mod m) + (b mod m)) mod m

ab mod m = (a mod m)(b mod m) mod m

在之前RSA算法证明、`3.2 欧拉定理`的证明以及`5.2 大数的幂模运算`中都会用到本规律，它们的证明方法是一样的，以乘法为例，证明如下：

(a mod m)(b mod m) mod m = (a - k<sub>1</sub>m)(b - k<sub>2</sub>m) - k<sub>3</sub>m

= ab - (ak<sub>2</sub> + k<sub>1</sub>b - k<sub>1</sub>k<sub>2</sub>m - k<sub>3</sub>)m

= ab mod m

得证。

### 2.3 互素的传递性

1. 如果整数a和b 都与c互素，那么ab也与c互素，因为他们都没有公约数。

2. 如果整数p和q互素，那么(p mod q)后仍然与q互素。

第2点可用反证法：

设 p mod q = r，即：p = r + kq，假设r与q不互素，那么r和q存在公约数d，上面等式可以改写为：p = dr' + kdq' = d(r' + kq')

如此一来d也能整除p，这与p，q互素矛盾。

### 2.4 消去律

该定理将会用于`3.2 欧拉定理`的证明中。

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

## 第三章 欧拉定理

欧拉定理是RSA算法的核心，在有了模运算基础后，就可以证明此定理。

### 3.1 欧拉函数
在数论中，将[1, n)集合中与n互素的个数记为φ(n)，被称之为`欧拉函数`。

例如φ(8) = 4，这是因为1，3，5，7这4个数和8互素。

若参数是素数例如7，那么φ(7) = 7 - 1 = 6，这是因为[1,7)集合中都与其互素。

### 3.2 欧拉定理
如果两个正整数a和n互素，φ(n)是n的欧拉函数，存在下面的等式：
a<sup>φ(n)</sup> ≡ 1 (mod n)

即：a<sup>φ(n)</sup> = 1 + kn，或：a<sup>φ(n)</sup> mod n = 1

比如，3和7互素，而7的欧拉函数φ(7)等于6，则 3<sup>6</sup> mod 7 = 1

证明分两步：
#### 3.2.1证明以下两个集合相等
Zn = {x<sub>1</sub>, x<sub>2</sub>, ... , x<sub>φ(n)</sub>}

S = {ax<sub>1</sub> mod n, ax<sub>2</sub> mod n, ... , ax<sub>φ(n)</sub> mod n}

从[1,n)集合中，找到与n互素的正整数，组成Zn集合，根据欧拉函数的定义，Zn集合中的元素的个数有φ(n)个。

将Zn集合中的每个元素乘a，再模n，得到S集合，其中a也是与n互素的正整数，S集合也有φ(n)个元素。

##### 3.2.1.1 证明S与Zn集合元素个数相等
在S集合中，因为a与n互素，并且x<sub>1</sub>, x<sub>2</sub>, ... , x<sub>φ(n)</sub>都与n互素，所以根据`2.3 互素的传递性`ax<sub>1</sub> mod n，ax<sub>2</sub> mod n，……，ax<sub>φ(n)</sub> mod n，它们都与n互素，所以都属于Zn集合，且S中的元素个数与Zn元素的个数相等，下面证明S集合中的每个元素都互不相等，就能说明Zn = S

##### 3.2.1.2 用反证法证明S集合中的每个元素各不相同
假设ax<sub>i</sub> mod n = ax<sub>j</sub> mod n ，因a，n互素，根据`2.4 消去律`得知：x<sub>i</sub> mod n = x<sub>j</sub> mod n，又，x<sub>i</sub> 和 x<sub>j</sub>都属于集合Zn，它们都小于n，所以x<sub>i</sub> = x<sub>j</sub>，这个结论与事实矛盾，如此就证明了Zn = S 。

#### 3.2.2 证明1 ≡ a<sup>φ(n)</sup> (mod n)
因Zn = S，故分别将Zn集合和S集合中的每个元素相乘，根据`2.1 同余式乘法`它们也应该同余相等：
x<sub>1</sub> x<sub>2</sub> ... x<sub>φ(n)</sub> ≡ a<sup>φ(n)</sup> x<sub>1</sub> x<sub>2</sub> ... x<sub>φ(n)</sub> mod n

已知x<sub>1</sub>，x<sub>2</sub>，...，x<sub>φ(n)</sub>与n互素，那么x<sub>1</sub> x<sub>2</sub> ... x<sub>φ(n)</sub>也与n互素，上式通过`2.4 消去律`得到：

1 ≡ a<sup>φ(n)</sup> (mod n)

得证。

### 3.3 欧拉等式

两个不同的素数p，q，它们的乘积n = pq，则φ(n) = φ(p)φ(q)，这被称之为欧拉等式，在`1.2.3.2`中应用到，证明如下：

在小于n的域中，与n互素的集合中的个数有：

{1,2,3,……,n-1} - {p,2p,……,(q-1)p} – {q,2q,……,(p-1)q}

所以φ(n) = (n-1)-(q-1)–(p-1) = (p-1)(q-1) = φ(p)φ(q)

去掉中间步骤，得到重要结论（本该用`中国剩余定理`来证明的）

## 第四章 RSA的密钥

根据第一章的描述，RSA算法需要6个参数，p， q， n， φ(n)， e， d ，其中：

- p，q是随机生成的大素数；
- n = pq ，是加密和解密计算时的模；
- φ(n)是n的欧拉函数值；
- e是加密时中使用的公钥，它与φ(n)互素；
- d是解密时需要的私钥，它是通过e和φ(n)计算出来的，它们的关系是：ed ≡ 1 (mod φ(n))，也可以称d是e关于模φ(n)的`逆元`（与乘法倒数类似）。即：e*d mod φ(n) = 1，这个等式还可以写成：ed = 1 + u φ(n)

### 4.1 获取RSA算法所需参数

1. 对于p，q的选择，比较困难，小素数（比如1亿）的测试，还可以用蛮力法进行测试：

> 首先由java生成随机数p，过滤掉偶数，然后依次用2到sqrt(p)之间的数去整除p，如果没有一个数能被整除，说明p是素数。

但是对于成百上千位的大素数来说，这种方式的性能是不可接受的，对于大素数的筛选方法有Miller检验，不过它理论我还没弄懂，所以并没有实现该方法，这里使用JDK，BigInteger自带的：

```java
BigInteger probablePrime(int bitLength, Random rnd);
```

> 需要注意的是，该方法返回的可能是素数的，还需要在RSA中进行测试，看看是否能解密还原。

2. 找到p，q后，就得到n = pq，由于p，q互素，φ(n) = φ(pq) = φ(p)φ(q) = (p - 1)(q - 1)，如此也计算出了φ(n)。

3. 对于公钥e，来说选择就比较容易，只要与φ(n)互素即可，一般e选择素数65537，e要求是素数的原因一直没有提到，在`4.2 计算d`中有说明。

4. 现在最重要的是找到私钥d，目前只知道d与e，φ(n)之间存在关系：ed = 1 + u φ(n)

### 4.2 计算d
将方程 ed = 1 + u φ(n) 改为：

-φ(n)u + ed = 1

这是一个二元一次方程，e，φ(n)作为系数是已知量，而u，d未知变量，只要e互素φ(n)，那么它们的最大公约数就是1，根据`5.1 裴蜀定理`，u，d存在整数解，这就是**e要求是素数**的原因。

令a = - φ(n)，e = b，u = x，**d = y**，可将方程式改为传统二元一次方程模样：

ax + by = 1

再参考`5.3.1 算法描述`即可推导出d，此处展示代码的实现。
```java
class Euclid {
  private BigInteger fn, e, x, y, d;// x，y成员变量，用于保存generateD()计算时的中间值
  Euclid(BigInteger fn, BigInteger e) {// fn和e即已知量，fn即算法中的a，e即算法中的b
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
      x = BigInteger.ONE;
      y = BigInteger.ZERO;
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
```

## 第五章 裴蜀定理和扩展欧几里得算法

### 5.1 裴蜀定理

> gcd(a,b)是a，b的最大公约数，存在整数x和y使得ax + by = gcd(a,b)

为什么x和y一定会存在整数解？下面将将分三步来证明裴蜀定理：

#### 5.1.1 第一步
设ax + by是一个正整数的集合，该集合存在最小的正整数，设为d，先证明ax + by集合中任意整数都是d的倍数

可以用反证法证明d能整除ax + by集合中的所有值：

当x = x<sub>0</sub>，y = y<sub>0</sub>时，有等式：ax<sub>0</sub> + by<sub>0</sub> = d

当x = m，y = n时，有等式：am + bn = e

当然e ≥ d，假设d不能整除e，对e做带余除法，必定存在 p，r 使 e = pd + r，显然，r &lt; d
但是通过下面的推导却会得出r &gt; d矛盾的结果：

r = e - pd = (am + bn) - p(ax<sub>0</sub> + by<sub>0</sub>) = (m - px<sub>0</sub>)a + (n - py<sub>0</sub>)b

观察上面等式的首尾，说明存在整数 m - px<sub>0</sub>， n-p<sub>y0</sub> 使 ax + by = r &lt; d，这与d的最小性矛盾。

所以d整除e，即d能整除ax + by集合中的任意值。

#### 5.1.2 第二步
证明d是a和b的公约数

令x = 1，y = 0，则ax + by = a，所以d能整除a

同理d也能整除b，这就说明d是a，b的公约数。

#### 5.1.3 第三步
实际上，d就是a和b的最大公约数，证明如下：

对于a和b的任意公约数d’，存在a = kd’，b = ld’，有等式成立：

d = kd’x + ld’y = d’(kx + ly)

所以a，b的任意公约数d’都能整除d，即d就是a和b的最大公约数（最大公约数等于各公约数的积）

> 特例：当a，b互素时，d = 1，这就说明了对于互素的a和b，一定存在x，y满足 ax + by = 1

### 5.2 欧几里德算法
欧几里德算法又称辗转相除法，用于计算两个整数a,b的最大公约数。算法基于下面这个等式：

gcd(a,b) = gcd(b,a mod b)

证明：

设gcd(a,b) = g，那么可写成：a = mg，b = ng，这里的**m和n一定是互素**的，否则g就不是a和b的最大公约数。

a和b存在关系式：r = a - kb = mg - kng = (m - kn)g

现在比较这两个式子：

r = (m - kn)g

b = ng

可以看出，g是r和b的公约数，另外，因m与n互素，所以(m - kn)与n互素，如此就说明g也是r和b的最大公约数，所以证明了gcd(a,b) = gcd(b,r) = gcd(b,a mod b)

### 5.3 扩展欧几里得算法

RSA获取私钥d的过程，实际上是解二元一次方程，该方程在`5.1 裴蜀定理`中，是最大公约数为1时的特殊形式，所以存在x, y的整数解。要解开此方程，还需要借助`5.2 欧几里德算法`中最大公约数串联的特性，结合起来就是`扩展欧几里得算法`。

#### 5.3.1 算法描述
设x<sub>0</sub>,y<sub>0</sub>是二元一次方程：ax + by = gcd(a, b) 的一组解，算法最终就是要求得**x<sub>0</sub>,y<sub>0</sub>**。

根据`5.1 裴蜀定理`，存在x<sub>1</sub>,y<sub>1</sub>是二元一次方程：bx + (a mod b)y = gcd(b, a mod b) 的一组解，又根据`5.2 欧几里德算法` gcd(a, b) = gcd(b, a mod b)，所以：

ax<sub>0</sub> + by<sub>0</sub> = bx<sub>1</sub> + (a mod b)y<sub>1</sub>

这里的a mod b在程序中可以写成 a - (a/b)*b （a/b指的是a除以b取整的意思，例如System.out.println(20/7)，显示的结果是2）
所以等式可以改写成：

ax<sub>0</sub> + by<sub>0</sub> = bx<sub>1</sub> + (a-(a/b)b)y<sub>1</sub> = ay<sub>1</sub> + bx<sub>1</sub> - (a/b)by<sub>1</sub> = a(y<sub>1</sub>) + b(x<sub>1</sub>-(a/b)y<sub>1</sub>)

对比等式左右两边发现迭代关系如下：

x<sub>0</sub> = y<sub>1</sub>

y<sub>0</sub> = x<sub>1</sub>-(a/b)*y<sub>1</sub>

在欧几里得算法中，递归地辗转相除，直到 a<sub>n</sub> mod b<sub>n</sub> = 0，此时gcd(a<sub>n</sub>, b<sub>n</sub>) = a<sub>n</sub>。

根据`5.1 裴蜀定理`，存在x<sub>n</sub>,y<sub>n</sub>是二元一次方程：a<sub>n</sub>x<sub>n</sub> + b<sub>n</sub>y<sub>n</sub> = gcd(a<sub>n</sub>, b<sub>n</sub>) 的一组解，所以有：

a<sub>n</sub>x<sub>n</sub> + b<sub>n</sub>y<sub>n</sub> = a<sub>n</sub>

该等式成立的必要条件是 x<sub>n</sub> = 1, y<sub>n</sub> = 0，这就得到了递归最后一层的结果，此时递归将逐层返回，并迭代计算出每层的x,y，最后求得**x<sub>0</sub>,y<sub>0</sub>**

## 第五章 代码实现
长篇理论终于翻篇了，下面补充说明一下Java代码中一些实现。

### 5.1 获取大素数p，q
大素数校验理论较为艰深，我的代码中并未实现，直接调用JDK中的：

> BigInteger probablePrime(int bitLength, Random rnd);

### 5.2 大数的幂模运算
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

private static BigInteger squareAndMod(BigInteger bottomNumber, BigInteger module) {
  return bottomNumber.multiply(bottomNumber).remainder(module);
}
```
### 5.3 加密与解密
终于，所有的准备工作已完成，剩下的就是简单的加密和解密。下面的一段测试代码将直接说明加密与解密的过程：

```java

private String plainText = "hello wolrd!";

private static boolean test(BigInteger e, BigInteger d, BigInteger n) {
  // 先将明文转为数字
  byte[] bytes = plainText.getBytes();
  BigInteger mm = new BigInteger(bytes);
  // 用公钥 e,n 加密
  BigInteger cc = powModByMontgomery(mm, e, n);
  // 用私钥 d,n 解密
  BigInteger dm = powModByMontgomery(cc, d, n);
  if (mm.compareTo(dm) != 0) {
    LOG.debug("Test failed");
    return false;
  } else {
    LOG.debug("Test passed ");
  }
  return true;
}
```

