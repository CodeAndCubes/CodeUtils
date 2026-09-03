package com.mrleonardos.codeutils.platform;

public final class LoginDoor {

    private volatile boolean closed;
    private volatile String reasonKey = "";

    public void close(String key) {
        this.reasonKey = key == null ? "" : key;
        this.closed = true;
    }

    public void open() {
        this.closed = false;
    }

    public boolean closed() {
        return closed;
    }

    public String reasonKey() {
        return reasonKey;
    }
}
