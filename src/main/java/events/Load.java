package events;

import models.Record;

public class Load<K,V> extends Event<K,V> {
    public Load(Record<K, V> record, long timeStamp) {
        super(record, timeStamp);
    }
}
