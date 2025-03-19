package tools.vitruv.dsls.tgg.emoflonintegration.patternconversion.echange;

import java.util.*;

/**
 * We do not implement Stack or Map, this is a one-purpose class...
 * T
 */
public class MapStack<K, V> {

    private final Map<K, V> map;
    private final Stack<K> stack;

    public MapStack() {
        this.map = new HashMap<>();
        this.stack = new Stack<>();
    }

    /**
     * Copies the map and puts everything in the stack. The stack order depends on the map's iterator.
     * @param map
     */
    public MapStack(Map<K, V> map) {
        this();
        map.forEach(this::putPush);
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     *
     * @param key the key whose associated value is to be returned
     * @return the associated value if present.
     */
    public V get(K key) {
        return map.get(key);
    }

    public V putPush(K key, V value) {
        stack.push(key);
        return map.put(key, value);
    }

    public V removePop() {
        return map.remove(stack.pop());
    }

    /**
     * Pop the stack until and including the given key occurs. Pop the key also!
     * If the key isn't contained, we throw!
     * @param key our "stop condition"
     * @return all popped keys until and including K
     */
    public Set<K> removePopUntil(K key) {
        if (!map.containsKey(key)) {
            throw new IllegalArgumentException("Cannot pop until key " + key + ": " + "MapStack doesn't contain key.");
        }
        Set<K> keysUntil = new HashSet<>();
        K currentKey;
        do {
            currentKey = stack.pop();
            keysUntil.add(currentKey);
            map.remove(currentKey);
        } while (currentKey != key);
        return keysUntil;
    }

    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    public Map<K, V> getMap() {
        return map;
    }
}
