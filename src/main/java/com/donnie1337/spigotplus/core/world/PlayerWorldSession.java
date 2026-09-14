package com.donnie1337.spigotplus.core.world;

import java.util.UUID;

public final class PlayerWorldSession {
    private final UUID uniqueId;
    private final String username;
    private double x = 0.5, y = 64.0, z = 0.5;
    private float yaw, pitch;
    public PlayerWorldSession(UUID uniqueId, String username) { this.uniqueId = uniqueId; this.username = username; }
    public UUID uniqueId() { return uniqueId; }
    public String username() { return username; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
    public void position(double x, double y, double z, float yaw, float pitch) { this.x=x; this.y=y; this.z=z; this.yaw=yaw; this.pitch=pitch; }
}
