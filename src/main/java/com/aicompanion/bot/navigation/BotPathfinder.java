package com.aicompanion.bot.navigation;

import com.aicompanion.bot.AiBotPlayer;
import com.aicompanion.bot.learning.HiveMindBrain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Vanilla Villager / GroundPathNavigation-style 3D Voxel A* Pathfinder.
 *
 * Serves as the deterministic base navigation engine for AI Companion Bots:
 * - Evaluates walkable floor surfaces, 2-block headroom, 1-block step-ups, and safe 1-3 block step-downs.
 * - Treats closed wooden doors and fence gates as openable passageways (just like vanilla Villagers).
 * - Enforces strict passage-axis alignment through doors and gates so bots walk squarely through doorways.
 * - Forbids corner-clipping diagonal moves when either adjacent cardinal block is obstructed.
 * - Layers HiveMindBrain learned trail pheromones and hazard penalties on top of base A* edge costs.
 */
public final class BotPathfinder {

    public enum NodeType {
        WALKABLE,
        STAIRS,
        DOOR_OPEN,
        DOOR_CLOSED_OPENABLE,
        WATER,
        FENCE_OR_WALL,
        HAZARD,
        BLOCKED
    }

    private static final class PathNode implements Comparable<PathNode> {
        final BlockPos pos;
        final long key;
        final NodeType type;
        float gCost;
        float fCost;
        @Nullable PathNode parent;

        PathNode(BlockPos pos, NodeType type, float gCost, float fCost, @Nullable PathNode parent) {
            this.pos = pos;
            this.key = pos.asLong();
            this.type = type;
            this.gCost = gCost;
            this.fCost = fCost;
            this.parent = parent;
        }

        @Override
        public int compareTo(PathNode other) {
            return Float.compare(this.fCost, other.fCost);
        }
    }

    private BotPathfinder() {}

