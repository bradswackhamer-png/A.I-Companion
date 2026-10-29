package com.aicompanion.bot.learning;

import com.aicompanion.bot.AiBotPlayer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Shared Hive-Mind Reinforcement Learning (RL) & Neural Network Coordinator.
 *
 * Combines:
 * 1. A Pure-Java Multi-Layer Perceptron (MLP: 24 -> 32 -> 16 -> 9) trained online via
 *    Policy Gradient / Temporal-Difference Eligibility Traces & Experience Replay.
 * 2. A 3D Spatial Pheromone & Waypoint Graph (learning optimal desire paths, player-demonstrated
 *    breadcrumb trails, jump nodes, and obstacle/stuck hazard zones).
 * 3. A Modded Block Walkability & Collision Knowledge Base (learning passability, shoulder-centering,
 *    and step-jump requirements per Block Registry ID).
 * 4. A Task & Routine Q-Table (learning player-rewarded and player-penalized task habits, roles,
 *    and preferred chest/work locations).
 */
public class HiveMindBrain {

    // =========================================================================
    // 1. MICRO-STEERING NEURAL NETWORK (24 -> 32 -> 16 -> 9)
    // =========================================================================
    public static final int INPUT_SIZE = 24;
    public static final int HIDDEN1_SIZE = 32;
    public static final int HIDDEN2_SIZE = 16;
    public static final int ACTION_SIZE = 9;

    // Micro-Steering Action Indices
    public static final int ACT_SPRINT_DIRECT = 0;
    public static final int ACT_WALK_CENTERED = 1;
    public static final int ACT_JUMP_ASCEND = 2;
    public static final int ACT_STRAFE_LEFT = 3;
    public static final int ACT_STRAFE_RIGHT = 4;
    public static final int ACT_BACKSTEP_PIVOT = 5;
    public static final int ACT_INTERACT_OPEN = 6;
    public static final int ACT_LEAP_GAP = 7;
    public static final int ACT_MACRO_DETOUR = 8;

    public static final String[] ACTION_NAMES = {
        "SPRINT_DIRECT",
        "WALK_CENTERED",
        "JUMP_ASCEND",
        "STRAFE_LEFT",
        "STRAFE_RIGHT",
        "BACKSTEP_PIVOT",
        "INTERACT_OPEN",
        "LEAP_GAP",
        "MACRO_DETOUR"
    };

    private static final float LEARNING_RATE = 0.018f;
    private static final float GAMMA = 0.92f;
    private static final float LEAKY_ALPHA = 0.05f;

    // Shared MLP Weights & Biases
    private static final float[][] W1 = new float[HIDDEN1_SIZE][INPUT_SIZE];
    private static final float[] B1 = new float[HIDDEN1_SIZE];
    private static final float[][] W2 = new float[HIDDEN2_SIZE][HIDDEN1_SIZE];
    private static final float[] B2 = new float[HIDDEN2_SIZE];
    private static final float[][] W3 = new float[ACTION_SIZE][HIDDEN2_SIZE];
    private static final float[] B3 = new float[ACTION_SIZE];

    // Telemetry & Hive Statistics
    private static long totalTrainingSteps = 0;
    private static double cumulativeReward = 0.0;
    private static int positivePlayerRewards = 0;
    private static int negativePlayerPunishments = 0;
    private static float explorationEpsilon = 0.14f;
    private static final Random RNG = new Random(42L);

    // Experience Replay Buffer (stores high-impact transitions for batch consolidation)
    public static class ExperienceStep {
        public final float[] state;
        public final int action;
        public float reward;
        public final long timestamp;
        public final @Nullable BlockPos pos;
        public final String dimension;
        public final String taskName;

        public ExperienceStep(float[] state, int action, float reward, @Nullable BlockPos pos, String dimension, String taskName) {
            this.state = state.clone();
            this.action = action;
            this.reward = reward;
            this.timestamp = System.currentTimeMillis();
            this.pos = pos != null ? pos.immutable() : null;
            this.dimension = dimension != null ? dimension : "minecraft:overworld";
            this.taskName = taskName != null ? taskName : "IDLE";
        }
    }

    private static final Deque<ExperienceStep> REPLAY_BUFFER = new ArrayDeque<>(512);
    private static final Map<UUID, Deque<ExperienceStep>> BOT_ELIGIBILITY_TRACES = new ConcurrentHashMap<>();

    // =========================================================================
    // 2. SPATIAL PHEROMONE & WAYPOINT NAVIGATION GRAPH
    // =========================================================================
    public static class NavNode {
        public int x;
        public int y;
        public int z;
        public String dimension = "minecraft:overworld";
        public float traversalScore = 1.0f; // Positive = good path, Negative = hazard/stuck
        public int stuckCount = 0;
        public int successCount = 0;
        public boolean playerDemonstrated = false;
        public boolean jumpRequired = false;
        public List<Long> neighbors = new ArrayList<>(); // Packed BlockPos of connected next steps

        public NavNode() {}

        public NavNode(BlockPos pos, String dimension) {
            this.x = pos.getX();
            this.y = pos.getY();
            this.z = pos.getZ();
            this.dimension = dimension;
        }

        public BlockPos toBlockPos() {
            return new BlockPos(x, y, z);
        }
    }

    private static final int MAX_NAV_NODES = 6000;
    private static final Map<String, Map<Long, NavNode>> DIMENSION_NAV_GRAPHS = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> BOT_LAST_NODE_POS = new ConcurrentHashMap<>();

    // Player Demonstration / Path Recording Mode (/bot learnpath)
    private static final Set<UUID> ACTIVE_PATH_TEACHERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, BlockPos> TEACHER_LAST_POS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> TEACHER_NODES_RECORDED = new ConcurrentHashMap<>();

    // =========================================================================
    // 3. MODDED BLOCK WALKABILITY & COLLISION KNOWLEDGE BASE
    // =========================================================================
    public static class BlockProfile {
        public String blockId = "";
        public float walkabilityScore = 0.0f;      // [-10, +10]
        public float centerAlignmentNeed = 0.0f;   // [0, 5] -> high for narrow stairs/bridges
        public float jumpAssistNeed = 0.0f;        // [0, 5] -> high for steps/ledges
        public int collisionsRecorded = 0;
        public int smoothTraversals = 0;

