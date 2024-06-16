package events;

import models.Record;

public class Write<K, V> extends Event<K, V> {
    public Write(Record<K, V> record, long timeStamp) {
        super(record, timeStamp);
    }
}
