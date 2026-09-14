package com.donnie1337.spigotplus.protocol;

public interface ProtocolAdapter {
    ProtocolVersion version();
    Object decode(Object wirePacket);
    Object encode(Object internalPacket);
}