    /**
     * Computes a voxel A* path from the bot's current block position to the target position.
     * Returns an ordered list of BlockPos waypoints from start (exclusive) to goal (inclusive).
     */
    public static List<BlockPos> findPath(AiBotPlayer bot, BlockPos startPos, BlockPos goalPos, int maxExpandedNodes) {
        if (!(bot.level() instanceof ServerLevel level)) {
            return Collections.emptyList();
        }
        String dimension = level.dimension().identifier().toString();

        // Snap startPos to a valid walkable Y near the bot's feet
        BlockPos start = snapToStandable(level, startPos);
        // Resolve the best standable goal block (either goalPos itself or an adjacent floor block next to a bed/workstation/chest)
        BlockPos resolvedGoal = resolveGoalStandPos(level, goalPos, start);

        if (start.equals(resolvedGoal)) {
            return List.of(resolvedGoal);
        }

        PriorityQueue<PathNode> openSet = new PriorityQueue<>();
        Map<Long, PathNode> allNodes = new HashMap<>();
        Set<Long> closedSet = new HashSet<>();

        NodeType startType = evaluateFootprint(level, start);
        PathNode startNode = new PathNode(start, startType, 0.0f, heuristic(start, resolvedGoal), null);
        openSet.add(startNode);
        allNodes.put(startNode.key, startNode);

        PathNode bestFallbackNode = startNode;
        float bestFallbackHeuristic = startNode.fCost;

        int expanded = 0;
        int limit = Math.max(128, Math.min(2048, maxExpandedNodes));

        int[] dxs = {0, 0, 1, -1, 1, 1, -1, -1};
        int[] dzs = {-1, 1, 0, 0, -1, 1, -1, 1};

        while (!openSet.isEmpty() && expanded < limit) {
            PathNode current = openSet.poll();
            if (!closedSet.add(current.key)) {
                continue;
            }
            expanded++;

            if (isAtGoal(level, current.pos, resolvedGoal, goalPos)) {
                return reconstructPath(current);
            }

            float hToGoal = heuristic(current.pos, resolvedGoal);
            if (hToGoal < bestFallbackHeuristic) {
                bestFallbackHeuristic = hToGoal;
                bestFallbackNode = current;
            }

            for (int i = 0; i < 8; i++) {
                int dx = dxs[i];
                int dz = dzs[i];
                boolean isDiagonal = (dx != 0 && dz != 0);

                // If currently standing in a doorway or gate, only allow exiting along the passage axis!
                if (isDoorOrGate(current.type)) {
                    if (isDiagonal) continue;
                    Direction moveDir = cardinalFromDelta(dx, dz);
                    if (moveDir != null && !isAlignedWithDoorPassage(level, current.pos, moveDir)) {
                        continue;
                    }
                }

                // Strict corner-clipping prevention for diagonals (just like vanilla WalkNodeEvaluator)
                if (isDiagonal) {
                    BlockPos sideX = current.pos.offset(dx, 0, 0);
                    BlockPos sideZ = current.pos.offset(0, 0, dz);
                    NodeType typeX = evaluateFootprint(level, sideX);
                    NodeType typeZ = evaluateFootprint(level, sideZ);
                    if (!isFreelyPassableForDiagonal(typeX) || !isFreelyPassableForDiagonal(typeZ)) {
                        continue;
                    }
                }

                PathNodeCandidate candidate = findTransitionCandidate(level, current.pos, dx, dz, isDiagonal);
                if (candidate == null) {
                    continue;
                }

                // If entering a doorway or gate, forbid diagonal entry and enforce passage axis alignment!
                if (isDoorOrGate(candidate.type)) {
                    if (isDiagonal) continue;
                    Direction moveDir = cardinalFromDelta(dx, dz);
                    if (moveDir != null && !isAlignedWithDoorPassage(level, candidate.pos, moveDir)) {
                        continue;
                    }
                }

                long neighborKey = candidate.pos.asLong();
                if (closedSet.contains(neighborKey)) {
                    continue;
                }

                // Base A* movement cost
                float stepCost = isDiagonal ? 1.4142f : 1.0f;
                int dy = candidate.pos.getY() - current.pos.getY();
                if (dy > 0) {
                    stepCost += (candidate.type == NodeType.STAIRS) ? 0.15f : 0.4f;
                } else if (dy < 0) {
                    stepCost += Math.abs(dy) * 0.25f;
                }

                if (candidate.type == NodeType.DOOR_CLOSED_OPENABLE) {
                    stepCost += 0.25f;
                } else if (candidate.type == NodeType.WATER) {
                    stepCost += 3.5f;
                } else if (candidate.type == NodeType.STAIRS) {
                    stepCost *= 0.92f;
                }

                // Layer HiveMindBrain learned pheromones & block walkability on top of base A* cost
                float learnedScore = HiveMindBrain.getNodeScore(dimension, candidate.pos);
                if (learnedScore > 0.5f) {
                    float discount = Math.max(0.45f, 1.0f - (learnedScore / 70.0f));
                    stepCost *= discount;
                } else if (learnedScore < -0.5f) {
                    stepCost += Math.min(45.0f, -learnedScore * 2.5f);
                }

                Block floorBlock = level.getBlockState(candidate.pos.below()).getBlock();
                String floorId = BuiltInRegistries.BLOCK.getKey(floorBlock).toString();
                float walkability = HiveMindBrain.getBlockWalkability(floorId);
                if (walkability < -0.15f) {
                    stepCost += Math.min(15.0f, -walkability * 8.0f);
                }

                float tentativeG = current.gCost + stepCost;
                PathNode existing = allNodes.get(neighborKey);
                if (existing == null) {
                    float f = tentativeG + heuristic(candidate.pos, resolvedGoal);
                    PathNode nextNode = new PathNode(candidate.pos, candidate.type, tentativeG, f, current);
                    allNodes.put(neighborKey, nextNode);
                    openSet.add(nextNode);
                } else if (tentativeG < existing.gCost) {
                    openSet.remove(existing);
                    existing.gCost = tentativeG;
                    existing.fCost = tentativeG + heuristic(candidate.pos, resolvedGoal);
                    existing.parent = current;
                    openSet.add(existing);
                }
            }
        }

        // Only use partial fallback path when the destination is far away beyond the search horizon (> 28 blocks).
        // If a nearby destination (<= 28 blocks, e.g. inside a house) was not reached within maxExpandedNodes,
        // it is walled off / unreachable — do NOT walk up to the exterior wall closest to the indoor target!
        int manhattanDist = start.distManhattan(resolvedGoal);
        if (manhattanDist > 28 && bestFallbackNode != startNode && bestFallbackHeuristic < startNode.fCost - 2.0f) {
            return reconstructPath(bestFallbackNode);
        }

        return Collections.emptyList();
    }

    private record PathNodeCandidate(BlockPos pos, NodeType type) {}

