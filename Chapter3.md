# すべてのオブジェクトに共通のメソッド
Objectは具象クラスであるにもかかわらず、主に拡張されるために設計されている。final ではないメソッド(equals, hashCode, toString, clone, finalize)はオーバーライドされるように設計されているので、明示的な`一般契約`をもつ。それらの一般契約に従うことは、それらのメソッドをオーバーライドするクラスの責任である。契約に従わないクラスは、その契約に依存している他のクラスと一緒には適切に機能しなくなる。

3章ではfinalではない Object のメソッドを、いつどのようにオーバーライドするかについて説明する finalize メソッドについては項目8で説明したので本章では省く。

# 項目10 equalsをオーバーライドするときは一般契約に従う
equals メソッドをオーバーライドするときは一般契約に従う。equals メソッドを間違ったやり方でオーバーライドする方法は沢山ある。問題を避ける最も良い方法はメソッドをオーバーライドしないことである。

## equalsをオーバーライドしてはならないとき
- クラスの個々のインスタンスが本質的に一意である
    - Thread のような値ではなく、能動的な実体を表すクラスに対しては Object が提供している equals の実装が最良である
- クラスが「論理的等価性」の検査を提供する必要がない
    - 特定のクラスに equals をオーバーライドしても、クライアントがその機能を必要としない場合は Object から継承された equals の実装を使う
- スーパークラスがすでに equals をオーバーライドしており、スーパークラスの振る舞いがこのクラスに対して適切である
- クラスが private あるいはパッケージプライベートであり、そのequalsメソッドが呼び出されないことが確かである
    - うっかり呼び出されないようにequalsメソッドを次のようにオーバーライドできる

```java
@Override public boolean equals(Object o) {
    throw new AssertionError() //メソッドは呼び出されない
}
```

- 1つのオブジェクトしか存在しないようにするためにインスタンス制御(項目1)を使うクラス
    - enum(項目34)はこれに当てはまる

## equalsをオーバーライドしてもよいとき
- クラスがオブジェクトの同一性を超えた、「論理的等価性」の概念を持っている
- スーパークラスが equals をオーバーライドしていない時

上記を満たすのは Integer や String などのように値を表現する値クラスである場合が多い。

## equalsの一般契約
Object の equals をオーバーライドする場合に、厳守しなければならない一般契約の性質は以下の通りである。
- 反射性（reflexive）
- 対照性（symmetric）
- 推移性（transitive）
- 整合性（consistent）
- 非null性（non-null）

## 反射性
`定義：nullではない任意の参照値xに対して、x.equals(x)はtrueを返さなければならない`

オブジェクトがそれ自身と等しくならなければならないことを要求している。

## 対称性
`定義：nullではない任意の参照値xとyに対して、y.equals(x)がtrueを返す場合のみ、x.equals(y)はtrueを返さなければならない`

いかなる2つのオブジェクトでも、それらが等しいかどうか合意しなければならないことを要求している。

この条件を無視した equals を実装する。
以下に大文字小文字を区別しない文字列を実装している CaseInsensitiveString クラスを示す。

```java
// 対称性を守っていない
public final class CaseInsensitiveString {
    private final String s;

    public CaseInsensitiveString(String s) {
        this.s = Objects.requireNonNull(s);
    }

    // 対称性を守っていない
    @Override public boolean equals(Object o) {
        if (o instanceof CaseInsensitiveString) {
            return s.equalsIgnoreCase(((CaseInsensitiveString) o).s);
        }

        if (o instanceof String) { // 一方向の相互作用
            return s.equalsIgnoreCase((String) o);
        }

        return false;
    }
}
```

この実装では x.equals.(y) と y.equals(x) それぞれの真偽値が異なってしまう。これは、String クラスの equals は CaseInsensitiveString に関しては何も知らないために起きる。

```java
CaseInsensitiveString cis = new CaseInsensitiveString("Polish");
String s = "polish";
cis.equals(s); // true
s.equals(cis); //false
```

CaseInsensitiveString をコレクションに入れたと仮定する。

```java
List<CaseInsensitiveString> list = new ArrayList<>();
list.add(cis);
list.contains(s); //何を返すかは実装に依存する
```

この問題はequalsメソッドからStringを扱う条件分岐をを取り除くことで解決する