        public BlockProfile() {}

        public BlockProfile(String blockId) {
            this.blockId = blockId;
        }
    }

    private static final Map<String, BlockProfile> BLOCK_KNOWLEDGE = new ConcurrentHashMap<>();

    // =========================================================================
    // 4. TASK & ROUTINE PREFERENCE Q-TABLE
    // =========================================================================
    private static final Map<String, Float> TASK_Q_VALUES = new ConcurrentHashMap<>();
    private static final Map<String, BlockPos> PREFERRED_CHEST_LOCATIONS = new ConcurrentHashMap<>();

    static {
        initializeWarmStartNetwork();
    }

    /**
     * Initializes the MLP with sensible heuristic priors so bots start competent
     * and smoothly refine weights via online reinforcement learning.
     */
    public static synchronized void initializeWarmStartNetwork() {
        RNG.setSeed(1337L);
        for (int i = 0; i < HIDDEN1_SIZE; i++) {
            for (int j = 0; j < INPUT_SIZE; j++) {
                W1[i][j] = (RNG.nextFloat() - 0.5f) * 0.25f;
            }
            B1[i] = 0.02f;
        }
        for (int i = 0; i < HIDDEN2_SIZE; i++) {
            for (int j = 0; j < HIDDEN1_SIZE; j++) {
                W2[i][j] = (RNG.nextFloat() - 0.5f) * 0.25f;
            }
            B2[i] = 0.02f;
        }
        for (int i = 0; i < ACTION_SIZE; i++) {
            for (int j = 0; j < HIDDEN2_SIZE; j++) {
                W3[i][j] = (RNG.nextFloat() - 0.5f) * 0.20f;
            }
            B3[i] = 0.0f;
        }
        // Heuristic baseline biases so direct movement is favored when path is clear
        B3[ACT_SPRINT_DIRECT] = 0.65f;
        B3[ACT_WALK_CENTERED] = 0.35f;
        B3[ACT_JUMP_ASCEND] = 0.15f;
        B3[ACT_STRAFE_LEFT] = 0.10f;
        B3[ACT_STRAFE_RIGHT] = 0.10f;
        B3[ACT_INTERACT_OPEN] = 0.20f;
    }

    // =========================================================================
    // STATE FEATURE EXTRACTION (24 INPUTS)
    // =========================================================================
    /**
     * Builds the 24-element normalized sensory input vector for a bot moving toward target.
     *
     * Index breakdown:
     *  0: deltaX normalized (-1..1)
     *  1: deltaY normalized (-1..1)
     *  2: deltaZ normalized (-1..1)
     *  3: horizontal distance normalized (0..1, clamped at 32m)
     *  4: bot speed normalized (0..1)
     *  5: onGround (+1 or -1)
     *  6: horizontalCollision (+1 or -1)
     *  7: stagnation ratio (moveNoProgressTicks / 20.0, 0..1)
     *  8: front-feet solid/blocked (-1 clear, +1 blocked)
     *  9: front-head solid/blocked (-1 clear, +1 blocked)
     * 10: front is stair/slab (+1 or 0)
     * 11: current feet is stair/narrow (+1 or 0)
     * 12: front is door/gate/trapdoor (+1 or 0)
     * 13: left probe passable (+1 clear, -1 blocked)
     * 14: right probe passable (+1 clear, -1 blocked)
     * 15: front-left diagonal passable (+1 clear, -1 blocked)
     * 16: front-right diagonal passable (+1 clear, -1 blocked)
     * 17: front drop hazard (+1 deep pit, 0 flat, -0.5 safe step down)
     * 18: 1-block jumpable gap ahead (+1 or 0)
     * 19: learned spatial node score ahead (-1 hazard .. +1 highway)
     * 20: learned spatial node score left (-1 .. +1)
     * 21: learned spatial node score right (-1 .. +1)
     * 22: learned block walkability score ahead (-1 .. +1)
     * 23: learned block center-alignment requirement (0 .. +1)
     */
    public static float[] extractSensoryState(AiBotPlayer bot, Vec3 target, int moveNoProgressTicks, boolean frontStair, boolean currentStair, boolean frontDoorOrGate, boolean frontDeepPit) {
        float[] s = new float[INPUT_SIZE];
        Vec3 pos = bot.position();
        double dx = target.x - pos.x;
        double dy = target.y - pos.y;
        double dz = target.z - pos.z;
        double horizDist = Math.sqrt(dx * dx + dz * dz);
        double invDist = horizDist > 0.001 ? 1.0 / horizDist : 0.0;

        // Rotate relative to bot's facing yaw so left/right are egocentric
        float yawRad = (float) Math.toRadians(bot.getYRot());
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double rightX = -forwardZ;
        double rightZ = forwardX;

        double relForward = (dx * forwardX + dz * forwardZ) * invDist;
        double relRight = (dx * rightX + dz * rightZ) * invDist;

        s[0] = clamp((float) relForward, -1.0f, 1.0f);
        s[1] = clamp((float) (dy / 6.0), -1.0f, 1.0f);
        s[2] = clamp((float) relRight, -1.0f, 1.0f);
        s[3] = clamp((float) (horizDist / 32.0), 0.0f, 1.0f);

        Vec3 vel = bot.getDeltaMovement();
        double speed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        s[4] = clamp((float) (speed / 0.35), 0.0f, 1.0f);
        s[5] = bot.onGround() ? 1.0f : -1.0f;
        s[6] = bot.horizontalCollision ? 1.0f : -1.0f;
        s[7] = clamp(moveNoProgressTicks / 20.0f, 0.0f, 1.0f);

        BlockPos feet = bot.blockPosition();
        net.minecraft.core.Direction dir = bot.getDirection();
        net.minecraft.core.Direction leftDir = dir.getCounterClockWise();
        net.minecraft.core.Direction rightDir = dir.getClockWise();

        BlockPos frontFeet = feet.relative(dir);
        BlockPos frontHead = frontFeet.above();
        BlockState ffState = bot.level().getBlockState(frontFeet);
        BlockState fhState = bot.level().getBlockState(frontHead);

        s[8] = (!ffState.isAir() && !ffState.canBeReplaced()) ? 1.0f : -1.0f;
        s[9] = (!fhState.isAir() && !fhState.canBeReplaced()) ? 1.0f : -1.0f;
        s[10] = frontStair ? 1.0f : 0.0f;
        s[11] = currentStair ? 1.0f : 0.0f;
        s[12] = frontDoorOrGate ? 1.0f : 0.0f;

        BlockPos leftPos = feet.relative(leftDir);
        BlockPos rightPos = feet.relative(rightDir);
        BlockPos diagLeft = frontFeet.relative(leftDir);
        BlockPos diagRight = frontFeet.relative(rightDir);

        s[13] = isSimplePassable(bot, leftPos) ? 1.0f : -1.0f;
        s[14] = isSimplePassable(bot, rightPos) ? 1.0f : -1.0f;
        s[15] = isSimplePassable(bot, diagLeft) ? 1.0f : -1.0f;
        s[16] = isSimplePassable(bot, diagRight) ? 1.0f : -1.0f;
        s[17] = frontDeepPit ? 1.0f : 0.0f;

        BlockPos twoAhead = frontFeet.relative(dir);
        boolean jumpableGap = ffState.isAir() && bot.level().getBlockState(frontFeet.below()).isAir()
                && bot.level().getBlockState(twoAhead).isAir() && !bot.level().getBlockState(twoAhead.below()).isAir();
        s[18] = jumpableGap ? 1.0f : 0.0f;

        String dim = bot.level().dimension().identifier().toString();
        s[19] = clamp(getNodeScore(dim, frontFeet) / 20.0f, -1.0f, 1.0f);
        s[20] = clamp(getNodeScore(dim, leftPos) / 20.0f, -1.0f, 1.0f);
        s[21] = clamp(getNodeScore(dim, rightPos) / 20.0f, -1.0f, 1.0f);

        String frontBlockId = BuiltInRegistries.BLOCK.getKey(ffState.getBlock()).toString();
        BlockProfile bp = BLOCK_KNOWLEDGE.get(frontBlockId);
        if (bp != null) {
            s[22] = clamp(bp.walkabilityScore / 10.0f, -1.0f, 1.0f);
            s[23] = clamp(bp.centerAlignmentNeed / 5.0f, 0.0f, 1.0f);
        } else {
            String belowId = BuiltInRegistries.BLOCK.getKey(bot.level().getBlockState(feet.below()).getBlock()).toString();
            BlockProfile bpBelow = BLOCK_KNOWLEDGE.get(belowId);
            s[22] = bpBelow != null ? clamp(bpBelow.walkabilityScore / 10.0f, -1.0f, 1.0f) : 0.0f;
            s[23] = bpBelow != null ? clamp(bpBelow.centerAlignmentNeed / 5.0f, 0.0f, 1.0f) : 0.0f;
        }

        return s;
    }

