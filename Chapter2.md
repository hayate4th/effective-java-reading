# Creating and Destroying Objects

## Item 1: Consider static factory methods instead of constructors

### まえおき
クラスは、自身のインスタンスを戻り値として返す static factory メソッドを提供することによって public なコンストラクタ以外でもインスタンスの生成ができるようになる。この節ではこのメソッドを public なコンストラクタの代わりとして提供することよる長所や短所について紹介する。
```java
// boolean 値を受け取って Boolean オブジェクトを返す
public static Boolean valueOf(boolean b) {
  return b ? Boolean.TRUE : Boolean.FALSE;
}
```
**※ static factory メソッドは GoF の Factory メソッドパターンと異なる**

### static factory メソッドの長所
#### 1. 名前がついている
#### 2. 呼び出されるたびにインスタンスを生成する必要がない
#### 3. あらゆるサブクラスを返せる
#### 4. 引数によって返すインスタンスを変えられる
#### 5. メソッドを含むクラスを書いた段階で戻り値のクラスが存在する必要がない
```java
// インタフェースを戻り値として持つ (コンパイルは通る)
public static MyInterface getMyInterfaceInstance() {
  // ランタイムでインスタンスを生成 (読み込む)
}
```

### static factory メソッドの短所
#### 1. public/protected なコンストラクタを持たないクラスはサブクラス化できない
#### 2. 探しにくい, 見つけにくい


## Item 2: Consider a builder when faced with many constructor parameters

### まえおき
static factory メソッドやコンストラクタは多数のオプショナル引数に対してうまくスケーリングができない。この節では telescoping constructor パターンと JavaBeans パターンと比較しながら、両方の長所を持つ Builder パターンについて紹介する。

### telescoping constructor パターン
```java
public class NutritionFacts {
  private final int servingSize;   // required
  private final int servings;      // required
  private final int calories;      // optional
  private final int fat;           // optional
  private final int sodium;        // optional 
  private final int carbohydrate;  // optional

  public NutritionFacts(int servingSize, int servings) {
    this(servingSize, servings, 0);
  }

  public NutritionFacts(int servingSize, int servings, int calories) {
    this(servingSize, servings, calories, 0);
  }

  public NutritionFacts(int servingSize, int servings, int calories, int fat) {
    this(servingSize, servings, calories, fat, 0);
  }

  public NutritionFacts(int servingSize, int servings, int calories, int fat, int sodium) {
    this(servingSize, servings, calories, fat, sodium, 0);
  }

  public NutritionFacts(int servingSize, int servings, int calories, int fat, int sodium, int carbohydrate) {
    this.servingSize = servingSize;
    this.servings = servings;
    this.calories = calories;
    this.fat = fat;
    this.sodium = sodium;
    this.carbohydrate = carbohydrate;
  }
}
```
```java
// 使用例
NutritionFacts cocaCola = new NutritionFacts(240, 8, 0, 35, 27);
```
- オプショナル引数の数だけコンストラクタを用意する
- 与えたい引数を持つオプショナル引数の数が最小のコンストラクタでインスタンスを生成する
  - fat の値のみを渡したい場合は上から 2 番目のコンストラクタ
- 引数に渡す必要のないものは結局 0 のような値を渡さないといけない
- **オプショナル引数が多いと書きにくくなる**
- **何番目の引数が何の値なのかわからない**

### JavaBeans パターン
```java
public class NutritionFacts {
  private int servingSize = -1; // required
  private int servings = -1; // required
  private int calories = 0;
  private int fat = 0;
  private int sodium = 0;
  private int carbohydrate = 0;

  public NutritionFacts() { }

  public void setServingSize(int val) { servingSize = val; }
  public void setServings(int val) { servings = val; }
  public void setCalories(int val) { calories = val; }
  public void setFat(int val) { fat = val; }
  public void setSodium(int val) { sodium = val; }
  public void setCarbohydrate(int val) { carbohydrate = val; }
}
```
```java
// 使用例
NutritionFacts cocaCola = new NutritionFacts();
cocaCola.setServingSize(240);
cocaCola.setServings(8);
cocaCola.setCalories(100);
cocaCola.setSodium(35);
cocaCola.setCarbohydrate(27);
```
- **整合性が保てない**
  - 値がきちんとセットされていない状態でインスタンスが生成される
- **immutable (Item 17) にできない**
  - final じゃない (セッターから値が変えられる)
  - スレッドセーフにするために余計な手間が発生する


