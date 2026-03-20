package com.example.fabricopt;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.apache.logging.log4j.Logger;

public class EntityTickManager {
    private final ConfigManager config;
    private final Logger logger;

    public EntityTickManager(ConfigManager config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public void onTick(MinecraftServer server, long serverTick) {
        if (serverTick % config.getThrottleInterval() != 0) {
            return;
        }
        int worldCount = 0;
        for (ServerWorld world : server.getWorlds()) {
            worldCount++;
        }
        logger.info("{} EntityTickManager PoC: tick={} worlds={}", FabricOptMod.MOD_ID, serverTick, worldCount);
        logger.info("{} EntityTickManager PoC: entity throttling not implemented yet; this is a scaffold. Implement via mixins to skip Entity#tick.", FabricOptMod.MOD_ID);
    }
}
