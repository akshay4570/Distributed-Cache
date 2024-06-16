import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import models.*;
import models.Record;

public class Cache<K, V> {
    private final int maximumSize;
    private final PersistAlgorithm persistAlgorithm;
    private final Duration expiryTime;
    private final Map<K, CompletionStage<Record<K, V>>> cache;
    private final DataSource<K, V> dataSource;
    private final ConcurrentSkipListMap<AccessDetails, List<K>> priorityQueue;
    private final ConcurrentSkipListMap<Long, List<K>> expiryQueue;
    private final Timer timer;

    public Cache(int maximumSize,
                 PersistAlgorithm persistAlgorithm,
                 EvictionAlgorithm evictionAlgorithm,
                 Duration expiryTime,
                 DataSource<K, V> dataSource,
                 Timer timer) {
        this.maximumSize = maximumSize;
        this.persistAlgorithm = persistAlgorithm;
        this.expiryTime = expiryTime;
        this.dataSource = dataSource;
        this.timer = timer;
        this.cache = new ConcurrentHashMap<>();
        priorityQueue = new ConcurrentSkipListMap<>((first, second) -> {
            final int accessTimeDiff = (int) (first.getLastAccessTime() - second.getLastAccessTime());
            if(evictionAlgorithm.equals(EvictionAlgorithm.LRU)){
                return accessTimeDiff;
            }else{
                final var accessTimeCount = first.getAccessCount() - second.getAccessCount();
                return accessTimeCount != 0 ? accessTimeCount : accessTimeDiff;
            }
        });
        expiryQueue = new ConcurrentSkipListMap<>();
    }

    public CompletionStage<V> get(K key){
        return getFromCache(key);
    }

    public CompletionStage<Void> set(K key, V value){
        return setInCache(key, value);
    }

    public CompletionStage<V> getFromCache(K key){
        CompletionStage<Record<K, V>> result;
        if(!cache.containsKey(key)){
            result = addToCache(key, loadFromDB(dataSource, key));
        }else{
            result = cache.get(key).thenCompose(record -> {
                if(hasExpired(record)){
                    priorityQueue.get(record.getAccessDetails()).remove(key);
                    expiryQueue.get(record.getInsertionTime()).remove(key);
                    return addToCache(key, loadFromDB(dataSource, key));
                }else{
                    return CompletableFuture.completedFuture(record);
                }
            });
        }
        return result.thenApply(record -> {
           priorityQueue.get(record.getAccessDetails()).remove(key);
           AccessDetails updatedAccessDetails = record.getAccessDetails().update(timer.getCurrentTime());
           priorityQueue.putIfAbsent(updatedAccessDetails, new CopyOnWriteArrayList<>());
           priorityQueue.get(updatedAccessDetails).add(key);
           record.setAccessDetails(updatedAccessDetails);
           return record.getValue();
        });
    }

    public CompletionStage<Void> setInCache(K key, V value){
        CompletionStage<Void> result = CompletableFuture.completedFuture(null);
        if(cache.containsKey(key)){
            result = cache.remove(key).thenAccept(oldRecord -> {
                priorityQueue.get(oldRecord.getAccessDetails()).remove(key);
                expiryQueue.get(oldRecord.getInsertionTime()).remove(key);
            });
        }
        return result.thenCompose(__ -> addToCache(key, CompletableFuture.completedFuture(value)))
                     .thenCompose(record -> persistAlgorithm == PersistAlgorithm.WRITE_THROUGH ? persistInDB(record) : CompletableFuture.completedFuture(null));
    }
    public CompletionStage<Record<K, V>> addToCache(K key, CompletionStage<V> valueFuture){
        evictStaleRecords();
        final CompletionStage<Record<K,V> >recordFuture = valueFuture.thenApply(value -> {
            Record<K, V> record = new Record<>(key, value, timer.getCurrentTime());
            expiryQueue.putIfAbsent(record.getInsertionTime(), new CopyOnWriteArrayList<>());
            expiryQueue.get(record.getInsertionTime()).add(key);
            priorityQueue.putIfAbsent(record.getAccessDetails(), new CopyOnWriteArrayList<>());
            priorityQueue.get(record.getAccessDetails()).add(key);
            return record;
        });
        cache.put(key, recordFuture);
        return recordFuture;
    }

    private synchronized void evictStaleRecords() {
        if(cache.size() >= maximumSize){
            while (!expiryQueue.isEmpty() && hasExpired(expiryQueue.firstKey())){
                List<K> listKeys = expiryQueue.pollFirstEntry().getValue();
                for(K key : listKeys){
                    Record<K, V> expiredRecord = cache.remove(key).toCompletableFuture().join();
                    priorityQueue.remove(expiredRecord.getAccessDetails());
                }
            }
        }
        if(cache.size() >= maximumSize){
            List<K> listKeys = priorityQueue.pollFirstEntry().getValue();
            while(listKeys.isEmpty()){
                listKeys = priorityQueue.pollFirstEntry().getValue();
            }
            for(K key : listKeys){
                Record<K, V> lowestPriorityRecord = cache.remove(key).toCompletableFuture().join();
                expiryQueue.get(lowestPriorityRecord.getInsertionTime()).remove(lowestPriorityRecord.getKey());
            }
        }
    }

    private CompletionStage<V> loadFromDB(DataSource<K,V> dataSource, K key) {
        return dataSource.load(key);
    }

    private boolean hasExpired(Record<K,V> record) {
        return hasExpired(record.getInsertionTime());
    }

    private boolean hasExpired(final Long time){
        return Duration.ofNanos(timer.getCurrentTime() - time).compareTo(expiryTime) > 0;
    }

    private CompletionStage<Void> persistInDB(Record<K, V> record){
        return dataSource.persist(record.getKey(), record.getValue(), record.getInsertionTime());
    }
}