### Builder パターン
```java
public class NutritionFacts {
  private final int servingSize;
  private final int servings;
  private final int calories;
  private final int fat;
  private final int sodium;
  private final int carbohydrate;

  public static class Builder {
    // required
    private final int servingSize;
    private final int servings;

    // optional
    private int calories = 0;
    private int fat = 0;
    private int sodium = 0;
    private int carbohydrate = 0;

    public Builder(int servingSize, int servings) {
      this.servingSize = servingSize;
      this.servings = servings;
    }

    public Builder calories(int val) {
      calories = val;
      return this;
    }

    public Builder fat(int val) {
      fat = val;
      return this;
    }

    public Builder sodium(int val) {
      sodium = val;
      return this;
    }

    public Builder carbohydrate(int val) {
      carbohydrate = val;
      return this;
    }

    public NutritionFacts build() {
      return new NutritionFacts(this);
    }
  }

  private NutritionFacts(Builder builder) {
    servingSize = builder.servingSize;
    servings = builder.servings;
    calories = builder.alories;
    fat = builder.fat;
    sodium = builder.sodium;
    carbohydrate = builder.carbohydrate;
  }
}
```
```java
NutritionFacts cocaCola = new NutritionFacts.Builder(240, 8)
                            .calories(100)
                            .sodium(35)
                            .carbohydrate(27)
                            .build();
```
- **書くのも読むのも容易**
  - telescoping constructor パターンの欠点改善
- オプショナル引数の全ての組み合わせでインスタンスが生成できる
- 引数のバリデーションが可能
- たくさんの引数を持つ時に向いてる
- 階層構造を持つクラスでも使える

```java
public abstract class Pizza {
  public enum Topping { HAM, MUSHROOM, ONION, PEPPER, SAUSAGE }
  final Set<Topping> toppings;

  abstract static class Builder<T extends Builder<T>> {
    EnumSet<Topping> toppings = EnumSet.noneOf(Topping.class);
    public T addTopping(Topping topping) {
      toppings.add(Objects.requireNonNull(topping));
      return self();
    }

    abstract Pizza build();

    protected abstract T self();
  }

  Pizza(Builder<?> builder) {
    toppings = builder.toppings.clone(); // Item 50
  }
}
```
```java
public class NyPizza extends Pizza {
  public enum Size { SMALL, MEDIUM, LARGE }
  private final Size size;

  public static class Builder extends Pizza.Builder<Builder> {
    private final Size size;

    public Builder(Size size) {
      this.size = Objects.requireNonNull(size);
    }

    @Override public NyPizza build() {
      return new NyPizza(this);
    }

    @Override protected Builder self() { return this; }
  }

  private NyPizza(Builder builder) {
    super(builder);
    size = builder.size;
  }
}
```
```java
public class Calzone extends Pizza {
  private final boolean sauceInside;

  public static class Builder extends Pizza.Builder<Builder> {
    private boolean sauceInside = false;

    public Builder sauceInside() {
      sauceInside = true;
      return this;
    }

    @Override public Calzone build() {
      return new Calzone(this);
    }

    @Override protected Builder self() { return this; }
  }

  private Calzone(Builder builder) {
    super(builder);
    sauceInside = builder.sauceInside;
  }
}
```
```java
// 使用例
NyPizza pizza = new NyPizza.Builder(SMALL)
                  .addTopping(SAUSAGE)
                  .addTopping(ONION)
                  .build();
Calzone calzone = new Calzone.Builder()
                  .addTopping(HAM)
                  .sauceInside()
                  .build();
```

## Item 3: Enforce the singleton property with a private constructor or an enum type

### まえおき
ただ一度だけのインスタンス生成が保証される singleton は**モックにすることが不可能なためテストが困難である**という特徴を持つ。その singleton を実現するための方法について紹介する。

### public static final フィールドを提供する
```java
public class Elvis {
  public static final Elvis INSTANCE = new Elvis();
  private Elvis() { ... }
  
  public void leaveTheBuilding() { ... }
}
```
- private コンストラクタは一度だけしか呼ばれない
- ただし、リフレクションで呼び出して AccessibleObject.setAccessible メソッドでアクセス修飾子を緩めることができる
  - 二つめのインスタンスを生成する時に例外を投げることで対処
- クラスが singleton であることが明確にわかる API
  - public static フィールドが final である
- 単純

### static factory メソッドを提供する
```java
public class Elvis {
  private static final Elvis INSTANCE = new Elvis();
  private Elvis() { ... }
  public static Elvis getInstance() { return INSTANCE; }

  public void leaveTheBuilding() { ... }
}
```
- Elvis.getInstance() は必ず同じインスタンスの参照を返す
- static factory メソッドの処理を変えるだけで singleton じゃなくすることもできる柔軟性を持つ
- generic singleton factory (Item 30) を書ける
- method reference を supplier として使える
- serializable (Chapter 12) 可能にしたい場合、ただ implements Serializable すればいいという話ではない
  - 全てのフィールドを transient 宣言しシリアライズの対象外とする
  - readResolve メソッド (Item 89) で元のインスタンスを返すようにする
  - デシリアライズするたびに新しいインスタンスが生成されて singleton が保証されない


