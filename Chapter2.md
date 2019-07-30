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

### JavaBeans パターン

### Builder パターン


## Item 3: Enforce the singleton property with a private constructor or an enum type

### まえおき
ただ一度だけのインスタンス生成が保証される singleton はモックにすることが不可能なためテストが困難であるという特徴を持つ。その singleton を実現するための方法について紹介する。

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

// TODO: Serializable についてきちんと書く？


### 単一要素を持つ enum で宣言する
```java
public enum Elvis {
  INSTANCE;

  public void leaveTheBuilding() { ... }
}
```
- singleton が単一のインスタンスであることが保証される
  - リフレクションや直列化による攻撃の心配もない
- Enum 以外のスーパクラスを継承している場合はダメ
- 一番いいのでは？

## Item 4: Enforce noninstantiability with a private constructor

### まえおき


## Item 5: Prefer dependency injection to hardwiring resources

### まえおき


## Item 6: Avoid creating unnecessary objects

### まえおき


## Item 7: Eliminate obsolete object references

### まえおき


## Item 8: Avoid finalizers and cleaners

### まえおき


## Item 9: Prefer *try*-with-resources to *try*-*finally*

### まえおき