    private static @Nullable PathNodeCandidate findTransitionCandidate(
            ServerLevel level, BlockPos fromPos, int dx, int dz, boolean isDiagonal) {
        int targetX = fromPos.getX() + dx;
        int targetZ = fromPos.getZ() + dz;
        int fromY = fromPos.getY();

        // 1. Try flat move (dy = 0)
        BlockPos flatPos = new BlockPos(targetX, fromY, targetZ);
        NodeType flatType = evaluateFootprint(level, flatPos);
        if (isWalkableType(flatType)) {
            return new PathNodeCandidate(flatPos, flatType);
        }

        // Diagonals are only allowed on the same Y level to prevent clipping wall ledges
        if (isDiagonal) {
            return null;
        }

        // 2. Try 1-block step UP (dy = +1)
        // Requires 3rd block above current feet (fromPos.above(2)) to be passable so head doesn't hit ceiling
        BlockPos stepUpPos = new BlockPos(targetX, fromY + 1, targetZ);
        if (!isFenceOrWallBlock(level, flatPos) && isHeadroomPassable(level, fromPos.above(2))) {
            NodeType upType = evaluateFootprint(level, stepUpPos);
            if (isWalkableType(upType)) {
                return new PathNodeCandidate(stepUpPos, upType);
            }
        }

        // 3. Try 1 to 3 block step DOWN (dy = -1, -2, -3)
        // Requires flatPos and flatPos.above() to be passable air/doors so the bot can walk off the ledge
        if (isPassableSpace(level, flatPos) && isHeadroomPassable(level, flatPos.above())) {
            for (int drop = 1; drop <= 3; drop++) {
                BlockPos downPos = new BlockPos(targetX, fromY - drop, targetZ);
                NodeType downType = evaluateFootprint(level, downPos);
                if (downType == NodeType.HAZARD || downType == NodeType.FENCE_OR_WALL) {
                    return null; // Don't drop onto hazards or fences
                }
                if (isWalkableType(downType)) {
                    return new PathNodeCandidate(downPos, downType);
                }
                if (!isPassableSpace(level, downPos)) {
                    break;
                }
            }
        }

        return null;
    }

    /**
     * Evaluates whether a 1x2 bot footprint with feet at `pos` and head at `pos.above()`
     * and supporting floor at `pos.below()` is walkable.
     */
    public static NodeType evaluateFootprint(ServerLevel level, BlockPos pos) {
        BlockState feetState = level.getBlockState(pos);
        BlockState headState = level.getBlockState(pos.above());
        BlockState floorState = level.getBlockState(pos.below());
        FluidState feetFluid = level.getFluidState(pos);
        FluidState floorFluid = level.getFluidState(pos.below());

        // Check hazards first
        if (isHazard(feetState, feetFluid) || isHazard(headState, level.getFluidState(pos.above())) || isHazard(floorState, floorFluid)) {
            return NodeType.HAZARD;
        }

        // Never stand on or walk through fences/walls
        if (isFenceOrWallState(feetState) || isFenceOrWallState(headState) || isFenceOrWallState(floorState)) {
            return NodeType.FENCE_OR_WALL;
        }

        // Check Doors (Feet or Head)
        if (feetState.getBlock() instanceof DoorBlock door) {
            if (!hasSolidSupport(level, pos.below(), floorState)) {
                return NodeType.BLOCKED;
            }
            if (feetState.getValue(DoorBlock.OPEN)) {
                return NodeType.DOOR_OPEN;
            }
            return door.type().canOpenByHand() ? NodeType.DOOR_CLOSED_OPENABLE : NodeType.BLOCKED;
        }

        // Check Fence Gates
        if (feetState.getBlock() instanceof FenceGateBlock) {
            if (!hasSolidSupport(level, pos.below(), floorState)) {
                return NodeType.BLOCKED;
            }
            if (!isHeadroomPassable(level, pos.above())) {
                return NodeType.BLOCKED;
            }
            return feetState.getValue(FenceGateBlock.OPEN) ? NodeType.DOOR_OPEN : NodeType.DOOR_CLOSED_OPENABLE;
        }

        // Headroom must always be clear of solid collision
        if (!isHeadroomPassable(level, pos.above())) {
            return NodeType.BLOCKED;
        }

        // Stairs & Slabs at feet position (e.g., walking up stairs where feetPos is the stair block itself)
        if (isStairOrSlab(feetState)) {
            if (isHeadroomPassable(level, pos.above(2))) {
                return NodeType.STAIRS;
            }
            return NodeType.BLOCKED;
        }

        // Feet space must be passable
        if (!isPassableSpace(level, pos)) {
            return NodeType.BLOCKED;
        }

        // Shallow water check
        if (feetFluid.is(FluidTags.WATER)) {
            if (hasSolidSupport(level, pos.below(), floorState)) {
                return NodeType.WATER;
            }
            return NodeType.BLOCKED;
        }

        // Floor support check
        if (isStairOrSlab(floorState)) {
            return NodeType.STAIRS;
        }
        if (hasSolidSupport(level, pos.below(), floorState)) {
            return NodeType.WALKABLE;
        }

        return NodeType.BLOCKED;
    }

