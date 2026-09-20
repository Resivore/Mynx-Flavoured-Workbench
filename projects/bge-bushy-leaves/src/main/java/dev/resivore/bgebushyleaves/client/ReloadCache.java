package dev.resivore.bgebushyleaves.client;

import java.util.IdentityHashMap;
import java.util.Optional;

/** Small identity cache whose lifetime is exactly one client resource reload. */
final class ReloadCache<K, V> {
    private final IdentityHashMap<K, V> values = new IdentityHashMap<>();

    void put(K key, V value) { values.put(key, value); }
    Optional<V> find(K key) { return Optional.ofNullable(values.get(key)); }
    void clear() { values.clear(); }
}
