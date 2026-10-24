package com.aicompanion.bot.village;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.ArrayList;
import java.util.List;

/**
 * Architectural blueprint catalog for companion construction.
 * Supports:
 * - Cottage (Residential home with bed, lighting, windows, doors)
 * - Watchtower (3-story defensive bastion with ladders and battlements)
 * - Barn (High-vaulted granary with double doors, hay bales, and storage chests)
 * - Forge (Blacksmith workshop with blast furnace, furnace, crafting table, and anvil)
 */
public class HouseBlueprint {

    public record VoxelStep(BlockPos pos, BlockState state, boolean isClearAir) {}

    public static List<VoxelStep> generateBlueprint(String type, BlockPos origin, Direction facing, ArchitecturalPalette palette) {
        String t = type.toLowerCase();
        if (t.contains("tower") || t.contains("watchtower") || t.contains("bunker")) {
            return generateWatchtower(origin, facing, palette);
        } else if (t.contains("barn") || t.contains("granary") || t.contains("farmhouse")) {
            return generateBarn(origin, facing, palette);
        } else if (t.contains("forge") || t.contains("blacksmith") || t.contains("workshop")) {
            return generateForge(origin, facing, palette);
        } else {
            return generateCottage(origin, facing, palette);
        }
    }

    public static List<VoxelStep> generateCottage(BlockPos origin, Direction facing) {
        return generateCottage(origin, facing, new ArchitecturalPalette());
    }

