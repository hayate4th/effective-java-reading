# ラムダとストリーム

## 項目42 無名クラスよりもラムダを選ぶ
単一の抽象メソッドを持つインターフェースは関数型として使われてきた。
それらのインスタンスである関数オブジェクトを作成する手段は無名クラスだった。

#### 無名クラスの例
```java
interface Echo {
    void hoge();
}

public class Main {
    public static void main(String[] args) throws Exception {
        // インターフェースを実装した新しいクラスのインスタンスを作成している。
        Echo echo = new Echo() { 
            @Override
            public void hoge() {
                System.out.println("hoge");   
            }
        };
        echo.hoge();
    }
}
```

#### 長さ順に文字列のリストをソートするコードの例（無名クラス）
```java
Collections.sort(words, new Comparator<String>() {
    @Override
    public int compare(String s1, String s2) {
        return Integer.compare(s1.length(), s2.length());
    }
});
```

無名クラスは冗長である。
Java8では単一の抽象メソッドを持つインターフェースを関数型インターフェースとし、ラムダ式を使ってこれらのインタフェースをインスタンス化できるようにした。

#### 長さ順に文字列のリストをソートするコードの例（ラムダ式）
```java
Collections.sort(words, (s1, s2) -> Integer.compare(s1.length(), s2.length()));
```

ラムダ式では型推論が使える。

以下の場合は型を明示的に書く
- 型を明示してプログラムが読みやすくなるとき
- コンパイラが型を推論できないとき

ちなみに、上記コード内のコンパレータは、ラムダの代わりに`コンパレータ構築メソッド`を使うとより短く書くことができる。

```java
Collections.sort(words, conparetingInt(String::length));
```

Listインターフェースに追加されたsortメソッドを使用するとさらに短くかける

```java
words.sort(conparetingInt(String::length));
```

言語へのラムダの追加は、以前は意味がなかった場所で関数オブジェクトを使うことを実用的にしている。例えば項目34の Operation の enum をラムダに書き換えてみる。

#### 関数オブジェクトフィールドと定数固有の振る舞いをもつenum型
``` java
public enum Operation {
    PLUS("+", (x, y) -> x + y),
    MINUS("-", (x, y) -> x - y),
    TIMES("*", (x, y) -> x * y),
    DIVIDE("/", (x, y) -> x / y);

    private final String symbol;
    private final String DoubleBinaryOperator op;
    Operation(String symbol, DoubleBinaryOperator op) { 
        this.symbol = symbol;
        this.op = op;
    }

    @Override public String toString() { return symbol; }

    public double apply(double x, double y) {
        return op.appluAsDouble(x, y);
    }
```

DoubleBinaryOperatorインタフェースは java.util.function にある関数型インターフェースの１つ。2つの double型 の引数を受け取り、 double型 の結果を返す関数を表している。

すべての定数固有メソッド本体をラムダに書き換えることができるわけではない。なぜなら`ラムダは名前とドキュメンテーションが欠けている`からだ。以下の場合は定数固有クラスを使う。

- 計算のコードが自明でないとき
- 計算のコードが3行を越えるとき
- インスタンスフィールドやインスタンスメソッドへアクセスする必要があるとき

※ enum のコンストラクタに渡される引数は static の文脈で評価されるので、enum のインスタンスメンバにアクセスできない。

関数型インタフェースではない型のインスタンスを作成する必要があるときだけ、関数オブジェクトとして無名クラスを使う。

### 無名クラスでのみできること
- 抽象クラスのインスタンスを作成する
- 複数の抽象メソッドを持つインタフェースのインスタンスを作成する

