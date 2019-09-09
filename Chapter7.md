# ラムダとストリーム

## 項目42 無名クラスよりもラムダを選ぶ
単一の抽象メソッドを持つインターフェースは関数型として使われてきた。
それらのインスタンスである関数オブジェクトを作成する手段は無名クラスだった。

無名クラスの例
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

長さ順に文字列のリストをソートするコードの例（無名クラス）
```java
Collections.sort(words, new Comparator<String>() {
    @Override
    public int compare(String s1, String s2) {
        return Integer.compare(s1.length(), s2.length());
    }
});
```

無名クラスはStrategyパターン（項目95）に対しては適切だった。Comparatorインターフェースはソートのための abstract strategy を表しており、無名クラスは concrete strategy を表している。

無名クラスは冗長なので、Java8では単一の抽象メソッドを持つインターフェースを関数型インターフェースとし。ラムダ式を使ってこれらのインタフェースをインスタンス化できるようにした。


長さ順に文字列のリストをソートするコードの例（ラムダ式）
```java
Collections.sort(words, (s1, s2) -> Integer.compare(s1.length(), s2.length()));
```

ラムダ式では型推論が使える。

以下の場合は型を明示的に書く
- 型を明示してプログラムが読みやすくなるとき
- コンパイラが型を推論できないとき
- 戻り値をキャストする必要があるとき



