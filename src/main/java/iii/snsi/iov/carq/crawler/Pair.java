package iii.snsi.iov.carq.crawler;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by myco on 8/12/16.
 */
// Pair: http://stackoverflow.com/a/521235
public class Pair<K, V> {

    private final K key;
    private final V val;

    public Pair(@JsonProperty("key") K key, @JsonProperty("val") V val) {
        this.key = key;
        this.val = val;
    }

    public K getKey() { return key; }
    public V getVal() { return val; }
}
