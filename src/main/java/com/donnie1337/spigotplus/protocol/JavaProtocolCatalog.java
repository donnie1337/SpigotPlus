package com.donnie1337.spigotplus.protocol;

import java.util.List;

/** Supported Java protocol families. Packet translation remains isolated behind ProtocolAdapter. */
public final class JavaProtocolCatalog {
    public static final int MIN_SUPPORTED_PROTOCOL = 759; // Java 1.19
    public static final int MAX_SUPPORTED_PROTOCOL = 776; // Java 26.2

    private JavaProtocolCatalog() {}

    public static boolean supports(int protocol) {
        return protocol >= MIN_SUPPORTED_PROTOCOL && protocol <= MAX_SUPPORTED_PROTOCOL;
    }

    /** Known protocol boundaries used by the compatibility layer. */
    public static List<Integer> knownProtocols() {
        return List.of(759, 760, 761, 762, 763, 764, 765, 766, 767, 768, 769, 770, 771, 772, 773, 774, 775, 776);
    }
}
