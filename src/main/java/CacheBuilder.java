import models.EvictionAlgorithm;
import models.PersistAlgorithm;
import models.Timer;

import java.sql.Time;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class CacheBuilder<K, V> {
    private final Set<K> onStartLoad;
    private int maximumSize;
    private Duration expiryTime;
    private EvictionAlgorithm evictionAlgorithm;
    private PersistAlgorithm persistAlgorithm;
    private DataSource<K, V> dataSource;
    private Timer timer;
    private int poolSize;

    public CacheBuilder(){
        maximumSize = 1000;
        expiryTime = Duration.ofDays(365);
        persistAlgorithm = PersistAlgorithm.WRITE_THROUGH;
        evictionAlgorithm = EvictionAlgorithm.LRU;
        onStartLoad = new HashSet<>();
        poolSize = 1;
        timer = new Timer();
    }

    public CacheBuilder<K, V> maximumSize(int maximumSize){
        this.maximumSize = maximumSize;
        return this;
    }

    public CacheBuilder<K, V> expiryTime(Duration expiryTime){
        this.expiryTime = expiryTime;
        return this;
    }

    public CacheBuilder<K, V> persistAlgorithm(PersistAlgorithm persistAlgorithm){
        this.persistAlgorithm = persistAlgorithm;
        return this;
    }

    public CacheBuilder<K, V> evictionAlgorithm(EvictionAlgorithm evictionAlgorithm){
        this.evictionAlgorithm = evictionAlgorithm;
        return this;
    }

    public CacheBuilder<K, V> loadKeysOnStart(Set<K> keys){
        this.onStartLoad.addAll(keys);
        return this;
    }

    public CacheBuilder<K, V> poolSize(int poolSize){
        this.poolSize = poolSize;
        return this;
    }

    public CacheBuilder<K, V> dataSource(DataSource<K, V> dataSource){
        this.dataSource = dataSource;
        return this;
    }

    public CacheBuilder<K, V> timer(Timer timer){
        this.timer = timer;
        return this;
    }

    public Cache<K, V> build(){
        if(dataSource == null){
            throw new IllegalArgumentException("No Data Source Configured");
        }
        return new Cache<>(maximumSize, persistAlgorithm, evictionAlgorithm, expiryTime, dataSource, timer, poolSize, onStartLoad);
    }
}
