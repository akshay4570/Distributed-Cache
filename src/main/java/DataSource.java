import java.util.concurrent.CompletionStage;

public interface DataSource<K, V> {

    CompletionStage<V> load(K key);

    CompletionStage<Void> persist(K key, V value, long timeStamp);
}
