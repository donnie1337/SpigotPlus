package com.donnie1337.spigotplus.protocol;

public record ProtocolVersion(int major, int minor, int patch) implements Comparable<ProtocolVersion> {
    @Override public int compareTo(ProtocolVersion other) {
        int a = Integer.compare(major, other.major); if (a != 0) return a;
        a = Integer.compare(minor, other.minor); return a != 0 ? a : Integer.compare(patch, other.patch);
    }
    @Override public String toString() { return major + "." + minor + "." + patch; }
}
