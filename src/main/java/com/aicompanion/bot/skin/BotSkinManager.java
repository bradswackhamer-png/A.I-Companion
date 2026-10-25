package com.aicompanion.bot.skin;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

public class BotSkinManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Cache of discovered skin file paths: relative name -> full Path
    private static final Map<String, Path> DISCOVERED_SKINS = new LinkedHashMap<>();
    // Role-tagged skin files: role -> list of relative skin keys
    private static final Map<String, List<String>> ROLE_SKINS = new LinkedHashMap<>();

    public static Path getSkinsDirectory() {
        return FMLPaths.GAMEDIR.get().resolve("aicompanion").resolve("skins");
    }

    public static Path getConfigSkinsDirectory() {
        return FMLPaths.CONFIGDIR.get().resolve("aicompanion").resolve("skins");
    }

    public static synchronized void ensureDirectoriesExist() {
        Path mainDir = getSkinsDirectory();
        try {
            if (!Files.exists(mainDir)) {
                Files.createDirectories(mainDir);
                LOGGER.info("Created AI Companion skins folder at: {}", mainDir);
            }
            Path readme = mainDir.resolve("README.md");
            if (!Files.exists(readme)) {
                String help = """
                        # AI Companion Skins Folder
                        
                        Drop any standard Minecraft skin `.png` files (64x64 or 64x32) into this folder!
                        
                        ### How Skin Selection Works:
                        1. **Role Subfolders & Auto-Assignment:**
                           You can organize skins into subfolders by role (or prefix filenames with role names):
                           - `/guard/` or `guard_*.png` -> Assigned to Guard companions
                           - `/builder/` or `builder_*.png` -> Assigned to Builder companions
                           - `/lumberjack/` or `lumberjack_*.png` -> Assigned to Lumberjack companions
                           - `/rancher/` or `rancher_*.png` -> Assigned to Rancher / Husbandry companions
                           - `/miner/` or `miner_*.png` -> Assigned to Miner companions
                           - `/farmer/` or `farmer_*.png` -> Assigned to Farmer companions
                           - `/conductor/` or `conductor_*.png` -> Assigned to Conductor companions
                        
                        2. **Exact Bot Name Match:**
                           Name a skin file after a companion (e.g. `gemini.png` or `bob.png`).
                           A companion with that name or nickname will automatically use that skin!
                        
                        3. **Random / Sequential Pool:**
                           Any other skin files placed here will be available in the pool for new bots to pick from.
                        
                        4. **Slim (Alex 3px) vs Wide (Steve 4px) Arms:**
                           Include `_slim`, `slim`, or `alex` in the filename to force Alex 3px arms.
                           Otherwise, standard 4px wide arms are used (or automatically detected from alpha transparency).
                        
                        ### Commands:
                        - `/bot skin list` - Lists all detected skins and their model types.
                        - `/bot skin reload` - Reloads skin textures from disk immediately.
                        - `/bot skin set <botName> <skinName>` - Assigns a specific skin to a bot.
                        """;
                Files.writeString(readme, help);
            }

            // Also check if user has desktop folder and copy initial skins if target is empty
            Path desktopSkins = Path.of(System.getProperty("user.home"), "Desktop", "minecraft skins");
            if (Files.exists(desktopSkins) && isEmptyDir(mainDir)) {
                copyDirectoryRecursively(desktopSkins, mainDir);
                LOGGER.info("Imported starter skins from Desktop/minecraft skins into {}", mainDir);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not setup skins directory: {}", e.getMessage());
        }
        scanSkins();
    }

    private static boolean isEmptyDir(Path dir) {
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.getFileName().toString().endsWith(".png")).findAny().isEmpty();
        } catch (Exception e) {
            return true;
        }
    }

    private static void copyDirectoryRecursively(Path source, Path target) {
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(src -> {
                try {
                    Path dest = target.resolve(source.relativize(src));
                    if (Files.isDirectory(src)) {
                        if (!Files.exists(dest)) Files.createDirectories(dest);
                    } else if (src.getFileName().toString().endsWith(".png")) {
                        if (!Files.exists(dest)) Files.copy(src, dest);
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    public static synchronized void scanSkins() {
        DISCOVERED_SKINS.clear();
        ROLE_SKINS.clear();

        List<Path> scanRoots = new ArrayList<>();
        Path main = getSkinsDirectory();
        if (Files.exists(main)) scanRoots.add(main);
        Path cfg = getConfigSkinsDirectory();
        if (Files.exists(cfg)) scanRoots.add(cfg);
        Path desktop = Path.of(System.getProperty("user.home"), "Desktop", "minecraft skins");
        if (Files.exists(desktop)) scanRoots.add(desktop);

        for (Path root : scanRoots) {
            try (Stream<Path> walk = Files.walk(root)) {
                walk.filter(Files::isRegularFile).forEach(p -> {
                    String fileName = p.getFileName().toString();
                    if (fileName.toLowerCase(Locale.ROOT).endsWith(".png")) {
                        String rel = root.relativize(p).toString().replace('\\', '/');
                        String key = rel.toLowerCase(Locale.ROOT);
                        DISCOVERED_SKINS.putIfAbsent(key, p);

                        // Tag with role if path or filename contains role identifier
                        String check = key;
                        tagRoleIfMatches(check, "guard", key);
                        tagRoleIfMatches(check, "builder", key);
                        tagRoleIfMatches(check, "lumberjack", key);
                        tagRoleIfMatches(check, "rancher", key);
                        tagRoleIfMatches(check, "husbandry", key);
                        tagRoleIfMatches(check, "farmer", key);
                        tagRoleIfMatches(check, "miner", key);
                        tagRoleIfMatches(check, "conductor", key);
                    }
                });
            } catch (Exception e) {
                LOGGER.warn("Error scanning skins in {}: {}", root, e.getMessage());
            }
        }
        LOGGER.info("Discovered {} custom companion skins across {} role categories", DISCOVERED_SKINS.size(), ROLE_SKINS.size());
    }

    private static void tagRoleIfMatches(String pathKey, String role, String fullKey) {
        if (pathKey.contains(role)) {
            ROLE_SKINS.computeIfAbsent(role, k -> new ArrayList<>()).add(fullKey);
        }
    }

    public static synchronized Map<String, Path> getDiscoveredSkins() {
        if (DISCOVERED_SKINS.isEmpty()) {
            ensureDirectoriesExist();
        }
        return new LinkedHashMap<>(DISCOVERED_SKINS);
    }

    public static synchronized List<String> getAvailableSkinNames() {
        return new ArrayList<>(getDiscoveredSkins().keySet());
    }

    public static synchronized String determineSkinForBot(String botName, String role, List<String> aliases, UUID botUuid) {
        ensureDirectoriesExist();
        if (DISCOVERED_SKINS.isEmpty()) {
            return "";
        }

        // 1. Check exact bot name match
        String cleanName = botName.toLowerCase(Locale.ROOT).trim();
        for (String key : DISCOVERED_SKINS.keySet()) {
            String base = getBaseName(key);
            if (base.equals(cleanName)) {
                return key;
            }
        }

        // 2. Check alias matches
        if (aliases != null) {
            for (String alias : aliases) {
                String cleanAlias = alias.toLowerCase(Locale.ROOT).trim();
                for (String key : DISCOVERED_SKINS.keySet()) {
                    String base = getBaseName(key);
                    if (base.equals(cleanAlias)) {
                        return key;
                    }
                }
            }
        }

        // 3. Check role matches
        String r = role != null ? role.toLowerCase(Locale.ROOT) : "";
        for (var entry : ROLE_SKINS.entrySet()) {
            if (r.contains(entry.getKey()) && !entry.getValue().isEmpty()) {
                List<String> list = entry.getValue();
                int idx = Math.abs(botUuid.hashCode()) % list.size();
                return list.get(idx);
            }
        }

        // 4. Fallback: deterministic selection from all skins in pool
        List<String> all = new ArrayList<>(DISCOVERED_SKINS.keySet());
        int idx = Math.abs(botUuid.hashCode()) % all.size();
        return all.get(idx);
    }

    public static String getBaseName(String pathKey) {
        int slash = pathKey.lastIndexOf('/');
        String fn = (slash >= 0) ? pathKey.substring(slash + 1) : pathKey;
        if (fn.endsWith(".png")) {
            return fn.substring(0, fn.length() - 4);
        }
        return fn;
    }
}
