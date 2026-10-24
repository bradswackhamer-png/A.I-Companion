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
        List<VoxelStep> steps = new ArrayList<>();

        Direction right = facing.getClockWise();
        Direction back = facing.getOpposite();

        // Calculate offset vector so origin is just in front of the front door
        // We build from x = -3 to +3 (relative right), z = 1 to 7 (relative back)
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

        // Step 2: Lay solid cobblestone foundation / floor (y = 0)
        for (int w = -halfWidth; w <= halfWidth; w++) {
            for (int d = 1; d <= depth + 1; d++) {
                BlockPos floorPos = origin.relative(right, w).relative(back, d);
                steps.add(new VoxelStep(floorPos, Blocks.COBBLESTONE.defaultBlockState(), false));
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
                        if (h == 1) {
                            BlockState doorLower = Blocks.OAK_DOOR.defaultBlockState()
                                    .setValue(DoorBlock.FACING, facing)
                                    .setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER);
                            steps.add(new VoxelStep(p, doorLower, false));
                        } else if (h == 2) {
                            BlockState doorUpper = Blocks.OAK_DOOR.defaultBlockState()
                                    .setValue(DoorBlock.FACING, facing)
                                    .setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER);
                            steps.add(new VoxelStep(p, doorUpper, false));
                        } else {
                            // Lintel above door
                            steps.add(new VoxelStep(p, Blocks.OAK_PLANKS.defaultBlockState(), false));
                        }
                        continue;
                    }

                    // Log corner pillars
                    if (isCorner) {
                        steps.add(new VoxelStep(p, Blocks.OAK_LOG.defaultBlockState(), false));
                        continue;
                    }

                    // Windows at height 2 in the center of side walls
                    boolean isWindow = (h == 2) && ((Math.abs(w) == halfWidth && d == 4) || (w == 0 && d == depth + 1));
                    if (isWindow) {
                        steps.add(new VoxelStep(p, Blocks.GLASS.defaultBlockState(), false));
                    } else {
                        steps.add(new VoxelStep(p, Blocks.OAK_PLANKS.defaultBlockState(), false));
                    }
                }
            }
        }

        // Step 4: Roof (Flat layer at y = 4 + overhanging eaves)
        int roofY = wallHeight + 1;
        for (int w = -halfWidth - 1; w <= halfWidth + 1; w++) {
            for (int d = 0; d <= depth + 2; d++) {
                BlockPos p = origin.relative(right, w).relative(back, d).above(roofY);
                steps.add(new VoxelStep(p, Blocks.OAK_PLANKS.defaultBlockState(), false));
            }
        }

        // Step 5: Interior furnishing - Torches for lighting (prevent hostiles)
        BlockPos torchPos1 = origin.relative(right, -2).relative(back, 2).above(2);
        BlockPos torchPos2 = origin.relative(right, 2).relative(back, depth).above(2);
        steps.add(new VoxelStep(torchPos1, Blocks.TORCH.defaultBlockState(), false));
        steps.add(new VoxelStep(torchPos2, Blocks.TORCH.defaultBlockState(), false));

        // Step 6: Interior furnishing - Bed (Red bed in the cozy back corner)
        // Bed foot at back-left, head against back wall
        BlockPos bedFoot = origin.relative(right, -2).relative(back, depth - 1).above(1);
        BlockPos bedHead = origin.relative(right, -2).relative(back, depth).above(1);

        BlockState footState = Blocks.BED.red().defaultBlockState()
                .setValue(BedBlock.FACING, back)
                .setValue(BedBlock.PART, BedPart.FOOT);
        BlockState headState = Blocks.BED.red().defaultBlockState()
                .setValue(BedBlock.FACING, back)
                .setValue(BedBlock.PART, BedPart.HEAD);

        steps.add(new VoxelStep(bedFoot, footState, false));
        steps.add(new VoxelStep(bedHead, headState, false));

        return steps;
    }
}
