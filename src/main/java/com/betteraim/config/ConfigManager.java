package com.betteraim.config;

import com.betteraim.BetterAimClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("better-aim.json");

    private static BetterAimConfig config = null;

    // Prevent instantiation
    private ConfigManager() {}

    // ── Public API ──────────────────────────────────────────────────────────

    /** Load (or create) the config from disk.  Never returns null. */
    public static BetterAimConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                BetterAimConfig loaded = GSON.fromJson(reader, BetterAimConfig.class);
                if (loaded != null) {
                    loaded.validate();
                    config = loaded;
                    return config;
                }
            } catch (Exception e) {
                BetterAimClient.LOGGER.warn("[BetterAIM] Failed to load config; resetting to defaults.", e);
                backupCorruptedConfig();
            }
        }
        config = new BetterAimConfig();
        save();
        return config;
    }

    /** Get the currently active config. Calls load() if not yet initialised. */
    public static BetterAimConfig getConfig() {
        if (config == null) load();
        return config;
    }

    /** Persist the current config to disk atomically. */
    public static void save() {
        if (config == null) return;
        try {
            Path tmp = CONFIG_PATH.resolveSibling("better-aim.json.tmp");
            try (Writer writer = Files.newBufferedWriter(tmp)) {
                GSON.toJson(config, writer);
            }
            Files.move(tmp, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            BetterAimClient.LOGGER.error("[BetterAIM] Failed to save config.", e);
        }
    }

    /** Reset the entire config to defaults and save. */
    public static void resetAll() {
        config = new BetterAimConfig();
        save();
    }

    // ── Internal ────────────────────────────────────────────────────────────

    private static void backupCorruptedConfig() {
        try {
            Path backup = CONFIG_PATH.resolveSibling("better-aim.json.bak");
            Files.copy(CONFIG_PATH, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}
