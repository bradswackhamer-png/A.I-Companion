package com.aicompanion.bot.client;

import com.aicompanion.bot.AiCompanionMod;
import com.aicompanion.bot.skin.BotSkinManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class BotClientSkinManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<String, PlayerSkin> LOADED_SKINS = new ConcurrentHashMap<>();
    private static final Map<UUID, String> BOT_UUID_TO_SKIN = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerSkin> APPLIED_PLAYER_INFO_SKINS = new ConcurrentHashMap<>();
    private static final List<PlayerSkin> ALL_LOADED_SKINS_LIST = new ArrayList<>();
    private static volatile long lastEmptyScanTimeMs = 0L;

    public static void onRegisterRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState renderState) {
                PlayerSkin customSkin = resolveSkinFor(avatar);
                if (customSkin != null) {
                    renderState.skin = customSkin;
                    renderState.showHat = true;
                    renderState.showJacket = true;
                    renderState.showLeftPants = true;
                    renderState.showRightPants = true;
                    renderState.showLeftSleeve = true;
                    renderState.showRightSleeve = true;
                }
            }
        });
    }

    public static void handleSyncPayload(UUID botUuid, String skinName, byte[] skinBytes) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        if (!RenderSystem.isOnRenderThread()) {
            mc.execute(() -> handleSyncPayload(botUuid, skinName, skinBytes));
            return;
        }

        if ("__reload__".equalsIgnoreCase(skinName)) {
            loadAllSkins();
            return;
        }

        String cleanName = normalizeSkinKey(skinName);
        if (skinBytes != null && skinBytes.length > 0 && !cleanName.isBlank()) {
            registerSkinFromBytes(cleanName, skinBytes);
        }

        setBotSkin(botUuid, cleanName);
    }

    public static synchronized void setBotSkin(UUID botUuid, String skinName) {
        if (botUuid == null) return;
        APPLIED_PLAYER_INFO_SKINS.remove(botUuid);
        String clean = normalizeSkinKey(skinName);
        if (clean.isBlank()) {
            BOT_UUID_TO_SKIN.remove(botUuid);
            return;
        }

        BOT_UUID_TO_SKIN.put(botUuid, clean);
        PlayerSkin found = findLoadedSkin(clean);
        if (found == null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                if (RenderSystem.isOnRenderThread()) {
                    loadAllSkins();
                    found = findLoadedSkin(clean);
                } else {
                    mc.execute(BotClientSkinManager::loadAllSkins);
                }
            }
        }
        if (found != null) {
            applySkinToPlayerInfo(botUuid, found);
        }
    }

    public static synchronized void loadAllSkins() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        if (!RenderSystem.isOnRenderThread()) {
            mc.execute(BotClientSkinManager::loadAllSkins);
            return;
        }
        if (mc.getTextureManager() == null) {
            return;
        }

        BotSkinManager.ensureDirectoriesExist();
        BotSkinManager.scanSkins();
        Map<String, Path> discovered = BotSkinManager.getDiscoveredSkins();

        LOADED_SKINS.clear();
        ALL_LOADED_SKINS_LIST.clear();
        APPLIED_PLAYER_INFO_SKINS.clear();

        if (discovered.isEmpty()) {
            LOGGER.info("No custom bot skins detected to load.");
            return;
        }

        for (var entry : discovered.entrySet()) {
            String key = entry.getKey();
            Path path = entry.getValue();
            try {
                byte[] bytes = Files.readAllBytes(path);
                registerSkinFromBytes(key, bytes);
            } catch (Exception e) {
                LOGGER.warn("Failed to load companion skin '{}' from {}: {}", key, path, e.getMessage());
            }
        }

        for (var entry : BOT_UUID_TO_SKIN.entrySet()) {
            PlayerSkin skin = findLoadedSkin(entry.getValue());
            if (skin != null) {
                applySkinToPlayerInfo(entry.getKey(), skin);
            }
        }

        LOGGER.info("Successfully loaded {} custom companion skin textures into client TextureManager", ALL_LOADED_SKINS_LIST.size());
    }

    private static synchronized PlayerSkin registerSkinFromBytes(String rawKey, byte[] pngBytes) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getTextureManager() == null || pngBytes == null || pngBytes.length == 0) {
            return null;
        }

        String key = normalizeSkinKey(rawKey);
        if (key.isBlank()) {
            return null;
        }

        try {
            NativeImage rawImage = NativeImage.read(pngBytes);
            int origWidth = rawImage.getWidth();
            int origHeight = rawImage.getHeight();

            boolean isSlim = key.contains("slim") || key.contains("alex");
            if (!isSlim && origWidth == 64 && origHeight == 64) {
                int pixel54 = rawImage.getPixel(54, 20);
                int pixel55 = rawImage.getPixel(55, 20);
                if (ARGB.alpha(pixel54) == 0 || ARGB.alpha(pixel55) == 0) {
                    isSlim = true;
                }
            }

            NativeImage processedImage = processSkinImage(rawImage);
            PlayerModelType model = isSlim ? PlayerModelType.SLIM : PlayerModelType.WIDE;

            String safeKey = key.replaceAll("[^a-z0-9_.-]", "_");
            Identifier textureId = Identifier.fromNamespaceAndPath(AiCompanionMod.MODID, "skin/" + safeKey);
            DynamicTexture texture = new DynamicTexture(() -> "ai_bot_skin_" + safeKey, processedImage);
            mc.getTextureManager().register(textureId, texture);

            // Use the 2-arg ResourceTexture(id, texturePath) constructor so skin.body().texturePath()
            // matches the exact Identifier registered in TextureManager!
            ClientAsset.ResourceTexture asset = new ClientAsset.ResourceTexture(textureId, textureId);
            PlayerSkin skin = PlayerSkin.insecure(asset, null, null, model);

            LOADED_SKINS.put(key, skin);
            if (!key.endsWith(".png")) {
                LOADED_SKINS.put(key + ".png", skin);
            }
            String base = BotSkinManager.getBaseName(key);
            LOADED_SKINS.put(base, skin);
            LOADED_SKINS.put(base + ".png", skin);

            if (!ALL_LOADED_SKINS_LIST.contains(skin)) {
                ALL_LOADED_SKINS_LIST.add(skin);
            }

            LOGGER.debug("Loaded custom companion skin: {} ({}x{} -> {}x{}, Model: {})",
                    key, origWidth, origHeight, processedImage.getWidth(), processedImage.getHeight(), model);
            return skin;
        } catch (Exception e) {
            LOGGER.warn("Failed to register companion skin texture '{}': {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Converts legacy 64x32 skins (and scaled 2:1 legacy skins) into 64x64 format with mirrored
     * left arm/leg regions, Notch hat transparency fix, and opaque base body regions matching
     * vanilla SkinTextureDownloader.processLegacySkin.
     */
    private static NativeImage processSkinImage(NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        if (width == 64 && (height == 32 || height == 64)) {
            boolean isLegacy = height == 32;
            if (isLegacy) {
                NativeImage newImage = new NativeImage(64, 64, true);
                newImage.copyFrom(image);
                image.close();
                image = newImage;
                image.fillRect(0, 32, 64, 32, 0);
                image.copyRect(4, 16, 16, 32, 4, 4, true, false);
                image.copyRect(8, 16, 16, 32, 4, 4, true, false);
                image.copyRect(0, 20, 24, 32, 4, 12, true, false);
                image.copyRect(4, 20, 16, 32, 4, 12, true, false);
                image.copyRect(8, 20, 8, 32, 4, 12, true, false);
                image.copyRect(12, 20, 16, 32, 4, 12, true, false);
                image.copyRect(44, 16, -8, 32, 4, 4, true, false);
                image.copyRect(48, 16, -8, 32, 4, 4, true, false);
                image.copyRect(40, 20, 0, 32, 4, 12, true, false);
                image.copyRect(44, 20, -8, 32, 4, 12, true, false);
                image.copyRect(48, 20, -16, 32, 4, 12, true, false);
                image.copyRect(52, 20, -8, 32, 4, 12, true, false);
            }

            setNoAlpha(image, 0, 0, 32, 16);
            if (isLegacy) {
                doNotchTransparencyHack(image, 32, 0, 64, 32);
            }
            setNoAlpha(image, 0, 16, 64, 32);
            setNoAlpha(image, 16, 48, 48, 64);
            return image;
        } else if (width > 64 && width == height * 2) {
            // Support HD legacy 2:1 skins (e.g. 128x64) by converting to square 1:1
            int s = width / 64;
            NativeImage newImage = new NativeImage(width, width, true);
            newImage.copyFrom(image);
            image.close();
            image = newImage;
            image.fillRect(0, 32 * s, 64 * s, 32 * s, 0);
            image.copyRect(4 * s, 16 * s, 16 * s, 32 * s, 4 * s, 4 * s, true, false);
            image.copyRect(8 * s, 16 * s, 16 * s, 32 * s, 4 * s, 4 * s, true, false);
            image.copyRect(0, 20 * s, 24 * s, 32 * s, 4 * s, 12 * s, true, false);
            image.copyRect(4 * s, 20 * s, 16 * s, 32 * s, 4 * s, 12 * s, true, false);
            image.copyRect(8 * s, 20 * s, 8 * s, 32 * s, 4 * s, 12 * s, true, false);
            image.copyRect(12 * s, 20 * s, 16 * s, 32 * s, 4 * s, 12 * s, true, false);
            image.copyRect(44 * s, 16 * s, -8 * s, 32 * s, 4 * s, 4 * s, true, false);
            image.copyRect(48 * s, 16 * s, -8 * s, 32 * s, 4 * s, 4 * s, true, false);
            image.copyRect(40 * s, 20 * s, 0, 32 * s, 4 * s, 12 * s, true, false);
            image.copyRect(44 * s, 20 * s, -8 * s, 32 * s, 4 * s, 12 * s, true, false);
            image.copyRect(48 * s, 20 * s, -16 * s, 32 * s, 4 * s, 12 * s, true, false);
            image.copyRect(52 * s, 20 * s, -8 * s, 32 * s, 4 * s, 12 * s, true, false);
            setNoAlpha(image, 0, 0, 32 * s, 16 * s);
            doNotchTransparencyHack(image, 32 * s, 0, 64 * s, 32 * s);
            setNoAlpha(image, 0, 16 * s, 64 * s, 32 * s);
            setNoAlpha(image, 16 * s, 48 * s, 48 * s, 64 * s);
            return image;
        }

        return image;
    }

    private static void doNotchTransparencyHack(NativeImage image, int x0, int y0, int x1, int y1) {
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                int pix = image.getPixel(x, y);
                if (ARGB.alpha(pix) < 128) {
                    return;
                }
            }
        }
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                image.setPixel(x, y, image.getPixel(x, y) & 0x00FFFFFF);
            }
        }
    }

    private static void setNoAlpha(NativeImage image, int x0, int y0, int x1, int y1) {
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                image.setPixel(x, y, ARGB.opaque(image.getPixel(x, y)));
            }
        }
    }

    public static PlayerSkin resolveSkinFor(Avatar avatar) {
        if (avatar == null || !isCompanionAvatar(avatar)) {
            return null;
        }

        if (ALL_LOADED_SKINS_LIST.isEmpty() && RenderSystem.isOnRenderThread()) {
            long now = System.currentTimeMillis();
            if (now - lastEmptyScanTimeMs > 3000L) {
                lastEmptyScanTimeMs = now;
                loadAllSkins();
            }
        }

        if (ALL_LOADED_SKINS_LIST.isEmpty()) {
            return null;
        }

        PlayerSkin resolved = null;
        UUID uuid = avatar.getUUID();
        if (uuid != null) {
            String assigned = BOT_UUID_TO_SKIN.get(uuid);
            if (assigned != null) {
                resolved = findLoadedSkin(assigned);
            }
        }

        if (resolved == null) {
            String name = avatar.getName().getString().toLowerCase(Locale.ROOT).trim();
            resolved = findLoadedSkin(name);
        }

        if (resolved == null) {
            int idx = Math.floorMod(avatar.getUUID().hashCode(), ALL_LOADED_SKINS_LIST.size());
            resolved = ALL_LOADED_SKINS_LIST.get(idx);
        }

        if (resolved != null && uuid != null) {
            applySkinToPlayerInfo(uuid, resolved);
        }
        return resolved;
    }

    private static PlayerSkin findLoadedSkin(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return null;
        }
        String key = normalizeSkinKey(rawName);
        PlayerSkin s = LOADED_SKINS.get(key);
        if (s != null) return s;
        if (!key.endsWith(".png")) {
            s = LOADED_SKINS.get(key + ".png");
            if (s != null) return s;
        }
        String base = BotSkinManager.getBaseName(key);
        s = LOADED_SKINS.get(base);
        if (s != null) return s;
        for (var entry : LOADED_SKINS.entrySet()) {
            if (entry.getKey().endsWith("/" + key) || entry.getKey().endsWith("/" + base + ".png")) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static void applySkinToPlayerInfo(UUID uuid, PlayerSkin customSkin) {
        if (uuid == null || customSkin == null) return;
        if (APPLIED_PLAYER_INFO_SKINS.get(uuid) == customSkin) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) return;
        PlayerInfo info = connection.getPlayerInfo(uuid);
        if (info == null) return;

        try {
            for (Field f : PlayerInfo.class.getDeclaredFields()) {
                if (Supplier.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    f.set(info, (Supplier<PlayerSkin>) () -> customSkin);
                    APPLIED_PLAYER_INFO_SKINS.put(uuid, customSkin);
                    break;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static String normalizeSkinKey(String skinName) {
        if (skinName == null) return "";
        String clean = skinName.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
        if (clean.startsWith("\"") && clean.endsWith("\"") && clean.length() >= 2) {
            clean = clean.substring(1, clean.length() - 1).trim();
        }
        return clean;
    }

    private static boolean isCompanionAvatar(Avatar avatar) {
        if (avatar == null) return false;
        if (BOT_UUID_TO_SKIN.containsKey(avatar.getUUID())) {
            return true;
        }
        String name = avatar.getName().getString().toLowerCase(Locale.ROOT).trim();
        UUID expectedUuid = UUID.nameUUIDFromBytes(("AiBotCompanion:" + name).getBytes(StandardCharsets.UTF_8));
        return expectedUuid.equals(avatar.getUUID());
    }
}
