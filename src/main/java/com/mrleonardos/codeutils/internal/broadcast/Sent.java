package com.mrleonardos.codeutils.internal.broadcast;

public final class Sent {

    public enum Result {

        DONE,
        NOBODY,
        UNKNOWN_SET,
        SET_OFF
    }

    private static final Sent NOBODY = new Sent(Result.NOBODY, 0);
    private static final Sent UNKNOWN_SET = new Sent(Result.UNKNOWN_SET, 0);
    private static final Sent SET_OFF = new Sent(Result.SET_OFF, 0);

    private final Result result;
    private final int recipients;

    private Sent(Result result, int recipients) {
        this.result = result;
        this.recipients = recipients;
    }

    public static Sent to(int recipients) {
        return recipients > 0 ? new Sent(Result.DONE, recipients) : NOBODY;
    }

    public static Sent nobody() {
        return NOBODY;
    }

    public static Sent unknownSet() {
        return UNKNOWN_SET;
    }

    public static Sent setOff() {
        return SET_OFF;
    }

    public Result result() {
        return result;
    }

    public int recipients() {
        return recipients;
    }

    public boolean done() {
        return result == Result.DONE;
    }

    @Override
    public String toString() {
        return result == Result.DONE ? "sent to " + recipients : result.name();
    }
}
