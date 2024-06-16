package events;

import models.Record;

public class Update<K,V> extends Event<K, V> {
    private final Record<K, V> previousValue;

    public Update(Record<K, V> record, long timeStamp, Record<K, V> previousValue) {
        super(record, timeStamp);
        this.previousValue = previousValue;
    }
}
