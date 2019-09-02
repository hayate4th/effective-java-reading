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
渓谷を取り除くことができなくても、警告を起こしているコードが型安全だと明確に示すことができれば、そのときかつそのときに限り `@SuppressWarnings("unchecked")` アノテーションで警告を抑制すること。

なぜなら最初にコードが型安全であることを示すことなく警告を抑制すると、誤った安心感を持たせるだけであるし、
安全だとわかっている無検査警告を抑制せずに無視したら、本当の問題を示す新たな警告が出たときに埋もれてしまう可能性があるからである。

`SuppressWarnings` アノテーションはローカル変数の宣言からクラス全体まで、どのような宣言でも使えるが、できるだけ最小のスコープに対して使うこと（例：変数宣言、短いメソッド、短いコンストラクタなど）。
クラス全体にこのアノテーションを使うと重大な渓谷を隠匿する可能性がある。

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
*　無検査警告を取り除く最善の努力を行うこと。
* 取り除くことができない場合、型安全であると明確に示せる場合、`@SuppressWarnings("unchecked")` を最小のスコープに対して使うことで渓谷を抑制すること。
* 渓谷を抑制する場合、そう決めた理由をコメントに書き残すこと。


