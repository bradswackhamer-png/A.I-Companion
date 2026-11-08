package com.aicompanion.bot.navigation;

import com.aicompanion.bot.AiBotPlayer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Genuine Vanilla Minecraft Mob Navigation Engine for {@link AiBotPlayer}.
 *
 * Uses Minecraft's actual {@link GroundPathNavigation}, {@link WalkNodeEvaluator},
 * {@link PathFinder}, {@link Path}, and {@link net.minecraft.world.entity.ai.control.MoveControl}
 * via a lightweight, unspawned {@link PathfinderMob} proxy paired with each bot.
 */
public final class BotPathfinder {

    private BotPathfinder() {}

    /**
     * Custom extension of Vanilla {@link WalkNodeEvaluator} that adds FenceGate support
     * (treating closed fence gates like closed wooden doors when gate passage is allowed)
     * and enforces cardinal alignment through doorways and fence gates.
     */
    public static final class BotWalkNodeEvaluator extends WalkNodeEvaluator {
        private boolean canOpenFenceGates = true;

        public void setCanOpenFenceGates(boolean canOpenFenceGates) {
            this.canOpenFenceGates = canOpenFenceGates;
        }

        public boolean canOpenFenceGates() {
            return this.canOpenFenceGates;
        }

        @Override
        public PathType getPathType(PathfindingContext context, int x, int y, int z) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = context.getBlockState(pos);

            if (state.getBlock() instanceof FenceGateBlock) {
                BlockPos belowPos = pos.below();
                BlockState belowState = context.getBlockState(belowPos);
                if (!belowState.getCollisionShape(context.level(), belowPos).isEmpty()) {
                    if (state.getValue(FenceGateBlock.OPEN)) {
                        return PathType.DOOR_OPEN;
                    }
                    if (this.canOpenDoors() && this.canPassDoors() && this.canOpenFenceGates) {
                        return PathType.DOOR_WOOD_CLOSED;
                    }
                }
                return PathType.FENCE;
            }

            // Ensure the head space (y + 1) directly above a fence gate is treated as open air,
            // and prevent the top of a fence gate from ever being evaluated as a walkable surface.
            BlockPos belowPos = pos.below();
            if (context.getBlockState(belowPos).getBlock() instanceof FenceGateBlock
                    && state.getCollisionShape(context.level(), pos).isEmpty()) {
                return PathType.OPEN;
            }

            return super.getPathType(context, x, y, z);
        }

        @Override
        protected boolean isNeighborValid(@Nullable Node neighbor, Node current) {
            return super.isNeighborValid(neighbor, current) && neighbor != null;
        }

        @Override
        protected boolean isDiagonalValid(Node root, @Nullable Node xNode, @Nullable Node zNode) {
            if (xNode != null && (xNode.type == PathType.DOOR_OPEN || xNode.type == PathType.WALKABLE_DOOR)) {
                return false;
            }
            if (zNode != null && (zNode.type == PathType.DOOR_OPEN || zNode.type == PathType.WALKABLE_DOOR)) {
                return false;
            }
            if (root.type == PathType.DOOR_OPEN || root.type == PathType.WALKABLE_DOOR) {
                return false;
            }
            return super.isDiagonalValid(root, xNode, zNode);
        }