    private static boolean hasSolidSupport(ServerLevel level, BlockPos floorPos, BlockState floorState) {
        if (floorState.isAir()) return false;
        if (isFenceOrWallState(floorState)) return false;
        if (floorState.getBlock() instanceof DoorBlock || floorState.getBlock() instanceof FenceGateBlock) return false;
        VoxelShape shape = floorState.getCollisionShape(level, floorPos);
        return !shape.isEmpty();
    }

    private static boolean isPassableSpace(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return true;
        if (isFenceOrWallState(state)) return false;
        if (state.getBlock() instanceof DoorBlock door) {
            return state.getValue(DoorBlock.OPEN) || door.type().canOpenByHand();
        }
        if (state.getBlock() instanceof FenceGateBlock) {
            return true;
        }
        if (state.getBlock() instanceof CarpetBlock) {
            return true;
        }
        if (state.getBlock() instanceof TrapDoorBlock) {
            return !state.getValue(TrapDoorBlock.OPEN);
        }
        VoxelShape shape = state.getCollisionShape(level, pos);
        if (shape.isEmpty()) return true;
        // Allow very thin ground cover (< 0.2 blocks tall, e.g. snow layer, carpet, pressure plate, rail)
        return shape.max(Direction.Axis.Y) <= 0.25;
    }

