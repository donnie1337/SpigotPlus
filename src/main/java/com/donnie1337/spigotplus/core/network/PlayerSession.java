package com.donnie1337.spigotplus.core.network;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Lightweight authenticated identity/session created during the Java login phase. */
public final class PlayerSession {
    private final UUID uniqueId;
    private final String username;
    private final int protocolVersion;
    private final UUID sessionId;

    public PlayerSession(String username, int protocolVersion, UUID sessionId) {
        this.username = username;
        this.protocolVersion = protocolVersion;
        this.sessionId = sessionId;
        this.uniqueId = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }

    public UUID uniqueId() {
        return uniqueId;
    }

    public String username() {
        return username;
    }

    public int protocolVersion() {
        return protocolVersion;
    }

    public UUID sessionId() {
        return sessionId;
    }
}