    /**
     * Residential Cottage (7x7)
     */
    public static List<VoxelStep> generateCottage(BlockPos origin, Direction facing, ArchitecturalPalette palette) {
        List<VoxelStep> steps = new ArrayList<>();
        Direction right = facing.getClockWise();
        Direction back = facing.getOpposite();

        int halfWidth = 3;
        int depth = 6;
        int wallHeight = 3;

        // Step 1: Clear interior
        for (int w = -halfWidth; w <= halfWidth; w++) {
            for (int d = 1; d <= depth + 1; d++) {
                for (int h = 1; h <= wallHeight + 2; h++) {
                    BlockPos p = origin.relative(right, w).relative(back, d).above(h);
                    steps.add(new VoxelStep(p, Blocks.AIR.defaultBlockState(), true));
                }
            }
        }

        // Step 2: Foundation (y = 0)
        for (int w = -halfWidth; w <= halfWidth; w++) {
            for (int d = 1; d <= depth + 1; d++) {
                BlockPos floorPos = origin.relative(right, w).relative(back, d);
                steps.add(new VoxelStep(floorPos, palette.foundation, false));
            }
        }

        // Step 3: Walls & Windows
        for (int h = 1; h <= wallHeight; h++) {
            for (int w = -halfWidth; w <= halfWidth; w++) {
                for (int d = 1; d <= depth + 1; d++) {
                    boolean isCorner = (w == -halfWidth || w == halfWidth) && (d == 1 || d == depth + 1);
                    boolean isPerimeter = (w == -halfWidth || w == halfWidth || d == 1 || d == depth + 1);
                    if (!isPerimeter) continue;

                    BlockPos p = origin.relative(right, w).relative(back, d).above(h);

                    // Front Doorway
                    if (w == 0 && d == 1) {
                        if (palette.door.getBlock() instanceof DoorBlock) {
                            if (h == 1) {
                                steps.add(new VoxelStep(p, palette.door.setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), false));
                            } else if (h == 2) {
                                steps.add(new VoxelStep(p, palette.door.setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), false));
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

                    if (isCorner) {
                        steps.add(new VoxelStep(p, palette.cornerPillars, false));
                        continue;
                    }

                    boolean isWindow = (h == 2) && ((Math.abs(w) == halfWidth && d == 4) || (w == 0 && d == depth + 1));
                    if (isWindow) {
                        steps.add(new VoxelStep(p, palette.windows, false));
                    } else {
                        steps.add(new VoxelStep(p, palette.walls, false));
                    }
                }
            }
        }

        // Step 4: Roof
        int roofY = wallHeight + 1;
        for (int w = -halfWidth - 1; w <= halfWidth + 1; w++) {
            for (int d = 0; d <= depth + 2; d++) {
                BlockPos p = origin.relative(right, w).relative(back, d).above(roofY);
                steps.add(new VoxelStep(p, palette.roof, false));
            }
        }

        // Step 5: Interior Lighting
        steps.add(new VoxelStep(origin.relative(right, -2).relative(back, 2).above(2), palette.interiorLighting, false));
        steps.add(new VoxelStep(origin.relative(right, 2).relative(back, depth).above(2), palette.interiorLighting, false));

        // Step 6: Bed
        BlockPos bedFoot = origin.relative(right, -2).relative(back, depth - 1).above(1);
        BlockPos bedHead = origin.relative(right, -2).relative(back, depth).above(1);
        steps.add(new VoxelStep(bedFoot, palette.bed.setValue(BedBlock.FACING, back).setValue(BedBlock.PART, BedPart.FOOT), false));
        steps.add(new VoxelStep(bedHead, palette.bed.setValue(BedBlock.FACING, back).setValue(BedBlock.PART, BedPart.HEAD), false));

        // Step 7: Optional Furniture
        if (palette.furniture != null) {
            steps.add(new VoxelStep(origin.relative(right, 2).relative(back, depth - 1).above(1), palette.furniture, false));
        }

        return steps;
    }

    /**
     * Defensive Guard Watchtower (5x5, 3 stories tall, 10 blocks high)
     */
    public static List<VoxelStep> generateWatchtower(BlockPos origin, Direction facing, ArchitecturalPalette palette) {
        List<VoxelStep> steps = new ArrayList<>();
        Direction right = facing.getClockWise();
        Direction back = facing.getOpposite();

        int halfWidth = 2;
        int depth = 4;
        int height = 8;

        // Clear site
        for (int w = -halfWidth; w <= halfWidth; w++) {
            for (int d = 1; d <= depth + 1; d++) {
                for (int h = 1; h <= height + 3; h++) {
                    steps.add(new VoxelStep(origin.relative(right, w).relative(back, d).above(h), Blocks.AIR.defaultBlockState(), true));
                }
            }
        }

        // Foundation (y = 0)
        for (int w = -halfWidth; w <= halfWidth; w++) {
            for (int d = 1; d <= depth + 1; d++) {
                steps.add(new VoxelStep(origin.relative(right, w).relative(back, d), Blocks.COBBLESTONE.defaultBlockState(), false));
            }
        }

        // Outer Stone/Log Tower Walls (h = 1 to 8)
        for (int h = 1; h <= height; h++) {
            boolean isFloor2 = (h == 4);
            boolean isRoofDeck = (h == 8);

            for (int w = -halfWidth; w <= halfWidth; w++) {
                for (int d = 1; d <= depth + 1; d++) {
                    boolean isCorner = (w == -halfWidth || w == halfWidth) && (d == 1 || d == depth + 1);
                    boolean isPerimeter = (w == -halfWidth || w == halfWidth || d == 1 || d == depth + 1);
                    BlockPos p = origin.relative(right, w).relative(back, d).above(h);

                    // Floor intermediate slabs
                    if ((isFloor2 || isRoofDeck) && !isCorner) {
                        // Leave hole for ladder at back-left (w = -1, d = depth)
                        if (!(w == -1 && d == depth)) {
                            steps.add(new VoxelStep(p, palette.walls, false));
                            continue;
                        }
                    }

                    if (!isPerimeter) continue;

                    // Ground Door at w = 0, d = 1
                    if (w == 0 && d == 1 && (h == 1 || h == 2)) {
                        if (palette.door.getBlock() instanceof DoorBlock) {
                            BlockState ds = palette.door.setValue(DoorBlock.FACING, facing)
                                    .setValue(DoorBlock.HALF, h == 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
                            steps.add(new VoxelStep(p, ds, false));
                        } else {
                            steps.add(new VoxelStep(p, palette.door, false));
                        }
                        continue;
                    }

                    // Log corner pillars
                    if (isCorner) {
                        steps.add(new VoxelStep(p, palette.cornerPillars, false));
                        continue;
                    }

                    // Arrow slit windows at height 6
                    if (h == 6 && ((Math.abs(w) == halfWidth && d == 3) || (w == 0 && (d == 1 || d == depth + 1)))) {
                        steps.add(new VoxelStep(p, palette.windows, false));
                    } else {
                        steps.add(new VoxelStep(p, Blocks.STONE_BRICKS.defaultBlockState(), false));
                    }
                }
            }
        }

        // Ladder ascending back wall (w = -1, d = depth, h = 1 to 8)
        for (int h = 1; h <= 8; h++) {
            BlockPos lp = origin.relative(right, -1).relative(back, depth).above(h);
            BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing);
            steps.add(new VoxelStep(lp, ladder, false));
        }

        // Top Deck Battlements (Crenelations at h = 9)
        int deckH = height + 1;
        for (int w = -halfWidth; w <= halfWidth; w++) {
            for (int d = 1; d <= depth + 1; d++) {
                boolean isCorner = (w == -halfWidth || w == halfWidth) && (d == 1 || d == depth + 1);
                boolean isEdge = (w == -halfWidth || w == halfWidth || d == 1 || d == depth + 1);
                if (!isEdge) continue;

                BlockPos bp = origin.relative(right, w).relative(back, d).above(deckH);
                if (isCorner || (Math.abs(w) == halfWidth && d == 3) || (w == 0)) {
                    steps.add(new VoxelStep(bp, Blocks.COBBLESTONE.defaultBlockState(), false));
                    // Torches on corner battlements
                    if (isCorner) {
                        steps.add(new VoxelStep(bp.above(), palette.interiorLighting, false));
                    }
                }
            }
        }

        return steps;
    }

    /**
     * Farmhouse Barn / Granary (9x9 with high vaulted roof, hay bales, and storage chests)
     */
    public static List<VoxelStep> generateBarn(BlockPos origin, Direction facing, ArchitecturalPalette palette) {
        List<VoxelStep> steps = new ArrayList<>();
        Direction right = facing.getClockWise();
        Direction back = facing.getOpposite();

        int halfWidth = 4;
        int depth = 8;
        int wallHeight = 4;

        // Clear site
        for (int w = -halfWidth; w <= halfWidth; w++) {
            for (int d = 1; d <= depth + 1; d++) {
                for (int h = 1; h <= wallHeight + 4; h++) {
                    steps.add(new VoxelStep(origin.relative(right, w).relative(back, d).above(h), Blocks.AIR.defaultBlockState(), true));
                }
            }
        }

        // Foundation (y = 0)
        for (int w = -halfWidth; w <= halfWidth; w++) {
            for (int d = 1; d <= depth + 1; d++) {
                steps.add(new VoxelStep(origin.relative(right, w).relative(back, d), Blocks.COBBLESTONE.defaultBlockState(), false));
            }
        }

        // Walls (h = 1 to 4)
        for (int h = 1; h <= wallHeight; h++) {
            for (int w = -halfWidth; w <= halfWidth; w++) {
                for (int d = 1; d <= depth + 1; d++) {
                    boolean isCorner = (w == -halfWidth || w == halfWidth) && (d == 1 || d == depth + 1);
                    boolean isPerimeter = (w == -halfWidth || w == halfWidth || d == 1 || d == depth + 1);
                    if (!isPerimeter) continue;

                    BlockPos p = origin.relative(right, w).relative(back, d).above(h);

                    // Front Double Barn Doors at w = -1 and w = 0, d = 1
                    if ((w == -1 || w == 0) && d == 1 && (h == 1 || h == 2)) {
                        if (palette.door.getBlock() instanceof DoorBlock) {
                            BlockState ds = palette.door.setValue(DoorBlock.FACING, facing)
                                    .setValue(DoorBlock.HALF, h == 1 ? DoubleBlockHalf.LOWER : DoubleBlockHalf.UPPER);
                            steps.add(new VoxelStep(p, ds, false));
                        } else {
                            steps.add(new VoxelStep(p, palette.door, false));
                        }
                        continue;
                    }

                    if (isCorner || (d == 5 && Math.abs(w) == halfWidth)) {
                        steps.add(new VoxelStep(p, palette.cornerPillars, false));
                        continue;
                    }

                    steps.add(new VoxelStep(p, palette.walls, false));
                }
            }
        }

        // Vaulted Wooden Roof (h = 5 to 7)
        for (int r = 0; r <= 2; r++) {
            int currentY = wallHeight + 1 + r;
            int inset = r;
            for (int w = -halfWidth + inset; w <= halfWidth - inset; w++) {
                for (int d = 1; d <= depth + 1; d++) {
                    BlockPos rp = origin.relative(right, w).relative(back, d).above(currentY);
                    steps.add(new VoxelStep(rp, palette.roof, false));
                }
            }
        }

        // Interior: Hay Bales stacked in rear right corner
        steps.add(new VoxelStep(origin.relative(right, 2).relative(back, depth - 1).above(1), Blocks.HAY_BLOCK.defaultBlockState(), false));
        steps.add(new VoxelStep(origin.relative(right, 3).relative(back, depth - 1).above(1), Blocks.HAY_BLOCK.defaultBlockState(), false));
        steps.add(new VoxelStep(origin.relative(right, 2).relative(back, depth).above(1), Blocks.HAY_BLOCK.defaultBlockState(), false));
        steps.add(new VoxelStep(origin.relative(right, 3).relative(back, depth).above(1), Blocks.HAY_BLOCK.defaultBlockState(), false));
        steps.add(new VoxelStep(origin.relative(right, 3).relative(back, depth).above(2), Blocks.HAY_BLOCK.defaultBlockState(), false));

        // Interior: Double storage chest for farm harvests
        steps.add(new VoxelStep(origin.relative(right, -3).relative(back, depth).above(1), Blocks.CHEST.defaultBlockState(), false));
        steps.add(new VoxelStep(origin.relative(right, -2).relative(back, depth).above(1), Blocks.CHEST.defaultBlockState(), false));

        // Interior Lighting
        steps.add(new VoxelStep(origin.relative(right, -3).relative(back, 3).above(3), palette.interiorLighting, false));
        steps.add(new VoxelStep(origin.relative(right, 3).relative(back, 3).above(3), palette.interiorLighting, false));

        return steps;
    }

    /**
     * Blacksmith Forge / Workshop (7x7 open stone pavilion with blast furnace, furnace, crafting table, and anvil)
     */
    public static List<VoxelStep> generateForge(BlockPos origin, Direction facing, ArchitecturalPalette palette) {
        List<VoxelStep> steps = new ArrayList<>();
        Direction right = facing.getClockWise();
        Direction back = facing.getOpposite();

        int halfWidth = 3;
        int depth = 6;
        int roofHeight = 4;

        // Clear site
        for (int w = -halfWidth; w <= halfWidth; w++) {
            for (int d = 1; d <= depth + 1; d++) {
                for (int h = 1; h <= roofHeight + 3; h++) {
                    steps.add(new VoxelStep(origin.relative(right, w).relative(back, d).above(h), Blocks.AIR.defaultBlockState(), true));
                }
            }
        }

        // Stone Brick Floor (y = 0)
        for (int w = -halfWidth; w <= halfWidth; w++) {
            for (int d = 1; d <= depth + 1; d++) {
                steps.add(new VoxelStep(origin.relative(right, w).relative(back, d), Blocks.STONE_BRICKS.defaultBlockState(), false));
            }
        }

        // Corner Stone Pillars (h = 1 to 4)
        for (int h = 1; h <= roofHeight; h++) {
            steps.add(new VoxelStep(origin.relative(right, -halfWidth).relative(back, 1).above(h), Blocks.COBBLESTONE.defaultBlockState(), false));
            steps.add(new VoxelStep(origin.relative(right, halfWidth).relative(back, 1).above(h), Blocks.COBBLESTONE.defaultBlockState(), false));
            steps.add(new VoxelStep(origin.relative(right, -halfWidth).relative(back, depth + 1).above(h), Blocks.COBBLESTONE.defaultBlockState(), false));
            steps.add(new VoxelStep(origin.relative(right, halfWidth).relative(back, depth + 1).above(h), Blocks.COBBLESTONE.defaultBlockState(), false));
        }

        // Solid Stone Back Wall (d = depth + 1, h = 1 to 3)
        for (int w = -halfWidth + 1; w <= halfWidth - 1; w++) {
            for (int h = 1; h <= 3; h++) {
                steps.add(new VoxelStep(origin.relative(right, w).relative(back, depth + 1).above(h), Blocks.STONE_BRICKS.defaultBlockState(), false));
            }
        }

        // Stone Chimney in rear-left corner (w = -2, d = depth) up to h = 6
        for (int h = 1; h <= 6; h++) {
            steps.add(new VoxelStep(origin.relative(right, -2).relative(back, depth).above(h), Blocks.COBBLESTONE.defaultBlockState(), false));
        }

        // Workstations along back wall (h = 1)
        steps.add(new VoxelStep(origin.relative(right, -1).relative(back, depth).above(1), Blocks.BLAST_FURNACE.defaultBlockState(), false));
        steps.add(new VoxelStep(origin.relative(right, 0).relative(back, depth).above(1), Blocks.FURNACE.defaultBlockState(), false));
        steps.add(new VoxelStep(origin.relative(right, 1).relative(back, depth).above(1), Blocks.CRAFTING_TABLE.defaultBlockState(), false));
        steps.add(new VoxelStep(origin.relative(right, 2).relative(back, depth).above(1), Blocks.ANVIL.defaultBlockState(), false));

        // Overhanging Roof (h = roofHeight + 1)
        int roofY = roofHeight + 1;
        for (int w = -halfWidth - 1; w <= halfWidth + 1; w++) {
            for (int d = 0; d <= depth + 2; d++) {
                steps.add(new VoxelStep(origin.relative(right, w).relative(back, d).above(roofY), Blocks.COBBLESTONE.defaultBlockState(), false));
            }
        }

        // Lighting
        steps.add(new VoxelStep(origin.relative(right, -halfWidth).relative(back, 1).above(3), palette.interiorLighting, false));
        steps.add(new VoxelStep(origin.relative(right, halfWidth).relative(back, 1).above(3), palette.interiorLighting, false));
        steps.add(new VoxelStep(origin.relative(right, 0).relative(back, depth - 1).above(2), palette.interiorLighting, false));

        return steps;
    }
}
