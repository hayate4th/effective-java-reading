# Enums and Annotations

## Item 34: Use enums instead of int constants

### まえおき
Java に enum 型が追加される前に列挙型を実現するためには、int enum パターンと呼ばれる方法が使われていた。これには様々なデメリットがあり、現在はあまりおすすめできない。int enum パターンと類似した String enum パターンもあるがこれも同様のことが言える。そんなデメリットを持つこのパターンを解消する enum 型について紹介する。


### int enum パターンのデメリット
```Java
// これが int enum パターンである！
public static final int APPLE_FUJI = 0;
public static final int APPLE_PIPPIN = 1;
public static final int APPLE_GRANNY_SMITH = 2;

public static final int ORANGE_NAVEL = 0;
public static final int ORANGE_TEMPLE = 1;
public static final int ORANGE_BLOOD = 2;
```

#### 型安全でない
```Java
// ふじりんごはネーブルオレンジである
APPLE_FUJI == ORANGE_NAVEL
```

#### 名前空間がない
接頭辞を追加して他の int と競合しないようにする。
```Java
// hoge という名前のりんごとオレンジがある
public static final int APPLE_FUJI = 0
public static final int APPLE_HOGE = 1;

public static final int ORANGE_NAVEL = 0
public static final int ORANGE_HOGE = 1;
```

#### バイナリ互換性がない
int enum は定数であるため、一旦コンパイルしたあとに定数の順番が変わったりするとクライアントは再コンパイルしなければきちんとした動作が保証できない。実行できるが、予期せぬ動きをすることが考えられる。

#### 文字列として出力ができない
デバッグしたい時に出力しようとしてもただの int でしかないから、数字しか出力されない。

これを解決するために String enum パターンもあるが前に列挙した問題は解決されない。
しかも文字列をハードコーディングしないといけない。
```Java
public static final String APPLE_GRANNY_SMITH = "APPLE_GRANNY_SMITH";
```

#### enum グループでイテレーションができない
いくら接頭辞がついていても個々は別々の変数なので、同じ接頭辞を持つ変数でイテレーションすることはできない。


### enum 型
```Java
public enum Apple { FUJI, PIPPIN, GRANNY_SMITH }
public enum Orange { NAVEL, TEMPLE, BLOOD }
```
C, C++, C# の列挙型に似ているかもしれないが、Java の enum はそれらより強力である。
以下に何が強力なのかについて紹介する。

#### クラスである
Java の enum 型は public static final フィールドを通してそれぞれ列挙された定数インスタンスにアクセスできるクラスである。
また、enum 型はコンストラクタにアクセスできないため、クライアントは enum 型の extend やインスタンスの生成が許されない (Item 3: シングルトン化されている)。

#### コンパイル時型安全であり、名前空間を持つ、バイナリ互換性をもっていて、しかも文字列出力ができる
1. コンパイル時に型を確認してくれる。
```Java
// int enum パターン
APPLE_FUJI == ORANGE_NAVEL // コンパイル OK

// enum 型
Apple.FUJI == Orange.NAVEL // コンパイルエラー
```

2. それぞれの enum グループで名前空間を持つ。
```Java
public enum Apple { FUJI, HOGE };
public enum Orange { NAVEL, HOGE };
```

3. グループに新しく定数を追加しても順番を入れ替えてもバイナリ互換性が保てる。

4. 文字列出力ができる。
```Java
// 定義されたまま文字列出力する
Apple.FUJI.name(); // "FUJI"

// toString() をオーバーライドしてみやすい文字列として出力できる
Apple.FUJI.toString(); // "Fuji" 
```

#### フィールド、メソッドをもてて、インタフェースの実装もできる
enum 型は Object (3章) の全てのメソッドを実装していて、Comparable (Item 14) も Serializable (12章) も実装している。
enum 型の実装の例をみて説明をする。

