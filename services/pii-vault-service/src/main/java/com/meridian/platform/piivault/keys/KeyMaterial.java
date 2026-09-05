package com.meridian.platform.piivault.keys;

public class KeyMaterial {

    private final int version;
    private final byte[] key;
    private final String source;

    public KeyMaterial(int version, byte[] key, String source) {
        this.version = version;
        this.key = key;
        this.source = source;
    }

    public int getVersion() { return version; }
    public byte[] getKey() { return key; }
    public String getSource() { return source; }
}
