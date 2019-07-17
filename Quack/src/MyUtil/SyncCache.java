package MyUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class SyncCache<K, V> {
    int n;

    Map<K, CacheEntry<V>> map = new HashMap();

    static class CacheEntry<V> {
        long lastTouched;

        V value;

        public CacheEntry(V v) {
            value = v;
        }

        public void touch() {
            lastTouched = System.currentTimeMillis();
        }
    }

    public SyncCache(int n) {
        this.n = n;
    }

    synchronized public Set<K> getKeys() {
        return new HashSet<K>(map.keySet());
    }

    synchronized public List<V> getValues() {
        Vector<V> values = new Vector();
        for (CacheEntry<V> e : map.values()) {
            values.add(e.value);
        }
        return values;
    }

    synchronized public V get(K k) {
        CacheEntry<V> e = map.get(k);
        if (e != null)
            return e.value;
        return null;
    }

    synchronized public void removeOldest() {
        K oldest = null;
        long oldestTime = Long.MAX_VALUE;
        for (K k : map.keySet()) {
            long time = map.get(k).lastTouched;
            if (time < oldestTime) {
                oldestTime = time;
                oldest = k;
            }
        }
        map.remove(oldest);
    }
    
    synchronized public void makeRoom() {
        if (map.size() >= n) {
            removeOldest();
        }
    }

    synchronized public void put(K k, V v) {
        CacheEntry<V> e = map.get(k);
        if (e != null) {
            e.value = v;
            return;
        }
        e = new CacheEntry<V>(v);
        if (map.size() >= n)
            removeOldest();
        map.put(k, e);
    }

    synchronized public void replace(K k, V v) {
        CacheEntry<V> e = map.get(k);
        if (e != null)
            e.value = v;
    }

    synchronized public void remove(K k) {
        map.remove(k);
    }
    
    synchronized public void touch(K k) {
        CacheEntry<V> e = map.get(k);
        if (e != null) {
            e.touch();
        }
    }
}
