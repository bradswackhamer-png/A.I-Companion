package com.aicompanion.bot.client;

import com.aicompanion.bot.AiCompanionMod;
import com.aicompanion.bot.skin.BotSkinManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
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
    private static final List<PlayerSkin> ALL_LOADED_SKINS_LIST = new ArrayList<>();

    public static void onRegisterRenderStateModifiers(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState renderState) {
                PlayerSkin customSkin = resolveSkinFor(avatar);
                if (customSkin != null) {
                    renderState.skin = customSkin;
                }
            }
        });
    }

    public static synchronized void setBotSkin(UUID botUuid, String skinName) {
        if (botUuid == null) return;
        if (skinName == null || skinName.isBlank()) {
            BOT_UUID_TO_SKIN.remove(botUuid);
        } else {
            BOT_UUID_TO_SKIN.put(botUuid, skinName.toLowerCase(Locale.ROOT).trim());
        }
    }

    public static synchronized void loadAllSkins() {
        LOADED_SKINS.clear();
        ALL_LOADED_SKINS_LIST.clear();

        Map<String, Path> discovered = BotSkinManager.getDiscoveredSkins();
        if (discovered.isEmpty()) {
            LOGGER.info("No custom bot skins detected to load.");
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getTextureManager() == null) {
            return;
        }

        for (var entry : discovered.entrySet()) {
            String key = entry.getKey();
            Path path = entry.getValue();

            try (InputStream is = Files.newInputStream(path)) {
                NativeImage image = NativeImage.read(is);

                boolean isSlim = key.contains("slim") || key.contains("alex");
                if (!isSlim && image.getWidth() == 64 && image.getHeight() == 64) {
                    int pixel = image.getPixel(55, 20);
                    int alpha = (pixel >> 24) & 0xFF;
                    if (alpha == 0) {
                        isSlim = true;
                    }
                }
                PlayerModelType model = isSlim ? PlayerModelType.SLIM : PlayerModelType.WIDE;

                String safeKey = key.replaceAll("[^a-z0-9_.-]", "_");
                Identifier textureId = Identifier.fromNamespaceAndPath(AiCompanionMod.MODID, "skin/" + safeKey);
                DynamicTexture texture = new DynamicTexture(() -> "ai_bot_skin_" + safeKey, image);
                mc.getTextureManager().register(textureId, texture);

                PlayerSkin skin = PlayerSkin.insecure(new ClientAsset.ResourceTexture(textureId), null, null, model);
                LOADED_SKINS.put(key, skin);
                String base = BotSkinManager.getBaseName(key);
                LOADED_SKINS.put(base, skin);
                ALL_LOADED_SKINS_LIST.add(skin);

                LOGGER.debug("Loaded custom companion skin: {} (Model: {})", key, model);
            } catch (Exception e) {
                LOGGER.warn("Failed to load companion skin '{}' from {}: {}", key, path, e.getMessage());
            }
        }
        LOGGER.info("Successfully loaded {} custom companion skin textures into client TextureManager", ALL_LOADED_SKINS_LIST.size());
    }

    public static PlayerSkin resolveSkinFor(Avatar avatar) {
        if (avatar == null || ALL_LOADED_SKINS_LIST.isEmpty()) {
            return null;
        }

        UUID uuid = avatar.getUUID();
        if (uuid != null) {
            String assigned = BOT_UUID_TO_SKIN.get(uuid);
            if (assigned != null) {
                PlayerSkin s = LOADED_SKINS.get(assigned);
                if (s != null) return s;
                String base = BotSkinManager.getBaseName(assigned);
                s = LOADED_SKINS.get(base);
                if (s != null) return s;
            }
        }

        String name = avatar.getName().getString().toLowerCase(Locale.ROOT).trim();
        PlayerSkin namedMatch = LOADED_SKINS.get(name);
        if (namedMatch != null) {
            return namedMatch;
        }

        // If it's a companion bot (either custom UUID format or known bot name)
        if (isCompanionAvatar(avatar)) {
            int idx = Math.abs(avatar.getUUID().hashCode()) % ALL_LOADED_SKINS_LIST.size();
            return ALL_LOADED_SKINS_LIST.get(idx);
        }

        return null;
    }

    private static boolean isCompanionAvatar(Avatar avatar) {
        if (avatar == null) return false;
        // Check if the UUID matches BotManager's deterministic UUID generation
        String name = avatar.getName().getString().toLowerCase(Locale.ROOT).trim();
        UUID expectedUuid = UUID.nameUUIDFromBytes(("AiBotCompanion:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (expectedUuid.equals(avatar.getUUID())) {
            return true;
        }
        // Check if registered in client bot skin assignments
        return BOT_UUID_TO_SKIN.containsKey(avatar.getUUID());
    }
}
