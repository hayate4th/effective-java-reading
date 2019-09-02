# クラスとインタフェース

## 項目15 クラスとメンバーへのアクセス可能性を最小限にする

良い設計のコンポーネントとは？

- → その実装の詳細をすべて隠蔽し、実装と API をはっきりと分離している
- → 情報隠蔽（information hiding）、カプセル化（incapsulation）

情報隠蔽をすることで、システムを構成するコンポーネントを効果的に分離し、個別かつ並行して開発、テスト、最適化、利用、理解、修正することができる

各クラスをできる限りアクセスできないようにするべき

### トップレベルのクラスやインタフェースに指定できるアクセスレベル

- package-private（デフォルト）
- public

トップレベルの package-private のクラスまたはインタフェースが一つのクラスだけで使われているなら、その使用しているクラス内にネストされた private のクラスにすることを検討する（項目24）。

### フィールド、メソッド、ネストしたクラスやインタフェースに指定できるアクセスレベル

- private … これが宣言されたトップレベルのクラス内でのみアクセス可
- package-private（インタフェースのメンバー以外のデフォルト） … これが宣言されたパッケージ内のどのクラスからでもアクセス可
- protected … pakcage-private + これが宣言されたクラスのサブクラス
- public（インタフェースのメンバーのデフォルト） … どこからでもアクセス可

### とりあえず private にする

クラスの public な API をちゃんと設計したら、とりあえず他の全てのメンバーを private にする。パッケージ内の他クラスからのアクセスが必要な場合だけ private を外す（package-private になる）。頻繁に private を外しているなら、もっとうまく分離できないか設計を見直す。

とはいえ、private と package-private はどちらもクラスの公開 API に影響を与えない。しかし、クラスが Serializable（項目86, 項目87）を実装していれば、private や package-private なフィールドでも公開 API に漏洩してしまう。

### protected の扱いには要注意

public クラスのメンバーが package-private から protected に変更された場合、大幅にアクセス可能性が増大する。protected はあまり使いすぎないほうが良い。

### オーバーライド時のアクセスレベル

スーパークラスのメソッドをオーバーライドする場合、元のメソッドよりも低いアクセス権を設定することはできない（コンパイルエラーになる）。これは、リスコフの置換原則（項目10 参照）を満たす必要があるため。
また、インタフェースを実装する場合、そのクラスのメンバーは全て public として実装しなければならない。

### テストのためにアクセスレベルを緩めたいとき

テストのために public クラスのメソッドを package-private にするのは OK。

それ以上緩めるのは NG。テストのために公開 API の一部となるのはダメ。  
パッケージ内にテストを書けば package-private の要素にアクセスできるので、その必要はない。

### インスタンスフィールドは public にすべきではない (項目16)

final ではないインスタンスフィールドや可変オブジェクトへの参照であるインスタンスフィールドを public にした場合、そのフィールドに保存できる値を制限できなくなる。つまり、そのフィールドに関係する不変式の強制（？）やフィールドが変更されたときに何らかの処理を行うことができなくなる。このような理由から、public の可変フィールドを持つクラスは一般的にスレッドセーフでない。

また、不変オブジェクトを参照している final のフィールドを public にする場合、そのフィールドを今とは違う表現へ変更することが難しくなる。

### static フィールドも public にすべきではない。ただし、定数フィールドは可

static フィールドも同様に public にすべきではないが、定数は例外的に public static final にできる。これは、定数がクラスが提供する抽象化の不可欠な部分を構成するという仮定があるためである。定数フィールドは大文字で単語間をアンダースコアで区切った名前が付けられる(項目68)。

定数フィールドは基本データ型の値か不変オブジェクトへの参照のどちらかしか持たないようにする。変更可能オブジェクトへの参照を持つ final のフィールドは、その参照自体を変更することはできないが参照先のオブジェクトを変更できるため、final でないフィールドの欠点を全て持っている。

長さがゼロではない配列の内容は常に変更可能なので、クラスが public static final の配列フィールドやそのようなフィールドを返すアクセッサーを持つのは誤りである。

例えば、次のコードはセキュリティホールを持っている。

```
public static final Thing[] VALUES = {...};
```

