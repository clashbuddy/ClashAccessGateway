package studio.clashbuddy.clashaccess.gateway;

public class Pair<T,V> {
    private final T firstItem;
    private final V secondItem;

    private Pair(T firstItem, V secondItem) {
        this.firstItem = firstItem;
        this.secondItem = secondItem;
    }

    public T getFirstItem() {
        return firstItem;
    }

    public V getSecondItem() {
        return secondItem;
    }

    public static <T,V>Pair<T,V> of(T firstItem, V secondItem){
        return new Pair<>(firstItem,secondItem);
    }
}
