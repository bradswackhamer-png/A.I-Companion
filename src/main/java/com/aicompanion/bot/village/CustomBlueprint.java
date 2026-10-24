package com.aicompanion.bot.village;

import com.aicompanion.bot.area.SavedArea;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User-created or scanned architectural blueprint.
 * Captures in-world blocks from a designated SavedArea and can be reconstructed by builder bots.
 */
public class CustomBlueprint {
    public String name = "";
    public int sizeX;
    public int sizeY;
    public int sizeZ;
    public int groundOffsetY = 0; // Negative when blueprint is a subterranean pit/bunker below ground level
    public int totalBlocks;
    public String capturedFacing = "NORTH";
    public String author = "Player";
    public long createdTime = System.currentTimeMillis();
    public List<VoxelRecord> voxels = new ArrayList<>();
    public Map<String, Integer> materialSummary = new LinkedHashMap<>();

    public CustomBlueprint() {}

    public static class VoxelRecord {
        public int x; // relative dx from origin
        public int y; // relative dy from origin
        public int z; // relative dz from origin
        public String blockId = "minecraft:air";
        public Map<String, String> properties = new LinkedHashMap<>();

        public VoxelRecord() {}

        public VoxelRecord(int x, int y, int z, String blockId, Map<String, String> properties) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockId = blockId;
            if (properties != null) {
                this.properties.putAll(properties);
            }
        }
    }

    /**
     * Scans and captures all blocks inside a 3D cubed or polygonal SavedArea into a blueprint.
     */
    public static CustomBlueprint capture(ServerLevel level, SavedArea area, String name, Direction playerFacing, String author) {
        CustomBlueprint bp = new CustomBlueprint();
        bp.name = name;
        bp.author = author;
        bp.capturedFacing = playerFacing != null ? playerFacing.name() : "NORTH";
        bp.createdTime = System.currentTimeMillis();

        int minX = area.getMinX();
        int maxX = area.getMaxX();
        int minY = area.minY;
        int maxY = area.maxY;
        int minZ = area.getMinZ();
        int maxZ = area.getMaxZ();

        bp.sizeX = Math.max(1, maxX - minX + 1);
        bp.sizeY = Math.max(1, maxY - minY + 1);
        bp.sizeZ = Math.max(1, maxZ - minZ + 1);
        bp.groundOffsetY = minY - area.getBaseY();

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        List<VoxelRecord> list = new ArrayList<>();
        Map<String, Integer> counts = new LinkedHashMap<>();

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    if (area.contains(cursor)) {
                        BlockState state = level.getBlockState(cursor);
                        if (!state.isAir()) {
                            Identifier blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
                            String idStr = blockId.toString();

                            Map<String, String> props = new LinkedHashMap<>();
                            for (Property<?> prop : state.getProperties()) {
                                props.put(prop.getName(), getPropValueString(state, prop));
                            }

                            list.add(new VoxelRecord(x - minX, y - minY, z - minZ, idStr, props));
                            counts.merge(idStr, 1, Integer::sum);
                        }
                    }
                }
            }
        }

        // Sort voxels by Y ascending so foundations and floors are placed before walls and ceilings
        list.sort(Comparator.comparingInt((VoxelRecord v) -> v.y)
                .thenComparingInt(v -> v.z)
                .thenComparingInt(v -> v.x));

        bp.voxels = list;
        bp.totalBlocks = list.size();
        bp.materialSummary = counts;
        return bp;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String getPropValueString(BlockState state, Property<T> prop) {
        return prop.getName(state.getValue(prop));
    }

    /**
     * Converts relative voxels into world construction steps, rotating the structure to match targetFacing.
     */
    public List<HouseBlueprint.VoxelStep> toVoxelSteps(BlockPos origin, Direction targetFacing) {
        List<HouseBlueprint.VoxelStep> steps = new ArrayList<>();

        Direction origFacing = Direction.byName(capturedFacing);
        if (origFacing == null || origFacing.getAxis().isVertical()) {
            origFacing = Direction.NORTH;
        }

        // Calculate rotation between captured facing and target facing
        int angleDiff = (int) (targetFacing.toYRot() - origFacing.toYRot() + 360) % 360;
        Rotation rotation = switch (angleDiff) {
            case 90 -> Rotation.CLOCKWISE_90;
            case 180 -> Rotation.CLOCKWISE_180;
            case 270 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };

        Direction right = targetFacing.getClockWise();
        Direction front = targetFacing;

        // Determine offset dimensions under rotation
        int boundW = (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) ? sizeZ : sizeX;
        int boundD = (rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90) ? sizeX : sizeZ;

        // Anchor origin: place centered horizontally in front of the bot, applying subterranean groundOffsetY if it's a pit/basement
        BlockPos baseAnchor = origin.relative(front, 2).relative(right, -boundW / 2).above(groundOffsetY);

        // First pass: Excavate / clear obstructions from top to bottom so subterranean pits are dug from the surface down
        for (int y = sizeY - 1; y >= 0; y--) {
            for (int w = 0; w < boundW; w++) {
                for (int d = 0; d < boundD; d++) {
                    BlockPos p = baseAnchor.relative(right, w).relative(front, d).above(y);
                    steps.add(new HouseBlueprint.VoxelStep(p, Blocks.AIR.defaultBlockState(), true));
                }
            }
        }

        // Second pass: Place structure voxels
        for (VoxelRecord voxel : voxels) {
            int rx;
            int rz;

            switch (rotation) {
                case CLOCKWISE_90 -> {
                    rx = sizeZ - 1 - voxel.z;
                    rz = voxel.x;
                }
                case CLOCKWISE_180 -> {
                    rx = sizeX - 1 - voxel.x;
                    rz = sizeZ - 1 - voxel.z;
                }
                case COUNTERCLOCKWISE_90 -> {
                    rx = voxel.z;
                    rz = sizeX - 1 - voxel.x;
                }
                default -> {
                    rx = voxel.x;
                    rz = voxel.z;
                }
            }

            BlockPos targetPos = baseAnchor.relative(right, rx).relative(front, rz).above(voxel.y);

            BlockState state = resolveBlockState(voxel, rotation);
            steps.add(new HouseBlueprint.VoxelStep(targetPos, state, false));
        }

        return steps;
    }

    private BlockState resolveBlockState(VoxelRecord voxel, Rotation rotation) {
        try {
            Identifier id = Identifier.parse(voxel.blockId);
            Block block = BuiltInRegistries.BLOCK.getValue(id);
            if (block == Blocks.AIR && !voxel.blockId.equals("minecraft:air")) {
                block = Blocks.COBBLESTONE; // Safe fallback if a mod block was uninstalled
            }

            BlockState state = block.defaultBlockState();
            for (Map.Entry<String, String> entry : voxel.properties.entrySet()) {
                Property<?> prop = block.getStateDefinition().getProperty(entry.getKey());
                if (prop != null) {
                    state = applyProperty(state, prop, entry.getValue());
                }
            }

            if (rotation != Rotation.NONE) {
                state = state.rotate(rotation);
            }
            return state;
        } catch (Exception ignored) {
            return Blocks.COBBLESTONE.defaultBlockState();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState applyProperty(BlockState state, Property prop, String valStr) {
        var opt = prop.getValue(valStr);
        if (opt.isPresent()) {
            return state.setValue(prop, (Comparable) opt.get());
        }
        return state;
    }
}