これを解決する方法は2つある。どちらを使うかは「どちらの戻り値が便利か」「どちらのパフォーマンスが良いか」によって決める。

#### 解決法1: 配列を private にして、public の変更不可能なリストを作る

```
private static final Thing[] PRIVATE_VALUES = {...};
public static final List<Thing> VALUES = Collections.unmodifiableList(Arrays.asList(PRIVATE_VALUES));
```

#### 解決法2: 配列を private にして、そのコピーを返す public メソッドを作る

```
private static final Thing[] PRICATE_VALUES = {...};
public static final Thing[] values() {
    return PRIVATE_VALUES.clone();
}
```

### module のアクセスレベル

- パッケージ … クラスのグループ化
- モジュール … パッケージのグループ化

モジュールは、モジュール宣言内のエクスポート宣言によって一部のパッケージを明示的に公開する。

- エクスポートされたパッケージ … モジュール外から public や protected のメンバーにアクセスできる
- エクスポートされていないパッケージ … モジュール外から public や protected のメンバーにアクセスできない

ただし、モジュールに基づくアクセスレベルは勧告的なものである。例えば、モジュールの JAR ファイルをアプリケーションのモジュールパスではなくクラスパスに配置すると、モジュール内のパッケージはモジュールに対応していない振る舞いになる。

唯一 JDK 自身が持つ Java ライブラリにおけるモジュールのアクセスレベルは厳格に強制される。

以上のような理由から、JDK 自身以外でモジュールが広く使われていくかはわからないため、やむを得ない必要がない限りはモジュールを使うのは避けたほうが良い。

### まとめ

- 常にプログラムの要素のアクセス可能性をできる限り（理にかなった範囲で）低減させるべき
- 最低限の public の API を注意深く設計した後は、関係のないクラス、インタフェース、メンバーがその API の一部となるのを防ぐべき
- 定数としての役割を果たす public static final のフィールドの例外はあるが、public のクラスは public のフィールドを持つべきではない
- public static final のフィールドで参照されているオブジェクトが変更不可であるようにする

## 項目16 public のクラスでは、public のフィールドではなく、アクセッサーメソッドを使う

```
class Point {
    public double x;
    public double y;
}
```

このようなクラスのデータフィールドは直接アクセスされるので、カプセル化できていない。具体的には、

- API を変更せずに、その表現形式を変更できない
- 不変式を強制できない
- フィールドが変更されたときに何らかの補助処理ができない

という問題がある。

### アクセッサーメソッドとミューテーターを使う

フィールドを private 化し、アクセッサーメソッド（ゲッター）とミューテーター（セッター）を追加する。

```
class Point {
    private double x;
    private double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() { return x; }
    public double getY() { return y; }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
}
```

### アクセッサーメソッドとミューテーターを使う必要がない場合

クラスがパッケージの外からアクセスされない package-private や private のネストしたクラスである場合、データフィールドを直接公開することは問題ではない。（むしろ、クラス定義及びクラスを使っている場所の両方で見やすいコードになる。）

Java プラットフォームライブラリのクラスによっては、public のクラスはフィールドを直接公開しているものがある。例えば、 java.awt パッケージ内の Point クラスと Dimension クラスはフィールドを直接公開している。これは、真似すべき例ではない。項目67で説明されているように、Dimension クラスの内部を公開すると決めたことで、今日も存在する重大なパフォーマンス問題を生み出した。

### 不変フィールドを直接公開する

public クラスのフィールドを直接公開するのは良くないが、不変フィールドであれば害は少ない。

## 項目17 可変性を最小限にする

不変クラスとは、個々のインスタンスに保持される情報の全てがオブジェクトの生存期間中において変化しないクラスである。

Java プラットフォームライブラリには String、ボクシングされた基本データクラス、BigInteger、BigDecimal 等、多くの不変クラスがある。これは、不変クラスの設計、実装、使用が可変クラスよりも容易で、誤りにくく安全であるためである。

### 不変クラスを作るための規則

#### 1. オブジェクトの状態を変更するためのメソッドを提供しない

そのまま。

#### 2. クラスを拡張できないようにする

サブクラスが、オブジェクトの状態が変更されたかのように振る舞うことを防ぐ。