```Java
public enum Planet {
    MERCURY(3.302e+23, 2.439e6),
    VENUS(4.869e+24, 6.052e6),
    EARTH(5.975e+24, 6.378e6),
    MARS(6.419e+23, 3.393e6),
    JUPITER(1.899e+27, 7.149e7),
    SATURN(5.685e+26, 6.027e7),
    URANUS(8.683e+25, 2.556e7),
    NEPTUNE(1.024e+26, 2.477e7);

    // enum は一般的に不変 (シングルトン) のため final である
    private final double mass;
    private final double radius;
    private final double surfaceGravity;

    // 万有引力定数
    private static final double G = 6.67300E-11;

    Planet(double mass, double radius) {
        this.mass = mass;
        this.radius = radius;
        surfaceGravity = G * mass / (radius * radius); // 万有引力の法則
    }

    public double mass() { return mass; }
    public double radius() { return radius; }
    public double suraceGravity() { return surfaceGravity; }

    public double surfaceWeight(double mass) {
        return mass * surfaceGravity; // F = ma (ニュートンの運動方程式)
    }
}
```

```Java
double earthWeight = 1.0;
double mass = earthWeight / Planet.EARTH.surfaceGravity();

// イテレーションは values() でできる
for (Planet p : Planet.values())
    System.out.printf("Your weight on %s os %f%n", p, p.surfaceWeight(mass));
```



#### enum 型の定数に固有のメソッドができる
```Java
// enum 型 Operation に対してメソッド apply を実装する
// 気持ちなんかイケてない
public enum Operation {
    PLUS, MINUS, TIMES, DIVIDE;

    public double apply(double x, double y) {
        switch(this) {
            case PLUS: return x + y;
            case MINUS: return x - y;
            case TIMES: return x * y;
            case DIVIDE: return x / y;
        }
        // enum グループにないものは AssertionError (解釈: ここら辺が多分イケてない)
        throw new AssertionError("Unknown op: " + this);
    }
}
```

```Java
// 引用: 列挙しているところに書くから、新しい演算子を Operation に足しても
// apply メソッドをオーバーライドし忘れることはないと思われる (本当か？)
public enum Operation {
    PLUS { public double apply(double x, double y) { return x + y; }},
    MINUS { public double apply(double x, double y) { return x - y; }},
    TIMES { public double apply(double x, double y) { return x * y; }},
    DIVIDE { public double apply(double x, double y) { return x / y; }};

    public abstract double apply(double x, double y);
}
```

## Item 37: Use EnumMap instead of ordinal indexing

### 序数インデックスはダメ
配列のインデックスに oridinal() を使ってはいけない

```Java
class Plant {
    emun LifeCycle { ANNUAL, PERENNIAL, BIENNIAL }

    final String name;
    final LifeCycle lifeCycle;

    Plan(String name, LifeCycle lifeCycle) {
        this.name = name;
        this.lifeCycle = lifeCycle;
    }

    @Override public String toString() {
        return name;
    }
}

Set<Plant>[] plantsByLifeCycle = (Set<Plant>[]) new Set[Plant.LifeCycle.values().length];
for (int i = 0;i < plantsByLifeCycle.length; i ++)
    plantsByLifeCycle[i] = new HashSet<>();

// これはダメゼッタイ！
for (Plant p : garden)
    plantsByLifeCycle[p.lifeCycle.ordinal()].add(p);

for (int i = 0;i < plantsByLifeCycle.length;i ++) {
    System.out.printf("%s: %s%n", Plant.LifeCycle.values()[i], plantsByLifeCycle[i]);
}
```

1. 配列はジェネリクスと互換性がない (Item 28)
  - 無検査キャストの警告がでる
2. Enum の ordinal() を使ってるため型安全ではない
  - どのインデックスが何に該当するのかは実装者が把握していないといけない
  - 間違えたインデックスを与えると間違えた挙動をする
  - ラッキーなら ArrayIndexOutOfBoundsException が投げられる

### EnumMap を使おう
配列を使うこと自体が構造的に間違っている、マップを使おう
Java に　EnumMap という Enum をキー値とする Map がある、すごい

```Java
// Item 33 境界型型トークンが必要
Map<Plant.LifeCycle, Set<Plant>> plantsByLifeCycle = new EnumMap<>(Plant.LifeCycle.class);

for (Plant.LifeCycle lc : Plant.LifeCycle.values())
    plantsByLifeCycle.put(lc, new HashSet<>());

for (Plant p : garden)
    plantsByLifeCycle.get(p.lifeCycle).add(p)

System.out.println(plantsByLifeCycle);
```

### ストリームとの併用
ストリームの説明が終わってからみよう