```java
@Override public boolean equals(Object o ) {
    return o instanceof CaseInsensitiveString && ((CaseInsensitiveString) o).s.equalsIgnoreCase(s);
}
```

## 推移性
`定義：nullではない任意の参照値x,y,zに対して、もし、x.equals(y), y.equals(z)がtrueを返すならば、x.equals(z)はtrueを返さなければならない`

1つ目のオブジェクトが2つ目のオブジェクトと等しく、かつ、2つ目のオブジェクトが3つ目のオブジェクトと等しい場合は1つ目のオブジェクトは3つ目のオブジェクトと等しくならなければならない。

スーパークラスに新たな`値要素`を付加するサブクラスを考える。

```java
public class Point {
    private final int x;
    private final int y;
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Point)) {
            return false;
        }
        Point p = (Point) o;
        return p.x == this.x && p.y == this.y;
    }
}

public class ColorPoint extends Point {
    private Color color;
    public ColorPoint(int x, int y, Color color) {
        super(x, y);
        this.color = color;
    }
}
```
この場合、一般契約は破られていないが、equalsの比較で色情報が無視されてしまう。

引数に同じ位置と色と持つ他のカラーポイントが与えられた場合にtrueを返すequalsメソッドを作る。

```java
// 対象性を守っていない
@Override
public boolean equals(Object o) {
    if (!(o instanceof ColorPoint)) {
        return false;
    }
    return super.equals(o) && ((ColorPoint) o).color == color;
}
```

この場合、対称性が守られない。

```java
Point p = new Point(1, 2);
ColorPoint cp = new ColorPoint(1, 2, Color.RED);
p.equals(cp); // true
cp.equals(p); // false 
```

そこで、次のように ColorPoint.equals が色を無視することでこの問題を解決しようと試みる。

```java
@Override
// 推移性を守っていない
public boolean equals(Object o) {
    if (!(o instanceof Point)) {
        return false;
    }
    // oが普通のポイントなら、色を無視した比較をする
    if (!(o instanceof ColorPoint)) {
        return o.equals(this);
    }
    return super.equals(o) && ((ColorPoint) o).color == color;
}
```

この方法は対称性を守っているが、推移性を守っていない

```java
ColorPoint p1 = new ColorPoint(1, 2, Color.RED);
Point p2 = new Point(1, 2);
ColorPoint p3 = new ColorPoint(1, 2, Color.BLUE);
p1.equals(p2); // true   色が見えない
p2.equals(p3); // true   色が見えない
p1.equals(p3); // false  色を考慮している
```

また、この方法は無限再帰を引き起こす可能性がある。

Pointのサブクラスに ColorPoint と SmellPoint の2つがあり、それぞれにこのようなequalsメソッドがオーバーライドされていると仮定する。

```java
ColorPoint cp = new ColorPoint(1, 2, Color.RED); // Pointのサブクラス
SmellPoint sp = new SmellPoint(1, 2); // Pointのサブクラス
cp.equals(sp); // StackOverflowError
```

この問題の解決方法として、instanceOfの代わりに getClass() を用いる方法がある。

その場合はリスコフの置換原則を破ることになる。

