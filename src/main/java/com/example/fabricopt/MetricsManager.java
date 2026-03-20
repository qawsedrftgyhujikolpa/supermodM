package com.example.fabricopt;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple metrics collector for the PoC. Lightweight and safe to call from mixins.
 */
public final class MetricsManager {
    private static final AtomicLong canceledTicks = new AtomicLong(0);
    private static final AtomicLong reductionsApplied = new AtomicLong(0);
n    private MetricsManager() {}

    public static void recordCanceled() {
        canceledTicks.incrementAndGet();
    }

    public static void recordReduction() {
        reductionsApplied.incrementAndGet();
    }

    public static MetricsSnapshot snapshotAndReset() {
        long canceled = canceledTicks.getAndSet(0);
        long reductions = reductionsApplied.getAndSet(0);
        return new MetricsSnapshot(canceled, reductions);
    }

    public static final class MetricsSnapshot {
        public final long canceledTicks;
        public final long reductionsApplied;
        public MetricsSnapshot(long canceledTicks, long reductionsApplied) {
            this.canceledTicks = canceledTicks;
            this.reductionsApplied = reductionsApplied;
        }
    }
}
