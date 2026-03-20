package com.example.fabricopt;

import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Path;
import java.util.Properties;
import java.io.InputStream;
import java.nio.file.Files;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.Collections;

public class ConfigManager {
    private volatile int throttleInterval;
    private volatile boolean enablePoC;
    private volatile int restoreDelayTicks;
    private volatile boolean mixinLogOnly;
    private volatile Set<String> throttleWhitelist;
    private final Path cfgPath;

n    public ConfigManager() {
        this.cfgPath = FabricLoader.getInstance().getConfigDir().resolve("fabricopt.properties");
        // defaults
        this.throttleInterval = 100;
        this.enablePoC = true;
        this.restoreDelayTicks = 20;
        this.mixinLogOnly = true;
        this.throttleWhitelist = Collections.emptySet();

n        try {
            if (Files.exists(cfgPath)) {
                loadFromFile();
            } else {
                writeDefaults();
            }
        } catch (Exception e) {
            FabricOptMod.LOGGER.warn("{} failed to initialize config, using defaults: {}", FabricOptMod.MOD_ID, e.toString());
        } finally {
            // Ensure MixinBridge is initialized with current config snapshot
            MixinBridge.init(this);
        }
    }

n    private synchronized void loadFromFile() throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(cfgPath)) {
            props.load(in);
        }
        try {
            this.throttleInterval = Math.max(1, Integer.parseInt(props.getProperty("throttleInterval", String.valueOf(this.throttleInterval))));
        } catch (NumberFormatException e) {
            FabricOptMod.LOGGER.warn("{} invalid throttleInterval in config, using {}", FabricOptMod.MOD_ID, this.throttleInterval);
        }
        this.enablePoC = Boolean.parseBoolean(props.getProperty("enablePoC", String.valueOf(this.enablePoC)));
        try {
            this.restoreDelayTicks = Math.max(1, Integer.parseInt(props.getProperty("restoreDelayTicks", String.valueOf(this.restoreDelayTicks))));
        } catch (NumberFormatException e) {
            FabricOptMod.LOGGER.warn("{} invalid restoreDelayTicks in config, using {}", FabricOptMod.MOD_ID, this.restoreDelayTicks);
        }
        this.mixinLogOnly = Boolean.parseBoolean(props.getProperty("mixinLogOnly", String.valueOf(this.mixinLogOnly)));

        String whitelistProp = props.getProperty("throttleWhitelist", "").trim();
        if (whitelistProp.isEmpty()) {
            this.throttleWhitelist = Collections.emptySet();
        } else {
            Set<String> s = Arrays.stream(whitelistProp.split(","))
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .collect(Collectors.toSet());
            this.throttleWhitelist = Collections.unmodifiableSet(s);
        }

n        FabricOptMod.LOGGER.info("{} loaded config: throttleInterval={} enablePoC={} restoreDelayTicks={} mixinLogOnly={} throttleWhitelist={}", FabricOptMod.MOD_ID, this.throttleInterval, this.enablePoC, this.restoreDelayTicks, this.mixinLogOnly, this.throttleWhitelist);
    }

n    private synchronized void writeDefaults() {
        Properties props = new Properties();
        props.setProperty("throttleInterval", String.valueOf(this.throttleInterval));
        props.setProperty("enablePoC", String.valueOf(this.enablePoC));
        props.setProperty("restoreDelayTicks", String.valueOf(this.restoreDelayTicks));
        props.setProperty("mixinLogOnly", String.valueOf(this.mixinLogOnly));
        props.setProperty("throttleWhitelist", "");
        try {
            Files.createDirectories(cfgPath.getParent());
            try (OutputStream out = Files.newOutputStream(cfgPath)) {
                props.store(out, "FabricOptMod configuration");
                FabricOptMod.LOGGER.info("{} wrote default config to {}", FabricOptMod.MOD_ID, cfgPath);
            }
        } catch (IOException e) {
            FabricOptMod.LOGGER.error("{} failed to write default config to {}: {}", FabricOptMod.MOD_ID, cfgPath, e.toString(), e);
        }
    }

n    public synchronized boolean reload() {
        try {
            if (Files.exists(cfgPath)) {
                loadFromFile();
                FabricOptMod.LOGGER.info("{} config reloaded from {}", FabricOptMod.MOD_ID, cfgPath);
                // Update mixin bridge snapshot after reload
                MixinBridge.init(this);
                return true;
            } else {
                FabricOptMod.LOGGER.warn("{} config file not found at {}, writing defaults", FabricOptMod.MOD_ID, cfgPath);
                writeDefaults();
                // Ensure mixin bridge sees defaults
                MixinBridge.init(this);
                return true;
            }
        } catch (Exception e) {
            FabricOptMod.LOGGER.error("{} failed to reload config: {}", FabricOptMod.MOD_ID, e.toString(), e);
            return false;
        }
    }

n    public int getThrottleInterval() { return throttleInterval; }
    public boolean isEnablePoC() { return enablePoC; }
    public int getRestoreDelayTicks() { return restoreDelayTicks; }
    public boolean isMixinLogOnly() { return mixinLogOnly; }
    public Set<String> getThrottleWhitelist() { return throttleWhitelist; }
}