サブクラス化を防ぐには一般にクラスを final とするが、後で紹介する別の方法もある。

#### 3. すべてのフィールドを final にする

もし新たに生成されたインスタンスへの参照が一つのスレッドから他のスレッドへ同期なしで渡されるなら、メモリモデルで説明されているように、正しい振る舞いを保証する必要がある。

#### 4. すべてのフィールドを private にする

クライアントがフィールドから参照されている可変オブジェクトへアクセスして、そのオブジェクトを直接変更することを防ぐ。

不変クラスが public final フィールドを持つことは技術的に問題ないが、その後のリリースで内部表現を変更できなくなるので推奨されない（項目15、項目16）。

#### 5. 可変コンポーネントに対する独占的アクセスを保証する

クラスが可変オブジェクトを参照するフィールドを持っているなら、クライアントがそれらのオブジェクトへの参照を取得できないようにする必要がある。コンストラクタ、アクセッサー、readObject メソッド（項目88）で防御的コピー（項目50）をする。

### 関数的方法を用いる

```
public final class Complex {
    private final double re;
    private final double im;

    public Complex(double re, double im) {
        this.re = re;
        this.im = im;
    }

    public double realPart() { return re; }
    public double imaginaryPart() { return im; }

    public Complex plus(Complex c) {
        return new Complex(re + c.re, im + c.im);
    }

    public Complex minus(Complex c) {
        return new Complex(re - c.re, im - c.im);
    }

    public Complex times(Complex c) {
        return new Complex(re * c.re - im * c.im, re * c.im + im * c.re);
    }

    public Complex dividedBy(Complex c) {
        double tmp = c.re * c.re + c.im * c.im;
        return new Complex((re * c.re + im * c.im) / tmp, (im * c.re - re * c.im) / tmp);
    }

    @Override public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Complex))
            return false;
        Complex c = (Complex) o;

        // == の代わりに compare を使う理由については49ページを参照
        return Double.compare(c.re, re) == 0 && Double.compare(c.im, im) == 0;
    }

    @Override public int hashCode() {
        return 31 * Double.hashCode(re) + Double.hashCode(im);
    }

    @Override public String toString() {
        return "(" + re + " + " + im + "i)";
    }
}
```

上の Complex クラスでは、算術操作がインスタンスを変更するのではなく、関数的方法を用いて新たな Complex インスタンスを生成して返している。一般的な手続き的または命令的方法と比較すると、add のような動詞ではなく plus のような前置詞がメソッド名に使われており、これによってメソッドがオブジェクトの値を変更しないことを強調している。BigInteger クラスと BigDecimal クラスはこの命名規約に従っておらず、多くの誤用を引き起こした。

### 不変オブジェクトの共有

可変オブジェクトが任意の複雑な状態空間を持つ一方で、不変オブジェクトは必ず作られたときの状態を維持するので単純である。また、不変オブジェクトは本質的にスレッドセーフなので、同期を必要としない。これにより、不変クラスは制限なく共有することができる。

#### 定数として共有する

例えば、Complex クラスは頻繁に使われる値を次のように定数として提供することができる。

```
public static final Complex ZERO = new Complex(0, 0);
public static final Complex ONE = new Complex(1, 0);
public static final Complex I = new Complex(0, 1);
```

#### キャッシュして共有する

さらに発展して、既存のインスタンスを再利用できる場合に新たなインスタンスを避けるため、頻繁に利用されるインスタンスをキャッシュする static ファクトリメソッドを提供することもできる（項目1）。全てのボクシングされた基本データクラスと BigInteger は、このような static ファクトリメソッドを持っている。新たにクラスを設計する際に public のコンストラクタの代わりに static ファクトリメソッドを選択することで、クライアントを修正せずに後でキャッシュを追加する柔軟性を持たせられる。

不変オブジェクトは制限なく共有できるため、防御的コピー（項目50）を行う必要がない。コピーはもとのインスタンスと永久に同値なのでコピーを行う必要はない。よって、不変クラスに対して clone メソッドやコピーコンストラクタ（項目13）を提供する必要はないし、提供すべきではない。Java プラットフォームの初期にはこのことが十分に理解されていなかったため String クラスはコピーコンストラクタを持っているが、めったに使用されるべきではない（項目6）。

