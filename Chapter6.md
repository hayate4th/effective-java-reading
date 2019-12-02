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