        @Override
        protected boolean isDiagonalValid(@Nullable Node diagonalNode) {
            if (diagonalNode != null && (diagonalNode.type == PathType.DOOR_OPEN || diagonalNode.type == PathType.WALKABLE_DOOR)) {
                return false;
            }
            return super.isDiagonalValid(diagonalNode);
        }
    }

    /**
     * Vanilla {@link GroundPathNavigation} configured with {@link BotWalkNodeEvaluator}
     * and indoor/underground target support.
     */
    public static final class BotGroundPathNavigation extends GroundPathNavigation {
        private BotWalkNodeEvaluator botEvaluator;

        public BotGroundPathNavigation(PathfinderMob mob, Level level) {
            super(mob, level);
            this.setCanOpenDoors(true);
            this.setCanFloat(true);
            this.setCanWalkOverFences(false);
            this.setCanPathToTargetsBelowSurface(true);
        }

        @Override
        protected PathFinder createPathFinder(int maxVisitedNodes) {
            this.botEvaluator = new BotWalkNodeEvaluator();
            this.botEvaluator.setCanPassDoors(true);
            this.botEvaluator.setCanOpenDoors(true);
            this.botEvaluator.setCanFloat(true);
            this.botEvaluator.setCanWalkOverFences(false);
            this.nodeEvaluator = this.botEvaluator;
            return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
        }

        public BotWalkNodeEvaluator getBotEvaluator() {
            return this.botEvaluator;
        }

        @Override
        public boolean canCutCorner(PathType pathType) {
            return super.canCutCorner(pathType) && pathType != PathType.DOOR_OPEN;
        }
    }

    /**
     * Unspawned, server-side-only {@link PathfinderMob} proxy that mirrors an {@link AiBotPlayer}'s
     * position, bounding box, step height, and ground/fluid state so Vanilla's {@link GroundPathNavigation}
     * and {@link WalkNodeEvaluator} operate natively.
     */
    private static final class BotProxyMob extends PathfinderMob {
        private final AiBotPlayer bot;

        BotProxyMob(AiBotPlayer bot, ServerLevel level) {
            super(EntityTypes.VILLAGER, level);
            this.bot = bot;
            this.noPhysics = true;
            var followRange = this.getAttribute(Attributes.FOLLOW_RANGE);
            if (followRange != null) {
                followRange.setBaseValue(64.0);
            }
            var stepHeight = this.getAttribute(Attributes.STEP_HEIGHT);
            if (stepHeight != null) {
                stepHeight.setBaseValue(1.06);
            }
            var moveSpeed = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (moveSpeed != null) {
                moveSpeed.setBaseValue(0.35);
            }
            this.setPathfindingMalus(PathType.WATER, 16.0f);
            this.setPathfindingMalus(PathType.WATER_BORDER, 8.0f);
            this.navigation.updatePathfinderMaxVisitedNodes();
        }

        @Override
        protected PathNavigation createNavigation(Level level) {
            return new BotGroundPathNavigation(this, level);
        }

        void syncFromBot() {
            this.setPos(this.bot.getX(), this.bot.getY(), this.bot.getZ());
            this.setBoundingBox(this.bot.getBoundingBox());
            this.setYRot(this.bot.getYRot());
            this.setXRot(this.bot.getXRot());
            this.yHeadRot = this.bot.yHeadRot;
            this.yBodyRot = this.bot.yBodyRot;
            this.setOnGround(this.bot.onGround());
            this.horizontalCollision = this.bot.horizontalCollision;
            this.verticalCollision = this.bot.verticalCollision;

            boolean allowWater = this.bot.isWaterAllowed();
            this.setPathfindingMalus(PathType.WATER, allowWater ? 8.0f : -1.0f);
            this.setPathfindingMalus(PathType.WATER_BORDER, allowWater ? 4.0f : 16.0f);
        }

        @Override
        protected EntityDimensions getDefaultDimensions(Pose pose) {
            if (this.bot != null) {
                return this.bot.getDimensions(pose);
            }
            return super.getDefaultDimensions(pose);
        }

        @Override
        public boolean onGround() {
            return this.bot != null ? this.bot.onGround() : super.onGround();
        }

        @Override
        public boolean isInWater() {
            return this.bot != null ? this.bot.isInWater() : super.isInWater();
        }

        @Override
        public boolean isInLava() {
            return this.bot != null ? this.bot.isInLava() : super.isInLava();
        }

        @Override
        public boolean isInLiquid() {
            return this.bot != null ? (this.bot.isInWater() || this.bot.isInLava()) : super.isInLiquid();
        }

        @Override
        public boolean isPassenger() {
            return this.bot != null ? this.bot.isPassenger() : super.isPassenger();
        }

        @Override
        public float maxUpStep() {
            return this.bot != null ? this.bot.maxUpStep() : 1.06f;
        }

        @Override
        public int getMaxFallDistance() {
            return 3;
        }

        @Override
        public boolean canStandOnFluid(FluidState fluidState) {
            return this.bot != null && this.bot.canStandOnFluid(fluidState);
        }
    }

    /**
     * Per-bot Vanilla Navigation controller instance stored on {@link AiBotPlayer}.
     */
    public static final class Navigator {
        private final AiBotPlayer bot;
        private @Nullable BotProxyMob proxyMob;
        private @Nullable BlockPos lastTargetBlock = null;
        private int lastRecomputeTick = -999;
        private int unreachableTicks = 0;
        private int consecutiveVanillaStuckCount = 0;
        private final Set<BlockPos> openedDoorsAndGates = new HashSet<>();

        public Navigator(AiBotPlayer bot) {
            this.bot = bot;
        }

        private @Nullable BotProxyMob ensureProxy() {
            if (!(this.bot.level() instanceof ServerLevel serverLevel)) {
                return null;
            }
            if (this.proxyMob == null || this.proxyMob.level() != serverLevel) {
                this.proxyMob = new BotProxyMob(this.bot, serverLevel);
            }
            this.proxyMob.syncFromBot();
            return this.proxyMob;
        }

        public void stop() {
            if (this.proxyMob != null) {
                this.proxyMob.getNavigation().stop();
            }
            this.lastTargetBlock = null;
            this.lastRecomputeTick = -999;
            this.unreachableTicks = 0;
            this.consecutiveVanillaStuckCount = 0;
            if (this.bot.level() instanceof ServerLevel level) {
                closePassedDoorsAndGates(level, null);
            }
        }

        /**
         * Drives the bot toward {@code target} for a single server tick using Vanilla's
         * {@link GroundPathNavigation} and {@link WalkNodeEvaluator}.
         */
        public void tickMoveTowards(Vec3 target, boolean sprint) {
            BotProxyMob proxy = ensureProxy();
            if (proxy == null || !(this.bot.level() instanceof ServerLevel level)) {
                return;
            }

            BotGroundPathNavigation nav = (BotGroundPathNavigation) proxy.getNavigation();
            boolean forbidFenceExit = this.bot.shouldRespectFencePerimeter();
            nav.getBotEvaluator().setCanOpenFenceGates(!forbidFenceExit);

            BlockPos rawGoalBlock = BlockPos.containing(target);
            GoalResolution resolved = resolveGoal(level, rawGoalBlock, this.bot.blockPosition(), this.bot.isWaterAllowed());
            BlockPos goalBlock = resolved.goalPos();
            int accuracy = resolved.accuracy();

            double dxGoal = target.x - this.bot.getX();
            double dzGoal = target.z - this.bot.getZ();
            double horizDistSqToTarget = dxGoal * dxGoal + dzGoal * dzGoal;
            double vertDiffToTarget = Math.abs(target.y - this.bot.getY());

            // Direct arrival within final step when unobstructed
            if (horizDistSqToTarget <= 1.21 && vertDiffToTarget <= 1.5 && hasDirectStepClearance(level, this.bot, target)) {
                closePassedDoorsAndGates(level, null);
                steerBotDirectly(target, sprint);
                return;
            }

            boolean needRecompute = nav.isDone()
                    || this.lastTargetBlock == null
                    || !this.lastTargetBlock.closerThan(goalBlock, 1.5)
                    || (this.bot.tickCount - this.lastRecomputeTick >= 20);

            if (needRecompute) {
                Path newPath = nav.createPath(goalBlock, accuracy);
                this.lastTargetBlock = goalBlock.immutable();
                this.lastRecomputeTick = this.bot.tickCount;
                if (newPath != null) {
                    if (newPath.getNodeCount() > 1 && newPath.getNextNodeIndex() == 0) {
                        Node firstNode = newPath.getNode(0);
                        double dx = this.bot.getX() - (firstNode.x + 0.5);
                        double dz = this.bot.getZ() - (firstNode.z + 0.5);
                        if ((dx * dx + dz * dz) < 1.44) {
                            newPath.advance();
                        }
                    }
                    nav.moveTo(newPath, 1.0);
                } else {
                    nav.stop();
                }
            }

            Path path = nav.getPath();

            // Check if a nearby target (<= 28 blocks, e.g. inside a sealed building) is unreachable according to Vanilla PathFinder
            int manhattanToGoal = this.bot.blockPosition().distManhattan(goalBlock);
            boolean isInteractiveTarget = level.getBlockState(rawGoalBlock).getBlock() instanceof BedBlock
                    || level.getBlockState(rawGoalBlock).getBlock() instanceof net.minecraft.world.level.block.ChestBlock
                    || level.getBlockState(rawGoalBlock).getBlock() instanceof net.minecraft.world.level.block.ComposterBlock
                    || level.getBlockEntity(rawGoalBlock) instanceof Container;
            float maxAllowableDist = isInteractiveTarget ? 3.5f : Math.max(1.5f, accuracy + 0.5f);

            if (manhattanToGoal <= 28 && (path == null || (!path.canReach() && path.getDistToTarget() > maxAllowableDist))) {
                this.unreachableTicks++;
                if (this.unreachableTicks >= 60) {
                    this.bot.blacklistUnreachableTarget(rawGoalBlock, 400);
                    this.bot.blacklistUnreachableTarget(goalBlock, 400);
                    this.bot.stopMovement();
                    return;
                }
            } else {
                this.unreachableTicks = 0;
            }

            if (path != null && !path.isDone()) {
                // 1. Open any wooden doors or fence gates along the current/upcoming vanilla path nodes
                interactWithDoorsAlongPath(level, path, forbidFenceExit);

                // 2. Tick Vanilla GroundPathNavigation (advances nodes, checks corner cutting & 100-tick stuck detection)
                nav.tick();

                // 3. Check if Vanilla's 100-tick stuck detector triggered
                if (nav.isStuck()) {
                    this.consecutiveVanillaStuckCount++;
                    this.lastRecomputeTick = -999; // Force fresh path on next tick
                    if (this.consecutiveVanillaStuckCount >= 2) {
                        this.bot.blacklistUnreachableTarget(rawGoalBlock, 600);
                        this.bot.blacklistUnreachableTarget(goalBlock, 600);
                        this.bot.stopMovement();
                        return;
                    }
                } else {
                    this.consecutiveVanillaStuckCount = 0;
                }

                // 4. Steer bot toward the current Vanilla Path node (or final target on the last node)
                Path activePath = nav.getPath();
                if (activePath != null && !activePath.isDone()) {
                    int nextIdx = activePath.getNextNodeIndex();
                    Vec3 nextNodePos;
                    if (nextIdx >= activePath.getNodeCount() - 1 && accuracy == 0 && horizDistSqToTarget <= 4.0) {
                        nextNodePos = target;
                    } else {
                        Node node = activePath.getNode(nextIdx);
                        double floorY = WalkNodeEvaluator.getFloorLevel(level, node.asBlockPos());
                        nextNodePos = new Vec3(node.x + 0.5, floorY, node.z + 0.5);
                    }
                    closePassedDoorsAndGates(level, activePath);
                    steerBotDirectly(nextNodePos, sprint);
                    return;
                }
            }

            // Close any doors we passed once path finishes
            closePassedDoorsAndGates(level, null);

            // Arrival check: if within close reach of the goal, stop completely!
            double arrivalThresholdSq = Math.max(0.36, (double) (accuracy * accuracy));
            if (horizDistSqToTarget <= arrivalThresholdSq) {
                if (this.bot.isInWater() && !this.bot.isWaterAllowed()) {
                    this.bot.escapeWaterToDryLand();
                } else {
                    this.bot.zza = 0.0f;
                    this.bot.xxa = 0.0f;
                    this.bot.setJumping(this.bot.isInWater() || this.bot.isInLava());
                }
            } else if (horizDistSqToTarget <= 4.0 && hasDirectStepClearance(level, this.bot, target)) {
                steerBotDirectly(target, sprint);
            } else {
                if (this.bot.isInWater() && !this.bot.isWaterAllowed()) {
                    this.bot.escapeWaterToDryLand();
                } else {
                    this.bot.zza = 0.0f;
                    this.bot.xxa = 0.0f;
                    this.bot.setJumping(this.bot.isInWater() || this.bot.isInLava());
                }
            }
        }

        private void steerBotDirectly(Vec3 waypoint, boolean sprint) {
            Vec3 pos = this.bot.position();
            double dx = waypoint.x - pos.x;
            double dy = waypoint.y - pos.y;
            double dz = waypoint.z - pos.z;
            double horiz = Math.sqrt(dx * dx + dz * dz);
            if (horiz < 0.25) {
                // Arrived at waypoint! Stop forward movement to prevent overshoot and 180-degree yaw spinning
                this.bot.zza = 0.0f;
                this.bot.xxa = 0.0f;
                return;
            }
            float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
            float pitch = (float) (-(Mth.atan2(dy, horiz) * (180.0 / Math.PI)));
            this.bot.setYRot(yaw);
            this.bot.setYHeadRot(yaw);
            this.bot.setYBodyRot(yaw);
            this.bot.setXRot(Mth.clamp(pitch, -45.0f, 45.0f));
            this.bot.setSprinting(sprint);
            this.bot.zza = sprint ? 1.0f : 0.8f;
            this.bot.xxa = 0.0f;
            // With STEP_HEIGHT = 1.06, bots step up 1-block ledges and stairs smoothly without jumping;
            // only jump to swim in fluids.
            this.bot.setJumping(this.bot.isInWater() || this.bot.isInLava() || this.bot.isInHazardousFluid());
        }

        /**
         * Vanilla {@code InteractWithDoor}-style door and fence gate opener along the active {@link Path}.
         */
        private void interactWithDoorsAlongPath(ServerLevel level, Path path, boolean forbidFenceExit) {
            int nextIdx = path.getNextNodeIndex();
            int startIdx = Math.max(0, nextIdx - 1);
            int endIdx = Math.min(path.getNodeCount() - 1, nextIdx + 1);

            for (int i = startIdx; i <= endIdx; i++) {
                Node node = path.getNode(i);
                BlockPos nodePos = node.asBlockPos();
                if (this.bot.distanceToSqr(nodePos.getX() + 0.5, this.bot.getY(), nodePos.getZ() + 0.5) <= 4.0) {
                    tryOpenDoorOrGateAt(level, nodePos, forbidFenceExit);
                    tryOpenDoorOrGateAt(level, nodePos.above(), forbidFenceExit);
                }
            }

            // Also check the block the bot is currently standing in and immediately facing
            BlockPos feet = this.bot.blockPosition();
            tryOpenDoorOrGateAt(level, feet, forbidFenceExit);
            tryOpenDoorOrGateAt(level, feet.above(), forbidFenceExit);
            BlockPos front = feet.relative(this.bot.getDirection());
            if (this.bot.distanceToSqr(front.getX() + 0.5, this.bot.getY(), front.getZ() + 0.5) <= 2.5) {
                tryOpenDoorOrGateAt(level, front, forbidFenceExit);
                tryOpenDoorOrGateAt(level, front.above(), forbidFenceExit);
            }
        }

        private void tryOpenDoorOrGateAt(ServerLevel level, BlockPos pos, boolean forbidFenceExit) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof DoorBlock door) {
                if (door.type().canOpenByHand()) {
                    if (!state.getValue(DoorBlock.OPEN)) {
                        door.setOpen(this.bot, level, state, pos, true);
                    }
                    BlockPos bottomDoorPos = state.hasProperty(DoorBlock.HALF)
                            && state.getValue(DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER
                            ? pos.below() : pos.immutable();
                    this.openedDoorsAndGates.add(bottomDoorPos);
                    this.bot.rememberOpenedDoor(bottomDoorPos);
                }
            } else if (!forbidFenceExit && state.getBlock() instanceof FenceGateBlock) {
                if (!state.getValue(FenceGateBlock.OPEN)) {
                    BlockState openState = state.setValue(FenceGateBlock.OPEN, true);
                    if (openState.hasProperty(FenceGateBlock.FACING)) {
                        Direction botDir = this.bot.getDirection();
                        if (openState.getValue(FenceGateBlock.FACING) == botDir.getOpposite()) {
                            openState = openState.setValue(FenceGateBlock.FACING, botDir);
                        }
                    }
                    level.setBlockAndUpdate(pos, openState);
                    level.playSound(null, pos, SoundEvents.FENCE_GATE_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
                this.openedDoorsAndGates.add(pos.immutable());
                this.bot.rememberOpenedGate(pos.immutable());
            }
        }

        /**
         * Closes doors and fence gates behind the bot once it has passed the doorway node
         * and is clear of the door's bounding box (matching Vanilla {@code InteractWithDoor}).
         */
        private void closePassedDoorsAndGates(ServerLevel level, @Nullable Path activePath) {
            if (this.openedDoorsAndGates.isEmpty()) {
                return;
            }
            BlockPos prevNodePos = (activePath != null && activePath.getPreviousNode() != null)
                    ? activePath.getPreviousNode().asBlockPos() : null;
            BlockPos nextNodePos = (activePath != null && !activePath.isDone())
                    ? activePath.getNextNodePos() : null;

            Iterator<BlockPos> it = this.openedDoorsAndGates.iterator();
            while (it.hasNext()) {
                BlockPos pos = it.next();
                if (pos.equals(prevNodePos) || pos.equals(nextNodePos)) {
                    continue;
                }
                if (pos.equals(this.bot.blockPosition()) || pos.equals(this.bot.blockPosition().below())) {
                    continue;
                }
                AABB doorBox = new AABB(pos).inflate(0.3);
                if (this.bot.getBoundingBox().intersects(doorBox)) {
                    continue;
                }
                double distSq = this.bot.distanceToSqr(pos.getX() + 0.5, this.bot.getY(), pos.getZ() + 0.5);
                if (distSq < 1.8) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof DoorBlock door && door.type().canOpenByHand()) {
                    if (state.getValue(DoorBlock.OPEN)) {
                        door.setOpen(this.bot, level, state, pos, false);
                    }
                } else if (state.getBlock() instanceof FenceGateBlock) {
                    if (state.getValue(FenceGateBlock.OPEN)) {
                        level.setBlockAndUpdate(pos, state.setValue(FenceGateBlock.OPEN, false));
                        level.playSound(null, pos, SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS, 1.0f, 1.0f);
                    }
                }
                it.remove();
            }
        }
    }

    private record GoalResolution(BlockPos goalPos, int accuracy) {}

    /**
     * Resolves a target {@link BlockPos} (which may be a walkable floor block, an air block above floor,
     * or a solid interactive block like a Bed, Workstation, or Chest) into the optimal goal position and
     * reach accuracy for Vanilla {@link GroundPathNavigation#createPath(BlockPos, int)}.
     */
    private static GoalResolution resolveGoal(ServerLevel level, BlockPos rawGoal, BlockPos startPos, boolean allowWater) {
        BlockState rawState = level.getBlockState(rawGoal);

        // If rawGoal is a Bed, Workstation, Chest, or solid obstacle, find the best adjacent walkable floor block
        // so Vanilla PathFinder paths directly onto the floor tile next to it with accuracy = 0.
        if (rawState.getBlock() instanceof BedBlock || isSolidInteractiveOrObstacle(level, rawGoal, rawState)) {
            BlockPos bestAdj = null;
            double bestDistSq = Double.MAX_VALUE;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                for (int dy : new int[]{0, -1, 1}) {
                    BlockPos adj = rawGoal.relative(dir).above(dy);
                    if (isStandableForGoal(level, adj, allowWater)) {
                        double d = adj.distSqr(startPos);
                        if (d < bestDistSq) {
                            bestDistSq = d;
                            bestAdj = adj;
                        }
                    }
                }
            }
            if (bestAdj != null) {
                return new GoalResolution(bestAdj, 0);
            }
            return new GoalResolution(rawGoal, 1);
        }

        // If rawGoal is slightly above the floor (e.g. Vec3.atCenterOf), snap down 1 block to the floor
        if (!isStandableForGoal(level, rawGoal, allowWater) && isStandableForGoal(level, rawGoal.below(), allowWater)) {
            return new GoalResolution(rawGoal.below(), 0);
        }

        // If rawGoal itself is in water and water is not allowed, snap to the nearest dry standable block on the bank
        if (!allowWater && !isStandableForGoal(level, rawGoal, false)) {
            BlockPos dryNear = findNearestDryStandableNear(level, rawGoal, 4);
            if (dryNear != null) {
                return new GoalResolution(dryNear, 0);
            }
        }

        return new GoalResolution(rawGoal, 0);
    }

    private static @Nullable BlockPos findNearestDryStandableNear(ServerLevel level, BlockPos center, int radius) {
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 2, radius))) {
            if (isStandableForGoal(level, p, false)) {
                double d = center.distSqr(p);
                if (d < bestDistSq) {
                    bestDistSq = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    private static boolean isSolidInteractiveOrObstacle(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir()) return false;
        if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof FenceGateBlock) return false;
        if (state.getBlock() instanceof StairBlock || state.getBlock() instanceof SlabBlock) return false;
        if (state.getBlock() instanceof CarpetBlock || state.getBlock() instanceof TrapDoorBlock) return false;
        VoxelShape shape = state.getCollisionShape(level, pos);
        return !shape.isEmpty() && shape.max(Direction.Axis.Y) > 0.3;
    }

    private static boolean isStandableForGoal(ServerLevel level, BlockPos pos, boolean allowWater) {
        if (!allowWater && (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.below()).isEmpty())) {
            return false;
        }
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState floor = level.getBlockState(pos.below());
        if (feet.getBlock() instanceof BedBlock) return false;
        if (!head.getCollisionShape(level, pos.above()).isEmpty()
                && !(head.getBlock() instanceof DoorBlock)
                && !(head.getBlock() instanceof FenceGateBlock)) {
            return false;
        }
        if (feet.getBlock() instanceof StairBlock || feet.getBlock() instanceof SlabBlock) {
            return level.getBlockState(pos.above(2)).getCollisionShape(level, pos.above(2)).isEmpty();
        }
        VoxelShape feetShape = feet.getCollisionShape(level, pos);
        if (!feetShape.isEmpty() && feetShape.max(Direction.Axis.Y) > 0.25
                && !(feet.getBlock() instanceof DoorBlock)
                && !(feet.getBlock() instanceof FenceGateBlock)) {
            return false;
        }
        return !floor.getCollisionShape(level, pos.below()).isEmpty();
    }

    private static boolean hasDirectStepClearance(ServerLevel level, AiBotPlayer bot, Vec3 target) {
        Vec3 from = new Vec3(bot.getX(), bot.getY() + 0.6, bot.getZ());
        Vec3 to = new Vec3(target.x, target.y + 0.6, target.z);
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bot));
        if (hit.getType() == HitResult.Type.MISS) {
            return true;
        }
        BlockPos hitPos = hit.getBlockPos();
        BlockState hitState = level.getBlockState(hitPos);
        if (hitState.getBlock() instanceof DoorBlock && hitState.getValue(DoorBlock.OPEN)) {
            return true;
        }
        if (hitState.getBlock() instanceof FenceGateBlock && hitState.getValue(FenceGateBlock.OPEN)) {
            return true;
        }
        return hitPos.equals(BlockPos.containing(target));
    }

    /**
     * Computes a list of {@link BlockPos} waypoints using Vanilla's {@link GroundPathNavigation}.
     */
    public static List<BlockPos> findPath(AiBotPlayer bot, BlockPos startPos, BlockPos goalPos, int maxExpandedNodes) {
        if (!(bot.level() instanceof ServerLevel level)) {
            return Collections.emptyList();
        }
        BotProxyMob proxy = new BotProxyMob(bot, level);
        proxy.syncFromBot();
        BotGroundPathNavigation nav = (BotGroundPathNavigation) proxy.getNavigation();
        nav.getBotEvaluator().setCanOpenFenceGates(!bot.shouldRespectFencePerimeter());
        GoalResolution resolved = resolveGoal(level, goalPos, startPos, bot.isWaterAllowed());
        Path path = nav.createPath(resolved.goalPos(), resolved.accuracy());
        if (path == null) {
            return Collections.emptyList();
        }
        List<BlockPos> waypoints = new ArrayList<>(path.getNodeCount());
        for (int i = 0; i < path.getNodeCount(); i++) {
            waypoints.add(path.getNodePos(i));
        }
        return waypoints;
    }
}
