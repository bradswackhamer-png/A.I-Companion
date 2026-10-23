package com.aicompanion.bot.compat;

import com.aicompanion.bot.AiBotPlayer;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Clean reflection compatibility bridge for the MiningTools train & locomotive mod.
 * Allows AI Companion conductors to board locomotives, fuel them, blow the whistle,
 * start/stop trains, navigate along rails to stations, and load/unload cargo freight.
 * If MiningTools is not installed, all methods safely no-op without crashes.
 */
public class TrainCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean AVAILABLE;

    private static Class<?> BASIC_LOCO_CLASS = null;
    private static Class<?> DIESEL_LOCO_CLASS = null;
    private static Class<?> ELECTRIC_LOCO_CLASS = null;
    private static Class<?> CARGO_CART_CLASS = null;

    private static Method SET_RUNNING_METHOD = null;
    private static Method IS_RUNNING_METHOD = null;
    private static Method SET_SPEED_METHOD = null;
    private static Method SET_REVERSED_METHOD = null;
    private static Method IS_REVERSED_METHOD = null;
    private static Method TRY_REFUEL_METHOD = null;
    private static Method BLOW_HORN_METHOD = null;
    private static Method GET_CONNECTED_CARTS_METHOD = null;

    static {
        boolean avail = false;
        try {
            BASIC_LOCO_CLASS = Class.forName("com.example.miningtools.entity.BasicLocomotiveEntity");
            SET_RUNNING_METHOD = BASIC_LOCO_CLASS.getMethod("setRunning", boolean.class);
            IS_RUNNING_METHOD = BASIC_LOCO_CLASS.getMethod("isRunning");
            SET_SPEED_METHOD = BASIC_LOCO_CLASS.getMethod("setSpeedLevel", int.class);
            SET_REVERSED_METHOD = BASIC_LOCO_CLASS.getMethod("setReversed", boolean.class);
            IS_REVERSED_METHOD = BASIC_LOCO_CLASS.getMethod("isReversed");
            TRY_REFUEL_METHOD = BASIC_LOCO_CLASS.getMethod("tryRefuel");
            BLOW_HORN_METHOD = BASIC_LOCO_CLASS.getMethod("blowHorn", Player.class);
            GET_CONNECTED_CARTS_METHOD = BASIC_LOCO_CLASS.getMethod("getConnectedTrainCarts");

            try {
                DIESEL_LOCO_CLASS = Class.forName("com.example.miningtools.entity.DieselLocomotiveEntity");
            } catch (Exception ignored) {}
            try {
                ELECTRIC_LOCO_CLASS = Class.forName("com.example.miningtools.entity.ElectricLocomotiveEntity");
            } catch (Exception ignored) {}
            try {
                CARGO_CART_CLASS = Class.forName("com.example.miningtools.entity.CargoMinecartEntity");
            } catch (Exception ignored) {}

            avail = true;
            LOGGER.info("MiningTools Train & Locomotive compatibility bridge successfully initialized!");
        } catch (ClassNotFoundException e) {
            avail = false;
        } catch (Exception e) {
            LOGGER.warn("Failed to initialize MiningTools Locomotive reflection bridge", e);
            avail = false;
        }
        AVAILABLE = avail;
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static boolean isLocomotive(@Nullable Entity entity) {
        if (!AVAILABLE || entity == null) return false;
        if (BASIC_LOCO_CLASS != null && BASIC_LOCO_CLASS.isInstance(entity)) return true;
        if (DIESEL_LOCO_CLASS != null && DIESEL_LOCO_CLASS.isInstance(entity)) return true;
        if (ELECTRIC_LOCO_CLASS != null && ELECTRIC_LOCO_CLASS.isInstance(entity)) return true;
        String typeStr = entity.getType().toString().toLowerCase(Locale.ROOT);
        return typeStr.contains("locomotive");
    }

    public static boolean isCargoCart(@Nullable Entity entity) {
        if (!AVAILABLE || entity == null) return false;
        if (CARGO_CART_CLASS != null && CARGO_CART_CLASS.isInstance(entity)) return true;
        String typeStr = entity.getType().toString().toLowerCase(Locale.ROOT);
        return typeStr.contains("cargo") || typeStr.contains("freight");
    }

    public static @Nullable Entity findNearbyLocomotive(ServerLevel level, Vec3 pos, double radius) {
        if (!AVAILABLE) return null;
        AABB box = new AABB(pos.x - radius, pos.y - radius, pos.z - radius,
                pos.x + radius, pos.y + radius, pos.z + radius);
        List<Entity> list = level.getEntities((Entity) null, box, TrainCompat::isLocomotive);
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

    public static boolean isRidingLocomotive(Player player) {
        if (!AVAILABLE || player == null) return false;
        Entity v = player.getVehicle();
        return isLocomotive(v);
    }

    public static @Nullable Entity getRiddenLocomotive(Player player) {
        if (!AVAILABLE || player == null) return null;
        Entity v = player.getVehicle();
        return isLocomotive(v) ? v : null;
    }

    public static boolean mountLocomotive(AiBotPlayer bot, Entity loco) {
        if (!AVAILABLE || loco == null || !isLocomotive(loco)) return false;
        if (bot.getVehicle() == loco) return true;
        return bot.startRiding(loco);
    }

    public static void dismountLocomotive(AiBotPlayer bot) {
        if (bot.getVehicle() != null) {
            bot.stopRiding();
        }
    }

    public static boolean refuelLocomotive(AiBotPlayer bot, Entity loco) {
        if (!AVAILABLE || loco == null) return false;
        try {
            // First try calling the locomotive's tryRefuel() method
            if (TRY_REFUEL_METHOD != null) {
                boolean refueled = (boolean) TRY_REFUEL_METHOD.invoke(loco);
                if (refueled) return true;
            }
            // If internal inventory had no fuel, transfer coal from bot inventory into locomotive
            if (loco instanceof Container container) {
                for (int i = 0; i < bot.getInventory().getContainerSize(); i++) {
                    ItemStack stack = bot.getInventory().getItem(i);
                    if (!stack.isEmpty() && (stack.is(Items.COAL) || stack.is(Items.CHARCOAL) || stack.is(Items.COAL_BLOCK))) {
                        for (int c = 0; c < container.getContainerSize(); c++) {
                            ItemStack cartStack = container.getItem(c);
                            if (cartStack.isEmpty()) {
                                container.setItem(c, stack.split(Math.min(16, stack.getCount())));
                                if (TRY_REFUEL_METHOD != null) TRY_REFUEL_METHOD.invoke(loco);
                                return true;
                            } else if (ItemStack.isSameItemSameComponents(cartStack, stack) && cartStack.getCount() < cartStack.getMaxStackSize()) {
                                int toAdd = Math.min(stack.getCount(), cartStack.getMaxStackSize() - cartStack.getCount());
                                cartStack.grow(toAdd);
                                stack.shrink(toAdd);
                                if (TRY_REFUEL_METHOD != null) TRY_REFUEL_METHOD.invoke(loco);
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not refuel locomotive", e);
        }
        return false;
    }

    public static void blowHorn(Entity loco, Player player) {
        if (!AVAILABLE || loco == null) return;
        try {
            if (BLOW_HORN_METHOD != null) {
                BLOW_HORN_METHOD.invoke(loco, player);
            }
        } catch (Exception e) {
            LOGGER.debug("Could not blow locomotive horn", e);
        }
    }

    public static boolean startTrain(Entity loco, int speedLevel, boolean reversed) {
        if (!AVAILABLE || loco == null) return false;
        try {
            if (SET_REVERSED_METHOD != null) {
                SET_REVERSED_METHOD.invoke(loco, reversed);
            }
            if (SET_SPEED_METHOD != null) {
                SET_SPEED_METHOD.invoke(loco, Math.max(1, Math.min(5, speedLevel)));
            }
            if (SET_RUNNING_METHOD != null) {
                SET_RUNNING_METHOD.invoke(loco, true);
                return true;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to start train locomotive", e);
        }
        return false;
    }

    public static boolean stopTrain(Entity loco) {
        if (!AVAILABLE || loco == null) return false;
        try {
            if (SET_RUNNING_METHOD != null) {
                SET_RUNNING_METHOD.invoke(loco, false);
                return true;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to stop train locomotive", e);
        }
        return false;
    }

    public static boolean isTrainRunning(Entity loco) {
        if (!AVAILABLE || loco == null || IS_RUNNING_METHOD == null) return false;
        try {
            return (boolean) IS_RUNNING_METHOD.invoke(loco);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isReversed(Entity loco) {
        if (!AVAILABLE || loco == null || IS_REVERSED_METHOD == null) return false;
        try {
            return (boolean) IS_REVERSED_METHOD.invoke(loco);
        } catch (Exception e) {
            return false;
        }
    }

    public static void toggleReversed(Entity loco) {
        if (!AVAILABLE || loco == null || SET_REVERSED_METHOD == null) return;
        try {
            boolean current = isReversed(loco);
            SET_REVERSED_METHOD.invoke(loco, !current);
        } catch (Exception ignored) {}
    }

    @SuppressWarnings("unchecked")
    public static List<Entity> getConnectedCarts(Entity loco) {
        List<Entity> result = new ArrayList<>();
        if (!AVAILABLE || loco == null) return result;
        try {
            if (GET_CONNECTED_CARTS_METHOD != null) {
                Object listObj = GET_CONNECTED_CARTS_METHOD.invoke(loco);
                if (listObj instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof Entity e) result.add(e);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not get connected train carts", e);
        }
        return result;
    }

    /**
     * Transfer freight items from a station chest into the train's connected cargo carts.
     */
    public static int transferFreightChestToTrain(Container chest, List<Entity> carts, @Nullable String itemFilter, int maxCount) {
        int transferred = 0;
        int remaining = maxCount > 0 ? maxCount : Integer.MAX_VALUE;

        for (int s = 0; s < chest.getContainerSize() && remaining > 0; s++) {
            ItemStack sourceStack = chest.getItem(s);
            if (sourceStack.isEmpty()) continue;

            String itemId = BuiltInRegistries.ITEM.getKey(sourceStack.getItem()).toString();
            String itemShort = BuiltInRegistries.ITEM.getKey(sourceStack.getItem()).getPath();
            if (itemFilter != null && !itemFilter.isBlank() && !itemFilter.equalsIgnoreCase("all")) {
                String filter = itemFilter.toLowerCase(Locale.ROOT).trim();
                if (!itemId.contains(filter) && !itemShort.contains(filter)) {
                    continue;
                }
            }

            int toMove = Math.min(sourceStack.getCount(), remaining);
            for (Entity cart : carts) {
                if (cart instanceof Container cartContainer) {
                    for (int cs = 0; cs < cartContainer.getContainerSize() && toMove > 0; cs++) {
                        ItemStack targetStack = cartContainer.getItem(cs);
                        if (targetStack.isEmpty()) {
                            ItemStack moved = sourceStack.split(toMove);
                            cartContainer.setItem(cs, moved);
                            transferred += moved.getCount();
                            remaining -= moved.getCount();
                            toMove -= moved.getCount();
                        } else if (ItemStack.isSameItemSameComponents(targetStack, sourceStack) && targetStack.getCount() < targetStack.getMaxStackSize()) {
                            int space = targetStack.getMaxStackSize() - targetStack.getCount();
                            int add = Math.min(toMove, space);
                            targetStack.grow(add);
                            sourceStack.shrink(add);
                            transferred += add;
                            remaining += add;
                            toMove -= add;
                        }
                    }
                }
            }
            if (sourceStack.isEmpty()) {
                chest.setItem(s, ItemStack.EMPTY);
            }
        }
        chest.setChanged();
        return transferred;
    }

    /**
     * Transfer freight items from the train's connected cargo carts into a station chest.
     */
    public static int transferFreightTrainToChest(List<Entity> carts, Container chest, @Nullable String itemFilter, int maxCount) {
        int transferred = 0;
        int remaining = maxCount > 0 ? maxCount : Integer.MAX_VALUE;

        for (Entity cart : carts) {
            if (!(cart instanceof Container cartContainer)) continue;
            for (int cs = 0; cs < cartContainer.getContainerSize() && remaining > 0; cs++) {
                ItemStack cartStack = cartContainer.getItem(cs);
                if (cartStack.isEmpty()) continue;

                String itemId = BuiltInRegistries.ITEM.getKey(cartStack.getItem()).toString();
                String itemShort = BuiltInRegistries.ITEM.getKey(cartStack.getItem()).getPath();
                if (itemFilter != null && !itemFilter.isBlank() && !itemFilter.equalsIgnoreCase("all")) {
                    String filter = itemFilter.toLowerCase(Locale.ROOT).trim();
                    if (!itemId.contains(filter) && !itemShort.contains(filter)) {
                        continue;
                    }
                }

                int toMove = Math.min(cartStack.getCount(), remaining);
                for (int s = 0; s < chest.getContainerSize() && toMove > 0; s++) {
                    ItemStack chestStack = chest.getItem(s);
                    if (chestStack.isEmpty()) {
                        ItemStack moved = cartStack.split(toMove);
                        chest.setItem(s, moved);
                        transferred += moved.getCount();
                        remaining -= moved.getCount();
                        toMove -= moved.getCount();
                    } else if (ItemStack.isSameItemSameComponents(chestStack, cartStack) && chestStack.getCount() < chestStack.getMaxStackSize()) {
                        int space = chestStack.getMaxStackSize() - chestStack.getCount();
                        int add = Math.min(toMove, space);
                        chestStack.grow(add);
                        cartStack.shrink(add);
                        transferred += add;
                        remaining += add;
                        toMove -= add;
                    }
                }
                if (cartStack.isEmpty()) {
                    cartContainer.setItem(cs, ItemStack.EMPTY);
                }
            }
            cartContainer.setChanged();
        }
        chest.setChanged();
        return transferred;
    }
}