#### 内部を共有する

不変オブジェクトはそれ自体を共有できるだけでなく、その内部も共有できる。

例えば、BigInteger クラスでは内部的に「符号 - 大きさ」を使っており、符号は int、大きさは int 配列で表現されている。negate メソッドは同じ大きさで符号を反転させた新たな BigInteger を生成する。この際、negate メソッドは大きさを表す int 配列をコピーする必要がない。

### 不変オブジェクトの利点

- 他のオブジェクトに対する素晴らしい構成要素を作り出す（？）
- 追加コストなしでエラーアトミック性を提供する（項目76）

### 不変クラスの欠点

不変クラスの唯一の欠点は、個々のオブジェクトを必要とすることである。

例えば、100万ビットから成る BigInteger があるとして、その最下位ビットを反転させる場合、flipBit メソッドはもとのインスタンスとたった1ビットしか違わない100万ビット長の新たな BigInteger インスタンスを生成する。この操作にはその大きさに比例した時間と空間が必要になる。

一方、可変クラスである java.util.BigSet は BigInteger と同様に任意長のビット列を表現しているが、一定時間で100万ビット中の1ビットの状態を変更することができる。

複数ステップの操作を行う際、各ステップで新たなオブジェクトを生成し、最終的に最後の結果以外のオブジェクトを破棄する場合、パフォーマンスの問題は大きくなる。

#### 解決策1: 複数ステップの操作をまとめる

どのような複数ステップの操作が一般的に要求されるかを推測して、それをまとめたメソッドを提供することでパフォーマンスを改善できる。これは、クライアントがどのような操作を行いたいかを正確に予想できればうまくいく。

例えば、BigInteger はべき剰余などの複数ステップの操作のスピードを上げるために、パッケージプライベートの可変「コンパニオンクラス」を持っている。

#### 解決策2: public の可変コンパニオンクラスを提供する

クライアントがどのような操作を行いたいか正確に予想できない場合、public の可変コンパニオンクラスを提供することでパフォーマンスを改善できる。

例えば、String クラスはその可変コンパニオンクラスである StringBuilder（と今は使われなくなった StringBuffer）を提供している。

### 不変クラスの設計

前述のとおり、不変性を保証するためにはサブクラス化を許してはならない。これはクラスを final にすることで実現できるが、代わりにそのクラスのコンストラクタを全て package-private または private として、public のファクトリメソッドを追加することでも実現できる（項目1）

```
public class Complex {
    private final double re;
    private final double im;

    private Complex(double re, double im) {
        this.re = re;
        this.im = im;
    }

    public static Complex valueOf(double re, double im) {
        return new Complex(re, im);
    }

    // 残りは変更なし
}
```

複数のパッケージプライベートの実装クラスを使うことができるため、この方法の方がより柔軟性がある。また、public あるいは protected のコンストラクタを持たないクラスを別のパッケージから拡張することは不可能であるため、このクラスは実質的に final であると言える。

さらに、この方法には static ファクトリメソッドのオブジェクトキャッシュ能力を後のリリースで改善することで、クラスのパフォーマンスをチューニングする余地もある。

##### BigInteger と BigDecimal は実質的に final でない

BigInteger と BigDecimal が書かれた当時、不変クラスは実質的に final でなければならないことが広く理解されていなかったため、メソッドは全てオーバーライド可能になってしまっている。残念ながら、後方互換性を保ちながらこれを修正することはできていない。

そのため、信頼できないクライアントから引数として受け取った BigInteger または BigDecimal が不変であることを期待するなら、その引数が「本物の」BigInteger か BigDecimal であるか検査しなければならない。もしそれが信頼できないサブクラスのインスタンスならば、それを防御的にコピーしなければならない（項目50）。

```
public static BigInteger safeInstance(BigInteger val) {
    return val.getClass() == BigInteger.class ? val : new BigInteger(val.toByteArray());
}
```

### final でないフィールドを許容する場合

