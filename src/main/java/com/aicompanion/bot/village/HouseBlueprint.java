package com.aicompanion.bot.village;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates structured 3D voxel blueprints for autonomous companion construction.
 * Houses include a level foundation, corner log pillars, plank walls, glass windows,
 * a working wooden door, a safe roof, interior torch lighting, and a bed.
 */
public class HouseBlueprint {

    public record VoxelStep(BlockPos pos, BlockState state, boolean isClearAir) {}

    /**
     * Generates a 7x7 cozy village cottage facing a given direction.
     * Dimensions:
     * - Footprint: 7 blocks wide, 7 blocks deep
     * - Height: 4 blocks walls + 2 blocks roof = 6 blocks total
     * - Floor: Cobblestone
     * - Corner pillars: Oak logs
     * - Walls: Oak planks
     * - Windows: Glass
     * - Door: Oak Door with lower & upper halves
     * - Interior: Oak bed + wall torch
     */
    public static List<VoxelStep> generateCottage(BlockPos origin, Direction facing) {
        return generateCottage(origin, facing, new ArchitecturalPalette());
    }

    public static List<VoxelStep> generateCottage(BlockPos origin, Direction facing, ArchitecturalPalette palette) {
        List<VoxelStep> steps = new ArrayList<>();

        Direction right = facing.getClockWise();
        Direction back = facing.getOpposite();

        int halfWidth = 3;
        int depth = 6;
        int wallHeight = 3; // y = 1 to 3

        // Step 1: Clear interior and envelope of clutter/foliage
        for (int w = -halfWidth; w <= halfWidth; w++) {
            for (int d = 1; d <= depth + 1; d++) {
                for (int h = 1; h <= wallHeight + 2; h++) {
                    BlockPos p = origin.relative(right, w).relative(back, d).above(h);
                    steps.add(new VoxelStep(p, Blocks.AIR.defaultBlockState(), true));
                }
            }
        }

        // Step 2: Lay solid foundation / floor (y = 0)
        for (int w = -halfWidth; w <= halfWidth; w++) {
            for (int d = 1; d <= depth + 1; d++) {
                BlockPos floorPos = origin.relative(right, w).relative(back, d);
                steps.add(new VoxelStep(floorPos, palette.foundation, false));
            }
        }

        // Step 3: Build walls, corner pillars, and windows (y = 1 to 3)
        for (int h = 1; h <= wallHeight; h++) {
            for (int w = -halfWidth; w <= halfWidth; w++) {
                for (int d = 1; d <= depth + 1; d++) {
                    boolean isCorner = (w == -halfWidth || w == halfWidth) && (d == 1 || d == depth + 1);
                    boolean isPerimeter = (w == -halfWidth || w == halfWidth || d == 1 || d == depth + 1);

                    if (!isPerimeter) continue;

                    BlockPos p = origin.relative(right, w).relative(back, d).above(h);

                    // Front Doorway at w = 0, d = 1
                    if (w == 0 && d == 1) {
                        if (palette.door.getBlock() instanceof DoorBlock) {
                            if (h == 1) {
                                BlockState doorLower = palette.door
                                        .setValue(DoorBlock.FACING, facing)
                                        .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
                                steps.add(new VoxelStep(p, doorLower, false));
                            } else if (h == 2) {
                                BlockState doorUpper = palette.door
                                        .setValue(DoorBlock.FACING, facing)
                                        .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
                                steps.add(new VoxelStep(p, doorUpper, false));
                            } else {
                                steps.add(new VoxelStep(p, palette.walls, false));
                            }
                        } else {
                            if (h <= 2) {
                                steps.add(new VoxelStep(p, palette.door, false));
                            } else {
                                steps.add(new VoxelStep(p, palette.walls, false));
                            }
                        }
                        continue;
                    }

                    // Log corner pillars
                    if (isCorner) {
                        steps.add(new VoxelStep(p, palette.cornerPillars, false));
                        continue;
                    }

                    // Windows at height 2 in the center of side walls
                    boolean isWindow = (h == 2) && ((Math.abs(w) == halfWidth && d == 4) || (w == 0 && d == depth + 1));
                    if (isWindow) {
                        steps.add(new VoxelStep(p, palette.windows, false));
                    } else {
                        steps.add(new VoxelStep(p, palette.walls, false));
                    }
                }
            }
        }

        // Step 4: Roof (y = wallHeight + 1 + overhanging eaves)
        int roofY = wallHeight + 1;
        for (int w = -halfWidth - 1; w <= halfWidth + 1; w++) {
            for (int d = 0; d <= depth + 2; d++) {
                BlockPos p = origin.relative(right, w).relative(back, d).above(roofY);
                steps.add(new VoxelStep(p, palette.roof, false));
            }
        }

        // Step 5: Interior furnishing - Lighting
        BlockPos torchPos1 = origin.relative(right, -2).relative(back, 2).above(2);
        BlockPos torchPos2 = origin.relative(right, 2).relative(back, depth).above(2);
        steps.add(new VoxelStep(torchPos1, palette.interiorLighting, false));
        steps.add(new VoxelStep(torchPos2, palette.interiorLighting, false));

        // Step 6: Interior furnishing - Bed
        BlockPos bedFoot = origin.relative(right, -2).relative(back, depth - 1).above(1);
        BlockPos bedHead = origin.relative(right, -2).relative(back, depth).above(1);

        BlockState footState = palette.bed
                .setValue(BedBlock.FACING, back)
                .setValue(BedBlock.PART, BedPart.FOOT);
        BlockState headState = palette.bed
                .setValue(BedBlock.FACING, back)
                .setValue(BedBlock.PART, BedPart.HEAD);

        steps.add(new VoxelStep(bedFoot, footState, false));
        steps.add(new VoxelStep(bedHead, headState, false));

        // Step 7: Optional Furniture (Macaw's chair / desk)
        if (palette.furniture != null) {
            BlockPos furnPos = origin.relative(right, 2).relative(back, depth - 1).above(1);
            steps.add(new VoxelStep(furnPos, palette.furniture, false));
        }

        return steps;
    }
}