    private static boolean isSimplePassable(AiBotPlayer bot, BlockPos pos) {
        BlockState f = bot.level().getBlockState(pos);
        BlockState h = bot.level().getBlockState(pos.above());
        return (f.isAir() || f.canBeReplaced()) && (h.isAir() || h.canBeReplaced());
    }

    // =========================================================================
    // FORWARD PASS & ACTION SELECTION
    // =========================================================================
    public static synchronized float[] forward(float[] input) {
        float[] h1 = new float[HIDDEN1_SIZE];
        for (int i = 0; i < HIDDEN1_SIZE; i++) {
            float sum = B1[i];
            for (int j = 0; j < INPUT_SIZE; j++) {
                sum += W1[i][j] * input[j];
            }
            h1[i] = sum >= 0 ? sum : LEAKY_ALPHA * sum;
        }

        float[] h2 = new float[HIDDEN2_SIZE];
        for (int i = 0; i < HIDDEN2_SIZE; i++) {
            float sum = B2[i];
            for (int j = 0; j < HIDDEN1_SIZE; j++) {
                sum += W2[i][j] * h1[j];
            }
            h2[i] = sum >= 0 ? sum : LEAKY_ALPHA * sum;
        }

        float[] out = new float[ACTION_SIZE];
        for (int i = 0; i < ACTION_SIZE; i++) {
            float sum = B3[i];
            for (int j = 0; j < HIDDEN2_SIZE; j++) {
                sum += W3[i][j] * h2[j];
            }
            out[i] = (float) Math.tanh(sum); // Bounded Q-score in [-1, 1]
        }
        return out;
    }

    /**
     * Selects the best micro-steering action for the current state, applying context masks
     * and epsilon-greedy exploration when stuck.
     */
    public static synchronized int selectAction(AiBotPlayer bot, float[] state) {
        float[] qValues = forward(state);

        // Context-aware priors added to learned Q-values so the network specializes cleanly
        boolean blockedAhead = state[8] > 0.5f || state[9] > 0.5f;
        boolean onOrNearStair = state[10] > 0.5f || state[11] > 0.5f;
        boolean doorAhead = state[12] > 0.5f;
        boolean stagnating = state[7] > 0.65f;
        boolean leftClear = state[13] > 0.0f;
        boolean rightClear = state[14] > 0.0f;
        boolean gapAhead = state[18] > 0.5f;
        boolean needsCentering = state[23] > 0.25f || onOrNearStair || doorAhead;

        if (doorAhead) qValues[ACT_INTERACT_OPEN] += 0.55f;
        if (needsCentering) qValues[ACT_WALK_CENTERED] += 0.45f;
        if (onOrNearStair && state[1] >= -0.05f) qValues[ACT_JUMP_ASCEND] += 0.50f;
        if (gapAhead) qValues[ACT_LEAP_GAP] += 0.60f;
        if (blockedAhead || stagnating) {
            qValues[ACT_SPRINT_DIRECT] -= 0.60f;
            if (leftClear && state[20] >= state[21]) qValues[ACT_STRAFE_LEFT] += 0.40f;
            if (rightClear && state[21] >= state[20]) qValues[ACT_STRAFE_RIGHT] += 0.40f;
            if (!leftClear && !rightClear) qValues[ACT_BACKSTEP_PIVOT] += 0.45f;
            if (state[7] > 0.85f) qValues[ACT_MACRO_DETOUR] += 0.50f;
        }

        // Epsilon-greedy exploration primarily when experiencing friction/stagnation
        float effectiveEps = stagnating ? Math.max(explorationEpsilon, 0.25f) : (explorationEpsilon * 0.2f);
        int chosen = 0;
        if (RNG.nextFloat() < effectiveEps) {
            chosen = RNG.nextInt(ACTION_SIZE);
        } else {
            float bestQ = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < ACTION_SIZE; i++) {
                if (qValues[i] > bestQ) {
                    bestQ = qValues[i];
                    chosen = i;
                }
            }
        }