### 単一要素を持つ enum で宣言する
```java
public enum Elvis {
  INSTANCE;

  public void leaveTheBuilding() { ... }
}
```
- singleton が保証される
  - リフレクションやシリアライズによる攻撃の心配もない
- Enum 以外のスーパクラスを継承している場合はダメ
- **一番いいのでは？**


## Item 4: Enforce noninstantiability with a private constructor

### まえおき
インスタンス生成を想定しないユーティリティクラスなどは**抽象化するだけではインスタンス生成は防げない**。これは結局、抽象クラスを継承したサブクラスからインスタンスを生成することができしまう。この節ではこのようなクラスのインスタンス生成を防ぐための方法を紹介する。

### private コンストラクタを宣言する
```java
public class UtilityClass {
  private UtilityClass() {
    throw new AssertionError();
  }
}
```
- **private コンストラクタを宣言する**
  - コンストラクタを宣言しないと、デフォルトコンストラクタが呼ばれてしまう
- 例外を投げることによって、間違えてクラス内部でコンストラクタを読んだ時にも対処


## Item 5: Prefer dependency injection to hardwiring resources

### まえおき
クラスは複数の資源 (クラス) に依存していることが多い。これらの資源をクラスに直接持たせることは柔軟性に欠けてテストが困難になる。この節では dependency injection (依存性注入) による依存資源の提供方法について紹介する。

### 柔軟性のない実装
```java
// static ユーティリティクラス (Item 4)
public class SpellChecker {
  private static final Lexicon dictionary = ...;
  
  private SpellChecker() {}

  public static boolean isValid(String word) { ... }
  public static List<String> suggestions(String typo) { ... }
}
```
```java
// singleton (Item 3)
public class SpellChecker {
  private final Lexicon dictionary = ...;

  private SpellChecker(...) {}
  public static INSTANCE = new SpellChecker(...);

  public boolean isValid(String word) { ... }
  public List<String> suggestions(String typo) { ... }
}
```
- **SpellChecker を実現するための辞書が一つしか存在しなく、柔軟性がない**
  - 辞書は英語や日本語のように複数考えられる
  - 日本語辞書を持っている時は英語の SpellCheck はできない (テストも落ちる)

### dependency injection (依存性注入) による実装
```java
public class SpellChecker {
  private final Lexicon dictionary;

  public SpellChecker(Lexicon dictionary) {
    this.dictionary = Objects.requireNonNull(dictionary);
  }

  public boolean isValid(String word) { ... }
  public List<String> suggestions(String typo) { ... }
}
```
- インスタンスを生成する時に依存資源を渡す
- 知らずにみんなやってるけど、これを依存性注入って言うんだよ！
- 依存する資源を共有できる
- 大きなプロジェクトだとごちゃごちゃしちゃう
  - 多数の資源が依存し合ってる
  - 依存性注入フレームワーク (Dagger, Guice, Spring) を使うと解決できる


## Item 6: Avoid creating unnecessary objects

### 不必要なインスタンスは生成しない
```java
// ダメな例
String s = new String("bikini"); // DON'T DO THIS!
```
```java
// いい例
String s = "bikini";
```
- ループとかで回した時、上は毎回インスタンスを生成する
  - 下は単一の String インスタンスを参照する

```java
static boolean isRomanNumeral(String s) {
  return s.matches("^(?=.)M*(C[MD]|D?C{0,3})" + "(X[CL]|L?X{0,3})(I[XV]|V?I{0,3})$");
}
```
```java
public class RomanNumerals {
  private static final Pattern ROMAN = Pattern.compile("^(?=.)M*(C[MD]|D?C{0,3})" + "(X[CL]|L?X{0,3})(I[XV]|V?I{0,3})$");

  static boolean isRomanNumeral(String s) {
    return ROMAN.matcher(s).matches();
  }
}
```
- 上の書き方だと内部で Pattern インスタンスを一度生成して、GC にすぐ回収される
  - 何度も同じ条件でやる場合は勿体無い
- 下の場合だと isRomanNumeral が使われなかったら Pattern インスタンスは無駄になる
  - 生成した Pattern インスタンスをキャッシュする
  - lazily initializing (Item 83) で isRomanNumeral が初めて呼ばれた時にインスタンスを生成する

