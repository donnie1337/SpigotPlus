package com.donnie1337.spigotplus.core.network;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.function.Consumer;

/** Bounded per-connection packet queue; rejects work instead of allowing unbounded memory growth. */
public final class NetworkBackpressure<T> {
    private final int capacity;
    private final ArrayDeque<T> queue;
    private long accepted;
    private long rejected;

    public NetworkBackpressure(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity");
        this.capacity = capacity;
        this.queue = new ArrayDeque<>(capacity);
    }

    public synchronized boolean offer(T packet) {
        Objects.requireNonNull(packet, "packet");
        if (queue.size() >= capacity) { rejected++; return false; }
        queue.addLast(packet); accepted++; return true;
    }

    public synchronized int drain(int maxPackets, Consumer<T> consumer) {
        if (maxPackets < 1) return 0;
        int count = 0;
        while (count < maxPackets && !queue.isEmpty()) {
            consumer.accept(queue.removeFirst());
            count++;
        }
        return count;
    }

    public synchronized int size() { return queue.size(); }
    public synchronized long accepted() { return accepted; }
    public synchronized long rejected() { return rejected; }
    public int capacity() { return capacity; }
}
