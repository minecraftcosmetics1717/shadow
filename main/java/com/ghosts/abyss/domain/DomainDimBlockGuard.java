package com.ghosts.abyss.domain;

public final class DomainDimBlockGuard {
    private static final ThreadLocal<Integer> ALLOW = ThreadLocal.withInitial(() -> 0);

    private DomainDimBlockGuard() {}

    public static void open() {
        ALLOW.set(ALLOW.get() + 1);
    }

    public static void close() {
        int v = ALLOW.get() - 1;
        ALLOW.set(Math.max(0, v));
    }

    public static boolean isOpen() {
        return ALLOW.get() > 0;
    }
}