この項目の最初で、不変クラスのどのメソッドもオブジェクトを変更しないし、全てのフィールドは final でなければならないと述べたが、これらの規則は必要以上に厳しい。つまり、外部に分からない変更であれば許容しても良い。

不変クラスによっては、コストが高く付く計算の結果をその計算が初めて要求されたときにキャッシュするための final でない冗長なフィールドを持っている。オブジェクトが不変であるため、計算が再び行われたとしても同じ結果になることが保証される。

例えば、PhoneNumber の hashCode メソッド（項目11、55ページ）は最初に呼び出されたときにハッシュコードを計算して、再び必要になったときのためにキャッシュしている。このような方法は遅延初期化（項目83）と呼ばれ、String クラスでも使われている。

### 不変クラスで Serializable を実装する際の注意事項

もし不変クラスが Seriarizable を実装していて、そのクラスが可変オブジェクトを参照しているフィールドを持っていたら、たとえデフォルトのシリアライズ形式が受け入れられるものであっても、明示的に readObject メソッドか readResolve メソッドを提供するか、ObjectOutputStream.writeUnshared メソッドと ObjectInputStream.readUnshared メソッドを使わなければならない。そうしなければ攻撃者はクラスの可変インスタンスを生成できる。これについては項目88で詳細に説明する。

### まとめ

- 可変にすべき正当な理由がない限り、クラスは不変であるべき
- 特に小さな値オブジェクトは常に不変にすべき（java.util.Date や java.awt.Point などは不変とすべきだったが、不変になっていない）
- 

## 項目18 継承よりもコンポジションを選ぶ

継承が常に再利用のための最善の方法であるとは限らない。

以下のような場合には、継承を安全に使うことができる。

- サブクラスとスーパークラスの実装が同じプログラマの管理下にある場合
- 拡張のために設計されて、かつ拡張のために文書化されているクラスを拡張する場合（項目19）

一方、パッケージをまたがって普通の具象クラスから継承することは危険である。

### この項目の注意事項

この本では、（あるクラスが他のクラスを拡張した場合の）実装継承の意味で「継承」を使う。本項目で議論される問題は（クラスがインタフェースを実装した場合や、インタフェースが他のインタフェースを拡張した場合の）インタフェース継承には適用されない。

### 不適切な継承の例

#### オーバーライドするメソッドの実装に依存する場合

メソッド呼び出しと異なり、継承はカプセル化を破る。つまり、サブクラスはスーパークラスの実装の詳細に依存する。スーパークラスの実装はリリースごとに変更されるかもしれないし、もし変更されたらサブクラスはコードが一切変更されていなくても動かなくなるかもしれない。

例えば、HashSet が生成されてからいくつの要素が追加されたかを知るため、HashSet を継承して要素の挿入回数を記録するクラスを作るとする。HashSet クラスは要素を追加するために add と addAll の2つのメソッドを持っているので、それぞれを次のようにオーバーライドする。

```
// 継承の不適切な利用の例
public class InstrumentedHashSet<E> extends HashSet<E> {
    // 要素が挿入された回数
    private int addCount = 0;

    public InstrumentedHashSet() {}

    public InstrumentedHashSet(int intCap, float loadFactor) {
        super(initCap, loadFactor);
    }

    @Override public boolean add(E e) {
        addCount++;
        return super.add(e);
    }

    @Override public boolean addAll(Collection<? extends E> c) {
        addCount += c.size();
        return super.addAll(c);
    }

    public int getAddCount() {
        return addCount;
    }
}
```

このクラスは妥当に見えるが、正しく動作しない。

```
InstrumentedHashSet<String> s = new InstrumentedHashSet<>();
s.addAll(List.of("Snap", "Crackle", "Pop"));
s.getAddCount(); // 6
```

これは、HashSet の addAll メソッドが内部で add メソッドを使っているためである。このため、addAll メソッドを用いて追加された要素が重複してカウントされてしまった。

addAll メソッドのオーバーライドをやめることでこのサブクラスを一時的に修理することはできるが、これは HashSet の addAll メソッドが add メソッドを使って実装されているという事実に依存してしまっている。これは Java プラットフォームの全ての実装で行われる保証はないし、リリースごとに変更される可能性もあるため、InstrumentedHashSet クラスは脆弱になってしまう。