### 無名クラスとラムダの違い
- 無名クラスの this → 無名クラスのインスタンスを参照する
- ラムダの this → エンクロージングインスタンスを参照する([エンクロージングインスタンスとは](http://www.kab-studio.biz/Programing/JavaA2Z/Word/00000994.html))

### ラムダをシリアライズすべきではない
ラムダと無名クラスはシリアライズ/デシリアライズを行えない特性を持っている。Comparator などのシリアライズしたい関数オブジェクトを持っているなら static クラスのインスタンスを使う(項目25)。

## 項目43 ラムダよりもメソッド参照を選ぶ
ラムダよりも簡潔な関数オブジェクトを生成する方法がメソッド参照である。

任意のキーから Integer値 のマップを持つプログラムを考える。キーがマップになければ数値1をキーに関連付けし、キーが既に存在していれば関連づけされた値を1つ増加させる

``` java
map.merge(key, 1, (count, incr) -> count + incr);
```

これをメソッド参照で書き換えると以下のようになる
``` java
map.merge(key, 1, Integer::sum);
```

基本的にはIDEの提案を受け入れてメソッド参照を使うべきだが、ラムダの方がコードが見やすいときもある。たとえば GoshThisClassNameIsHumangous というクラスの内部にある次のコードを考える。

```java
service.execute(GoshThisClassNameIsHumangous::action);
```

ラムダで同じコードは以下のようになる
```java
service.execute(() -> action());
```

#### メソッド参照の種類 P200参照
- static: 基本的に static のメソッドを参照する
- バウンド: メソッドが働きかける先のオブジェクトはメソッド参照の中で指定される
- アンバウンド: メソッド宣言されたパラメータの前に追加パラメータとしてオブジェクトを指定する。
- コンストラクタ: ファクトリオブジェクトとしての役割を果たす


## 項目44 標準の関数型インタフェースを使う
新たなキーがマップに追加されるたびに呼び出される protected の LinkedHashMap#removeElderEntry メソッドについて考える。
```java
protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    return size() > 100;
}
```

関数オブジェクトの場合はマップを補足できないので size() を呼び出すことができない。そのため引数としてマップを受け取る関数型インタフェースを考える。


```java
@FunctionalInterface interface EldestEntryRemoveFunction<K, V> {
    boolean remove(Map<K, V> map, Map.Entry<K, V> eldest);
}
```

このインタフェースはうまく動作するが、使用するべきではない。java.util.function はみなさんが使うための標準の関数型インタフェースを多く提供しており、`一般的にその標準の関数型インタフェースを使用するべきである`。6個の基本な関数型インタフェースについて考える。P202の表を参照。

6個の基本インタフェースに3つの変形があり、それぞれint, long, doubleに対して操作する。
それらのインタフェースの名前は基本インタフェースから導出され、先頭に基本データの名前がついている。Function の変形は戻り値型でパラメータ化されている。

例：
- int値を受け取りint値を返す→IntPredicate
- 2つのlong値を受け取りlong値を返す→LongBinaryOperator
- longを受け取りintの配列を返す→LongFunction<int[]>

Functionインタフェースは、引数の型と結果の型が基本データであれば、Functionの前に SrcToResult を付ける。例：LongToIntFunction

Functionインタフェースは、引数の型が基本データで、結果の型がオブジェクトであれば、Functionの前に SrcToObj を付ける。例：DoubleToObjFunction

2個の引数を取る3この基本の関数型インタフェースは、以下の3つ。
- BiPredicate<T,U>
- BiFunction<T,U,R>
- BiConsumer<T,U>

基本データ型を返す変形→ToIntBiFunction<T,U>

1個のオブジェクト参照と1個の基本データ型の2個の引数を受け取る→ObjDoubleConsumer<T>

ボクシングされた基本データ型で標準の関数型インタフェースを使わない（項目61）。

#### 関数型インタフェースを自作する
以下の特性を1つでも持つ場合は関数型インタフェースの自作を検討する
- 広く使われていて、説明的な名前から恩恵を受けられる
- インタフェースに関連付けられた強い契約を持っている
- 特別なデフォルトメソッドから恩恵を得られる
- 3つのパラメータを受け取る場合や、例外を投げる関数型インタフェースが必要な場合

#### 関数型インタフェースには@FunctionalInterfaceを付ける
@FunctionalInterfaceを付けることで以下の3つの目的を果たす
- インタフェースがラムダで使用可能であることをクラスとそのドキュメンテーションの読み手に伝える
- インタフェースが抽象メソッドを1つだけ持っている場合にのみコンパイルが通るようになる
- 保守担当者が誤って抽象メソッドを追加することを防ぐ

#### まとめ
- ラムダを考慮してAPIを設計することは必須である。
- 入力に対して関数型インタフェースを受け取り、出力に対して関数型インタフェースを返す。
- 基本的に java.util.function.Function で提供されている標準のインタフェースを使用する。
- 独自のインタフェースを書く場合@FunctionalInterfaceを付ける。


## 項目45 ストリームを注意して使う
ストリームAPIは、大量操作の逐次処理や並列処理を行いやすくするために Java8 で追加された。このAPIはデータ要素のシーケンスを表す`ストリーム`と、データ要素の有限、あるいは無限なシーケンスを表す`ストリームパイプライン`を抽象化して提供している。

ストリーム内のデータ要素はオブジェクト参照か基本データ型の値である。

ストリームパイプラインは、ソースのストリーム、それに続く0個以上の中間操作、その後に1つの終端操作から構成される。

ストリームパイプラインは遅延して評価される。つまり、終端操作が呼び出されるまで開始されませんし、終端操作を完了させるために必要の無いデータ操作は計算されない。

辞書ファイルから単語を読み出して、ユーザーが指定した最小の大きさに合致するアナグラムのグループをすべて表示するプログラムを考える。

```java
public class Anagrams {
    public static void main(String[] args) throws IOException {
        File dictionary = new File(args[0]);
        int minGroupSize = Integer.parseInt(args[1]);

        Map<String, Set<String>> groups = new HashMap<>();
        try (Scanner s = new Scanner(dictionary)) {
            while (s.hasNext()) {
                String word = s.next();
                groups.computeIfAbsent(alphabetize(word),
                        (unused) -> new TreeSet<>()).add(word);
            }
        }

        for (Set<String> group : groups.values())
            if (group.size() >= minGroupSize)
                System.out.println(group.size() + ": " + group);
    }

    private static String alphabetize(String s) {
        char[] a = s.toCharArray();
        Arrays.sort(a);
        return new String(a);
    }
```

computeIfAbsent は、第1引数に渡されたキーが存在する場合はキーに関連付けられている値を返す。関連付けられていない場合は、第2引数に与えられた関数オブジェクトを適用して、その値をキーと関連付けして値を返す。

ここでストリームに書き換えた値について考える。

```java
public class Anagrams {
    public static void main(String[] args) throws IOException {
        Path directory = Paths.get(args[0]);
        int minGroupSize = Integer.parseInt(args[1]);

        try (Stream<String> words = Files.lines(directory)) {
            words.collect(
                groupingBy(word -> word.chars().sorted()
                                .collect(StringBuilder::new,
                                    (sb, c) -> sb.append((char) c),
                                    StringBuilder::append).toString()))
                .values()
                .stream()
                .filter(group -> group.size() >= minGroupSize)
                .map(group -> group.size + ": " + group)
                .forEach(System.out::println);
            )
        }
    }
```

補足：
- [Streamのcollectメソッドを学ぶ](https://backpaper0.github.io/2014/10/04/stream_collect.html)
- [クラスStream](https://docs.oracle.com/javase/jp/8/docs/api/java/util/stream/Stream.html)
- [クラスCollectors](https://docs.oracle.com/javase/jp/8/docs/api/java/util/stream/Collectors.html)

このように、ストリームの乱用はプログラムの理解や保守を難しくする。ストリームを乱用せずに短く明瞭なプログラムを書いてみる。

```java
public class Anagrams {
    public static void main(String[] args) throws IOException {
        Path directory = Paths.get(args[0]);
        int minGroupSize = Integer.parseInt(args[1]);

        try (Stream<String> words = Files.lines(directory)) {
            words.collect(groupingBy(word -> alphabetize(word)))
                .values()
                .stream()
                .filter(group -> group.size() >= minGroupSize)
                .forEach(group -> System.out.println(group.size + ": " + group));
            )
        }
    }

    private static String alphabetize(String s) {
        char[] a = s.toCharArray();
        Arrays.sort(a);
        return new String(a);
    }
```

ストリームパイプラインの可読性を上げる工夫
- パラメータを g とすることがあるが、ラムダは明示的な型がないのでストリームパイプラインの可読生を上げるためにも group と書くべき。
- ヘルパーメソッドを使う。


#### alphabetizeメソッドをストリームで書いてみる
```java
"Hello world!".chars().forEach(System.out::plintln);
```

結果は 72101108....と表示される。これは chars() が返すストリームの要素が int値 だからである。
やろうと思えば以下のようにキャストを使ってストリームで表現することができる。

```java
"Hello world!".chars().forEach(x -> System.out.plingln((char) x));
```

しかし、これは可読性と保守性を下げるのでやめたほうが良い。

複雑な処理はストリームとループを組み合わせて行うのが最善である。ストリームを使うことで意味がある場合にだけストリームを使用する。

#### 関数オブジェクトではできないけどコードブロックではできること
- スコープ内のローカル変数を読み出したり修正したりできる。ラムダからは final か実質的 final の変数を読みだせるだけで、ローカル変数を修正できない。
- return, break, continue, 例外のスロー。ラムダはいずれもできない。

#### ストリームを使うと良い場面
- 均一に要素のシーケンスを変換する
- 要素のシーケンスをフィルターする
- 加算、結合、最小値を計算してまとめる
- 共通の属性でグループ化してコレクションに蓄積する
- 条件を満たす要素のシーケンスを検索する

#### ストリームかループのどちらを使用するか
ストリームで行うのが困難なことの1つは、パイプラインの複数ステージから対応する要素に同時にアクセスすることである。何かしらの値をmapしてしまうと元の値が失われてしまう。良い回避策は結果から元の値を逆計算することである。

例えば最初の20個のメルセンヌ素数を表示するプログラムを考える。メルセンヌ数（2^p - 1）があるとき、pが素数で結果も素数の値がメルセンヌ素数である。

まずはすべての素数を取得する無限のストリームを用意する。
```java
static Stream<BigInteger> primes(){
    Stream.iterate(BigInteger.TWO, BigInteger::nextProbablePrime)
}

public static void main(String[] args) {
    primes().map(p -> TWO.pow(p.intValueExact()).subtract(ONE))
            .filter(mersenne -> mersenne.isProbablePrime(50))
            .limit(20)
            .forEach(System.out::println);
}

```

isProbablePrime の引数は素数判定の制御値である。
終端処理でメルセンヌ素数の p を表示したいケースを考える。メルセンヌ数の指数は単純にバイナリ表現中のビット数であるため、以下のようにして求めることができる。

```java
.forEach(mp -> System.out.println(mp.bitLength() + ": " + mp));
```

ストリームかループのどちらを使用するか確信が持てないときは両方を試してみて調べる。P210 ~P211のソースコード参照。

## 項目46 ストリームで副作用のない関数を選ぶ

テキストファイル内の単語の頻度表を構築するプログラムを考える
```java
// ストリームAPIを使用しているがパラダイムを使っていない。このプログラムを使用してはならない

Map<String, Long> freq = new HashMap<>();
try (Stream<String> words = new Scanner(file).tokens()) {
    words.forEach(word -> {
        freq.merge(word.toLowercase(), 1L, Long::sum);
    });
}
```

上記のコードの悪い点は以下の2つである
- ストリームAPIからの恩恵を受けていない
- 外部の状態を更新するラムダを使用している

特に `forEach 操作はストリームの計算結果を報告するためだけに使い、計算を行うために使用されるべきではない`。forEachは明示的なループであるため並列化に適していない。ストリームの計算結果を既存のコレクションに追加するといった目的に使用されることはある。

StreamAPIを正しく使用したコードに書き換える。
```java
Map<String, Long> freq;
try (Stream<String> words = new Scanner(file).tokens()) {
    freq = words.collect(groupingBy(String::toLowerCase, counting()));
}
```

#### Collectorについて
改善されたコードは Collector が使われている。(Collectors.counting())

CollectorsのAPIを使用すると複雑さを掘り下げることなく実装ができる。

例えばストリームの要素を本物の Collection に集めたい場合、toList()、toSet()、toCollection(collectionFactory) を使うと簡単に実装できる。頻度表からトップ10のリストを抽出するストリームパイプラインを考える

```java
// 頻度表から単語のトップ10リストを得るパイプライン
List<String> topTen = freq.keySet().stream()
    .sorted(comparing(freq::get).reversed())
    .limit(10)
    .collect(toList());
```

Collectors.toList() と書かないのは、ストリームパイプラインの可読性を上げるために Collectors すべてのメンバーを static インポートするのが慣習だからである。

comparing は、型TからComparableソート・キーを抽出する関数を受け取り、そのソート・キーで比較するComparator<T>を返す。

各ストリーム要素はキーと値に関連付けられ、複数のストリーム要素を同一のキーに関連付けることができる。

最も単純なマップのコレクターの使用例として、以下に文字列から enum へのマップを作成するtoMapコレクターを使用したプログラムを示す。

```java
private static final Map<String, Operation> stringToEnum = Stream.of(values()).collect(toMap(Object::toString, e -> e));
```

ストリームの複数の要素が同一のキーにマッピングされる場合は IllegalSteateException を返す。

groupingBy および toMap はこのような衝突を回避するための手段をいくつか持っている。

1.BinaryOperator などのマージ関数を持つ toMap メソッドを使用する方法。
2.toMap の第3引数を使用する。

たとえば、さまざまなアーティストによるレコードアルバムのストリームを持っていて、レコーディングアーティストからベストセラーへのアルバムへのマップがほしいと仮定する。
```java
// キーからキーに対して選択された要素へのマップを生成するコレクター
Map<Artist, Album> topHits = album.collect(toMap(Album::artist, a->a, maxBy(comparing(Album::sales))));
```

※ maxBy は最大要素を生成するCollectorを返す。

以下は衝突があるときに、最後の書き込みを優先するコレクターのプログラムである。
```java
toMap(keyMapper, valueMapper, (oldValue, newValue) -> newValue)
```

groupingBy はキーに対応するリストのマップを生成する

```java
List<Integer> nums = List.of(1,2,3,4,5,6,7,8,9,1,1,1);
Map<Integer, Long> map = nums.stream().collect(Collectors.groupingBy(i ->i));
System.out.println(map);

=> {1=[1, 1, 1, 1], 2=[2], 3=[3], 4=[4], 5=[5], 6=[6], 7=[7], 8=[8], 9=[9]}
```

[後世まで残したいCollectors.groupingByの話](https://qiita.com/tasogarei/items/5492e09b78f6bfc98801)

groupingBy にリスト以外の値を持つマップを生成する Collector を返すようにしたいときは、`ダウンストリームコレクター`を指定する。
groupingBy メソッドの場合は、第2引数にダウンストリームコレクターを指定する。

```java
List<Integer> nums = List.of(1,2,3,4,5,6,7,8,9,1,1,1);
Map<Integer, Long> map = nums.stream().collect(Collectors.groupingBy(i ->i, Collectors.counting()));
System.out.println(map);

=> {1=4, 2=1, 3=1, 4=1, 5=1, 6=1, 7=1, 8=1, 9=1}
```

counting メソッドが返す Collector はダウンストリームコレクターとして利用されることを想定しているため、 Stream の count メソッドを使っても同じことができる。

```java
Stream.count() <=> Stream.collect(counting())
```

Collectors のメソッドには、他にも maxBy, minBy, joining, summing, averaging, summarizing で始まるメソッドが存在する → [Collectorsクラス](https://docs.oracle.com/javase/jp/8/docs/api/java/util/stream/Collectors.html)

#### まとめ
- 終端操作 forEach はストリームによって行われた計算結果を表示するためだけに使われるべき。
- ストリームをコレクションに変換するために Collector を使う。場合によってはダウンストリームコレクターを使う。

## 項目47 戻り値型として Stream よりも Collection を選ぶ
Streamが登場する前は Collection, Set, List, Iterable, 配列型、どれを戻り値として返すか決めるのは簡単だった。基本的にコレクションのインタフェースを返すのが普通であり、
Collection のメソッドを実装できない場合は Iterable インタフェースが使用された。基本データ型を扱う場合や、厳しいパフォーマンス制約がある場合は配列型が使用された。ところが、Streamが追加されたことによって返り値を決めるのが難しくなってしまった。

例えば、戻り値として Stream を選択した場合、for-each文を使うことはできない。Stream は Iterable のメソッドと互換性があるため、Iteratorを使ってforEachを実装することはできる

```java
public static <E> Iterable<E> iterableOf(Stream<E> stream) {
    return stream::iterator();
}

~~~
for (ProcessHandle ph : iterableOf(ProcessHandle.allProcesses())) {
    // プロセスを処理する
}
```

以下の実装だと、コンパイルエラーになったり、人間が読みにくい実装になってしまう。

```java
for (ProcessHandle ph : ProcessHandle.allProcesses()::iterator) { // エラー：　ここではメソッド参照は予期されていません

for (ProcessHandle ph : (Iterable<ProcessHandle>) ProcessHandle.allProcesses()::iterator) // キャストすれば動作するけど複雑になる
```

項目45の Anagram プログラムでは Files.lines を使っているが、ループを使ったプログラムでは Scanner を使っている。Scannerを使用したのは、File.lines() の戻り値がストリームだからである。

オブジェクトのシーケンスを返すメソッドを書いていて、それがストリームパイプラインでしか使われないのであればストリームを返すべきである。また、ループでしか使われないのであれば Iterable を返すべきである。
だが、理想的にはストリームパイプライン、for-each文を書きたいユーザーどちらにも備えるべきであり、Collectionインタフェースは Iterable のサブタイプで、streamメソッドを持っているので、`一般的に Collection、もしくはそのサブタイプを戻り値として返す`とよい。

ただし、`コレクションを返すためだけに大きなシーケンスをメモリに保存しない`ようにする。

入力セットのべき集合をコレクションとして返すプログラムを考える（p.219参照）

入力セットを30個に制限することで、シーケンスの長さをInteger.MAX_VALUEを超えないようにしている。

指数ほどの大きさではないが、入力値のリストの大きさのn倍の量のメモリが必要なケースについて考える（p.220参照）

入力リストのサブリストのストリームを実装するのは容易である。

メモ：range/rangeClosedの違い
```java
IntStream.range(0,5).forEach(System.out::print);
System.out.println("");
IntStream.rangeClosed(0,5).forEach(System.out::print);

=> 01234
=> 012345
```

### まとめ
- ユーザーはストリームとして処理したいかもしれないし、for-each文でループを回したいかもしれない。だから Streamへの変換が容易なコレクションを返す。ストリームパイプラインを使用することしか想定していない場合は Stream を返す。
- シーケンスの要素数が多くなる場合はコレクションの実装を検討する。

