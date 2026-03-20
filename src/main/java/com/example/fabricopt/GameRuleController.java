package com.example.fabricopt;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public class GameRuleController {
    private final Logger logger;
    private final Map<String, Integer> prevRandomTickSpeed = new ConcurrentHashMap<>();
    private final Map<String, Integer> reducedRandomTickSpeed = new ConcurrentHashMap<>();
    private final Map<String, Long> worldScheduledAt = new ConcurrentHashMap<>();
    private final ConcurrentSkipListMap<Long, Set<String>> restoreSchedule = new ConcurrentSkipListMap<>();
    private final int restoreDelayTicks;

n    private final AtomicInteger consecutiveErrors = new AtomicInteger(0);
    private final int MAX_CONSECUTIVE_ERRORS = 5;

n    public GameRuleController(int restoreDelayTicks, Logger logger) {
        this.restoreDelayTicks = restoreDelayTicks;
        this.logger = logger;
    }

n    public void applyReduction(ServerWorld world, MinecraftServer server, long serverTick) {
        String worldKey = world.getRegistryKey().getValue().toString();
        int current;
        try {
            current = world.getGameRules().get(GameRules.RULE_RANDOM_TICK_SPEED).get();
        } catch (Exception e) {
            int errors = consecutiveErrors.incrementAndGet();
            logger.error("{} failed to read randomTickSpeed for world {}: {}", FabricOptMod.MOD_ID, worldKey, e.toString(), e);
            checkDisable(errors);
            return;
        }
        prevRandomTickSpeed.putIfAbsent(worldKey, current);
        int reduced = Math.max(1, current / 2);
        if (reduced < current) {
            try {
                world.getGameRules().get(GameRules.RULE_RANDOM_TICK_SPEED).set(reduced, server);
                logger.info("{} reduced randomTickSpeed for world {} from {} to {}", FabricOptMod.MOD_ID, worldKey, current, reduced);
                com.example.fabricopt.MetricsManager.recordReduction();
                reducedRandomTickSpeed.put(worldKey, reduced);
                long restoreTick = serverTick + restoreDelayTicks;
                Long prevScheduled = worldScheduledAt.put(worldKey, restoreTick);
                if (prevScheduled != null && !prevScheduled.equals(restoreTick)) {
                    restoreSchedule.computeIfPresent(prevScheduled, (k, set) -> {
                        set.remove(worldKey);
                        return set.isEmpty() ? null : set;
                    });
                }
                restoreSchedule.compute(restoreTick, (k, set) -> {
                    if (set == null) set = ConcurrentHashMap.newKeySet();
                    set.add(worldKey);
                    return set;
                });
                consecutiveErrors.set(0);
            } catch (Exception e) {
                int errors = consecutiveErrors.incrementAndGet();
                logger.error("{} failed to set randomTickSpeed for world {}: {}", FabricOptMod.MOD_ID, worldKey, e.toString(), e);
                checkDisable(errors);
            }
        }
    }

n    public void restoreDue(long serverTick, MinecraftServer server) {
        while (true) {
            Entry<Long, Set<String>> entry = restoreSchedule.firstEntry();
            if (entry == null || entry.getKey() > serverTick) break;
            Long key = entry.getKey();
            Set<String> worlds = restoreSchedule.remove(key);
            if (worlds == null) continue;
            for (String worldKey : worlds) {
                // Do NOT remove prev/reduced until restore is successful. Keep state for retry if necessary.
                Integer prev = prevRandomTickSpeed.get(worldKey);
                Integer expectedReduced = reducedRandomTickSpeed.get(worldKey);
                worldScheduledAt.remove(worldKey);
                if (prev == null) continue; // nothing to restore
                ServerWorld target = findWorld(server, worldKey);
                if (target != null) {
                    try {
                        int currentNow = target.getGameRules().get(GameRules.RULE_RANDOM_TICK_SPEED).get();
                        if (expectedReduced != null && currentNow != expectedReduced.intValue()) {
                            // Something else changed the rule in the meantime. Don't discard prev - retry later with backoff.
                            logger.info("{} skipping restore for world {}: current randomTickSpeed {} != expected reduced {} - will retry", FabricOptMod.MOD_ID, worldKey, currentNow, expectedReduced);
                            long nextTry = serverTick + Math.max(1, restoreDelayTicks / 2);
                            worldScheduledAt.put(worldKey, nextTry);
                            restoreSchedule.compute(nextTry, (k, set) -> {
                                if (set == null) set = ConcurrentHashMap.newKeySet();
                                set.add(worldKey);
                                return set;
                            });
                        } else {
                            target.getGameRules().get(GameRules.RULE_RANDOM_TICK_SPEED).set(prev, server);
                            // only remove state after successful restore
                            prevRandomTickSpeed.remove(worldKey);
                            reducedRandomTickSpeed.remove(worldKey);
                            worldScheduledAt.remove(worldKey);
                            logger.info("{} restored randomTickSpeed for world {} to {}", FabricOptMod.MOD_ID, worldKey, prev);
                        }
                        consecutiveErrors.set(0);
                    } catch (Exception e) {
                        int errors = consecutiveErrors.incrementAndGet();
                        logger.error("{} failed to restore randomTickSpeed for world {}: {}", FabricOptMod.MOD_ID, worldKey, e.toString(), e);
                        checkDisable(errors);
                    }
                }
            }
        }
    }

n    public void restoreAll(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            String worldKey = world.getRegistryKey().getValue().toString();
            Integer prev = prevRandomTickSpeed.remove(worldKey);
            Integer reduced = reducedRandomTickSpeed.remove(worldKey);
            worldScheduledAt.remove(worldKey);
            if (prev != null) {
                try {
                    world.getGameRules().get(GameRules.RULE_RANDOM_TICK_SPEED).set(prev, server);
                    logger.info("{} restored randomTickSpeed for world {} during shutdown to {}", FabricOptMod.MOD_ID, worldKey, prev);
                } catch (Exception e) {
                    logger.error("{} failed to restore randomTickSpeed for world {} during shutdown: {}", FabricOptMod.MOD_ID, worldKey, e.toString(), e);
                }
            }
        }
        restoreSchedule.clear();
        worldScheduledAt.clear();
        reducedRandomTickSpeed.clear();
    }

n    private ServerWorld findWorld(MinecraftServer server, String worldKey) {
        for (ServerWorld w : server.getWorlds()) {
            if (w.getRegistryKey().getValue().toString().equals(worldKey)) return w;
        }
        return null;
    }

n    private void checkDisable(int errors) {
        if (errors >= MAX_CONSECUTIVE_ERRORS) {
            logger.error("{} encountered {} consecutive errors; disabling PoC to preserve server stability", FabricOptMod.MOD_ID, errors);
            FabricOptMod.ENABLE_POC = false;
            MixinBridge.setEnabled(false);
        }
    }
}
