package com.mrleonardos.codeutils.internal;

import java.util.function.LongSupplier;

/** Часы, которые идут только тогда, когда их переставит тест. */
public final class FakeClock implements LongSupplier {

    private long millis;

    public FakeClock(long millis) {
        this.millis = millis;
    }

    public void at(long value) {
        millis = value;
    }

    @Override
    public long getAsLong() {
        return millis;
    }
}