### 多次元的な表現
```Java
public enum Phase {
    SOLID, LIQUID, GAS;

    public enum Transition {
        MELT, FREEZE, BOIL, CONDENSE, SUBLIME, DEPOSIT;

        private static final Transition[][] TRANSITIONS = {
            { null, MELT, SUBLIME },
            { FREEZE, null, BOIL },
            { DEPOSIT, CONDENSE, null }
        };

        public static Transition from(Phase from, Phase to) {
            return TRANSITIONS[from.ordinal()][to.ordinal()];
        }
    }
}
```

1. 定数の追加・削除の時の変更を忘れランタイムエラーになりやすい
2. null が増えると無駄が増える
3. Phase の 2乗の大きさになる

避けるために EnumMap をネストする
```Java
public enum Phase {
    SOLID, LIQUID, GAS, PLASMA; // PLASMA を新規追加してみる
    public enum Transition {
        MELT(SOLID, LIQUID), FREEZE(LIQUID, SOLID),
        BOIL(LIQUID, GAS), CONDENSE(GAS, LIQUID),
        SUBLIME(SOLID, GAS), DEPOSIT(GAS, SOLID),
        IONIZE(GAS, PLASMA), DEIONIZE(PLASMA, GAS); // これ追加するだけ


        private final Phase from;
        private final Phase to;

        public Transition(Phase from, Phase to) {
            this.from = from;
            this.to = to;
        }

        // 初期化
        private static final Map<Phase, Map<Phase, Transition>> m =
            Stream.of(values()).collect(groupingBy(t -> t.from, () -> new EnumMap<>(Phase.class), toMap(t -> t.to, t -> t, (x, y) -> y, () -> new EnumMap<>(Phase.class))));

        public static Transition from(Phase from, Phase to) {
            return m.get(from).get(to);
        }
    }
}
```

ordinal() を使っていいケースなんてほとんどない、基本的に EnumMap を使おう。

## Item 38: Emulate extensible enums with interfaces

Enum は拡張できないし、拡張するべきではない！
ただオペレーションコード（オペコード）の場合は拡張できる Enum が欲しい、
固有の操作をユーザ自身が実装する形

Enum がインタフェースを実装できるという点を利用して擬似的な拡張をする

```Java

public interface Operation {
    double apply(double x, double y);
}

public enum BasicOperation implements Operation {
    PLUS("+") {
        public double apply(double x, double y) { return x + y; }
    },
    MINUS("-") {
        public double apply(double x, double y) { return x + y; }
    },
    TIMES("*") {
        public double apply(double x, double y) { return x + y; }
    },
    DIVIDE("/") {
        public double apply(double x, double y) { return x + y; }
    };

    private final String symbol;

    BasicOperation(String symbol) {
        this.symbol = symbol;
    }

    @Override public String toString() {
        return symbol;
    }
}
```

BasicOperation は Enum のため拡張できないが、Operation は可能であるためそれで拡張する


```Java

public enum ExtendedOperation implements Operation {
    EXP("^") { public double apply(double x, double y) { return Math.pow(x, y);}},
    REMAINDER("%") { public double apply(double x, double y) { return x % y;}};

    private final String symbol;

    ExtendedOperation(String symbol) {
        this.symbol = symbol;
    }

    @Override public String toString() {
        return symbol;
    }
}
```

一部共通的なコードが重複してしまうが、それを避けたければヘルパーメソッドを実装する。

### 擬似拡張した Enum の列挙
```Java
private static <T extends Enum<T> & Operation> void test(Class<T> opEnumType, double x, double y) {
    for (Operation op : opEnumType.getEnumConstants())
        System.out.printf("%f %s %f = %f%n", x, op, y, op.apply(x, y));
}
```

Enum であり Operation を継承している Class<T>


## Item 39: Prefer annotations to naming patterns


### 命名パターン
「テストケースメソッドの先頭には必ず test を書いてね！」とか
1. test って書いてなくてもコンパイルできちゃう
  - テストケースとして認識されない
2. 利用箇所に制限がかけられない
  - static メソッドだけとか、クラスにだけとか
3. 引数との関連付けが困難
  - ある例外を投げるテストケースにだけとか
  - 命名パターンだけじゃ関連づけることはできない

### アノテーション
ソースコードにメタデータを付与する仕組み
コンパイラや仮想マシンがその情報を使ったりする

