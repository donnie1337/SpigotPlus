package com.donnie1337.spigotplus.core.world;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class WorldSession {
    private final String name;
    private final ConcurrentMap<UUID, PlayerWorldSession> players = new ConcurrentHashMap<>();
    public WorldSession(String name) { this.name=name; }
    public String name() { return name; }
    public PlayerWorldSession addPlayer(UUID id, String username) { PlayerWorldSession p=new PlayerWorldSession(id,username); players.put(id,p); return p; }
    public void removePlayer(UUID id) { players.remove(id); }
    public int playerCount() { return players.size(); }
}