上の方法に比べると、指定されたコレクションをイテレートして、個々の要素に対して add メソッドを呼び出すように addAll メソッドをオーバーライドする方法の方が多少マシである。ただしこれは、自己利用しているかわからないスーパークラスのメソッドを再実装しているため、実装が困難で誤りやすく、パフォーマンスの低下を引き起こすかもしれない。さらに、メソッドによってはサブクラスがアクセスできない private フィールドへのアクセスなしでは実装できない場合があるため、この方法が常に可能だとは限らない。

```
public class InstrumentedHashSet<E> extends HashSet<E> {
    // 省略
    
    @Override public boolean add(E e) {
        addCount++;
        return super.add(e);
    }

    @Override public boolean addAll(Collection<? extends E> c) {
        for(E e: c) {
            this.add(e);
        }
    }

    // 省略
}
```

#### 将来のリリースで追加されるメソッドによって不正な値が入ってしまう可能性

仮に要素の追加を行う全てのメソッドをオーバーライドしてうまく動作するようになったとしても、将来のリリースで要素を追加するためのメソッドが新たに追加された場合、サブクラスのインスタンスに不正な要素を追加できるようになってしまう。実際に Hashtable と Vector をコレクションフレームワークのいち員とするために修正した際、同様のセキュリティホールを塞がなければならなかった。

#### 将来のリリースで追加されるメソッドと名前が衝突する可能性

上記の問題はいずれもメソッドをオーバーライドしたことで起こっているが、メソッドをオーバーライドせず単にメソッドを追加した場合でも問題が起こる可能性がある。

もし、後のリリースでスーパークラスに新たなメソッドが追加され、サブクラスにも同じシグニチャで異なる戻り値型のメソッドを定義していたら、サブクラスはコンパイルできなくなる。また、サブクラスに同じシグニチャで同じ戻り値型のメソッドをていぎしていたら、スーパークラスのメソッドをオーバーライドしていまい前述のような問題を引き起こす。さらに、サブクラスのメソッドが書かれた時点でスーパークラスの新たなメソッドの契約は書かれていないので、その契約を満たすかは疑わしい。

### コンポジションを使う

既存のクラスを拡張する代わりに、新たなクラスに既存クラスのインスタンスを参照する private フィールドを持たせることで、前述の問題を全て避けることができる。これをコンポジションという。

新たなクラスの各インスタンスメソッドは、保持している既存クラスのインスタンスに対して対応するメソッドを呼び出して、その結果を返す。これを転送と言い、これを行うメソッドを転送メソッドと言う。

転送は既存クラスの実装の詳細に依存しない上、既存クラスに新たなメソッドが追加されたとしても影響がない。

InstrumentedHashSet をコンポジション/転送を使って書き換えると次のようになる。

```
// ラッパークラス - 継承の代わりにコンポジションを使っている
public class InstrumentedSet<E> extends ForwardingSet<E> {
    private int addCount = 0;

    public InstrumentedSet(Set<E> s) {
        super(s);
    }

    @Override public boolean add(E e) {
        addCount++;
        return super.add(e);
    }

    @Override public boolean addAll(Collection<? extends E> c) {
        addCount += c.size();
        return super.addAll(c);
    }

    public int getAddCount() {
        return addCount;
    }
}

// 再利用可能な転送クラス
public class ForwardingSet<E> implements Set<E> {
    private final Set<E> s;
    public ForwardingSet(Set<E> s) { this.s = s; }

    public void clear() { s.clear(); }
    public boolean contains(Object o) { return s.contains(o); }
    public boolean isEmpty() { return s.isEmpty(); }
    public int size() { return s.size(); }
    public Iterator<E> iterator() { return s.iterator(); }
    public boolean add(E e) { return s.add(e); }
    public boolean remove(Object o) { return s.remove(o); }
    public boolean containsAll(Collection<?> c) { return s.containsAll(c); }
    public boolean addAll(Collection<? extends E> c) { return s.addAll(c); }
    public boolean removeAll(Collection<? extends E> c) { return s.removeAll(c); }
    public boolean retainAll(Collection<? extends E> c) { return s.retainAll(c); }
    public Object[] toArray() { return s.toArray(); }
    public <T> T[] toArray(T[] a) { return s.toArray(a); }
    @Override public boolean equals(Object o) { return s.equals(o); }
    @Override public int hashCode() { return s.hashCode(); }
    @Override public String toString() { return s.toString(); }
}
```