Java の標準アノテーションもあるし、新しくアノテーションを作ることもできる

```Java
import java.lang.annotation.*;

// 自作アノテーション @Test
@Retention(RetentionPolicy.RUNTIME) // メタアノテーション
@Target(ElementType.METHOD) // メタアノテーション (引数なし static メソッドにしか付与できないよ！)
public @interface Test {
}
```
メタアノテーションでアノテーションに制限をかける

```Java
public class Sample {
    @Test public static void m1() {}
    public static void m2() {}
    @Test public static void m3() {
        throw new RuntimeException("Boom");
    }
    public static void m4() {}
    @Test public void m5() {}  // static じゃないから @Test は使えないよ！
    ...
}
```

### どうやってアノテーションを確認してるか
```Java
import java.lang.reflect.*;

public class RunTests {
    public static void main(String[] args) throws Exception {
        int tests = 0;
        int passed = 0;
        Class<?> testClass = Class.forName(args[0]);
        for (Method m : testClass.getDeclaredMethods()) {
            // Test のアノテーションが付与されているかをチェック
            if (m.isAnnotationPresent(Test.class)) {
                tests++;
                try {
                    m.invoke(null);
                    passed++;
                } catch (InvocationTargetException wrappedExc) {
                    Throwable exc = wrapped.getCause();
                    System.out.println(m + " failed: " + exc);
                } catch (Exception exc) {
                    System.out.println("INVALID @Test: " + m);
                }
            }
        }
        System.out.printf("Passed: %d, Failed: %d%n", passed, tests - passed);
    }
}
```

### 値を付与するアノテーション
```Java
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME) // メタアノテーション
@Target(ElementType.METHOD) // メタアノテーション (引数なし static メソッドにしか付与できないよ！)
public @interface ExceptionTest {
    Class<? extends Throwable> value();
}
```

```Java
public class Sample2 {
    @ExceptionTest(ArithmeticException.class)
    public static void m1() {
        int i = 0;
        i = i / i;
    }
}
```

複数引数を与えるパターン
```Java
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME) // メタアノテーション
@Target(ElementType.METHOD) // メタアノテーション (引数なし static メソッドにしか付与できないよ！)
public @interface ExceptionTest {
    Class<? extends Throwable>[] value();
}
```

## Item 40: Consistently use the Override annotation

@Override アノテーションは常に使おう

### オーバーライドアノテーション
スーパークラスのメソッドをオーバーライドしていることを示している

Override アノテーションを使うと、オーバーライドした際の引数エラーなどでコンパイル時に検査してくれる

抽象クラスの抽象メソッドをオーバーライドする場合や、インタフェースの宣言を実装する場合には @Override をつける必要はない
つけても問題はないのでつけよう

## Item 41: Use marker interfaces to define types

### マーカーインタフェースとは
メソッド宣言を一つももたないインタフェース
- Serializable とか (ObjectOutputStream)

ObjectOutputStream.write(Object o) に Serializable を実装していないインスタンスを渡すと例外が発生します。 本来、write の宣言は Object ではなく Serializable を引数にとるべきでした。 そうすれば、実行時の例外ではなくコンパイル時のエラーとして間違いを見つけることが出来ます。

### マーカーアノテーションとは
一方、マーカーアノテーションはその名の通り、何らかの特性を示すためのアノテーションです。

アノテーション自体がメタデータを示すものなので、大抵、アノテーションはマーカーアノテーションとしての役割を果たします。


### どっちがよい？
マーカーインタフェースが利用できるならばマーカーアノテーションよりもマーカーインタフェースを使うべきです。

マーカーインタフェースは型を定義できるため、コンパイル時にその特性をチェックすることができます。 マーカーアノテーションは一般的に実行時までその特性をチェックすることはできません。 （コンパイラが対応している @Override アノテーションなどは特別です）

特に、このマーカーを付与したクラスをメソッドの引数に使いたい場合は、マーカーインタフェースを用いるべきです。

ただし、マーカーインタフェースはクラス以外をマークすることが出来ません。 メソッドやフィールドをマークしたい場合はマークアのテーションを使う必要があります。

マーカーアノテーションにデフォルト値をもたせて置けば、あとから値を追加することができます。 しかし、マーカーインタフェースにあとからメソッドを追加することは一般的にはできません。 コンパイル互換性を破壊するからです。