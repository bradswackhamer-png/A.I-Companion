package com.aicompanion.bot.area;

import com.aicompanion.bot.BotPersistence;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class AreaManager {
    // In-memory active boundary points being selected by players with the Surveyor Wand
    private static final Map<UUID, List<BlockPos>> ACTIVE_SELECTIONS = new LinkedHashMap<>();

    public static synchronized List<BlockPos> getSelectedPoints(UUID playerId) {
        List<BlockPos> list = ACTIVE_SELECTIONS.get(playerId);
        return list != null ? new ArrayList<>(list) : Collections.emptyList();
    }

    public static synchronized int addPoint(ServerPlayer player, BlockPos pos) {
        UUID id = player.getUUID();
        List<BlockPos> points = ACTIVE_SELECTIONS.computeIfAbsent(id, k -> new ArrayList<>());

        // Don't add duplicate if clicked same block twice consecutively
        if (!points.isEmpty() && points.get(points.size() - 1).equals(pos)) {
            return points.size();
        }

        points.add(pos);
        int index = points.size();

        // Broadcast visual feedback particles
        ServerLevel level = player.level();
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 8, 0.1, 0.5, 0.1, 0.05);

        // Draw particle lines between all selected corners
        renderSelectionParticles(level, points);

        player.sendSystemMessage(Component.literal("§6[Surveyor Wand] §fCorner #" + index + " added at ("
                + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ") §7[" + index + " corners selected]"));

        if (index >= 3) {
            player.sendSystemMessage(Component.literal("§a✔ Polygon closed! Sneak + Right-Click in air to name and save this area, or type /bot area save <name>.")
                    .withStyle(ChatFormatting.GREEN));
        }

        return index;
    }

    public static synchronized void clearSelection(ServerPlayer player) {
        ACTIVE_SELECTIONS.remove(player.getUUID());
        player.sendSystemMessage(Component.literal("§e[Surveyor Wand] Cleared active boundary corner selection.")
                .withStyle(ChatFormatting.YELLOW));
    }

    public static synchronized boolean saveActiveSelection(ServerPlayer player, String name, String type) {
        List<BlockPos> points = ACTIVE_SELECTIONS.get(player.getUUID());
        if (points == null || points.size() < 3) {
            player.sendSystemMessage(Component.literal("§c[Surveyor Wand] You need at least 3 corner points to form an area! Right-click blocks to place corners.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        List<int[]> flatPoints = new ArrayList<>();

        for (BlockPos p : points) {
            flatPoints.add(new int[]{p.getX(), p.getZ(), p.getY()});
            if (p.getY() < minY) minY = p.getY();
            if (p.getY() > maxY) maxY = p.getY();
        }

        // Add standard height buffer (10 blocks below lowest corner, 40 blocks above highest corner)
        minY = Math.max(player.level().getMinY(), minY - 10);
        maxY = Math.min(player.level().getMaxY(), maxY + 40);

        String dim = player.level().dimension().identifier().toString();
        SavedArea area = new SavedArea(name, type, dim, minY, maxY, flatPoints);

        BotPersistence.saveArea(area);
        BotPersistence.saveAll(player.level().getServer());

        ACTIVE_SELECTIONS.remove(player.getUUID());

        // Emit celebration / confirmation particles around the new perimeter
        area.spawnPerimeterParticles(player.level(), ParticleTypes.HAPPY_VILLAGER);

        player.sendSystemMessage(Component.literal("§a★ Global Area '§f" + area.name + "§a' (Type: §e" + area.type
                + "§a, " + points.size() + " corners, ~" + (int) area.getAreaSqMeters() + " m²) saved to bot navigation network!")
                .withStyle(ChatFormatting.GOLD));

        return true;
    }

    public static void renderSelectionParticles(ServerLevel level, List<BlockPos> points) {
        if (points.size() < 2) return;
        int n = points.size();

        for (int i = 0; i < n; i++) {
            BlockPos p1 = points.get(i);
            BlockPos p2 = points.get((i + 1) % n);

            // If we don't have 3 points yet, only draw the single segment between p1 and p2 (don't close loop back to start)
            if (n < 3 && i == n - 1) {
                break;
            }

            double x1 = p1.getX() + 0.5;
            double y1 = p1.getY() + 1.1;
            double z1 = p1.getZ() + 0.5;

            double x2 = p2.getX() + 0.5;
            double y2 = p2.getY() + 1.1;
            double z2 = p2.getZ() + 0.5;

            double dx = x2 - x1;
            double dy = y2 - y1;
            double dz = z2 - z1;
            double dist = Math.sqrt(dx * dx + dz * dz);
            int steps = Math.max(1, (int) (dist / 0.8));

            for (int s = 0; s <= steps; s++) {
                double t = (double) s / steps;
                double px = x1 + t * dx;
                double py = y1 + t * dy;
                double pz = z1 + t * dz;
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 1, 0, 0, 0, 0);
            }
        }
    }
}