InstrumentedSet クラスの設計は、HashSet クラスの機能を表す Set インタフェースが存在するため可能となっている。

コンポジション/転送を用いた InstrumentedSet クラスは頑強であると共に柔軟性を持っている。継承を用いた方法では、スーパークラスで提供されるコンストラクタごとに別々のコンストラクタを必要とするが、この方法では一つのコンストラクタがあらゆる Set 型を受け取り、計測機能を持った Set 型に変換するため、既存のどのコンストラクタに対してもうまく対応できる。また、このラッパークラスは HashSet を含むどの Set の実装を計測するためにも使用することができる。

```
// 様々な Set の実装に対応している例
Set<Instant> times = new InstrumentedSet<>(new TreeSet<>(cmp));
Set<E> s = new InstrumentedSet<>(new HashSet<>(INIT_CAPACITY));
```

次のように、コンポジション/転送を用いた InstrumentedSet クラスは、計測なしで既に使われている Set インスタンスを一時的に計測するのにも使える。

```
static void walk(Set<Dog> dogs) {
    InstrumentedSet<Dog> iDogs = new InstrumentedSet<>(dogs);
    // このメソッド内では、dogs の代わりに iDogs を使う
}
```

個々の InstrumentedSet インスタンスは他の Set インスタンスを包むため、InstrumentedSet クラスはラッパークラスとも呼ばれる。また、InstrumentedSet クラスが計測機能を追加することで Set を装飾（decorate）するので、Decorator パターンとも呼ばれる。

コンポジションと転送の組み合わせが「委譲」と呼ばれることがあるが、技術的にはラッパーオブジェクトがラップしているオブジェクトへ自分自身を渡さない限り委譲ではない。

### ラッパークラスの注意点

ラッパークラスの欠点はほとんど無い。

注意事項としてラッパークラスは、オブジェクトが自分自身を後で他のオブジェクトに渡し、後で呼び出してもらう、コールバックフレームワークで使うのには向いていない。これは、ラップされたオブジェクトがラッパーを知らないため自分自身の参照を渡してしまい、コールバックがラッパーを避けてしまうことで起こる。これを SELF 問題と言う。

メソッド呼び出しの転送によるパフォーマンス低下やラッパーオブジェクトによるメモリ量への影響を懸念する声もあるが、実際にはいずれも大きな問題にはならない。

転送メソッドを書くのは少し面倒だが、個々のインタフェースに対して一度だけ再利用可能な転送クラスを書くだけ良い。さらに、転送クラスが公式に提供されている場合もある。例えば、Guava は全てのコレクションインタフェースに対する転送クラスを提供している。

### 継承を使うべきとき

サブクラスがスーパークラスのサブタイプである場合にだけ、継承を使うべきである。つまり、クラス B とクラス A との間に「is-a」関係が成り立つ場合にだけ、クラス A を拡張すべきである。

「すべての B は A であるか？」という問いに「はい」と答えられないならば、B は A を拡張すべきでない。この場合は大抵、B が A のインスタンスを private として保持して、異なる API を外部に公開すべきである。そうすると、A は B の一部分ではなく、B の実装の詳細にすぎない。

Java プラットフォームライブラリには、この原則を破っているものが多くある。

- スタックはベクターでないので、Stack は Vector を拡張すべきでない
- プロパティリストはハッシュテーブルではないので、Properties は Hashtable を拡張すべきでない

これらはいずれもコンポジションにするのが適切だっただろう。

## 項目19 継承のために設計および文書化する、でなければ継承を禁止する

## 項目20 抽象クラスよりもインタフェースを選ぶ

## 項目21 将来のためにインタフェースを設計する

## 項目22 型を定義するためだけにインタフェースを定義する

## 項目23 タグ付きクラスよりもクラス階層を選ぶ

## 項目24 非 static のメンバークラスよりも static のメンバークラスを選ぶ
