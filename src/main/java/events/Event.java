package events;

import models.Record;

import java.util.UUID;

public abstract class Event<K, V> {
    private final String Id;
    private final Record<K, V> record;
    private final long timeStamp;

    public Event(Record<K, V> record, long timeStamp) {
        Id = UUID.randomUUID().toString();
        this.record = record;
        this.timeStamp = timeStamp;
    }

    public String getId() {
        return Id;
    }

    public Record<K, V> getRecord() {
        return record;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    @Override
    public String toString() {
        return "Event{" +
                "Id='" + Id + '\'' +
                ", record=" + record +
                ", timeStamp=" + timeStamp +
                '}';
    }
}
