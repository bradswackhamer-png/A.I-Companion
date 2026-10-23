package com.aicompanion.bot.compat;

import com.aicompanion.bot.AiBotPlayer;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Clean reflection compatibility bridge for the JaneMoose tractor mod.
 * Allows AI Companion bots to mount, fuel, steer, and operate tractors & farm attachments.
 * If JaneMoose is not installed, all methods safely no-op without crashes.
 */
public class TractorCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean AVAILABLE;
    private static Class<?> TRACTOR_CLASS = null;
    private static Class<?> ATTACHMENT_CLASS = null;
    private static Method GET_FUEL_METHOD = null;
    private static Method SET_FUEL_METHOD = null;
    private static Method TOGGLE_LIFT_METHOD = null;
    private static Method TOGGLE_ACTIVE_METHOD = null;
    private static Method HAS_HITCHED_ATTACHMENT_METHOD = null;
    private static Method GET_HITCHED_ATTACHMENT_METHOD = null;
    private static Method ATTACHMENT_IS_LOWERED_METHOD = null;
    private static Method ATTACHMENT_IS_ACTIVE_METHOD = null;

    static {
        boolean avail = false;
        try {
            TRACTOR_CLASS = Class.forName("com.swack.janemoose.entity.JaneMooseTractorEntity");
            GET_FUEL_METHOD = TRACTOR_CLASS.getMethod("getFuel");
            SET_FUEL_METHOD = TRACTOR_CLASS.getMethod("setFuel", int.class);
            TOGGLE_LIFT_METHOD = TRACTOR_CLASS.getMethod("toggleAttachmentLift", Player.class);
            TOGGLE_ACTIVE_METHOD = TRACTOR_CLASS.getMethod("toggleAttachmentActive", Player.class);
            HAS_HITCHED_ATTACHMENT_METHOD = TRACTOR_CLASS.getMethod("hasHitchedAttachment");
            GET_HITCHED_ATTACHMENT_METHOD = TRACTOR_CLASS.getMethod("getHitchedAttachment");

            try {
                ATTACHMENT_CLASS = Class.forName("com.swack.janemoose.entity.JaneMooseAttachmentEntity");
                ATTACHMENT_IS_LOWERED_METHOD = ATTACHMENT_CLASS.getMethod("isLowered");
                ATTACHMENT_IS_ACTIVE_METHOD = ATTACHMENT_CLASS.getMethod("isActive");
            } catch (Exception ignored) {}

            avail = true;
            LOGGER.info("JaneMoose Tractor compatibility bridge successfully initialized!");
        } catch (ClassNotFoundException e) {
            avail = false;
        } catch (Exception e) {
            LOGGER.warn("Failed to initialize JaneMoose Tractor reflection bridge", e);
            avail = false;
        }
        AVAILABLE = avail;
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static boolean isTractor(@Nullable Entity entity) {
        if (!AVAILABLE || entity == null || TRACTOR_CLASS == null) return false;
        return TRACTOR_CLASS.isInstance(entity);
    }

    public static @Nullable Entity findNearbyTractor(ServerLevel level, Vec3 pos, double radius) {
        if (!AVAILABLE || TRACTOR_CLASS == null) return null;
        AABB box = new AABB(pos.x - radius, pos.y - radius, pos.z - radius,
                pos.x + radius, pos.y + radius, pos.z + radius);
        List<Entity> list = level.getEntities((Entity) null, box, TractorCompat::isTractor);
        if (list.isEmpty()) return null;

        Entity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (Entity e : list) {
            double dSq = e.distanceToSqr(pos);
            if (dSq < nearestDistSq) {
                nearestDistSq = dSq;
                nearest = e;
            }
        }
        return nearest;
    }

    public static boolean isRidingTractor(Player player) {
        if (!AVAILABLE || player == null) return false;
        Entity v = player.getVehicle();
        return isTractor(v);
    }

    public static @Nullable Entity getRiddenTractor(Player player) {
        if (!AVAILABLE || player == null) return null;
        Entity v = player.getVehicle();
        return isTractor(v) ? v : null;
    }

    public static boolean mountTractor(AiBotPlayer bot, Entity tractor) {
        if (!AVAILABLE || tractor == null || !isTractor(tractor)) return false;
        if (bot.getVehicle() == tractor) return true;
        // Mount tractor
        return bot.startRiding(tractor);
    }

    public static void dismountTractor(AiBotPlayer bot) {
        if (bot.getVehicle() != null) {
            bot.stopRiding();
            bot.zza = 0.0f;
            bot.xxa = 0.0f;
        }
    }

    public static int getFuel(Entity tractor) {
        if (!AVAILABLE || tractor == null || GET_FUEL_METHOD == null) return 0;
        try {
            return (int) GET_FUEL_METHOD.invoke(tractor);
        } catch (Exception e) {
            return 0;
        }
    }

    public static void setFuel(Entity tractor, int fuel) {
        if (!AVAILABLE || tractor == null || SET_FUEL_METHOD == null) return;
        try {
            SET_FUEL_METHOD.invoke(tractor, fuel);
        } catch (Exception ignored) {}
    }

    public static boolean refuelTractorIfLow(AiBotPlayer bot, Entity tractor) {
        if (!AVAILABLE || tractor == null) return false;
        int fuel = getFuel(tractor);
        if (fuel >= 500) return true; // Plenty of fuel

        // Try to consume coal/charcoal or blaze rod from bot inventory
        for (int i = 0; i < bot.getInventory().getContainerSize(); i++) {
            ItemStack stack = bot.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) {
                    stack.shrink(1);
                    setFuel(tractor, fuel + 800);
                    return true;
                } else if (stack.is(Items.BLAZE_ROD)) {
                    stack.shrink(1);
                    setFuel(tractor, fuel + 1200);
                    return true;
                } else if (stack.is(Items.LAVA_BUCKET)) {
                    bot.getInventory().setItem(i, new ItemStack(Items.BUCKET));
                    setFuel(tractor, fuel + 20000);
                    return true;
                }
            }
        }
        // If empty on fuel and no coal, grant starter tractor fuel reserve so companion isn't paralyzed
        if (fuel <= 10) {
            setFuel(tractor, 600);
            return true;
        }
        return false;
    }

    public static boolean ensureAttachmentOperational(Entity tractor, Player player) {
        if (!AVAILABLE || tractor == null) return false;
        try {
            if (HAS_HITCHED_ATTACHMENT_METHOD != null) {
                boolean hasHitched = (boolean) HAS_HITCHED_ATTACHMENT_METHOD.invoke(tractor);
                if (!hasHitched) return false;
            }

            if (GET_HITCHED_ATTACHMENT_METHOD != null && ATTACHMENT_IS_LOWERED_METHOD != null && ATTACHMENT_IS_ACTIVE_METHOD != null) {
                Object attachment = GET_HITCHED_ATTACHMENT_METHOD.invoke(tractor);
                if (attachment != null) {
                    boolean lowered = (boolean) ATTACHMENT_IS_LOWERED_METHOD.invoke(attachment);
                    if (!lowered && TOGGLE_LIFT_METHOD != null) {
                        TOGGLE_LIFT_METHOD.invoke(tractor, player);
                    }
                    boolean active = (boolean) ATTACHMENT_IS_ACTIVE_METHOD.invoke(attachment);
                    if (!active && TOGGLE_ACTIVE_METHOD != null) {
                        TOGGLE_ACTIVE_METHOD.invoke(tractor, player);
                    }
                    return true;
                }
            } else {
                if (TOGGLE_LIFT_METHOD != null) TOGGLE_LIFT_METHOD.invoke(tractor, player);
                if (TOGGLE_ACTIVE_METHOD != null) TOGGLE_ACTIVE_METHOD.invoke(tractor, player);
                return true;
            }
        } catch (Exception e) {
            LOGGER.debug("Could not toggle tractor attachment", e);
        }
        return false;
    }

    public static void steerTractor(AiBotPlayer bot, Entity tractor, Vec3 targetPos, double speedMultiplier) {
        if (!AVAILABLE || tractor == null) return;
        double dx = targetPos.x - tractor.getX();
        double dz = targetPos.z - tractor.getZ();
        double distSq = dx * dx + dz * dz;

        if (distSq <= 1.5) {
            bot.zza = 0.0f;
            bot.xxa = 0.0f;
            return;
        }

        float targetYaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
        // Smoothly steer towards target yaw
        bot.setYRot(targetYaw);
        bot.setYHeadRot(targetYaw);
        tractor.setYRot(targetYaw);
        if (tractor instanceof LivingEntity living) {
            living.setYHeadRot(targetYaw);
            living.yBodyRot = targetYaw;
        }

        // Apply forward throttle to vehicle
        bot.zza = (float) Math.min(1.0, Math.max(0.4, speedMultiplier));
        bot.xxa = 0.0f;
    }

    public static void stopTractor(AiBotPlayer bot, Entity tractor) {
        bot.zza = 0.0f;
        bot.xxa = 0.0f;
    }
}
