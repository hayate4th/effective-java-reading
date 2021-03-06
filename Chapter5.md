# 第5章 ジェネリックス
ジェネリクスはJava 5 以降にJava に導入された。
ジェネリックスより前では、コレクションからは読みだした全てのオブジェクトをキャストする必要があった。
ジェネリックスを使うと、コンパイラがキャストを自動的に挿入するため、
例えばコレクションに誤った型のオブジェクトを入れようとすると、
コンパイル時にエラーとなりみんなハッピーになる。

## 項目26 原型を使わない
Effective Javaの解説と、[Java Language Specification](https://docs.oracle.com/javase/specs/jls/se12/html/jls-4.html#jls-4.5) の用語の説明がやや異なってるため、
用語の定義はふわっと行う。

### 定義
* **ジェネリック**クラス、**ジェネリック**インターフェイス:\
一つ以上の型パラメータを受け取る型変数を宣言に持つクラスおよびインターフェイス。総称して**ジェネリック型** という。
* **型パラメータ**:\
宣言において型を受け取る引数。例えば `interface List<E> { ... }` における `E`。

* **パラメータ化された型**:\
型パラメータを対応する**型引数**で埋めた状態の型。
例えば `List<String>` は、`List<E>` の型パラメータ `E` に対応する型引数 `String` を与えられてパラメータ化された型である。

* **原型**\
ジェネリック型の、型パラメータを伴わないで使われる名前。例えば `List<E>` に対しては、`List` が原型である。原型は、型宣言から全ての型情報が消し去られて（`java.lang.Object`と置き換えられて）いるかのように振る舞う。これは前方互換性のために存在する。

### 原型は使用しない
ジェネリック型インターフェイス `Collection<E>` がある時に、以下のようなコードを書いてはならない。

```java
// Stamp インスタンスのみを含むように運用する。
private final Collection stamps = ... ;


// 誤った挿入について、コンパイラはエラーを出さない。
stamps.add(new Coin( ... )); // コンパイル時に、無検査呼び出しの警告を表示する。

// stamps から取り出し、 (Stamp) とキャストするまで実行時エラーが発生しない。
for (Iterator i = stamps.iterator(); i.hasNext(); ) {
    Stamp stamp = (Stamp) i.next(); // ClassCastException
    stamp.cancel();
}
```

* 誤りはできるだけ早い段階、理想的にはコンパイル時に発見されるのが良い（至言）。
* ひとたび `ClassCastException` を見たら、コードベース全体を捜索する羽目になる。

代わりに、以下のように正しくジェネリックすを使用すると型安全になる。
```java
private final Collection<Stamps> stamps = ... ;
```

すると、`stamps` には `Stamp` インスタンスだけを含むべきことをコンパイラが把握して、それを保証する。

```java
stamps.add(new Coin( ... )); // コンパイルエラーが発生する。
```

コレクションから要素を取り出す際には、コンパイラは目には見えないキャストを挿入し、キャストが成功することを保証する。

原型を使うと、ジェネリックスの安全性と表現力の全てを失う。
原型は、Javaの**移行互換性**を保つために導入され、**イレイジャ**を使って実装されている（項目28）。

### ジェネリックスの使用
`List<Object>` は、原型ではなくジェネリックスを使っており、どんなオブジェクトでも入れられることを明示しているため、パラメータ化された型を使えば型安全性を失わない。

* `List<String>` は `List` のサブタイプである（`List` のパラメータとして渡せる）
* `List<String>` は `List<Object>` のサブタイプではない

```java
import java.util.*;

public class TestCast {
    public static void main(String[] args) {
        List<String> strings = new ArrayList<>();
        unsafeAdd(strings, Integer.valueOf(42));
        String s = strings.get(0); // 実行時エラー
    }

    private static void unsafeAdd(List list, Object o) {
        list.add(o); // 原型を使っているので無検査呼び出し（コンパイル時の警告）
    }
}
```

このサンプルコードの実行例は以下の通りである。

```console
% javac TestCast.java
TestCast.java:11: 警告: [unchecked] raw型Listのメンバーとしてのadd(E)への無検査呼出しです
        list.add(o);
                ^
  Eが型変数の場合:
    インタフェース Listで宣言されているEはObjectを拡張します
警告1個
% java TestCast
Exception in thread "main" java.lang.ClassCastException: java.lang.Integer cannot be cast to java.lang.String
	at TestCast.main(TestCast.java:7)
```

`unsafeAdd` を以下のように置き換えると、めでたくコンパイルエラーとなる。

```java
    private static void unsafeAdd(List<Object> list, Object o) {
        list.add(o);
    }
```

```console
% javac TestCast.java
TestCast.java:6: エラー: 不適合な型: List<String>をList<Object>に変換できません:
        unsafeAdd(strings, Integer.valueOf(42));
```

### 非境界ワイルドカード型
要素の型が何であろうと構わないようなコレクションに対して原型を使いたくなるかもしれない。
以下の例は、2つのセットを受け取り、共通な要素の数を返すメソッドの、原型を使ってしまった例である。

```java
static int numElementsInCommon(Set s1, Set s2) {
    int result = 0;
    for (Object o1 : s1) {
        // ちなみにこのメソッドは bool contains(Object) という型を持つ
        if (s2.contains(o1)) {
            result++;
        }
    }
    return result;
}
```

このメソッドの安全な代替方法は、**非境界ワイルドカード型**を使うことである。
ジェネリック型を使いたいが、実際の型パラメータが何であるかわからなかったり、気にしない場合は、
代わりにクエスチョン記号を使える。例えば、 `Set<E>` に対する非境界ワイルドカード型は `Set<?>` である。

非境界ワイルドカード型は型安全である。原型のコレクションにはどのような要素も挿入できるが、非境界ワイルドカード型のコレクションには、(null以外の) **要素が挿入**できない。
それだけではなく、取り出すオブジェクトについて、どのような仮定もできない。
ただし、この制限が受け入れられない場合は、ジェネリックメソッド（項目30）や、境界ワイルドカード型（項目31）を使う。

### 原型を使う場合
原型を使うべきではないという規則には例外があり、クラスリテラルは原型を使わなければならない。

もう一つの例外は、`instanceof` 演算子に関することである。
**ジェネリック型の情報は実行時には消えているので**、非境界ワイルドカード型以外のパラメータ化された型に対する`instanceof` 演算子の仕様は許されていない。
また、非境界ワイルドカード型を`instanceof` 演算子で使ってもかまわない。

以下が、ジェネリック型で`instanceof`演算子を使う好ましい方法である。

```java
if (o instanceof Set) { // o instanceof Set<?> でもよい
    Set<?> s = (Set<?>) o;
}
```

また、一旦`o`が`Set`であると判断できれば、原型ではなく `Set<?>`にキャストしなければならない。
この場合、検査されたキャストなのでコンパイラの警告は出ない。

### まとめ
* 原型を使うと実行時に例外がスローされる可能性があるため、使わないこと。
* 原型は、ジェネリックスが導入される前のコードとの互換性のためにある。
* `Set<Object>` は任意の型のオブジェクトを含められる集合を表すパラメータ化された型である。
* `Set<?>` は、何らかの不明な型のオブジェクトだけを含むことが可能な集合を表す**ワイルドカード型**である。
* `Set` は原型であり、ジェネリックスの型システムから外れており、安全ではない。

用語のまとめは本文を参照。実型パラメータ、および仮型パラメータという用語は JLS になかったため、
JLS の用語定義にしたがった。


## 項目27 無検査警告を取り除く

ジェネリックスを用いてプログラミングをするときは、以下のような多くのコンパイラの警告を目にする。
- 無検査キャスト警告
- 無検査メソッド呼び出し警告
- パラメータ化された可変引数型警告
- 無検査変換警告

警告によっては取り除くのが困難だが、†やり抜いてください†。
取り除ける全ての無検査警告を取り除いてください。

### 型推論を使って警告を取り除く例
例えば、以下のようなコードは警告を出す。

```java
List<String> strings = new ArrayList();
```
```console
% javac TestCast.java
TestCast.java:5: 警告: [unchecked] 無検査変換
        List<String> strings = new ArrayList();
                               ^
  期待値: List<String>
  検出値:    ArrayList
警告1個
```

この場合は、Java 7 で導入されたダイアモンド演算子（`<>`）を使い、型推論をすることで型引数を省略して書ける。
```java
List<String> strings = new ArrayList<>();
```

### `SuppressWarnings` アノテーション
警告を取り除くことができなくても、警告を起こしているコードが型安全だと明確に示すことができれば、そのときかつそのときに限り `@SuppressWarnings("unchecked")` アノテーションで警告を抑制すること。

なぜなら最初にコードが型安全であることを示すことなく警告を抑制すると、誤った安心感を持たせるだけであるし、
安全だとわかっている無検査警告を抑制せずに無視したら、本当の問題を示す新たな警告が出たときに埋もれてしまう可能性があるからである。

`SuppressWarnings` アノテーションはローカル変数の宣言からクラス全体まで、どのような宣言でも使えるが、できるだけ最小のスコープに対して使うこと（例：変数宣言、短いメソッド、短いコンストラクタなど）。
クラス全体にこのアノテーションを使うと重大な警告を隠匿する可能性がある。

### アノテーションのスコープを短くする例

2行以上の長さのメソッド、コンストラクタに対して、 `SuppressWarnings` アノテーションを使っている場合は、アノテーションをローカル変数の宣言に移動できるかもしれない。ローカル変数が増えるがその価値はある。以下の例は、無検査警告を生み出す。

```java
    // コンパイルを通すためにやや変更している
    public static <T> T[] toArray(T[] a, T[] elementData, int size) {
        if (a.length < size) {
            return (T[]) Arrays.copyOf(elementData, size, a.getClass());
        }
        System.arraycopy(elementData, 0, a, 0, size); // 無検査警告
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }
```

仔細は気にしなくてもよいが、`Arrays.copyOf` のあたりで無検査キャストが発生する。
しかし、`return` 文は宣言ではないので、例のアノテーションをつけることはできない。

そこで、新たに変数を一つ導入する。
```java
    public static <T> T[] toArray(T[] a, T[] elementData, int size) {
        if (a.length < size) {
            // T[] として渡されたのと同じ型の配列を
            // 生成するのでこのキャストは正しい。
            @SuppressWarnings("unchecked") T[] result =
                (T[]) Arrays.copyOf(elementData, size, a.getClass());
            return result;
        }
        System.arraycopy(elementData, 0, a, 0, size); // 無検査警告
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }
```

このメソッドは警告なくコンパイルされ、無検査警告が抑制されるスコープを最小限にしている。

### 理由を述べるコメント
`@SuppressWarnings("unchecked")` アノテーションをつけるときは、
そうするのが安全である理由を述べるコメントを必ずつけること。
他の人が理解しやすくなるし、何より安全ではなくなる変更をしてしまう可能性を減らす。
このようなコメントを書くのが難しいと思っても、†考え続けてください†。
もしかすると、無検査操作が安全ではないと結局わかるかもしれません。

### まとめ
* 無検査警告は重要なので無視しないこと。
* すべての無検査警告は、実行時の `ClassCastException` の可能性を表している。
* 無検査警告を取り除く最善の努力を行うこと。
* 取り除くことができない場合、型安全であると明確に示せる場合、`@SuppressWarnings("unchecked")` を最小のスコープに対して使うことで警告を抑制すること。
* 警告を抑制する場合、そう決めた理由をコメントに書き残すこと。


## 項目28 配列よりもリストを選ぶ
配列は2つの点でジェネリクス型と異なっている。

### 1. 配列は*共変* (covariant)
一つ目は、配列は*共変*だが、ジェネリクスは*不変*であること。

これは、`Sub` が `Super` のサブタイプならば、 `Sub[]` は `Super[]` のサブタイプだと言うことを意味する（*共変*）。
一方、`List<Sub>` は `List<Super>` のサブタイプでもスーパータイプでもない（*不変*, invariant）。
例えば、以下のコードはコンパイルが許されている。
```java
Object[] objectArray = new Long[1];
objectArray[0] = "I don't fit in"; // ArrayStoreException
```

しかし、次のコードは許されない。
```java
List<Object> ol = new ArrayList<Long>(); // 互換性のない型
ol.add("I don't fit in");
```

もちろん、どちらの方法でもLongのコンテナにStringを入れることは許されないが、
前者は実行時に、後者はコンパイル時にエラーとなる。もちろん後者が優っている。

### 2. 配列は具象化 (reify) される
配列は実行時にその要素の型を知っており、それを強制する。
上で見たように、互換性のない型を配列にストアしようとすると、ArrayStoreException が上げられる。

他方、ジェネリックスはイレイジャ（erasure）を使って実装されている。
これは、ジェネリックスはコンパイル時にのみ型制約を強制し、
実行時には型の情報を破棄することを意味する。
イレイジャによって、ジェネリックスを使っていない既存のコードからの円滑な移行を保障した（項目26）。

これらの基本的な相違により、配列とジェネリックスはうまく調和しない。
例えば、以下の配列生成は全てコンパイル時にジェネリック配列生成エラーとなる。
* ジェネリック型の配列 (`new List<E>[]`)
* パラメータ化された型の配列 (`new List<String>[]`)
* 型パラメータの配列 (`new E[]`)

ジェネリック型の配列が許されていない理由は、型安全ではないからである。
次のコードを考える。

```java
List<String>[] stringLists = new List<String>[1];  // (1)
List<Integer> intList = List.of(42);               // (2)
Object[] objects = stringLists;                    // (3)
objects[0] = intList;                              // (4)
String s = stringLists[0].get(0)                   // (5)
```

* (1) が許されると仮定
* (2) はいいとする。
* (3) は、Object[] は List<String>[] のスーパータイプなので許される
* (4) は、実行時では型消去により、List型の配列に List を突っ込んでることになり合法。マジで？？
* (5) で実行時にClassCastExceptionが上がる。

これを防ぐために、(1) がダメであってくれないといけない。

`E`, `List<E>`, `List<String>` は、技術的には具象化不可能型として知られている。
直感的には、コンパイル時よりも実行時の方が情報の少ない型である。
イレイジャにより、唯一具象化可能なパラメータ化された型は非境界ワイルドカード型のみである。
これの配列は許されているが、あまり有用ではない。

一般に、ジェネリック型は、その要素型の配列を返すことが不可能ですが、部分的な解決方法は項目33を参照。
ジェネリック型の配列生成の禁止は、可変長引数のメソッド（項目53）をジェネリック型と組み合わせて使う場合、
困惑する警告を出す可能性を意味する。
なぜなら、可変長引数のメソッドを呼び出すと、必ず可変長パラメータを保持する配列が生成されるからである。
そして、この型が具象化可能型ではない場合は警告が発生する。
`SafeVarargs`アノテーションがこの場合に使える（項目32）。
ジェネリック配列生成エラーを回避する最良の方法は、`List<E>` を使うことである。それはそう。
パフォーマンスよりも、型安全性と相互運用性を手に入れることができる。

### 例：`Chooser` クラス
例えば、コレクションからランダムに要素を選んで返すメソッドを持つ`Chooser` クラスを書きたいとする。

```java
// (1) ジェネリック型を使わない場合
public class Chooser {
    private final Object[] choiceArray;

    public Chooser(Collection choices) {
        choiceArray = choices.toArray();
    }

    public Object choose() {
        Random rnd = ThreadLocalRandom.current();
        return choiceArray[rnd.nextInt(choiceArray.length)];
    }
}
```

このクラスの `choose` メソッドでは、要素を目的に応じた型にキャストする必要があるが、
実行時に失敗する可能性がある。
項目29の助言を受け止めて（まだ受け止めてない）、ジェネリック型に修正したい。

```java
// (2) ジェネリック化する試み（コンパイルできない）
public class Chooser<T> {
    private final T[] choiceArray;

    public Chooser(Collection<T> choices) {
        choiceArray = choices.toArray();
    }
    // chooseメソッドに変更はない。
}
```

(2) は以下のようなコンパイルエラーが発生する。

```console
% javac Chooser.java
Chooser.java:9: エラー: 不適合な型: Object[]をT[]に変換できません:
        choiceArray = choices.toArray();
                                     ^
  Tが型変数の場合:
    クラス Chooserで宣言されているTはObjectを拡張します
```

†たいしたことはない、Object配列をT配列にキャストするさ† と皆さんは言うでしょう。

```java
        choiceArray = (T[]) choices.toArray();
```

エラーは無くなるが、今度は警告が出る。

```console
% javac Chooser.java
Chooser.java:9: 警告: [unchecked] 無検査キャスト
        choiceArray = (T[]) choices.toArray();
                                           ^
  期待値: T[]
  検出値:    Object[]
  Tが型変数の場合:
    クラス Chooserで宣言されているTはObjectを拡張します
```

実行時の型が何であるかはプログラムには分からないため、実行時におけるキャストの安全性を検査できないと、コンパイラは伝えている（無検査キャスト）。

ジェネリックス型からは、その要素の型（型パラメータ）は実行時には消えていることに留意せよ。
このプログラムは、実行時に動作するが、コンパイラはそれを証明できない。

コメントに証明を書き、アノテーションで警告を抑制することもできるが、警告の原因を消し去る方が良い（項目27）。
未検査キャスト警告を取り除くためには、配列の代わりに結局リストを使うことである。

```java

public class Chooser<T> {
    private final ArrayList<T> choiceArray;

    public Chooser(Collection<T> choices) {
        choiceArray = new ArrayList<>(choices);
    }

    public T choose() {
        Random rnd = ThreadLocalRandom.current();
        return choiceArray.get(rnd.nextInt(choiceArray.size()));
    }
}
```

配列を使うよりも、`ArrayList<T>`を使う方がやや冗長で、少し遅いが実行時に`ClassCastException` が発生しないため若干安心です。
（本音を言うと、`rnd.nextInt` が配列の範囲に収まることも型システムに言明してもらいたい）。

### まとめ
* 配列とジェネリックスは異なる型規則を持つ。
* 配列はジェネリックスと調和しない。
* こいつらを混在させたコードを書いていてエラーや警告が起こるなら、配列をやめてリストを使う。

| おなまえ | サブタイプ規則 | 具象化 | 型安全性 |
|:--|:-----------|:--------|:--------|
|配列        | *共変* | 具象化される | 実行時のみ |
|ジェネリックス| *不変*  | 型消去される | コンパイル時のみ |


## 項目29 ジェネリック型を使う
自分独自のジェネリック型を書くことは、ただ使うだけよりも若干難しい。
ただ、この項目は簡単なので飛ばしつつ進める。
テキストの `Stack` クラスのジェネリック化の最初の試みを以下に示す。

```java
public class Stack<E> {
    private E[] elements;
    private int size = 0;
    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    public Stack() {
        elements = new Obeject[DEFAULT_INITIAL_CAPACITY];
    }

    public void push(E e) {
        ensureCapacity();
        elements[size++] = e;
    }

    public void pop(E e) {
        if (size == 0) {
            throw new EmptyStackException();
        }
        E result = elements[--size];
        elements[size] = null;
        return result;
    }
    // 中略
}
```

すると、具象化不可能型の配列の作成エラーとなる。
```java
        elements = new Obeject[DEFAULT_INITIAL_CAPACITY];
```

このクラスの正しいジェネリック化には2通りある。

### 1. 配列をキャストする方法

コンストラクタのみを以下のように書き換える方法。

```java
    public Stack() {
        elements = (E[]) new Obeject[DEFAULT_INITIAL_CAPACITY];
    }
```

* ただし、無検査キャスト警告が出る。
* コンパイラは正しさを証明できないが、†みなさんはできます†。
* 項目27のようにコメントを書き、警告を抑制すればOK

#### 利点
* 読みやすく簡潔。
* 配列が `E[]` と宣言されており、`E`のインスタンスしか含まないことを明示する。
* キャストが一箇所で済む
* みんなだいたいこちらを使う

#### 欠点
* ヒープ汚染（項目32）を起こす

### 2. 逐一キャストを行う方法

1. `elements` の型を `Object[]` に書き換え、
2. なおかつ、この配列から取り出す際にキャストを行う。

```java
    public void pop(E e) {
        if (size == 0) {
            throw new EmptyStackException();
        }
        E result = (E) elements[--size];
        elements[size] = null;
        return result;
    }
```

* もちろん、無検査キャスト警告が出るが、同じように対処する必要がある。

#### 利点
* ヒープ汚染が発生しない（項目32）。

#### 欠点
* 配列から取り出すたびにキャストが必要となる

### 議論

* 先の例は、項目28のなるべくリストを使うべき、に反するようだが、いつでも使えるわけではない。
* `Stack<int>` や `Stack<double>` など、プリミティブデータ型の`Stack` は作れない。
  * ボクシングされたデータ型を使えばよい（項目61）。
* **境界型パラメータ** を使い、型パラメータが何らかの型のサブタイプであることを要求できる。

```java
class DelayQueue<E extends Delayed> implements BlockingQueue<E>
```

* もちろん、自分自身は自分自身のサブタイプなので、`DelayQueue<Delayed>` の生成はOK。


## 項目30 ジェネリックメソッドを使う

クラスをジェネリック化できるように、メソッドもジェネリック化できる。
例えば、パラメータ化された型に対する static メソッドは大抵ジェネリックである。
メソッドをジェネリック化する例を見ていく。

```java
public static Set union(Set s1, Set s2) {
    Set result = new HashSet(s1);
    result.addAll(s2);
    return result;
}
```

型パラメータのリストは、メソッドの修飾子とメソッドの戻り値型の間に入る。
以下のようにジェネリック化できる。

```java
public static <E> Set<E> union(Set<E> s1, Set<E> s2) {
    Set result<E> = new HashSet<>(s1);
    result.addAll(s2);
    return result;
}
```

（ジェネリックメソッドをテストするプログラムは省略）

この `union` メソッドの制約は、3つのSetすべての肩が同じでなければいけないことである。
境界ワイルドカード型（項目31）を使うことでメソッドをより柔軟にできる。

### ジェネリック・シングルトン・ファクトリ
不変だが多くの型に適用できるオブジェクトを生成する必要がある。
ジェネリックはイレイジャ（項目28）によって実装されているため、
要求される全ての型パラメータに対して単一のオブジェクトを使うことができる。

ジェネリック・シングルトン・ファクトリと呼ばれるこのパターンは、`Collections.reverseOrder` 
といった関数オブジェクト（項目42）や`Collections.emptySet` といったコレクションで使われる。

恒等関数を自作することを考える。以下のように書ける。

```java
private static UnaryOperator<Object> IDENTITY_FN = (t) -> t;

@SuppressWarnings("unchecked")
public static <T> UnaryOperator<T> identityFunction() {
    return (UnaryOperator<T>) IDENTITY_FN;
}
```

もちろん無検査キャスト警告が発生するのですぐさま抑制しに入る。
しかし、恒等関数は引数を何も変更しないので、すべての型に対して安全であることがわかっているのでOK。

（使用例は略）

### 再帰型境界
まれだが、型パラメータをその型パラメータ自身が関係する何らかの式で制限することが可能である。

```java
public interface Comparable<T> {
    int compareTo(T o);
}
```

ここで型パラメータ`T`は、`Comparable<T>` を実装している型の要素との比較が可能な型である。
ただ、実際はほぼ全ての型が自分自身の型の要素同士だけで比較可能である。
例えば、`String` は `Comparable<String> ` を実装し、`Integer` は `Comparable<Integer>` を実装する。

ところで、コレクションをソートしたり、コレクション内の最小値や最大値を得るといったメソッドは多く存在する。
こういった操作を行うためには、コレクション内の全ての要素が、ほかの要素と相互に比較可能である必要がある。
この制約を表現する方法は以下の通りである。

```java
public static <E extends Comparable<E>> E max(Collection<E> c);
```

`<E extends Comparable<E>` は、自分自身と比較可能な任意の型`E` と読むことができる。

（実装例は省略）

### まとめ
- ジェネリックメソッドを使え、原型を使うな。
- キャストを必要とする既存のメソッドをジェネリック化しても互換性を保てる。


## 項目 31 APIの柔軟性向上のために境界ワイルドカードを使う
### ジェレリックスは*不変*
- パラメータ化された型は*不変* (invariant) である（項目28）
- 二つの異なる型 `Type1` と `Type2` に対して、
  - `List<Type1>` は `List<Type2>` のサブタイプでもスーパータイプでもない。

#### 具体例
- `List<String>` は `List<Object>` のサブタイプでない。
- 任意のオブジェクトを`List<Object>` に入れることはできるが、 `List<String>` には必ずしも入るとは言えない
- `List<String>` は`List<Object>` にできることが全てできるわけではないため、サブタイプではないと言える（リスコフの置換原則）

### プロデューサ

- *不変*型付けが提供する以上の柔軟性が必要な場合がある。項目29のスタックを考える

```java
public class Stack<E> {
    public Stack();
    public void push(E e);
    public E pop();
    public boolean isEmpty();
}
```

- 要素の列を受け取り、それらの要素を全てスタックにプッシュするメソッドを追加したい

```java
// 最初のバージョン。ワイルドカード型を使わない不十分なpushAllメソッド
public void pushAll(Iterable<E> src) {
    for (E e : src) {
        push(e);
    }
}
```

- このメソッドはエラーや警告なしでコンパイルされるが、柔軟性に欠ける
- `Iterable src` の要素の型がスタックの要素の型と完全に一致している場合はOK
- 単一の要素を追加する場合はサブタイプのオブジェクトがプッシュできる
  - `Stack<Number>` 型の `stack` があり、`Integer` 型の `intVal` があるとする
  - `Integer` は `Number` のサブタイプなので、 `stack.push(intValue)` ができるはずである
- したがって、以下のようなコードも動作すべきであるが、パラメータ化された型は*不変* であるため型エラーとなる。

```java
Stack<Number> numberStack = new Stack<>();
Iterable<Integer> integers = ... ;
numberStack.pushAll(integers);
```

#### 境界ワイルドカード型
- このような状況に対処するために、 *境界ワイルドカード型* と呼ばれる特殊なパラメータ化された型がある
- `pushAll` の引数は `E` の `Iterable` ではなく、 `E` の *何らかのサブタイプの* `Iterable` とすべきである
- 境界ワイルドカード型を用いると、これは `Iterable<? extends E>` と表される。
  - ここでの `extends` キーワードはクラスの継承という意味ではなく、サブタイプ関係を表す。
- そしてこの型を使うように修正すると、`pushAll` は以下のようになる。

```java
// E のプロデューサとしてのパラメータのためのワイルドカード型
public void pushAll(Iterable<? extends E> src) {
    for (E e : src) {
        push(e);
    }
}
```

- ある型`E` のオブジェクトを *一方的に提供する* 役割を持つオブジェクトを*プロデューサ* と呼ぶ。
- この変更によって、もとの`pushAll` ではコンパイルできなかったようなクライアント側のコードもコンパイル可能となる
- すべてのコードがエラーも警告もないため、型安全である

### コンシューマ

- `pushAll` のように `popAll` を書きたいとする
- 先ほどと同様に、追加先のコレクションの要素型とスタックの要素型が完全に一致する場合にはうまく動作する

```java
// 最初のバージョン。ワイルドカード型を使わない不十分なpopAllメソッド
public void popAll(Collection<E> dst) {
    while (!isEmpty()) {
        dst.add(pop());
    }
}
```

- ひとつのオブジェクトをpopすることを考える
- `Stack<Number>` 型の `stack` と、 `Object` 型の変数 `obj` があると仮定する
- `obj = stack.pop()` はコンパイルできて正常に動作する
- ならば以下のコードもコンパイルできるべきである

```java
Stack<Number> numberStack = new Stack<Number>();
Collection<Object> objects = ...;
numberStack.popAll(objects);
```

- このクライアント側のコードをコンパイルしようとすると、`pushAll` の時と同じようなエラーが発生する
- なぜなら、 `Collection<Object>` は `Collection<Number>` のサブタイプではないからである
- 同様に、境界ワイルドカード型が解決策を提供する

- `popAll` の引数の型は、`E` のコレクション ではなく、 `E` の何らかのスーパークラスのコレクション であるべきである
  - 自分自身は自分自身のスーパータイプであると、JLSで定義されている
- これを正確に表現するワイルドカード型は、 `Collection<? super E>` である
- これを用いて `popAll` を修正する。すると `Stack` とクライアントコードはエラーも警告もなくコンパイルされる。
- プロデューサとは逆に、ある型 `E` のオブジェクトを *一方的に受け取る* 役割を持つオブジェクトのことを*コンシューマ*という。

```java
// Eコンシューマ としてのパラメータのためのワイルドカード型
public void popAll(Collection<? super E> dst) {
    while (!isEmpty()) {
        dst.add(pop());
    }
}
```

### ここまでのまとめ
- 最大限の柔軟性のために、プロデューサまたはコンシューマを表す入力パラメータに対しては、境界ワイルドカード型を使うこと
  - ただし、入力パラメータがプロデューサ **かつ** コンシューマである場合は、ワイルドカード型ではなく正確な型一致が必要である

#### PECS

- パラメータ化された型が `T` のプロデューサ (P) を表ている → `<? extends T>` 
- パラメータ化された型が `T` のコンシューマ (C) を表ている → `<? super T>` 
- `pushAll` の `src` パラメータは `E` のインスタンスを生産するので `Iterable<? extends E>`
- `popAll` の `dst` パラメータは `E` のインスタンスを消費する（受け取る）ので `Collection<? super E>`
- PECS 略語はこの基本原則を捉えている。 Naftalin と Wadler はこれを Get and Put Principle と呼んでいる。

### 例

- `Chooser` のコンストラクタの宣言を見る（項目30）

```java
public Chooser(Collection<T> choices)
```

- `T` 型の値を生産するためだけにコレクション `choice` を使っており、後で使うために保存している。したがって、この宣言は以下のように書くべきである

```java
public Chooser(Collection<? super T> choices)
```

- すると、`Chooser<Number>` のコンストラクタに `List<Integer>` を渡すことができて嬉しい

### 戻り値型と明示的型引数
- 項目30の `union` メソッドを見る。

```java
public static <E> Set<E> union(Set<E> s1, Set<E> s2)
```

- `s1` も `s2` もどちらも`E`プロデューサなので、PECS略語に従うと宣言は次のようになる

```java
public static <E> Set<E> union(Set<? extends E> s1,
                               Set<? extends E> s2)
```

- **戻り値型として境界ワイルドカード型を使ってはならない**
  - ユーザに柔軟性を与えるよりも、むしろクライアント側でワイルドカード型を使うことを強制してしまう
- すると、次のコードはエラーも警告もなくコンパイルされる

```java
Set<Integer> integers = Set.of(1, 3, 5);
Set<Double> doubles = Set.of(2.0, 4.0, 6.0);
Set<Number> numbers = union(integers, doubles);
```

- 適切に用いるならば、ユーザはワイルドカード型にほとんど気づかない
  - クラスの利用者がワイルドカード型について考える必要があるならば、おそらくそのクラスのAPIは間違っている
- Java 8 以前は、型推論の規則は上のようなコードを処理できるほどには賢くなく、`E`の型を推論するには文脈的に指定された戻り値型（目的型, target type）を必要としていた
  - 上の `union` の呼び出しの目的型は `Set<Number>` である。Javaの古いバージョンでコードをコンパイルしようとすると、（省略するが）長いエラーメッセージが表示される
  - 望んでいる型をコンパイラが推論しない場合には、**明示的型引数** (explicit type argument) でどの型を使うべきかをコンパイラへ指示できる
  - Java 8 で目的型が導入される前であっても、これを頻繁に行う必要はない
    - 明示的型引数はきれいじゃないので、使わないに越したことはない

```java
Set<Number> numbers = Union.<Number>union(integers, doubles);
```

### 複雑な例: `max` メソッド

- 項目30の `max` メソッドは元々以下のような宣言であった

```java
public static <T extends Comparable<T>> T max(List<T> list)
```

- これをワイルドカード型を使って修正する
  - この宣言はこの本全体の中で最も複雑な宣言だそうです

```java
public static <T extends Comparable<? super T>> T max(List<? extends T> list)
```

1. パラメータ `list` については、型 `T` を一方的に供給するため、プロデューサにあたる
    - したがって、 `List<? extends T>` と書き換える。
2. 次に、`T extends Comparable<? super T>`という部分について考える
    - `T` は元々の宣言では、`Comparable<T>` のサブタイプであると指定されていた。
    - ここで、`T` と比較可能な型について考えると、以下のような `compare` メソッドを実装するため、
この型のオブジェクトは `T` のコンシューマであると言える。
```java
    // T を消費し、int を生産する
    int compareTo(T o);
```
  - したがって、`Comparable<T>` の部分は、`Comparable<? super T>` で置き換えられる。

- この修正がなされると、以下のようなリストで `max` メソッドが使える。

```java
List<ScheduledFuture<?>> scheduledFutures = ... ;
```

- `ScheduledFuture` は `Comparable<ScheduledFuture>` を直接は実装しない
- その代わり、`ScheduledFuture` は、 `Comparable<Delayed>` を実装している `Delayed` のサブインターフェイスである
- 言い換えるならば、`ScheduledFuture` は 他の `ScheduledFuture` との比較はできないが、
`Delayed` との比較は可能であり、これで十分である。

- `List<ScheduledFuture<?>> ` のようにワイルドカード型が必要となるのは、`Comparable` を直接実装していないが、
それを実装している型のサブタイプをサポートする場合である。

### ワイルドカード型のキャプチャ

- 型パラメータとワイルドカードには二重性があり、多くのメソッドはどちらかを使って宣言できる
- 例えば、リストの2つの要素を交換する static メソッドは以下のように書ける

```java
public static <E> void swap(List<E> list, int i, int j);
public static void swap(List<?> int i, int j);
```

- これら2つの宣言のうち、public の API には2つ目の方が単純であり好ましい
- 一般に、型パラメータがメソッド宣言中に一度しか現れないなら、それをワイルドカードで置き換えるべきである
  - 非境界型パラメータは非境界ワイルドカードに、
  - 境界型パラメータは境界ワイルドカードに置き換える

- ワイルドカードを使っている2つ目の `swap` の宣言には一つ問題がある
  - 素直に実装するとコンパイルが通らない

```java
public static void swap(List<?> list, int i, int j) {
    list.set(i, list.set(j, list.get(i)));
}
```

- 以下のようなあまり役に立たないエラーメッセージが出力される。

```
Hoge.java:5: エラー: 不適合な型: ObjectをCAP#1に変換できません:
        list.set(i, list.set(j, list.get(i)));
                                        ^
  CAP#1が新しい型変数の場合:
    CAP#1は?のキャプチャからObjectを拡張します
```

- 直感的には、要素を取り出した元々のリストに戻せないのは正しくないように思える。問題は、以下の二つである。
  1. `list` の型が `List<?>` であることと、
  2. `List<?>` には `null` しか挿入できないこと
- ワイルドカード型を **キャプチャ（捕捉: capture）** する private のジェネリックなヘルパーメソッドを書くと解決する

```java
public static void swap(List<?> list, int i, int j) {
    swapHelper(list, i, j);
}

public static <E> void swapHelper(List<E> list, int i, int j) {
    list.set(i, list.set(j, list.get(i)));
}
```

- こうすることで、 `List<?>` の `?` の型を `swapHelper` では `E` という型変数として捕捉できる
- そのリストから取り出した要素が `E` 型であり、`E` 型のリストに戻すのは安全であることがわかっているため、エラーも警告もなくコンパイルできる。

- こうすると、ワイルドカードを用いたきれいな宣言のみを公開でき、やや複雑なジェネリックメソッドを内部に隠すことができる
- ちなみに、`swapHelper` は 2つあった`swap`の候補のうち、複雑なので選択しなかった方のメソッドと正確に同じシグネチャを持っている

### まとめ
- ワイルドカード型の使用はやや難しいが、APIを柔軟にする
- 広く使われるライブラリを使うならば、ワイルドカード型を適切に使用することは必須である
- 基本原則: producer-extends, consumer-super (PECS)
- 全ての比較可能なオブジェクトとコンパレータはコンシューマである
- ちなみに、kintone のコードには `<? extends E>` はよく見かけるが、`<? super E>` はやはりほとんどなく、あったとしてもだいたい `Comparable` についていた。

## 項目32 ジェネリックスと可変長引数を注意して組み合わせる
- 可変長引数メソッド（項目53）とジェネリックスは、Java 5で同時に追加されましたがうまく協調しない。

- 可変長引数の目的は、クライアント側からメソッドに可変長の引数を渡すことだが、
これは **漏出抽象化 (leaky abstraction)** である
  - ちなみに "漏出抽象化"でググっても一件しかヒットしない

- 可変長引数メソッドを呼び出したとき、可変長のパラメータを保存するための配列が生成される
  - この配列というのが実装の詳細であるべきだが、見えてしまっている
- その結果、可変長パラメータがジェネリック型もしくはパラメータ化された型であった場合、
非常にわかりにくいコンパイラの警告が表示される

### 具象化不可能型
- **具象化不可能型 （non-reifieable type）** は、実行時の表現がコンパイル時よりも情報が少ない型であり、
ほとんど全てのジェネリック型、パラメータ化された型は具象化不可能である（項目28）
- 具象化不可能型を可変長パラメータとして宣言すると、宣言する側も、呼び出す側もコンパイラからの警告を受ける。

### ヒープ汚染
- **ヒープ汚染 (heap pollution)** は、パラメータ化された型の変数が、その型ではないオブジェクトを参照する場合に発生する
- これはコンパイラが生成したキャストが失敗する可能性をもたらし、型システムの保証を破る

- 項目28のコードを改変した次のメソッドを考える

```java
static void dangerous(List<String>... stringLists) {
    List<Integer> intList = List.of(42);
    Object[] objects = stringLists;     
    objects[0] = intList;               // ここでヒープ汚染が発生する
    String s = stringLists[0].get(0);   // ClassCastException
}
```

- ジェネリックの可変長配列パラメータに値を保存するのは安全ではない
- しかし、このコードはエラーが発生せず、警告のみしか生成されない
  - これは、ジェネリック型およびパラメータ化された型の可変長引数は実際に役に立つからである
- 実際に Java のライブラリには、型安全な以下のメソッドが存在する。

```java
Arrays.asList(T... a);
Collections.addAll(Collection<? super T> c, T... elements);
EnumSet.of(E first, E... rest);
```

### `SafeVarargs` アノテーション
- Java 7 より前にはジェネリック可変長パラメータの警告を抑制するには、
呼び出し元で逐一 `@SuppressWarnings("unchecked")` をする必要があった

- Java 7 では、 `@SafeVarargs` アノテーションが追加された。
  - これにより、ジェネリック可変長パラメータを持つメソッドの作成者によって、
利用者側の警告を抑制できるようになった。

- `@SafeVarargs` アノテーションによって、メソッドの作成者は利用者に対して、
そのメソッドが型安全であることを約束する
  - その代わりに、コンパイラは型安全ではない可能性のあるメソッド呼び出しをユーザに警告しなくなる

- 実際に安全ではない限り、`@SafeVarargs` アノテーションを付けてはならない。
型安全であるのは、以下の場合である
    1. メソッドにおいて、可変長パラメータの配列に値を上書きしないこと
    2. 可変長パラメータの配列の参照を、信頼できない他のコードへ渡さないこと
- 基本的には、可変長パラメータ配列は、呼び出し元から可変長の引数を渡すためだけに使われているならば安全である。

### 注意すべき反例
- 可変長パラメータ配列に値を上書き保存しない、というだけでは型安全性を主張するのに不十分である
- 以下は、可変長パラメータの配列を単純に返す危険な例である。

```java
static <T> T[] toArray(T... args) {
    return args;
}
```

- この配列の型は、メソッドへ渡された引数のコンパイル時の型で決定される
- しかし、コンパイラは正確な決定を行うための十分な情報を持っていない可能性がある
- したがって、可変長パラメータの配列を返すことにより、呼び出し元へヒープ汚染を伝搬させてしまう
- 具体例を示すために、次の例を見る

```java
static <T> T[] pickTwo(T a, T b, T c) {
    switch(ThreadLocalRandom.current().nextInt(3)) {
        case 0: return toArray(a, b);
        case 1: return toArray(b, c);
        case 2: return toArray(c, a);
    }
    throw new AssertionError();
}
```

- このメソッドをコンパイルすると、コンパイラは `toArray` に二つの`T`のインスタンスを渡すため、
可変長パラメータの配列を生成するコードを生成する
- このとき、`T` の型に関係なく、`Object` の配列を可変長パラメータとして生成するコードが生成される
- その結果として、 `pickTwo` は常に `Object` の配列を返す

```java
public static void main(String[] args) {
    String[] attributes = pickTwo("Good", "Fast", "Cheap");
}
```

- このメソッドは警告なしにコンパイルされるが、`ClassCastException` が発生する
- `pickTwo` の呼び出しの直後に、`String[]` への見えないキャストが挿入されている
- このことは不可解に思える
    -  このメソッドから、実際にヒープ汚染を発生させた `toArary` とは2レベル離れている
    - 可変長パラメータが修正されたわけではない

- 基本的に、他のメソッドにジェネリック可変長パラメータの配列にアクセスさせることは安全ではない
    - 例外1. `@SafeVarargs` が正しくついているメソッドに可変長引数として渡すこと
    - 例外2. 配列の内容から何かしらの計算をするだけの、可変長ではないメソッドへ渡すこと

### 安全な使い方の例
```java
@SafeVarargs
static <T> List<T> flatten(List<? extends T>... lists) {
    List<T> result = new ArrayList<>();
    for (List<? extends T> list : lists) {
        result.addAll(list);
    }
    return result;
}
```

- ジェネリック可変長引数メソッドが安全である場合は `@SafeVarargs` アノテーションは常に使うこと
- 以下の2つを満たす場合は安全である
    1. 可変長パラメータ配列に何も保存しない
    2. 信頼できないコードに対して、可変長パラメータ配列を参照できるようしていない
- `@SafeVarargs` アノテーションは、オーバーライドできないメソッドに対してのみ付けられる
    - Java 8 では、 static メソッドと finalのインスタンスメソッドに対してのみ許されていた
    - Java 9 では、privateのインスタンスメソッドにも付けられる
- `@SafeVarargs` アノテーションを使わない方法として、可変長パラメータを `List` で置き換えることがあげられる
```java
static <T> List<T> flatten(List<List<? extends T>> lists) { ... }
```
- すると、可変長引数の代わりに、ひとつのリストを渡すことになる。 例えば、 `flatten(List.of(a, b, c))` のように
- この方法は、`List.of` に `@SafeVarargs` アノテーションがつけられているという事実に基づく
- 長所：`@SafeVarargs` アノテーションを人の手でつける代わりに、コンパイラが型安全性を保証できる
- 短所：若干冗長であり、パフォーマンスが落ちること

### まとめ
- 可変長引数とジェネリックスはうまく協調しない
- 可変長引数は配列に基づいた leaky abstraction である
    - 配列とジェネリクスは異なる型規則を持っている
- ジェネリック可変長パラメータは型安全ではないが実用上は許されている
    - まず型安全であることを示し、`@SafeVarargs` アノテーションをつけること