    private static boolean isHeadroomPassable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return true;
        if (isFenceOrWallState(state)) return false;
        if (state.getBlock() instanceof DoorBlock door) {
            return state.getValue(DoorBlock.OPEN) || door.type().canOpenByHand();
        }
        if (state.getBlock() instanceof FenceGateBlock) {
            return true;
        }
        VoxelShape shape = state.getCollisionShape(level, pos);
        return shape.isEmpty();
    }

    private static boolean isHazard(BlockState state, FluidState fluid) {
        if (fluid.is(FluidTags.LAVA)) return true;
        if (!fluid.isEmpty() && !fluid.is(FluidTags.WATER)) return true;
        Block b = state.getBlock();
        return b == Blocks.LAVA || b == Blocks.FIRE || b == Blocks.SOUL_FIRE
                || b == Blocks.CAMPFIRE || b == Blocks.SOUL_CAMPFIRE
                || b == Blocks.MAGMA_BLOCK || b == Blocks.CACTUS
                || b == Blocks.SWEET_BERRY_BUSH || b == Blocks.WITHER_ROSE
                || b == Blocks.POWDER_SNOW;
    }

    private static boolean isFenceOrWallBlock(ServerLevel level, BlockPos pos) {
        return isFenceOrWallState(level.getBlockState(pos));
    }

    private static boolean isFenceOrWallState(BlockState state) {
        if (state.isAir()) return false;
        if (state.getBlock() instanceof FenceGateBlock) return false;
        if (state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS)
                || state.getBlock() instanceof FenceBlock
                || state.getBlock() instanceof WallBlock) {
            return true;
        }
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return (path.contains("fence") && !path.contains("gate"))
                || path.endsWith("_wall")
                || path.contains("railing")
                || path.contains("balustrade");
    }

    private static boolean isStairOrSlab(BlockState state) {
        if (state.isAir()) return false;
        if (state.is(BlockTags.STAIRS) || state.is(BlockTags.SLABS)
                || state.getBlock() instanceof StairBlock
                || state.getBlock() instanceof SlabBlock) {
            return true;
        }
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return path.contains("stair") || path.contains("step") || path.contains("bridge_stair");
    }

    private static boolean isWalkableType(NodeType type) {
        return type == NodeType.WALKABLE
                || type == NodeType.STAIRS
                || type == NodeType.DOOR_OPEN
                || type == NodeType.DOOR_CLOSED_OPENABLE
                || type == NodeType.WATER;
    }

    private static boolean isDoorOrGate(NodeType type) {
        return type == NodeType.DOOR_OPEN || type == NodeType.DOOR_CLOSED_OPENABLE;
    }

    private static boolean isFreelyPassableForDiagonal(NodeType type) {
        return type == NodeType.WALKABLE || type == NodeType.STAIRS;
    }

    private static @Nullable Direction cardinalFromDelta(int dx, int dz) {
        if (dx == 0 && dz == -1) return Direction.NORTH;
        if (dx == 0 && dz == 1) return Direction.SOUTH;
        if (dx == 1 && dz == 0) return Direction.EAST;
        if (dx == -1 && dz == 0) return Direction.WEST;
        return null;
    }

    /**
     * Ensures that entering or exiting a DoorBlock or FenceGateBlock only occurs along the door's passage axis,
     * never diagonally or through the solid jamb/wall beside the door.
     */
    private static boolean isAlignedWithDoorPassage(ServerLevel level, BlockPos doorPos, Direction moveDir) {
        BlockState state = level.getBlockState(doorPos);
        if (state.getBlock() instanceof FenceGateBlock && state.hasProperty(FenceGateBlock.FACING)) {
            Direction gateFacing = state.getValue(FenceGateBlock.FACING);
            return moveDir.getAxis() == gateFacing.getAxis();
        }
        if (state.getBlock() instanceof DoorBlock && state.hasProperty(DoorBlock.FACING)) {
            // Check adjacent walls to infer the wall axis vs passage axis
            boolean wallNorth = level.getBlockState(doorPos.north()).isSolid();
            boolean wallSouth = level.getBlockState(doorPos.south()).isSolid();
            boolean wallEast = level.getBlockState(doorPos.east()).isSolid();
            boolean wallWest = level.getBlockState(doorPos.west()).isSolid();
            if ((wallNorth || wallSouth) && !(wallEast || wallWest)) {
                return moveDir.getAxis() == Direction.Axis.X;
            }
            if ((wallEast || wallWest) && !(wallNorth || wallSouth)) {
                return moveDir.getAxis() == Direction.Axis.Z;
            }
            Direction doorFacing = state.getValue(DoorBlock.FACING);
            return moveDir.getAxis() == doorFacing.getAxis();
        }
        return true;
    }

    private static BlockPos snapToStandable(ServerLevel level, BlockPos pos) {
        if (isWalkableType(evaluateFootprint(level, pos))) {
            return pos;
        }
        for (int dy : new int[]{1, -1, -2}) {
            BlockPos candidate = pos.above(dy);
            if (isWalkableType(evaluateFootprint(level, candidate))) {
                return candidate;
            }
        }
        return pos;
    }

    /**
     * Resolves a target block (which may be a BedBlock, Workstation, Chest, or elevated block)
     * into the best walkable floor block from which the bot can stand and interact with it.
     */
    private static BlockPos resolveGoalStandPos(ServerLevel level, BlockPos goalPos, BlockPos startPos) {
        NodeType directType = evaluateFootprint(level, goalPos);
        if (isWalkableType(directType) && !(level.getBlockState(goalPos).getBlock() instanceof BedBlock)) {
            return goalPos;
        }

        // Check immediately above/below first if goalPos is a floor block or slightly off Y
        for (int dy : new int[]{1, -1}) {
            BlockPos vert = goalPos.above(dy);
            if (isWalkableType(evaluateFootprint(level, vert))
                    && !(level.getBlockState(vert).getBlock() instanceof BedBlock)
                    && level.getBlockState(goalPos).getCollisionShape(level, goalPos).isEmpty()) {
                return vert;
            }
        }

        // Target is a solid interactive block (Bed, Workstation, Chest, etc.):
        // Find all adjacent walkable floor tiles that have direct unobstructed access to goalPos
        BlockPos bestAdj = null;
        double bestDistSq = Double.MAX_VALUE;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int dy : new int[]{0, -1, 1}) {
                BlockPos adj = goalPos.relative(dir).above(dy);
                if (isWalkableType(evaluateFootprint(level, adj))) {
                    double d = adj.distSqr(startPos);
                    if (d < bestDistSq) {
                        bestDistSq = d;
                        bestAdj = adj;
                    }
                }
            }
        }
        return bestAdj != null ? bestAdj : goalPos;
    }

    private static boolean isAtGoal(ServerLevel level, BlockPos currentPos, BlockPos resolvedGoal, BlockPos rawGoal) {
        if (currentPos.equals(resolvedGoal) || currentPos.equals(rawGoal)) {
            return true;
        }
        // If rawGoal is a bed, workstation, or solid interactive block, standing cardinally adjacent on the same floor level counts as arrival
        if (currentPos.getY() == rawGoal.getY()
                && Math.abs(currentPos.getX() - rawGoal.getX()) + Math.abs(currentPos.getZ() - rawGoal.getZ()) == 1) {
            return true;
        }
        return false;
    }

    private static float heuristic(BlockPos a, BlockPos b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());
        int diag = Math.min(dx, dz);
        int straight = Math.max(dx, dz) - diag;
        return diag * 1.4142f + straight * 1.0f + dy * 1.2f;
    }

    private static List<BlockPos> reconstructPath(PathNode endNode) {
        List<BlockPos> path = new ArrayList<>();
        PathNode curr = endNode;
        while (curr != null && curr.parent != null) {
            path.add(curr.pos);
            curr = curr.parent;
        }
        Collections.reverse(path);
        return path;
    }
}
