package com.example.fabricopt;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FabricOptMod implements ModInitializer {
    public static final String MOD_ID = "fabricopt";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    // Exposed for mixins and other components (nullable until initialized)
    public static volatile ConfigManager CONFIG = null;
    public static volatile boolean ENABLE_POC = false;
    public static volatile boolean MIXIN_LOG_ONLY = true;
    public static volatile long SERVER_TICK = 0L;

    private ConfigManager config;
    private GameRuleController grController;
    private TickManager tickManager;

    @Override
    public void onInitialize() {
        this.config = new ConfigManager();
        FabricOptMod.CONFIG = this.config;
        FabricOptMod.ENABLE_POC = this.config.isEnablePoC();
        FabricOptMod.MIXIN_LOG_ONLY = this.config.isMixinLogOnly();
        MixinBridge.init(this.config);

        // Register a simple /fabricopt reload command so operators can reload config at runtime even if PoC is disabled.
        net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(net.minecraft.server.command.CommandManager.literal("fabricopt")
                .requires(src -> src.hasPermissionLevel(2))
                .then(net.minecraft.server.command.CommandManager.literal("reload")
                    .executes(context -> {
                        boolean ok = FabricOptMod.CONFIG.reload();
                        FabricOptMod.ENABLE_POC = FabricOptMod.CONFIG.isEnablePoC();
                        FabricOptMod.MIXIN_LOG_ONLY = FabricOptMod.CONFIG.isMixinLogOnly();
                        MixinBridge.init(FabricOptMod.CONFIG);
                        if (ok) {
                            FabricOptMod.LOGGER.info("{} config reloaded: enablePoC={} mixinLogOnly={} throttleInterval={}", MOD_ID, FabricOptMod.ENABLE_POC, FabricOptMod.MIXIN_LOG_ONLY, FabricOptMod.CONFIG.getThrottleInterval());
                            context.getSource().sendFeedback(net.minecraft.text.Text.literal("fabricopt: config reloaded"), false);
                            return 1;
                        } else {
                            context.getSource().sendError(net.minecraft.text.Text.literal("fabricopt: config reload failed"));
                            return 0;
                        }
                    })
                )
            );
        });

        // PoC may be enabled/disabled at runtime; continue initializing controllers and register tick handlers.
        this.grController = new GameRuleController(config.getRestoreDelayTicks(), LOGGER);
        EntityTickManager entityManager = new EntityTickManager(config, LOGGER);
        this.tickManager = new TickManager(config, grController, LOGGER, entityManager);

        ServerTickEvents.END_SERVER_TICK.register((MinecraftServer server) -> tickManager.onServerTick(server));
        ServerLifecycleEvents.SERVER_STOPPING.register((MinecraftServer server) -> grController.restoreAll(server));

        LOGGER.info("{} initialized with throttleInterval={} restoreDelayTicks={} mixinLogOnly={}", MOD_ID, config.getThrottleInterval(), config.getRestoreDelayTicks(), config.isMixinLogOnly());
    }
}