```java
Long hoge = 0L; // 自動ボクシング (型変換)
long fuga = hoge; // 自動アンボクシング (型変換)
```
```java
private static long sum() {
  Long sum = 0L; // ココ！
  for (long i = 0; i <= Integer.MAX_VALUE; i++)
    sum += i; // 自動ボクシングが起きる
  
  return sum;
}
```
- 2の31乗個の無駄なインスタンスが生成される
  - Long → long にする
- 基本データ型を使って自動ボクシングには気をつける

## Item 7: Eliminate obsolete object references

### 廃れた参照は消そう
```java
public class Stack {
  private Object[] elements;
  private int size = 0;
  private static final int DEFAULT_INITIAL_CAPACITY = 16;

  public Stack() {
    elements = new Object[DEFAULT_INITIAL_CAPACITY];
  }

  public void push(Object e) {
    ensureCapacity();
    elements[size++] = e;
  }

  public Object pop() {
    if (size == 0)
      throw new EmptyStackException();
    return elements[--size]; // ココ！
  }

  private void ensureCapacity() {
    if (elements.length == size)
      elements = Arrays.copyOf(elements, 2 * size + 1);
  }
}
```
- pop した時に elements[size] の参照が返ってくるが、elements 自身がelements[size] の参照を持ったまま
  - インスタンスが残ってしまって GC に回収されずメモリリークとなる

```java
public Object pop() {
  if (size == 0)
    throw new EmptyStackException();
  Object result = elements[--size];
  elements[size] = null; // 廃れた参照を消す
  return result;
}
```
- null を設定して参照を外す
  - 間違って elements[size] を参照した時についでに NullPointerException を返してくれるようになる
- **ただし、オブジェクトに null を設定するのは異例で通常はやるべきでない**
  - クラス自身がメモリを管理する機構を持つ時
  - クラスがキャッシュ機構を持つ時
    - キャッシュされていることが忘れがちのため、弱い参照を持つ WeakHashMap で持つ
    - ついでに利用されなくなったキャッシュを破棄する機構も入れる
  - リスナーやコールバックを持つ時
    - これも存在が忘れがちだから WeakHashMap で持つ
- 普通はスコープから外れたら参照が外れる
  - 可能な限り最小限のスコープ内で宣言する


## Item 8: Avoid finalizers and cleaners

### まえおき
インスタンスが破棄されたり、参照が外されるタイミングで実行される finalizer と cleaner はデメリットが多すぎるため使わないことを推奨する。この節ではこれらのデメリットと代替案を紹介する。  
※ Java9 では finalizer は deprecated だがライブラリでまだ使われている

### finalizer と cleaner のデメリット
#### いつ呼ばれるかがわからなし、必ず呼ばれるとも限らない
- finalize() や clean() は GC が行われる時に呼ばれる
  - そもそもいつ GC が行われるのかわからない
  - GC のタイミングも制御できない
  - GC される前に終了したらそもそも呼ばれない
    - finalize() や clean() で永続化に関わる処理があっても、何も起きない
```java
public class Teenager {
  public static void main(String[] args) {
    new Room(99); // Room は GC に回収される時に "Cleaning room." が出力される
    System.out.println("Peace out!");
  }
}
```
- Peace out! のあとに Cleaning room. が出力されて欲しかった

#### ひどくパフォーマンスが落ちる
- finalizer や cleaner は GC をとてつもなく非効率化する

#### ファイナライザには脆弱性がある
```java
public class Zombie {
  static Zombie zombie;

  public void finalize() {
    zombie = this;
  }
}
```
- zombie は GC で回収されるはずが、finalize() で復活する
- 何もしない final finalize() を作ったり、final クラスを作って対処する
  - final クラスは継承できない
  - final finalize() はオーバーライドできない

### 代替案と許容されるケース
#### AutoCloseable インタフェース implement する
```java
public class Room implements AutoCloseable {
  private static final Cleaner cleaner = Cleaner.create();

  private static class State implements Runnable {
    int numJunkPiles;

    State(int numJunkPiles) {
      this.numJunkPiles = numJunkPiles;
    }

    @Override public void run() {
      System.out.println("Cleaning room");
      numJunkPiles = 0;
    }
  }

  private final State state;

  // Room インスタンスが GC に回収される時に clean() する
  private final Cleaner.Cleanable cleanable;

  public Room(int numJunkPiles) {
    state = new State(numJunkPiles);
    cleanable = cleaner.register(this, state);
  }

  @Override public void close() {
    cleanable.clean();
  }
}
```
- close() をオーバーライドする
  - インスタインスが必要なくなったら close() を呼びだす
- clean() は close() し忘れた時の安全網として使う

#### GC が管理してないネイティブオブジェクトに対して使う
- GC で管理してるインスタンスが破棄される時に clean() でネイティブオブジェクトを一緒に破棄する


## Item 9: Prefer *try*-with-resources to *try*-*finally*

### まえおき