参考：[リスコフの置換原則](http://marupeke296.com/OOD_No7_LiskovSubstitutionPrinciple.html)

```java
// ColorPointのequals
@Override
public boolean equals(Object o) {
    if (!(o instanceof ColorPoint)) {
        return false;
    }
    return super.equals(o) && ((ColorPoint) o).color == color;
}

// Pointのequals
@Override
public boolean equals(Object o) {
    // リスコフのち缶原則を破っている
    if (o == null || o.getClass() != getClass()) {
        return false;
    }
    Point p = (Point) o;
    return p.x == this.x && p.y == this.y;
}
```

```java
// 推移性が守られる
ColorPoint p1 = new ColorPoint(1, 2, Color.RED);
Point p2 = new Point(1, 2);
ColorPoint p3 = new ColorPoint(1, 2, Color.BLUE);
p1.equals(p2); // => false
p2.equals(p3); // => false
p1.equals(p3); // => false
```

この場合、オブジェクト同士が同じ実装クラスである場合にのみ等しくなるので推移性が守られる。

しかし、PointとColorPointを HashSet に格納している場合に期待通りに動作しない。

以下に点が単位円の上にあるか判定するメソッドを示す。

```java
private static final Set<Point> unitCircle = Set.of(
        new Point(1, 0), new Point(0, 1),
        new Point(-1, 0), new Point(0, -1));

public static boolean onUnitCircle(Point p) {
    return unitCircle.contains(p);
}
```

次に、生成されたインスタンスの個数をコンストラクタで記録するようにPointクラスを拡張したCounterPointクラスを示す。

```java
public class CounterPoint extends Point {
    private static final AtomicInteger counter = new AtomicInteger();

    public CounterPoint(int x, int y) {
        super(x, y);
        // 以下省略
    }
}
```

Pointのサブクラスのインスタンスは Point として機能する必要がある(リスコフの置換原則)。しかし、以下のように onUnitCircle に対してCounterPointインスタンスを渡し、Pointクラスが getClass に基づくequalsメソッドを使っている場合、CounterPointメソッドのxとy座標に関係なく false を返すようになる。これは、HashSet といったほとんどのコレクションがequalsを使って実装されており、CounterPointインスタンスがどのPointとも等しくならないことが原因である。

```java
CounterPoint cp = new CounterPoint(0, 1);
onUnitCircle(cp) // false
```

`インスタンス可能なクラスを拡張してequalsの契約を守ったまま値要素を追加する方法はない`。 インスタンス化可能クラスを拡張して値を追加するよい方法は、拡張の代わりにコンポジションを持ちいてviewメソッドを利用する方法である（項目6）。

```java
// equals契約を破ることなく値要素を追加する
public class ColorPoint {
    private final Point point;
    private final Color color;

    public ColorPoint(int x, int y, Color color) {
        this.point = new Point(x, y);
        this.color = color;
    }

    // このカラーポイントとしてのビューを返す
    public Point asPoint() {
        return point;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ColorPoint)) {
            return false;
        }
        ColorPoint cp = (ColorPoint) o;
        return cp.point.equals(point) && cp.color.equals(color);
    }
}
```

抽象クラスのサブクラスにはequalsの契約を破ることなく値要素を追加することができる。上記で示した問題はスーパークラスのインスタンスが生成されない限り発生しない。

## 整合性
`定義：nullではない任意の参照値xとyに対して、x.equals(y)の複数回の呼び出しは、equalsの比較で使われる情報に変更がなければ一貫してtrueを返すかfalseを返さなければならない`

不変オブジェクトが時間の経過により異なるオブジェクトと等しくなることはない。一方で、可変オブジェクトは等しくなり得る。クラスが不変であるかに関係なく、`信頼できない資源に依存するequalsを書いてはならない`。そのような場合はこの整合性を守ることが非常に難しくなる。

java.net.URL の equals はホストをIPアドレスに変換する必要があるため、ネットワークアクセスが必要となる。 そのため、オブジェクトが変更されていないにも関わらず、常に一致することが保証できない。これは実際に問題を引き起こしている。

## 非null性
`定義：nullではない任意の参照値xに対して、x.equals(null)はfalseを返さなければならない`

すべてのオブジェクトはnullと等しくなってはならない。また、NullPointerExceptionをスローすることも認められていない。

これを満たすための実装を以下に示す。

```java
@Override public boolean equals(Object o) {
    if (!(o instanceof MyType)) {
        return false;
    }
    // 以下省略
}
```

equalsではnullチェックが必要であるが、instanceof演算子は第1オペランドがnullの場合は必ずfalseを返すため、明示的にnullをチェックする必要はない。

## 正しいequals書き方
1. 引数が自分自身のオブェクトであるかどうかを検査するために == 演算子を使用する。必須ではないが、比較のコストを低減することができる。

1. 引数が正しい型であるかを調べるために instanceof を使用する。

1. 引数を正しい型にキャストする。instanceof によって型が検査されているので、例外は発生しない。

1. 引数のオブジェクトのフィールドが、このオブジェクトの対応するフィールドと一致しているかどうか検査する
    - 基本データ型には == を、オブジェクトには equals を使用する。
    - float と double は Float.compare と Double.compare を利用する。※Float.equals, Double.equalsで比較できるが、比較ごとに自動ボクシングが伴うのでパフォーマンスが悪くなる。
    - 配列のフィールドは Array.equals が利用できる。
    - オブジェクトの参照フィールドが正当な値としてnullを含む場合があり、NullPointerException をさけるために Objects.equals(Object, Object); を使って同値性を検査する。
    - CaseInsensitiveString のようにフィールドの比較が複雑な場合、そのフィールドの正規形として CaseInsensitiveString がすべての小文字の形式を保存するとしたら、標準的でない比較よりも正確な比較を行える。この技法は不変クラス（項目17）に対して最も適切である。

1. equals を書き終えた後に「対称性」、「推移性」、「整合性」の三つの性質を満たしたかどうかを自問し、テストを書く。ただし、equalsメソッドを生成するためにAutoValue(51ページ)を使っている場合は単体テストを省略しても良い。「反射性」と「非 null 性」を満たす必要もあるが、この二つは大抵の場合満たされる。

```java
// 典型的なequalsメソッドをもつクラス
public class PhoneNumber {
    private final short areaCode, prefix, lineNum;

    public PhoneNumber(int areaCode, int prefix, int lineNumber) {
        this.areaCode = rangeCheck(areaCode, 999, "areaCode");
        this.prefix = rangeCheck(prefix, 999, "prefix");
        this.lineNum = rangeCheck(lineNumber, 9999, "line num");
    }

    private static void rangeCheck(int val, int max, String arg) {
        if (val < 0 || val > max) {
            throw new IllegalArgumentException(arg + ": " + val);
        }
        return (short) val;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PhoneNumber))
            return false;

        PhoneNumber phoneNumber = (PhoneNumber) o;
        return phoneNumber.lineNum == this.lineNum
                && phoneNumber.prefix == this.prefix
                && phoneNumber.areaCode == this.areaCode;
    }
}
```

## 最後の注意事項
- equals をオーバーライドするときは常に hashCode をオーバーライドする
- あまりにかしこくなろうとしない。
    - 単純にフィールドが等しいかどうかをテストすれば equals の契約を守れる。
    - 例えばFileクラスは、同じファイルを参照しているシンボリックリンクを等しいとみなすべきではない。
- equals の引数は Object 型である。
    - equals の引数を Object 以外にするとオーバーロード（項目52）になる。これを防ぐために @Override アノテーションをつける（項目40）。

## まとめ
- 必要ない限りequalsメソッドをオーバーライドしない。
- equalsをオーバーライドする場合は、クラスの意味のあるフィールドをすべて比較し、equalsの一般契約をすべて守る。

# 項目11 equalsをオーバーライドするときは、常にhashCodeをオーバーライドする
`equals をオーバーライドする時は、hashCode メソッドを必ずオーバーライドしなければならない`。オーバーライドしない場合、Object.hashCode の一般契約を破ることになり、HashMap、HashSet などのコレクションが適切に機能しない。

## Object.hashCode の一般契約
1. アプリケーション実行中のhashCodeメソッドの値は、equals比較で使われるオブジェクトの値に変更がなければ一貫して同じ値を返す。これは再びアプリケーションを実行した場合で一致する必要はない。
1. 2つのオブジェクトに対する equals による比較が等しければ、2つのオブジェクトの hashCode 呼び出しは同じ整数結果を返さなければならない。
1. 2つのオブジェクトに対する equals による比較が等しくなければ、2つのオブジェクトの hashCode 呼び出しの整数結果が異なる必要はない。
    - equals で等しくないオブジェクトに同じ整数結果を返すことはハッシュテーブルのパフォーマンスを大幅に低下させる。

## hashCode のオーバーライドを怠った場合
hashCode のオーバーライドを忘れると2つめの「等しいオブジェクトは同じ hashCode 値を持たなければならない」という一般契約を破ることになってしまう。

例えばHashMapのキーとして項目10のPhoneNumberクラスのインスタンスを使う。
```java
Map<PhoneNumber, String> hashMap = new HashMap<>();
hashMap.put(new PhoneNumber(707, 867, 5309), "Jenny");
hashMap.get(new PhoneNumber(707, 867, 5309)); // nullを返す
```

get は"Jenny"が返されることを期待しているが、nullが返却される。これは hashCode がオーバーライドされていないため、2つの等しいインスタンスがそれぞれ異なるhashCodeを持つからである。put はオブジェクトが保存されたバケットとは異なるバケットを検索する。

たまたま同じバケットで検索を始めても、HashMap の実装はハッシュコードが一致しないオブジェクトは同一だと見なさず、equals 比較を行わないのでほとんどの場合 null が返ることになる。

## hashCodeをオーバーライドする
上記の問題は hashCode をオーバーライドすると解決する。雑に解決する場合は以下のような実装になるが、これは使われるべきではない。
```java
@Override
public int hashCode() {
    return 42;
}
```

このメソッドは正当であるが、すべてのオブジェクトが同じハッシュコードを持つのでハッシュテーブルはLinkedListへと退化する。そのため、パフォーマンスが著しく悪化し、実質機能しないとの同じになる。

そこで、等しくないインスタンスに対して、等しくない値を生成する。理想的には等しくないいかなるインスタンスに対しても、intの範囲内ですべて異なるハッシュ値を生成する関数を用いるべきである。しかし、これは非常に困難であるため、適度に近似する関数を用いる。

## ハッシュコードの計算
1. result というint変数を宣言し、オブジェクト内の最初の意味のあるフィールドに対してハッシュコード c で初期化する。
1. オブジェクト内の残りすべての意味のあるフィールドfに対して次のことを行う
    1. そのフィールドが基本データ型であれば、Type.hashCode(f)を計算する。ここで Type は f の型にボクシングされた基本データ型である。※例えばfがint型ならInteger
    2. オブジェクト参照で、equals 内でフィールドを equals によって比較しているならばフィールドに対して再帰的にhashCodeを呼び出す。フィールドの値がnullなら0を返す。
    3. フィールドが配列である場合は、意味のある要素に対しhashCodeを計算して結合する。配列が意味を持たないのであれば0以外の定数を使う。すべての要素が意味のある要素であれば Arrays.hashCode を使う。
    4. 計算されたハッシュコード c をresultに入れる => `result = 31 * result + c`
1. resultを返す
    - 等しいインスタンスが等しいハッシュコードを持つかどうかを自問し、単体テストを書く

- 手順2の4での乗算によってハッシュ値はフィールドの順番に依存するようになる。同じようなフィールド値を複数持つオブジェクトの衝突する可能性が減る。
- 乗数 31 は奇数の素数なので選ばれている。 偶数を選んだ場合、乗算がオーバーフローすると情報が失われる。

PhoneNumberクラスに適用した場合は以下のような実装になる
```java
@Oberride 
public int hashCode() {
    int result = Short.hashCode(areaCode);
    result = 31 * result + Short.hashCode(prefix);
    result = 31 * result + Short.hashCode(lineNum);
    return result;
} 
```

より衝突が少ないハッシュ関数を必要とするならGuavaのcom.google.common.hash.Hashingを参考にする。

## ハッシュの最適化
Objectsクラスは、任意の個数を受け取り、それらのハッシュコードを返すstaticメソッドを持っている。このメソッドは、可変長の引数を渡すための配列の生成と、引数に基本データ型があればボクシング/アンボクシングを行うため実行が遅い。以下にこの技法を使って書かれたPhoneNumberのハッシュ関数を示す。
```java
// 1行hashCodeメソッド。あまりよくないパフォーマンス
@Oberride 
public int hashCode() {
    return Objects.hash(lineNum, prefix, areaCode);
}
```

もし、クラスが不変であることが保証されている場合、ハッシュコードをキャッシュして計算コストを低くすることができる。

- パフォーマンスを向上させるためにハッシュコードの計算から意味のある部分を排除してはならない。ハッシュ関数の動作は速くなるが、ハッシュテーブルのパフォーマンスを使えないほど遅くする可能性がある。
- String, Integer, Date などの Java ライブラリは hashCode の返り値として厳密な値を定義している。これは、さらによいハッシュ関数の実装が見つかった場合に、置き換えることができなくなるので良い考えではない。hashCodeが返す値を厳密に決めないことで、変更する柔軟性が生まれる。

# 項目12 toStringを常にオーバーライドする
ObjectはtoStringメソッドの実装を提供しているが、それが返す文字列は「クラス名@ハッシュコードの符号なし16進数表現」例：PhoneNumber@adbbdである。
```java
PhoneNumber phoneNumber = new PhoneNumber(707, 867, 5309);
phoneNumber.toString(); // PhoneNumber@adbbd
```

## ドキュメンテーション
- toStringの契約は、すべてのサブクラスがこのメソッドをオーバーライドすることを推奨している。
- toStringメソッドはオブジェクトに含まれる興味のあるすべての情報を含むべきである。
```java
// ↓{Jenny=PhoneNumber@adbbd} と {Jenny=707-867-5309} どちらで表示されるのが嬉しいか
System.out.println("Failed to connect: " + phoneNumber);
```
返すべき情報量が多い場合は、すべての情報を返すのは実用的ではないので、 `Manhattan white pages (1487536 listings)` のようなメッセージを返す。


- toString() を明示的に定義し、String を引数とする static ファクトリーメソッドやコンストラクタを提供すると、文字列表現とオブジェクトの相互変換が可能になる。 Java ライブラリの多くの値クラスではこれが採用されており、BigInteger や BigDecimal、基本データ型などがそれにあたる。
```java
Integer integer = new Integer("10");
new Integer(integer.toString());
```

- 値クラスに対しては toString() の返り値をドキュメントに明示的に定義することが推奨される。
- 戻り値の厳密な値を明記するかどうかに関わらず、toString() 実装の意図はドキュメントとして明記したほうがよい。
    - 戻り値を明示することの欠点は、クラスが広く使われていると、一度形式を明示してしまえば未来永劫変更することができなくなる点。

## cloneを注意してオーバーライドする
Cloneableメソッドを実装しているクラスは、適切に機能する public の clone メソッドを提供することが期待されている。
- Cloneable インターフェースにはcloneメソッドがない
- Objectのcloneメソッドはprotectedである
- リフレクション（項目65）の手助けなしでは単にインターフェースを実装しているだけのObjectに対してcloneメソッドを呼び出すことができない。
- Cloneableを実装していない場合は CloneNotSupportedException をスローする。

## clone メソッドの一般契約
```java
x.clone() != x // true
x.clone().getClass() == x.getClass(); // true 必須条件ではない
x.clone().equals(x); // true 必須条件ではない
```

cloneメソッドが返すオブジェクトは super.clone を呼び出すことで得るべきである。スーパークラスがすべてこの慣習に従えば以下が成り立つ。
```java
x.clone().getClass() == x.getClass();
```

クラスのcloneメソッドがコンストラクタの呼び出しで得たインスタンスを返すと、サブクラスが super.clone を呼び出した場合にサブクラスの cloneメソッドが誤ったオブジェクトになる。

```java
class Super implements Cloneable {
    private String value;

	public Super(String str) {
		this.value = str;
	}

	public String toString() {
		return value;
	}

	@Override
	public Super clone() {
		return new Super(value); // コンストラクタ呼び出しで得たインスタンスを返す
	}
}

class SubClass extends Super {
	private int number;

	public SubClass(String str, int n) {
		super(str);
		this.number = n;
		System.out.println(super.clone()); // サブクラスのインスタンスが返らない
	}
}
```
クラスがfinalである場合は継承できないので super.clone を呼び出す慣習を無視して良い。

不変オブジェクトの場合は無駄な複製を促すだけなので clone メソッドを提供する必要はない。

```java
// 可変な状態への参照を持たないメソッド
@Override
public PhoneNumber clone() {
    try {
        return (PhoneNumber) super.clone();
    } catch (CloneNotSupportExtension e) {
        throw new AssertionError(); // 起こり得ない
    }
}
```
共変戻り値型をサポートしているので、 clone メソッドが PhoneNumberを返している。
共変戻り値型：メソッドをオーバーライドしたとき、戻り値の型をサブクラスにできる。これにより、呼び出し側でキャストが不要になる

## 可変オブジェクトの参照がある場合
- シャローコピー：メンバ変数がオブジェクトである場合に、その参照をコピーする。
- ディープコピー：フィールドのオブジェクト自身も複写する方式。ディープコピーを自動的に行うメソッドは用意されていないので自分で作る必要がある。

可変オブジェクトの参照があるクラスの場合はディープコピーを実装する必要がある。
項目6で紹介した Stack クラスを考える
```java
public class Stack {
    private Object[] elements;
    private int size = 0;
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    
    public Stack() {
        this.elements = new Object[DEFAULT_INITIAL_CAPACITY];
    }
    
    // 省略
}
```

super.clone で返されるインスタンスは、elementsの参照先がコピーされている。
これは Stack のコンストラクタを呼び出した場合には発生しない。
正しくコピーするには、elements 配列に対して再帰的に clone を呼び出す必要がある。
```java
@Override
public Stack clone() {
    try {
        Stack result = (Stack) super.clone();
        result.elements = elements.clone();
        return result;
    } catch (CloneNotSupportedException e) {
        throw new AssertionError();
    }
}
```

- elements が final である場合は再代入できないため、clone のアーキテクチャは可変オブジェクトを参照する final フィールドとは両立しないことを意味している。
- ハッシュテーブルに対して clone メソッドを作成する場合は、元の buckets 配列をループしながら空ではない各バケットをコピーする。
- clone メソッド生成中の複製先に対して、オーバーライド可能なメソッドを呼び出すべきではない（項目19）。
    - clone メソッドの中で、複製先の状態を確定する前にそのメソッドが実行されると破損の可能性が高まる。
    - Stack の put メソッドは final か private であるべき

## cloneの実装方法について
- public の clone メソッドはthrowsを削除する
- 継承されるようにクラスを設計する場合は、Cloneableを実装すべきではない
    - CloneNotSupportedException をスローすると宣言し、 protectedのクローンメソッドを実装する
    - clone を呼び出すと例外を投げる clone メソッドを実装して、Cloneableをサポートしない

## オブジェクトのコピーを行う代替手段
コピーコンストラクタやコピーファクトリを提供する方法がある。
```java
public Stack(Stack orig) {
        this.size = orig.size;
        this.elements = orig.elements.clone();
    }

    public static Stack newInstance(Stack orig) {
        Stack s = new Stack();
        s.size = orig.size;
        s.elements = orig.elements.clone();
        return s;
    }
```

コピーコンストラクタやコピー static ファクトリーメソッドは Cloneable/clone アーキテクチャよりも多くの長所を持っている。 言語外のオブジェクト生成の仕組みに依存していませんし、final フィールドの使われ方と相反することがない。 また、不要な例外もスローしないし、キャストも必要としない。

# 項目14 Comparableの実装を検討する
アルファベット順、数値順、年代順などの自然な順序を持つ値クラスを書く場合はComparable インターフェースを実装するべき。

compareTo は指定されたオブジェクトと自分自身の順序を比較する。 オブジェクトが自身と比べて、小さい、等しい、大きい、に応じて、負の整数、ゼロ、正の整数を返す。

## compareToメソッドの一般契約
表記 sgn(EXPRESSION) は数学上の符号関数を意味し、負、ゼロ、正のどれであるかに応じて -1, 0, 1 を返す。

- すべての x と y に関して sgn(x.compareTo(y)) == -sgn(y.compareTo(x)) を保証する。これは y.compareTo(x) が例外をスローする場合にのみ、x.compareTo(y) は例外をスローしなければならないことを意味する。
- 0 < x.compareTo(y) かつ 0 < y.compareTo(z) ならば 0 < x.compareTo(z) であることを保証する。（推移性）
- すべての z に関して、x.compareTo(y) == 0 が sgn(x.compareTo(z)) == sgn(y.compareTo(z)) を保証する。
- (x.compareTo(y) == 0) == (x.equals(y)) は強く推奨されるが、厳密には必須ではない。ただし、この条件を破る場合はそのことを明記する。
- もし、指定されたオブジェクトが比較できない場合は ClassCastException をスローする。

## compareToの実装
- equals同様、インスタンス可能なクラスを拡張してcompareToの契約を守ったまま値を追加する方法はない （項目10）。この場合には「ビュー」メソッドを提供するべきである。
- compareToメソッドで関係演算子 < と > を使うのは冗長で間違えやすいので推奨されない。
- 比較は最も意味のあるフィールドから始めて、意味が弱くなる順番に順次比較を行う。※p71参考
- Java8 からComparatorインターフェースはコンパレータ構築メソッドを持ち、こちらを使うと簡潔に実装できる。

```java
return o1.hashCode() - o2.hashCode(); // 整数のオーバーフローとIEEE754浮動小数点算術の副作用の危険性がある

return Integer.compare(o1.hashCode(), o2.hashCode()); // static の compare を使う
```