        // Record step in bot's Eligibility Trace (sampled every 4 ticks to cover ~8 seconds in 40 entries)
        if (bot.tickCount % 4 == 0) {
            Deque<ExperienceStep> trace = BOT_ELIGIBILITY_TRACES.computeIfAbsent(bot.getUUID(), k -> new ArrayDeque<>(48));
            synchronized (trace) {
                while (trace.size() >= 40) {
                    trace.pollFirst();
                }
                String task = bot.getCurrentActionTypeForLearning();
                String dim = bot.level().dimension().identifier().toString();
                trace.addLast(new ExperienceStep(state, chosen, 0.0f, bot.blockPosition(), dim, task));
            }
        }

        return chosen;
    }

    // =========================================================================
    // ONLINE BACKPROPAGATION & ELIGIBILITY TRACE REINFORCEMENT
    // =========================================================================
    /**
     * Trains the MLP on a single (state, action, targetReward) tuple via gradient descent.
     */
    public static synchronized void trainSingleStep(float[] input, int action, float targetReward, float weightMultiplier) {
        if (action < 0 || action >= ACTION_SIZE || input.length != INPUT_SIZE) return;

        // Forward pass with cached activations
        float[] z1 = new float[HIDDEN1_SIZE];
        float[] h1 = new float[HIDDEN1_SIZE];
        for (int i = 0; i < HIDDEN1_SIZE; i++) {
            float sum = B1[i];
            for (int j = 0; j < INPUT_SIZE; j++) sum += W1[i][j] * input[j];
            z1[i] = sum;
            h1[i] = sum >= 0 ? sum : LEAKY_ALPHA * sum;
        }

        float[] z2 = new float[HIDDEN2_SIZE];
        float[] h2 = new float[HIDDEN2_SIZE];
        for (int i = 0; i < HIDDEN2_SIZE; i++) {
            float sum = B2[i];
            for (int j = 0; j < HIDDEN1_SIZE; j++) sum += W2[i][j] * h1[j];
            z2[i] = sum;
            h2[i] = sum >= 0 ? sum : LEAKY_ALPHA * sum;
        }

        float sumOut = B3[action];
        for (int j = 0; j < HIDDEN2_SIZE; j++) sumOut += W3[action][j] * h2[j];
        float pred = (float) Math.tanh(sumOut);

        float target = clamp(targetReward / 10.0f, -1.0f, 1.0f);
        float error = pred - target;
        float dOut = error * (1.0f - pred * pred) * clamp(weightMultiplier, 0.1f, 3.0f);

        float lr = LEARNING_RATE;

        // Gradients for Hidden2
        float[] dH2 = new float[HIDDEN2_SIZE];
        for (int j = 0; j < HIDDEN2_SIZE; j++) {
            dH2[j] = dOut * W3[action][j] * (z2[j] >= 0 ? 1.0f : LEAKY_ALPHA);
            W3[action][j] = clamp(W3[action][j] - lr * dOut * h2[j], -4.0f, 4.0f);
        }
        B3[action] = clamp(B3[action] - lr * dOut, -3.0f, 3.0f);

        // Gradients for Hidden1
        float[] dH1 = new float[HIDDEN1_SIZE];
        for (int i = 0; i < HIDDEN2_SIZE; i++) {
            for (int j = 0; j < HIDDEN1_SIZE; j++) {
                dH1[j] += dH2[i] * W2[i][j];
                W2[i][j] = clamp(W2[i][j] - lr * dH2[i] * h1[j], -4.0f, 4.0f);
            }
            B2[i] = clamp(B2[i] - lr * dH2[i], -3.0f, 3.0f);
        }

        for (int j = 0; j < HIDDEN1_SIZE; j++) {
            float dZ1 = dH1[j] * (z1[j] >= 0 ? 1.0f : LEAKY_ALPHA);
            for (int k = 0; k < INPUT_SIZE; k++) {
                W1[j][k] = clamp(W1[j][k] - lr * dZ1 * input[k], -4.0f, 4.0f);
            }
            B1[j] = clamp(B1[j] - lr * dZ1, -3.0f, 3.0f);
        }

        totalTrainingSteps++;
        // Gradually anneal exploration epsilon as the hive-mind matures (down to 0.03)
        if (explorationEpsilon > 0.03f && totalTrainingSteps % 50 == 0) {
            explorationEpsilon = Math.max(0.03f, explorationEpsilon * 0.995f);
        }
    }

    /**
     * Propagates a reward (positive or negative) backward along a bot's recent Eligibility Trace,
     * updating the Neural Network, the Spatial Pheromone Graph, the Block Walkability Table,
     * and the Task Preference Q-Table simultaneously.
     */
    public static synchronized int applyReinforcementSignal(AiBotPlayer bot, float reward, boolean isExplicitPlayerFeedback) {
        cumulativeReward += reward;
        if (isExplicitPlayerFeedback) {
            if (reward > 0) positivePlayerRewards++;
            else if (reward < 0) negativePlayerPunishments++;
        }

        Deque<ExperienceStep> trace = BOT_ELIGIBILITY_TRACES.get(bot.getUUID());
        int stepsUpdated = 0;
        if (trace != null) {
            List<ExperienceStep> snapshot;
            synchronized (trace) {
                snapshot = new ArrayList<>(trace);
            }
            float discount = 1.0f;
            float mult = isExplicitPlayerFeedback ? 2.2f : 0.8f;

            for (int i = snapshot.size() - 1; i >= 0; i--) {
                ExperienceStep step = snapshot.get(i);
                float discountedReward = reward * discount;
                trainSingleStep(step.state, step.action, discountedReward, mult);

                if (step.pos != null) {
                    recordNodeTraversal(step.dimension, step.pos, discountedReward * 0.5f, discountedReward < -2.0f, false, false, null);
                }

                // Store high-salience steps in replay buffer
                if (Math.abs(discountedReward) >= 1.5f) {
                    step.reward = discountedReward;
                    if (REPLAY_BUFFER.size() >= 512) REPLAY_BUFFER.pollFirst();
                    REPLAY_BUFFER.addLast(step);
                }

                discount *= GAMMA;
                stepsUpdated++;
                if (discount < 0.08f) break;
            }
        }

        // Also update Task Q-Table for current/recent task and role
        String role = bot.getBotRole().toLowerCase(Locale.ROOT);
        String task = bot.getCurrentActionTypeForLearning();
        updateTaskQValue(role + ":" + task, reward * (isExplicitPlayerFeedback ? 1.5f : 0.3f));

        // Update block knowledge at bot's current position
        BlockPos feet = bot.blockPosition();
        String frontBlockId = BuiltInRegistries.BLOCK.getKey(bot.level().getBlockState(feet.relative(bot.getDirection())).getBlock()).toString();
        String belowBlockId = BuiltInRegistries.BLOCK.getKey(bot.level().getBlockState(feet.below()).getBlock()).toString();
        recordBlockOutcome(frontBlockId, reward, bot.horizontalCollision);
        if (!belowBlockId.equals("minecraft:air")) {
            recordBlockOutcome(belowBlockId, reward * 0.5f, false);
        }

        // Replay mini-batch consolidation on explicit player feedback
        if (isExplicitPlayerFeedback && !REPLAY_BUFFER.isEmpty()) {
            List<ExperienceStep> bufferList = new ArrayList<>(REPLAY_BUFFER);
            int batchSize = Math.min(12, bufferList.size());
            for (int b = 0; b < batchSize; b++) {
                ExperienceStep sample = bufferList.get(RNG.nextInt(bufferList.size()));
                trainSingleStep(sample.state, sample.action, sample.reward, 0.75f);
            }
        }

        return stepsUpdated;
    }

    // =========================================================================
    // SPATIAL PHEROMONE & WAYPOINT GRAPH OPERATIONS
    // =========================================================================
    public static void recordBotStep(AiBotPlayer bot, boolean madeProgress, boolean isStuck, boolean jumped) {
        String dim = bot.level().dimension().identifier().toString();
        BlockPos current = bot.blockPosition();
        BlockPos prev = BOT_LAST_NODE_POS.get(bot.getUUID());

        if (isStuck) {
            // Penalize the block where the bot got stuck
            net.minecraft.core.Direction dir = bot.getDirection();
            BlockPos obstaclePos = current.relative(dir);
            recordNodeTraversal(dim, current, -3.5f, true, false, false, null);
            recordNodeTraversal(dim, obstaclePos, -5.0f, true, false, false, null);
            applyReinforcementSignal(bot, -3.0f, false);
            return;
        }

        if (madeProgress && (prev == null || !prev.equals(current))) {
            if (prev != null && prev.distManhattan(current) <= 4) {
                recordNodeTraversal(dim, prev, +0.6f, false, false, jumped, current);
            }
            recordNodeTraversal(dim, current, +0.4f, false, false, jumped, null);
            BOT_LAST_NODE_POS.put(bot.getUUID(), current.immutable());

            if (bot.tickCount % 20 == 0) {
                applyReinforcementSignal(bot, +0.5f, false);
            }
        }
    }

    public static synchronized void recordNodeTraversal(
            String dimension,
            BlockPos pos,
            float deltaScore,
            boolean stuckEvent,
            boolean playerDemo,
            boolean jumped,
            @Nullable BlockPos nextStep) {

        Map<Long, NavNode> map = DIMENSION_NAV_GRAPHS.computeIfAbsent(dimension, k -> new LinkedHashMap<>());
        long key = pos.asLong();
        NavNode node = map.get(key);
        if (node == null) {
            if (map.size() >= MAX_NAV_NODES) {
                // Evict oldest non-demonstrated low-magnitude node
                Long toRemove = null;
                for (Map.Entry<Long, NavNode> e : map.entrySet()) {
                    if (!e.getValue().playerDemonstrated && Math.abs(e.getValue().traversalScore) < 3.0f) {
                        toRemove = e.getKey();
                        break;
                    }
                }
                if (toRemove == null && !map.isEmpty()) {
                    toRemove = map.keySet().iterator().next();
                }
                if (toRemove != null) map.remove(toRemove);
            }
            node = new NavNode(pos, dimension);
            map.put(key, node);
        }

        node.traversalScore = clamp(node.traversalScore + deltaScore, -50.0f, 100.0f);
        if (stuckEvent) node.stuckCount++;
        else if (deltaScore > 0) node.successCount++;
        if (playerDemo) {
            node.playerDemonstrated = true;
            node.traversalScore = Math.max(node.traversalScore, 35.0f);
            node.stuckCount = 0;
        }
        if (jumped) node.jumpRequired = true;

        if (nextStep != null && !nextStep.equals(pos)) {
            long nextKey = nextStep.asLong();
            if (!node.neighbors.contains(nextKey)) {
                if (node.neighbors.size() >= 8) node.neighbors.remove(0);
                node.neighbors.add(nextKey);
            }
        }
    }

    public static float getNodeScore(String dimension, BlockPos pos) {
        Map<Long, NavNode> map = DIMENSION_NAV_GRAPHS.get(dimension);
        if (map == null) return 0.0f;
        NavNode n = map.get(pos.asLong());
        return n != null ? n.traversalScore : 0.0f;
    }

    public static @Nullable NavNode getNode(String dimension, BlockPos pos) {
        Map<Long, NavNode> map = DIMENSION_NAV_GRAPHS.get(dimension);
        return map != null ? map.get(pos.asLong()) : null;
    }

    /**
     * Queries the learned Spatial Pheromone & Player Breadcrumb Graph to find an intermediate
     * high-confidence waypoint or obstacle detour between start and target.
     */
    public static synchronized @Nullable BlockPos findLearnedWaypointTowards(String dimension, BlockPos start, Vec3 goalVec) {
        Map<Long, NavNode> map = DIMENSION_NAV_GRAPHS.get(dimension);
        if (map == null || map.isEmpty()) return null;

        BlockPos goalPos = BlockPos.containing(goalVec);
        double directDistSq = start.distSqr(goalPos);
        if (directDistSq < 4.0) return null;

        // 1. Check if the bot is on or adjacent to a learned node with connected edges leading closer to the goal
        NavNode bestNext = null;
        double bestScore = -999999.0;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos near = start.offset(dx, dy, dz);
                    NavNode nearNode = map.get(near.asLong());
                    if (nearNode == null || nearNode.traversalScore < 0) continue;

                    for (long neighborKey : nearNode.neighbors) {
                        NavNode nextNode = map.get(neighborKey);
                        if (nextNode == null || nextNode.traversalScore < -2.0f) continue;
                        BlockPos nextPos = nextNode.toBlockPos();
                        double nextDistToGoalSq = nextPos.distSqr(goalPos);
                        double distFromBotSq = nextPos.distSqr(start);
                        if (distFromBotSq < 1.0 || distFromBotSq > 64.0) continue;

                        // Progress toward goal + learned pheromone weight + bonus for player demonstration
                        double progress = Math.sqrt(directDistSq) - Math.sqrt(nextDistToGoalSq);
                        if (progress < -1.5 && !nextNode.playerDemonstrated) continue;

                        double candidateScore = progress * 4.0 + nextNode.traversalScore * 0.5
                                + (nextNode.playerDemonstrated ? 25.0 : 0.0);
                        if (candidateScore > bestScore) {
                            bestScore = candidateScore;
                            bestNext = nextNode;
                        }
                    }
                }
            }
        }

        if (bestNext != null && bestScore > 4.0) {
            return bestNext.toBlockPos();
        }

        // 2. Check if there is a known high-penalty hazard node directly ahead; if so, pick the best adjacent safe node
        Vec3 dirToGoal = goalVec.subtract(Vec3.atCenterOf(start)).normalize();
        BlockPos twoAhead = BlockPos.containing( start.getX() + 0.5 + dirToGoal.x * 2.0, start.getY(), start.getZ() + 0.5 + dirToGoal.z * 2.0 );
        if (getNodeScore(dimension, twoAhead) < -4.0f) {
            BlockPos bestDetour = null;
            double bestDetourScore = -999999.0;
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (Math.abs(dx) < 2 && Math.abs(dz) < 2) continue;
                    BlockPos cand = start.offset(dx, 0, dz);
                    float nodeScore = getNodeScore(dimension, cand);
                    if (nodeScore < -1.0f) continue;
                    double progress = Math.sqrt(directDistSq) - Math.sqrt(cand.distSqr(goalPos));
                    double score = progress * 2.0 + nodeScore;
                    if (score > bestDetourScore) {
                        bestDetourScore = score;
                        bestDetour = cand;
                    }
                }
            }
            return bestDetour;
        }

        return null;
    }

    // =========================================================================
    // PLAYER DEMONSTRATION PATH RECORDING (/bot learnpath)
    // =========================================================================
    public static boolean togglePlayerPathRecording(ServerPlayer player) {
        if (ACTIVE_PATH_TEACHERS.contains(player.getUUID())) {
            stopPlayerPathRecording(player);
            return false;
        } else {
            startPlayerPathRecording(player);
            return true;
        }
    }

    public static void startPlayerPathRecording(ServerPlayer player) {
        ACTIVE_PATH_TEACHERS.add(player.getUUID());
        TEACHER_LAST_POS.put(player.getUUID(), player.blockPosition().immutable());
        TEACHER_NODES_RECORDED.put(player.getUUID(), 1);
        String dim = player.level().dimension().identifier().toString();
        recordNodeTraversal(dim, player.blockPosition(), +40.0f, false, true, false, null);
    }

    public static int stopPlayerPathRecording(ServerPlayer player) {
        ACTIVE_PATH_TEACHERS.remove(player.getUUID());
        TEACHER_LAST_POS.remove(player.getUUID());
        Integer count = TEACHER_NODES_RECORDED.remove(player.getUUID());
        return count != null ? count : 0;
    }

    public static boolean isPlayerRecordingPath(UUID playerUuid) {
        return ACTIVE_PATH_TEACHERS.contains(playerUuid);
    }

    /**
     * Called on server player tick when a player has `/bot learnpath` enabled.
     * Records their footsteps and jumps into the shared Hive-Mind Navigation Graph.
     */
    public static void tickPlayerPathRecording(ServerPlayer player) {
        if (!ACTIVE_PATH_TEACHERS.contains(player.getUUID())) return;
        if (player.tickCount % 5 != 0) return;

        BlockPos current = player.blockPosition();
        BlockPos last = TEACHER_LAST_POS.get(player.getUUID());
        boolean jumped = !player.onGround() && player.getDeltaMovement().y > 0.05;

        if (last == null || !last.equals(current)) {
            String dim = player.level().dimension().identifier().toString();
            if (last != null && last.distManhattan(current) <= 6) {
                recordNodeTraversal(dim, last, +35.0f, false, true, jumped, current);
            }
            recordNodeTraversal(dim, current, +35.0f, false, true, jumped, null);
            TEACHER_LAST_POS.put(player.getUUID(), current.immutable());
            TEACHER_NODES_RECORDED.merge(player.getUUID(), 1, Integer::sum);

            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        current.getX() + 0.5, current.getY() + 0.25, current.getZ() + 0.5,
                        3, 0.15, 0.1, 0.15, 0.01);
            }
        }
    }

    // =========================================================================
    // MODDED BLOCK KNOWLEDGE BASE
    // =========================================================================
    public static synchronized void recordBlockOutcome(String blockId, float reward, boolean collided) {
        if (blockId == null || blockId.isBlank() || "minecraft:air".equals(blockId)) return;
        BlockProfile bp = BLOCK_KNOWLEDGE.computeIfAbsent(blockId, BlockProfile::new);
        bp.walkabilityScore = clamp(bp.walkabilityScore + (reward * 0.15f), -10.0f, 10.0f);
        if (collided || reward < -1.0f) {
            bp.collisionsRecorded++;
            String lower = blockId.toLowerCase(Locale.ROOT);
            if (lower.contains("stair") || lower.contains("bridge") || lower.contains("rail")
                    || lower.contains("balustrade") || lower.contains("door") || lower.contains("gate")
                    || lower.contains("catwalk") || lower.contains("plank")) {
                bp.centerAlignmentNeed = clamp(bp.centerAlignmentNeed + 0.4f, 0.0f, 5.0f);
                bp.jumpAssistNeed = clamp(bp.jumpAssistNeed + 0.25f, 0.0f, 5.0f);
            }
        } else if (reward > 0.5f) {
            bp.smoothTraversals++;
        }
    }

    public static float getBlockCenterAlignmentNeed(String blockId) {
        BlockProfile bp = BLOCK_KNOWLEDGE.get(blockId);
        return bp != null ? bp.centerAlignmentNeed : 0.0f;
    }

    // =========================================================================
    // TASK & ROUTINE PREFERENCE Q-TABLE
    // =========================================================================
    public static synchronized void updateTaskQValue(String key, float delta) {
        if (key == null || key.isBlank()) return;
        String cleanKey = key.toLowerCase(Locale.ROOT).trim();
        float cur = TASK_Q_VALUES.getOrDefault(cleanKey, 0.0f);
        TASK_Q_VALUES.put(cleanKey, clamp(cur + delta, -50.0f, 50.0f));
    }

    public static float getTaskQValue(String role, String actionType) {
        String key = (role + ":" + actionType).toLowerCase(Locale.ROOT).trim();
        return TASK_Q_VALUES.getOrDefault(key, 0.0f);
    }

    public static void recordPreferredChest(String category, BlockPos pos) {
        if (category != null && pos != null) {
            PREFERRED_CHEST_LOCATIONS.put(category.toLowerCase(Locale.ROOT), pos.immutable());
        }
    }

    public static @Nullable BlockPos getPreferredChest(String category) {
        return PREFERRED_CHEST_LOCATIONS.get(category.toLowerCase(Locale.ROOT));
    }

    // =========================================================================
    // VISUALIZATION & TELEMETRY SUMMARY
    // =========================================================================
    public static int getHiveIqScore() {
        int base = 100;
        int expBonus = (int) Math.min(60, Math.sqrt(totalTrainingSteps) * 1.2);
        int rewardBonus = (int) clamp((float) (cumulativeReward / 40.0), -25.0f, 50.0f);
        int nodeBonus = Math.min(30, getTotalNavNodesCount() / 15);
        return Math.max(50, base + expBonus + rewardBonus + nodeBonus);
    }

    public static int getTotalNavNodesCount() {
        int sum = 0;
        for (Map<Long, NavNode> m : DIMENSION_NAV_GRAPHS.values()) {
            sum += m.size();
        }
        return sum;
    }

    public static int getDemonstratedNodesCount() {
        int sum = 0;
        for (Map<Long, NavNode> m : DIMENSION_NAV_GRAPHS.values()) {
            for (NavNode n : m.values()) {
                if (n.playerDemonstrated) sum++;
            }
        }
        return sum;
    }

    public static int getHazardNodesCount() {
        int sum = 0;
        for (Map<Long, NavNode> m : DIMENSION_NAV_GRAPHS.values()) {
            for (NavNode n : m.values()) {
                if (n.traversalScore < -2.0f) sum++;
            }
        }
        return sum;
    }

    public static String getBrainStatsSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Hive-Mind IQ: %d | Neural Architecture: %d->%d->%d->%d MLP\n",
                getHiveIqScore(), INPUT_SIZE, HIDDEN1_SIZE, HIDDEN2_SIZE, ACTION_SIZE));
        sb.append(String.format("Training Updates: %d | Cumulative Reward: %+.1f | Exploration (eps): %.1f%%\n",
                totalTrainingSteps, cumulativeReward, explorationEpsilon * 100.0f));
        sb.append(String.format("Player Reinforcements: %d Rewards (+) / %d Corrections (-)\n",
                positivePlayerRewards, negativePlayerPunishments));
        sb.append(String.format("Spatial Graph: %d Total Nodes (%d Golden Demo Trails, %d Learned Hazard Zones)\n",
                getTotalNavNodesCount(), getDemonstratedNodesCount(), getHazardNodesCount()));
        sb.append(String.format("Modded/Block Profiles Learned: %d | Task Q-Entries: %d",
                BLOCK_KNOWLEDGE.size(), TASK_Q_VALUES.size()));
        return sb.toString();
    }

    public static String getTelemetryContextForGemini(String botRole) {
        StringBuilder sb = new StringBuilder();
        sb.append("HiveMindIQ=").append(getHiveIqScore())
          .append(",LearnedNodes=").append(getTotalNavNodesCount());
        String prefix = botRole.toLowerCase(Locale.ROOT) + ":";
        List<Map.Entry<String, Float>> matching = new ArrayList<>();
        for (Map.Entry<String, Float> e : TASK_Q_VALUES.entrySet()) {
            if (e.getKey().startsWith(prefix) && Math.abs(e.getValue()) >= 1.0f) {
                matching.add(e);
            }
        }
        if (!matching.isEmpty()) {
            matching.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
            sb.append(",TaskPreferences=[");
            for (int i = 0; i < Math.min(5, matching.size()); i++) {
                if (i > 0) sb.append(", ");
                String taskName = matching.get(i).getKey().substring(prefix.length());
                sb.append(taskName).append(String.format("(%+.1f)", matching.get(i).getValue()));
            }
            sb.append("]");
        }
        return sb.toString();
    }

    /**
     * Renders 3D particles around the player showing learned paths, player breadcrumb trails, and hazards.
     */
    public static int visualizeBrainAroundPlayer(ServerPlayer player, int radius) {
        if (!(player.level() instanceof ServerLevel sl)) return 0;
        String dim = sl.dimension().identifier().toString();
        Map<Long, NavNode> map = DIMENSION_NAV_GRAPHS.get(dim);
        if (map == null || map.isEmpty()) return 0;

        BlockPos center = player.blockPosition();
        double maxDistSq = radius * radius;
        int shown = 0;

        for (NavNode n : map.values()) {
            BlockPos pos = n.toBlockPos();
            if (pos.distSqr(center) > maxDistSq) continue;
            shown++;
            if (n.playerDemonstrated) {
                sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 4, 0.1, 0.1, 0.1, 0.01);
            } else if (n.traversalScore >= 2.0f) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 3, 0.15, 0.1, 0.15, 0.01);
            } else if (n.traversalScore < -2.0f) {
                sl.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 4, 0.15, 0.15, 0.15, 0.01);
            }
        }
        return shown;
    }

    public static synchronized void resetBrain() {
        DIMENSION_NAV_GRAPHS.clear();
        BLOCK_KNOWLEDGE.clear();
        TASK_Q_VALUES.clear();
        PREFERRED_CHEST_LOCATIONS.clear();
        REPLAY_BUFFER.clear();
        BOT_ELIGIBILITY_TRACES.clear();
        totalTrainingSteps = 0;
        cumulativeReward = 0.0;
        positivePlayerRewards = 0;
        negativePlayerPunishments = 0;
        explorationEpsilon = 0.14f;
        initializeWarmStartNetwork();
    }

    // =========================================================================
    // SERIALIZATION FOR BOTPERSISTENCE
    // =========================================================================
    public static class SavedBrainState {
        public long totalTrainingSteps = 0;
        public double cumulativeReward = 0.0;
        public int positivePlayerRewards = 0;
        public int negativePlayerPunishments = 0;
        public float explorationEpsilon = 0.14f;
        public float[][] w1;
        public float[] b1;
        public float[][] w2;
        public float[] b2;
        public float[][] w3;
        public float[] b3;
        public List<NavNode> navNodes = new ArrayList<>();
        public List<BlockProfile> blockProfiles = new ArrayList<>();
        public Map<String, Float> taskQValues = new LinkedHashMap<>();
    }

    public static synchronized SavedBrainState exportState() {
        SavedBrainState s = new SavedBrainState();
        s.totalTrainingSteps = totalTrainingSteps;
        s.cumulativeReward = cumulativeReward;
        s.positivePlayerRewards = positivePlayerRewards;
        s.negativePlayerPunishments = negativePlayerPunishments;
        s.explorationEpsilon = explorationEpsilon;
        s.w1 = deepCopyMatrix(W1);
        s.b1 = B1.clone();
        s.w2 = deepCopyMatrix(W2);
        s.b2 = B2.clone();
        s.w3 = deepCopyMatrix(W3);
        s.b3 = B3.clone();

        for (Map<Long, NavNode> dimMap : DIMENSION_NAV_GRAPHS.values()) {
            for (NavNode n : dimMap.values()) {
                // Persist demonstrated nodes, hazards, and well-traveled trails
                if (n.playerDemonstrated || Math.abs(n.traversalScore) >= 2.0f || !n.neighbors.isEmpty()) {
                    s.navNodes.add(n);
                    if (s.navNodes.size() >= 3000) break;
                }
            }
            if (s.navNodes.size() >= 3000) break;
        }

        s.blockProfiles.addAll(BLOCK_KNOWLEDGE.values());
        s.taskQValues.putAll(TASK_Q_VALUES);
        return s;
    }

    public static synchronized void importState(@Nullable SavedBrainState s) {
        if (s == null) return;
        totalTrainingSteps = s.totalTrainingSteps;
        cumulativeReward = s.cumulativeReward;
        positivePlayerRewards = s.positivePlayerRewards;
        negativePlayerPunishments = s.negativePlayerPunishments;
        explorationEpsilon = s.explorationEpsilon > 0 ? s.explorationEpsilon : 0.14f;

        copyMatrixInto(s.w1, W1);
        copyArrayInto(s.b1, B1);
        copyMatrixInto(s.w2, W2);
        copyArrayInto(s.b2, B2);
        copyMatrixInto(s.w3, W3);
        copyArrayInto(s.b3, B3);

        DIMENSION_NAV_GRAPHS.clear();
        if (s.navNodes != null) {
            for (NavNode n : s.navNodes) {
                if (n != null && n.dimension != null) {
                    Map<Long, NavNode> m = DIMENSION_NAV_GRAPHS.computeIfAbsent(n.dimension, k -> new LinkedHashMap<>());
                    m.put(n.toBlockPos().asLong(), n);
                }
            }
        }

        BLOCK_KNOWLEDGE.clear();
        if (s.blockProfiles != null) {
            for (BlockProfile bp : s.blockProfiles) {
                if (bp != null && bp.blockId != null) {
                    BLOCK_KNOWLEDGE.put(bp.blockId, bp);
                }
            }
        }

        TASK_Q_VALUES.clear();
        if (s.taskQValues != null) {
            TASK_Q_VALUES.putAll(s.taskQValues);
        }
    }

    private static float[][] deepCopyMatrix(float[][] src) {
        float[][] copy = new float[src.length][];
        for (int i = 0; i < src.length; i++) {
            copy[i] = src[i].clone();
        }
        return copy;
    }

    private static void copyMatrixInto(float[][] src, float[][] dst) {
        if (src == null || src.length != dst.length) return;
        for (int i = 0; i < dst.length; i++) {
            if (src[i] != null && src[i].length == dst[i].length) {
                System.arraycopy(src[i], 0, dst[i], 0, dst[i].length);
            }
        }
    }

    private static void copyArrayInto(float[] src, float[] dst) {
        if (src != null && src.length == dst.length) {
            System.arraycopy(src, 0, dst, 0, dst.length);
        }
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}
