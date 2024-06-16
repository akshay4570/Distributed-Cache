package events;

import models.Record;

public class Eviction<K, V> extends Event<K, V> {
    private final Type type;

    public Eviction(Record<K, V> record, long timeStamp, Type type) {
        super(record, timeStamp);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Eviction{" +
                "type=" + type +
                '}';
    }

    public enum Type{
        EXPIRY, REPLACEMENT
    }
}
