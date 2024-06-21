import events.Load;
import events.Update;
import events.Write;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public class TestCache {

    private static final String SOFTWARE_DEVELOPER = "SDE", PROGRAMMER_ANALYST = "PMA";
    private final Map<String, String> dataMap = new ConcurrentHashMap<>();
    private final Queue<CompletableFuture<Void>> writeOperationsQueue = new LinkedList<>();
    private DataSource<String, String> dataSource;
    private DataSource<String, String> writeBackDataSource;

    @Before
    public void dataSetup(){
        dataMap.clear();
        writeOperationsQueue.clear();
        dataMap.put(SOFTWARE_DEVELOPER, "Ram");
        dataMap.put(PROGRAMMER_ANALYST, "Shyam");
        dataSource = new DataSource<String, String>() {
            @Override
            public CompletionStage<String> load(String key) {
                if(dataMap.containsKey(key)){
                    return CompletableFuture.completedFuture(dataMap.get(key));
                }else{
                    return CompletableFuture.failedStage(new NullPointerException());
                }
            }

            @Override
            public CompletionStage<Void> persist(String key, String value, long timeStamp) {
                dataMap.put(key, value);
                return CompletableFuture.completedFuture(null);
            }
        };

        writeBackDataSource = new DataSource<String, String>() {
            @Override
            public CompletionStage<String> load(String key) {
                if(dataMap.containsKey(key)){
                    return CompletableFuture.completedFuture(dataMap.get(key));
                }else{
                    return CompletableFuture.failedStage(new NullPointerException());
                }
            }

            @Override
            public CompletionStage<Void> persist(String key, String value, long timeStamp) {
                final CompletableFuture<Void> hold = new CompletableFuture<>();
                writeOperationsQueue.add(hold);
                return hold.thenAccept(__ -> dataMap.put(key, value));
            }
        };
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCacheBuildWithFailure(){
        new CacheBuilder<>().build();
    }

    @Test
    public void testCacheDefaultBehaviour(){
        final var cache = new CacheBuilder<String, String>().dataSource(dataSource).build();
        Assert.assertNotNull(cache);
        Assert.assertTrue(isEqualTo(cache.get(SOFTWARE_DEVELOPER), "Ram"));
        Assert.assertTrue(isEqualTo(cache.set(SOFTWARE_DEVELOPER, "Pavan").thenCompose(__ -> cache.get(SOFTWARE_DEVELOPER)),"Pavan"));
        Assert.assertEquals(3, cache.getEventQueue().size());
        Assert.assertTrue(cache.getEventQueue().get(0) instanceof Load<String, String>);
        Assert.assertTrue(cache.getEventQueue().get(1) instanceof Update<String, String>);
        Assert.assertTrue(cache.getEventQueue().get(2) instanceof Write<String, String>);

    }

    private boolean isEqualTo(CompletionStage<String> future, String value) {
        return future.thenApply(result -> {
            if (result.equals(value)) {
                return true;
            } else {
                throw new AssertionError();
            }
        }).toCompletableFuture().join();
    }

}
