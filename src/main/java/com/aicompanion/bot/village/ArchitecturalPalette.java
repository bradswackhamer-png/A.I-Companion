package com.aicompanion.bot.village;

import com.aicompanion.bot.AiBotPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Dynamically scans the Minecraft block registry and nearby storage containers
 * to resolve an architectural building palette utilizing installed mods
 * (Macaw's Doors, Roofs, Windows, Lights, Furniture, etc.).
 */
public class ArchitecturalPalette {

    public BlockState foundation = Blocks.COBBLESTONE.defaultBlockState();
    public BlockState cornerPillars = Blocks.OAK_LOG.defaultBlockState();
    public BlockState walls = Blocks.OAK_PLANKS.defaultBlockState();
    public BlockState roof = Blocks.OAK_PLANKS.defaultBlockState();
    public BlockState door = Blocks.OAK_DOOR.defaultBlockState();
    public BlockState windows = Blocks.GLASS.defaultBlockState();
    public BlockState interiorLighting = Blocks.TORCH.defaultBlockState();
    public BlockState bed = Blocks.BED.red().defaultBlockState();
    public @Nullable BlockState furniture = null;

    public String themeName = "Classic Vanilla Oak";
    public List<String> moddedFeatures = new ArrayList<>();

    /**
     * Resolves an architectural palette by checking companion inventory, nearby chests,
     * and the game's block registry.
     */
    public static ArchitecturalPalette resolve(ServerLevel level, BlockPos origin, @Nullable AiBotPlayer bot) {
        ArchitecturalPalette palette = new ArchitecturalPalette();

        // 1. Gather all candidate item stacks from bot inventory and nearby chests
        List<ItemStack> candidates = new ArrayList<>();
        if (bot != null) {
            for (int i = 0; i < bot.getInventory().getContainerSize(); i++) {
                ItemStack s = bot.getInventory().getItem(i);
                if (!s.isEmpty()) candidates.add(s);
            }
        }

        // Search for nearby chests/containers within 16 blocks
        int radius = 16;
        for (int x = -radius; x <= radius; x += 4) {
            for (int y = -3; y <= 4; y += 2) {
                for (int z = -radius; z <= radius; z += 4) {
                    BlockPos p = origin.offset(x, y, z);
                    BlockEntity be = level.getBlockEntity(p);
                    if (be instanceof Container c) {
                        for (int i = 0; i < c.getContainerSize(); i++) {
                            ItemStack s = c.getItem(i);
                            if (!s.isEmpty()) candidates.add(s);
                        }
                    }
                }
            }
        }

        // 2. Identify Doors (Macaw's Doors or Vanilla)
        Block doorBlock = findBlock(candidates, "mcwdoors", "door");
        if (doorBlock == null) doorBlock = findBlockByClass(candidates, DoorBlock.class);
        if (doorBlock == null) doorBlock = getRegistryBlock("mcwdoors:oak_barn_door");
        if (doorBlock == null) doorBlock = getRegistryBlock("mcwdoors:oak_classic_door");
        if (doorBlock != null) {
            palette.door = doorBlock.defaultBlockState();
            Identifier id = BuiltInRegistries.BLOCK.getKey(doorBlock);
            if (id.getNamespace().contains("mcw")) {
                palette.moddedFeatures.add("Macaw's Door (" + id.getPath() + ")");
            }
        }

        // 3. Identify Roofs (Macaw's Roofs or Vanilla)
        Block roofBlock = findBlock(candidates, "mcwroofs", "roof");
        if (roofBlock == null) roofBlock = getRegistryBlock("mcwroofs:oak_roof");
        if (roofBlock == null) roofBlock = getRegistryBlock("mcwroofs:oak_planks_roof");
        if (roofBlock == null) roofBlock = findBlock(candidates, "minecraft", "stairs");
        if (roofBlock != null) {
            palette.roof = roofBlock.defaultBlockState();
            Identifier id = BuiltInRegistries.BLOCK.getKey(roofBlock);
            if (id.getNamespace().contains("mcw")) {
                palette.moddedFeatures.add("Macaw's Roof (" + id.getPath() + ")");
            }
        }

        // 4. Identify Windows (Macaw's Windows or Vanilla)
        Block winBlock = findBlock(candidates, "mcwwindows", "window");
        if (winBlock == null) winBlock = getRegistryBlock("mcwwindows:oak_window");
        if (winBlock == null) winBlock = getRegistryBlock("mcwwindows:oak_four_window");
        if (winBlock == null) winBlock = findBlock(candidates, "minecraft", "glass");
        if (winBlock != null) {
            palette.windows = winBlock.defaultBlockState();
            Identifier id = BuiltInRegistries.BLOCK.getKey(winBlock);
            if (id.getNamespace().contains("mcw")) {
                palette.moddedFeatures.add("Macaw's Windows (" + id.getPath() + ")");
            }
        }

        // 5. Identify Lights (Macaw's Lights or Vanilla)
        Block lightBlock = findBlock(candidates, "mcwlights", "lamp");
        if (lightBlock == null) lightBlock = findBlock(candidates, "mcwlights", "lantern");
        if (lightBlock == null) lightBlock = getRegistryBlock("mcwlights:oak_tiki_torch");
        if (lightBlock == null) lightBlock = findBlock(candidates, "minecraft", "lantern");
        if (lightBlock != null) {
            palette.interiorLighting = lightBlock.defaultBlockState();
            Identifier id = BuiltInRegistries.BLOCK.getKey(lightBlock);
            if (id.getNamespace().contains("mcw")) {
                palette.moddedFeatures.add("Macaw's Lighting (" + id.getPath() + ")");
            }
        }

        // 6. Identify Furniture (Macaw's Furnitures)
        Block furnBlock = findBlock(candidates, "mcwfurnitures", "chair");
        if (furnBlock == null) furnBlock = findBlock(candidates, "mcwfurnitures", "desk");
        if (furnBlock == null) furnBlock = getRegistryBlock("mcwfurnitures:oak_desk");
        if (furnBlock != null) {
            palette.furniture = furnBlock.defaultBlockState();
            palette.moddedFeatures.add("Macaw's Furniture");
        }

        // 7. Determine wood theme (e.g. Spruce, Birch, Acacia, Dark Oak)
        String detectedWood = detectWoodTheme(palette.door, palette.roof, palette.windows);
        applyWoodTheme(palette, detectedWood);

        if (!palette.moddedFeatures.isEmpty()) {
            palette.themeName = "Architectural [" + capitalize(detectedWood) + "] (" + String.join(", ", palette.moddedFeatures) + ")";
        } else {
            palette.themeName = "Classic Vanilla [" + capitalize(detectedWood) + "]";
        }

        return palette;
    }

    private static @Nullable Block findBlock(List<ItemStack> candidates, String namespace, String keyword) {
        for (ItemStack s : candidates) {
            if (s.getItem() instanceof BlockItem bi) {
                Identifier id = BuiltInRegistries.BLOCK.getKey(bi.getBlock());
                if (id.getNamespace().contains(namespace) && id.getPath().contains(keyword)) {
                    return bi.getBlock();
                }
            }
        }
        return null;
    }

    private static @Nullable Block findBlockByClass(List<ItemStack> candidates, Class<?> clazz) {
        for (ItemStack s : candidates) {
            if (s.getItem() instanceof BlockItem bi) {
                if (clazz.isInstance(bi.getBlock())) {
                    return bi.getBlock();
                }
            }
        }
        return null;
    }

    private static @Nullable Block getRegistryBlock(String fullId) {
        Identifier id = Identifier.tryParse(fullId);
        if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
            return BuiltInRegistries.BLOCK.getValue(id);
        }
        return null;
    }

    private static String detectWoodTheme(BlockState door, BlockState roof, BlockState window) {
        String[] woods = {"spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "oak"};
        String combined = (door.getBlock().toString() + " " + roof.getBlock().toString() + " " + window.getBlock().toString()).toLowerCase();
        for (String w : woods) {
            if (combined.contains(w)) return w;
        }
        return "oak";
    }

    private static void applyWoodTheme(ArchitecturalPalette palette, String wood) {
        switch (wood) {
            case "spruce" -> {
                palette.walls = Blocks.SPRUCE_PLANKS.defaultBlockState();
                palette.cornerPillars = Blocks.SPRUCE_LOG.defaultBlockState();
            }
            case "birch" -> {
                palette.walls = Blocks.BIRCH_PLANKS.defaultBlockState();
                palette.cornerPillars = Blocks.BIRCH_LOG.defaultBlockState();
            }
            case "jungle" -> {
                palette.walls = Blocks.JUNGLE_PLANKS.defaultBlockState();
                palette.cornerPillars = Blocks.JUNGLE_LOG.defaultBlockState();
            }
            case "acacia" -> {
                palette.walls = Blocks.ACACIA_PLANKS.defaultBlockState();
                palette.cornerPillars = Blocks.ACACIA_LOG.defaultBlockState();
            }
            case "dark_oak" -> {
                palette.walls = Blocks.DARK_OAK_PLANKS.defaultBlockState();
                palette.cornerPillars = Blocks.DARK_OAK_LOG.defaultBlockState();
            }
            case "mangrove" -> {
                palette.walls = Blocks.MANGROVE_PLANKS.defaultBlockState();
                palette.cornerPillars = Blocks.MANGROVE_LOG.defaultBlockState();
            }
            case "cherry" -> {
                palette.walls = Blocks.CHERRY_PLANKS.defaultBlockState();
                palette.cornerPillars = Blocks.CHERRY_LOG.defaultBlockState();
            }
            case "bamboo" -> {
                palette.walls = Blocks.BAMBOO_PLANKS.defaultBlockState();
                palette.cornerPillars = Blocks.BAMBOO_BLOCK.defaultBlockState();
            }
            default -> {
                palette.walls = Blocks.OAK_PLANKS.defaultBlockState();
                palette.cornerPillars = Blocks.OAK_LOG.defaultBlockState();
            }
        }
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
