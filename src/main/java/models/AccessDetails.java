package models;

import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;

public class AccessDetails {
    private final LongAdder accessCount;
    private long lastAccessTime;

    public AccessDetails(long lastAccessTime) {
        this.accessCount = new LongAdder();
        this.lastAccessTime = lastAccessTime;
    }

    public int getAccessCount() {
        return (int) accessCount.longValue();
    }

    public long getLastAccessTime() {
        return lastAccessTime;
    }

    public AccessDetails update(long lastAccessTime){
        AccessDetails accessDetails = new AccessDetails(lastAccessTime);
        accessDetails.accessCount.add(this.accessCount.longValue() + 1);
        return accessDetails;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessDetails that = (AccessDetails) o;
        return lastAccessTime == that.lastAccessTime && Objects.equals(accessCount, that.accessCount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessCount, lastAccessTime);
    }

    @Override
    public String toString() {
        return "AccessDetails{" +
                "accessCount=" + accessCount +
                ", lastAccessTime=" + lastAccessTime +
                '}';
    }
}
