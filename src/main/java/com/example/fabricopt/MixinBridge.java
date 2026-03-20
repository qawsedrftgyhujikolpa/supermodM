package com.example.fabricopt;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import java.util.Set;
import java.util.Collections;

/**
 * Small bridge to encapsulate logic mixins can safely call.
 * Keeps decision logic out of the mixin hot-path where possible by
 * publishing a single immutable snapshot reference.
 */
public final class MixinBridge {
    private static volatile ConfigSnapshot SNAPSHOT = new ConfigSnapshot(false, 1, true, false, Collections.emptySet());
    private static volatile long serverTick = 0L;

    private MixinBridge() {}

    private static final class ConfigSnapshot {
        final boolean enabled;
        final int throttleInterval;
        final boolean mixinLogOnly;
        final boolean debugEnabled;
        final Set<String> throttleWhitelist;

n        ConfigSnapshot(boolean enabled, int throttleInterval, boolean mixinLogOnly, boolean debugEnabled, Set<String> throttleWhitelist) {
            this.enabled = enabled;
            this.throttleInterval = throttleInterval;
            this.mixinLogOnly = mixinLogOnly;
            this.debugEnabled = debugEnabled;
            this.throttleWhitelist = throttleWhitelist != null ? throttleWhitelist : Collections.emptySet();
        }
    }

    public static void init(ConfigManager cfg) {
        boolean enabled = cfg != null && cfg.isEnablePoC();
        int throttle = cfg != null ? cfg.getThrottleInterval() : 1;
        boolean mixinLogOnly = cfg != null ? cfg.isMixinLogOnly() : true;
        boolean debugEnabled = FabricOptMod.LOGGER.isDebugEnabled();
        Set<String> whitelist = cfg != null ? cfg.getThrottleWhitelist() : Collections.emptySet();
        SNAPSHOT = new ConfigSnapshot(enabled, throttle, mixinLogOnly, debugEnabled, whitelist);
    }

    public static void setEnabled(boolean en) {
        ConfigSnapshot old = SNAPSHOT;
        SNAPSHOT = new ConfigSnapshot(en, old.throttleInterval, old.mixinLogOnly, old.debugEnabled, old.throttleWhitelist);
    }

    public static boolean isEnabled() { return SNAPSHOT.enabled; }

    public static void setServerTick(long t) { serverTick = t; }
    public static long getServerTick() { return serverTick; }

    public static int getThrottleInterval() { return SNAPSHOT.throttleInterval; }
    public static boolean isMixinLogOnly() { return SNAPSHOT.mixinLogOnly; }

    public static boolean isEntityWhitelisted(Entity e) {
        Set<String> wl = SNAPSHOT.throttleWhitelist;
        if (wl == null || wl.isEmpty()) return false;
        String fq = ((Object)e).getClass().getName();
        if (wl.contains(fq)) return true;
        String simple = ((Object)e).getClass().getSimpleName();
        return wl.contains(simple);
    }

n    public static boolean isThrottlingEnabled(Entity e) {
        if (!SNAPSHOT.enabled) return false;
        if (e instanceof Player) return false;
        if (SNAPSHOT.mixinLogOnly) return false;
        if (isEntityWhitelisted(e)) return false;
        return SNAPSHOT.throttleInterval > 1;
    }

    public static boolean shouldLogObservation(Entity e, long serverTick) {
        if (!SNAPSHOT.enabled) return false;
        if (!SNAPSHOT.mixinLogOnly) return false;
        if (!SNAPSHOT.debugEnabled) return false;
        int throttle = SNAPSHOT.throttleInterval;
        if (serverTick % Math.max(1, throttle) != 0) return false;
        // sample ~1/20 entities to avoid log storm
        return Math.abs(System.identityHashCode(e)) % 20 == 0;
    }

}
