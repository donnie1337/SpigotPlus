package com.donnie1337.spigotplus.core.world.chunk;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
public final class ActiveChunkTracker { private final Set<Long> active=ConcurrentHashMap.newKeySet(); public boolean activate(long key){return active.add(key);} public boolean deactivate(long key){return active.remove(key);} public int size(){return active.size();} public boolean contains(long key){return active.contains(key);} public Set<Long> snapshot(){return Collections.unmodifiableSet(Set.copyOf(active));} }
