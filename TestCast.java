import java.util.*;

public class TestCast {
    public static void main(String[] args) {
        List<String> strings = new ArrayList<>();
        unsafeAdd(strings, Integer.valueOf(42));
        String s = strings.get(0);
    }

    private static void unsafeAdd(List list, Object o) {
        if (o instanceof Set<?>) {
            Set<?> s = (Set<?>)o;
        }
    }

    // コンパイルを通すためにやや変更している
    public static <T> T[] toArray(T[] a, T[] elementData, int size) {
        if (a.length < size) {
            return (T[]) Arrays.copyOf(elementData, size, a.getClass());
        }
        System.arraycopy(elementData, 0, a, 0, size);
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }
}
