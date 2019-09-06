# 第5章 ジェネリックス
ジェネリクスはJava 5 以降にJava に導入された。
ジェネリックスより前では、コレクションからは読みだした全てのオブジェクトをキャストする必要があった。
ジェネリックスを使うと、コンパイラがキャストを自動的に挿入するため、
誤った型のオブジェクトを挿入しようとするとコンパイル時にエラーとなりみんなハッピーになる。

## 項目26 原型を使わない
Effective Javaの解説と、[Java Language Specification](https://docs.oracle.com/javase/specs/jls/se12/html/jls-4.html#jls-4.5) の用語の説明がやや異なってるため、
用語の定義はふわっと行う。

### 定義
* **ジェネリック**クラス、**ジェネリック**インターフェイス:\
一つ以上の型パラメータを受け取る型変数を宣言に持つクラスおよびインターフェイス。総称して**ジェネリック型** という。
* **型パラメータ**:\
宣言において型を受け取る引数。例えば `List<E> { ... }` における `E`。

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

非境界ワイルドカード型は型安全である。原型のコレクションにはどのような要素も挿入できるが、非境界ワイルドカード型のコレクションにはには、(null以外の) **要素が挿入**できない。
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

### 1. 配列は*共変*
一つ目は、配列は*共変*だが、ジェネリクスは*不変*であること。

これは、`Sub` が `Super` のサブタイプならば、 `Sub[]` は `Super[]` のサブタイプだと言うことを意味する（*共変*）。
一方、`List<Sub>` は `List<Super>` のサブタイプでもスーパータイプでもない（*不変*）。
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
