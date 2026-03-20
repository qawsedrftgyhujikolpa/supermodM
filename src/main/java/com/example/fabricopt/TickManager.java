package com.example.fabricopt;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.apache.logging.log4j.Logger;
import com.example.fabricopt.MixinBridge;

public class TickManager {
    private final ConfigManager config;
    private final GameRuleController controller;
    private final Logger logger;
    private final EntityTickManager entityManager;

    public TickManager(ConfigManager config, GameRuleController controller, Logger logger, EntityTickManager entityManager) {
        this.config = config;
        this.controller = controller;
        this.logger = logger;
        this.entityManager = entityManager;
    }

    public void onServerTick(MinecraftServer server) {
        long serverTick = server.getTicks();
        // expose latest server tick for mixins
        FabricOptMod.SERVER_TICK = serverTick;
        MixinBridge.setServerTick(serverTick);
        // Always attempt to restore any scheduled game-rule restores so we don't leave the world in a reduced state if PoC gets disabled.
        controller.restoreDue(serverTick, server);
        if (!MixinBridge.isEnabled()) {
            // PoC disabled - skip reductions and entity work this tick
            return;
        }
        // Emit metrics snapshot occasionally (every 200 ticks)
        if (serverTick % 200 == 0) {
            com.example.fabricopt.MetricsManager.MetricsSnapshot snap = com.example.fabricopt.MetricsManager.snapshotAndReset();
            logger.info("{} metrics snapshot: canceledTicks={} reductions={}", FabricOptMod.MOD_ID, snap.canceledTicks, snap.reductionsApplied);
        }
        if (serverTick % config.getThrottleInterval() == 0) {
            for (ServerWorld world : server.getWorlds()) {
                controller.applyReduction(world, server, serverTick);
            }
        }
        if (entityManager != null) {
            entityManager.onTick(server, serverTick);
        }
    }
}
