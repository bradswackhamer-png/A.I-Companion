package com.aicompanion.bot;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelFutureListener;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.network.protocol.game.ServerboundEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket;
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.stats.Stat;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class AiBotPlayer extends ServerPlayer {
    private final String botName;
    private final String botRole;
    private final Deque<BotAction> actionQueue = new ArrayDeque<>();
    private @Nullable BotAction currentAction = null;

    // Task execution state
    private int actionProgressCount = 0;
    private int actionTicks = 0;
    private @Nullable BlockPos targetBlockPos = null;
    private int miningTicks = 0;
    private int attackCooldown = 0;
    private int stuckTicks = 0;
    private Vec3 lastCheckedPos = Vec3.ZERO;
    private double bestGoalDistSq = Double.MAX_VALUE;
    private int noProgressTicks = 0;

    public AiBotPlayer(ServerLevel level, GameProfile profile, String role) {
        super(level.getServer(), level, profile, ClientInformation.createDefault());
        this.botName = profile.name();
        this.botRole = (role == null || role.isBlank()) ? "All-Around Survival Companion" : role;
        this.connection = new BotNetHandler(level.getServer(), this);
    }

    public String getBotName() {
        return this.botName;
    }

    public String getBotRole() {
        return this.botRole;
    }

    public void applyRoleStarterKit() {
        String r = this.botRole.toLowerCase();
        if (r.contains("guard") || r.contains("bodyguard") || r.contains("warrior") || r.contains("combat")) {
            this.getInventory().add(new ItemStack(Items.STONE_SWORD, 1));
            this.getInventory().add(new ItemStack(Items.SHIELD, 1));
            this.getInventory().add(new ItemStack(Items.IRON_HELMET, 1));
            this.autoEquipArmor();
            this.equipBestWeapon();
        } else if (r.contains("miner") || r.contains("mining")) {
            this.getInventory().add(new ItemStack(Items.STONE_PICKAXE, 1));
            this.getInventory().add(new ItemStack(Items.TORCH, 32));
        } else if (r.contains("lumber") || r.contains("wood")) {
            this.getInventory().add(new ItemStack(Items.STONE_AXE, 1));
            this.getInventory().add(new ItemStack(Items.OAK_SAPLING, 16));
        } else if (r.contains("builder") || r.contains("build")) {
            this.getInventory().add(new ItemStack(Items.COBBLESTONE, 64));
            this.getInventory().add(new ItemStack(Items.OAK_PLANKS, 64));
            this.getInventory().add(new ItemStack(Items.TORCH, 16));
        } else if (r.contains("courier") || r.contains("helper") || r.contains("collector")) {
            this.getInventory().add(new ItemStack(Items.CHEST, 2));
            this.getInventory().add(new ItemStack(Items.BREAD, 8));
        }
    }

    public String getCurrentTaskSummary() {
        if (currentAction == null && actionQueue.isEmpty()) {
            return "Idle (waiting for orders)";
        }
        StringBuilder sb = new StringBuilder();
        if (currentAction != null) {
            sb.append(currentAction.toString());
            if (currentAction.count > 1 && ("MINE".equals(currentAction.getNormalizedType()) || "ATTACK".equals(currentAction.getNormalizedType()))) {
                sb.append(" [").append(actionProgressCount).append("/").append(currentAction.count).append("]");
            }
        }
        if (!actionQueue.isEmpty()) {
            sb.append(" (+" + actionQueue.size() + " queued)");
        }
        return sb.toString();
    }

    public List<BotAction> getRemainingActionsSnapshot() {
        List<BotAction> list = new ArrayList<>();
        if (this.currentAction != null) {
            list.add(this.currentAction);
        }
        list.addAll(this.actionQueue);
        return list;
    }

    public void setActionPlan(List<BotAction> actions) {
        this.clearActions();
        if (actions != null) {
            for (BotAction a : actions) {
                if (a != null && !a.getNormalizedType().isEmpty()) {
                    if ("STOP".equals(a.getNormalizedType())) {
                        this.clearActions();
                        return;
                    }
                    this.actionQueue.addLast(a);
                }
            }
        }
        advanceNextAction();
    }

    public void clearActions() {
        if (this.targetBlockPos != null) {
            this.level().destroyBlockProgress(this.getId(), this.targetBlockPos, -1);
            this.targetBlockPos = null;
        }
        this.actionQueue.clear();
        this.currentAction = null;
        this.actionProgressCount = 0;
        this.actionTicks = 0;
        this.miningTicks = 0;
        this.stopMovement();
    }

    private void advanceNextAction() {
        if (this.targetBlockPos != null) {
            this.level().destroyBlockProgress(this.getId(), this.targetBlockPos, -1);
            this.targetBlockPos = null;
        }
        this.stopMovement();
        this.currentAction = this.actionQueue.pollFirst();
        this.actionProgressCount = 0;
        this.actionTicks = 0;
        this.miningTicks = 0;
        this.stuckTicks = 0;
        this.bestGoalDistSq = Double.MAX_VALUE;
        this.noProgressTicks = 0;
    }

    public void sayInChat(String message) {
        if (message == null || message.isBlank()) return;
        Component formatted = Component.literal("[" + this.botName + "] ")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                .append(Component.literal(message).withStyle(ChatFormatting.WHITE));
        this.level().getServer().getPlayerList().broadcastSystemMessage(formatted, false);
        GeminiService.recordMemory(this.botName, this.botName + ": " + message);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isRemoved()) return;

        // Run LivingEntity / Player physics, gravity, item pickup, and food tick
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.doTick();
        this.level().getChunkSource().move(this);

        if (!this.isAlive()) {
            return;
        }

        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }

        // Periodic survival maintenance every 20 ticks
        if (this.tickCount % 20 == 0) {
            if (BotConfig.AUTO_EAT.getAsBoolean()) {
                tryAutoEat();
            }
            autoEquipArmor();
            vacuumNearbyDrops();
        }

        // Auto-defend if attacked by a monster
        if (BotConfig.AUTO_DEFEND.getAsBoolean() && this.getLastHurtByMob() != null && this.getLastHurtByMob().isAlive()) {
            LivingEntity attacker = this.getLastHurtByMob();
            if (!(attacker instanceof Player) && this.distanceToSqr(attacker) < 64.0) {
                engageTarget(attacker);
                return;
            }
        }

        // Execute current action
        if (this.currentAction == null && !this.actionQueue.isEmpty()) {
            advanceNextAction();
        }

        if (this.currentAction != null) {
            this.actionTicks++;
            // Miner role passive: auto-place torch in dark caves while working
            if (this.tickCount % 30 == 0 && this.botRole.toLowerCase().contains("miner")) {
                if (this.level().getMaxLocalRawBrightness(this.blockPosition()) < 7) {
                    placeSpecificBlockFromInventory("torch", this.blockPosition());
                }
            }
            boolean finished = tickCurrentAction(this.currentAction);
            if (finished) {
                GeminiService.recordMemory(this.botName, "[Completed Action: " + this.currentAction + "]");
                advanceNextAction();
            }
        } else {
            tickPassiveRoleBehavior();
        }
    }

    private void tickPassiveRoleBehavior() {
        String r = this.botRole.toLowerCase();

        // 1. Guard / Bodyguard: Automatically patrol around the nearest human player and intercept monsters within 24m
        if (r.contains("guard") || r.contains("bodyguard") || r.contains("warrior")) {
            Player nearestHuman = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), 48.0, p -> p != this && !(p instanceof AiBotPlayer));
            BlockPos guardCenter = nearestHuman != null ? nearestHuman.blockPosition() : this.blockPosition();
            Monster threat = findNearestMonsterNear(guardCenter, 24.0);
            if (threat != null) {
                engageTarget(threat);
                return;
            }
            if (nearestHuman != null) {
                double distSq = this.distanceToSqr(nearestHuman);
                if (distSq > 16.0) {
                    moveTowards(nearestHuman.position(), distSq > 36.0);
                } else {
                    stopMovement();
                    if (this.tickCount % 10 == 0) lookAtPos(nearestHuman.getEyePosition());
                }
                return;
            }
        }

        // 2. Courier / Helper: Follow nearest human player and vacuum dropped items within 12m
        if (r.contains("courier") || r.contains("helper") || r.contains("collector")) {
            ItemEntity drop = findNearestDrop(12.0);
            if (drop != null) {
                moveTowards(drop.position(), true);
                return;
            }
            Player nearestHuman = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), 32.0, p -> p != this && !(p instanceof AiBotPlayer));
            if (nearestHuman != null && this.distanceToSqr(nearestHuman) > 16.0) {
                moveTowards(nearestHuman.position(), false);
                return;
            }
        }

        // 3. Miner: Auto-place torch if standing in darkness
        if (r.contains("miner") && this.tickCount % 40 == 0) {
            if (this.level().getMaxLocalRawBrightness(this.blockPosition()) < 7) {
                placeSpecificBlockFromInventory("torch", this.blockPosition());
            }
        }

        this.stopMovement();
        if (this.tickCount % 10 == 0) {
            Player nearest = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), 8.0, p -> p != this);
            if (nearest != null) {
                lookAtPos(nearest.getEyePosition());
            }
        }
    }

    private boolean tickCurrentAction(BotAction action) {
        String type = action.getNormalizedType();
        return switch (type) {
            case "STOP" -> {
                clearActions();
                yield true;
            }
            case "FOLLOW" -> tickFollowAction(action);
            case "GOTO" -> tickGotoAction(action);
            case "GUARD" -> tickGuardAction(action);
            case "MINE" -> tickMineAction(action);
            case "CRAFT" -> executeCraftAction(action);
            case "GIVE" -> tickGiveAction(action);
            case "ATTACK" -> tickAttackAction(action);
            case "EQUIP" -> {
                equipBestGear(action.getPrimaryTarget());
                yield true;
            }
            case "PLACE", "BUILD" -> executePlaceAction(action);
            case "CHEST_STORE" -> tickChestAction(action, true);
            case "CHEST_TAKE" -> tickChestAction(action, false);
            case "EAT" -> {
                forceEatFromInventory();
                yield true;
            }
            default -> true;
        };
    }

    private boolean tickFollowAction(BotAction action) {
        ServerPlayer target = findPlayer(action.getPrimaryTarget());
        if (target == null) {
            sayInChat("I can't find " + action.getPrimaryTarget() + " to follow.");
            return true;
        }

        // Defend guarded player from nearby monsters
        if (BotConfig.AUTO_DEFEND.getAsBoolean()) {
            Monster threat = findNearestMonsterNear(target.blockPosition(), 10.0);
            if (threat != null) {
                engageTarget(threat);
                return false;
            }
        }

        double desiredDist = Math.max(2.0, action.distance > 0 ? action.distance : 3.0);
        double distSq = this.distanceToSqr(target);
        if (distSq > 3600.0 && target.level() == this.level()) {
            // Teleport if >60 blocks behind while following
            this.teleportTo(target.getX() + 1.5, target.getY(), target.getZ() + 1.5);
            return false;
        }

        if (distSq > desiredDist * desiredDist) {
            moveTowards(target.position(), distSq > 25.0);
        } else {
            stopMovement();
            lookAtPos(target.getEyePosition());
        }
        return false; // Continuous until told to stop or given a new task
    }

    private boolean tickGotoAction(BotAction action) {
        Vec3 targetVec;
        if (action.x != null && action.y != null && action.z != null) {
            targetVec = new Vec3(action.x + 0.5, action.y, action.z + 0.5);
        } else {
            BotPersistence.SavedLocation savedLoc = BotPersistence.getLocation(action.getPrimaryTarget());
            if (savedLoc != null) {
                targetVec = new Vec3(savedLoc.x + 0.5, savedLoc.y, savedLoc.z + 0.5);
            } else {
                ServerPlayer targetPlayer = findPlayer(action.getPrimaryTarget());
                if (targetPlayer == null) {
                    sayInChat("Couldn't find player or saved location '" + action.getPrimaryTarget() + "'.");
                    return true;
                }
                targetVec = targetPlayer.position();
            }
        }

        double distSq = this.position().distanceToSqr(targetVec);
        if (distSq <= 4.5) {
            stopMovement();
            sayInChat("Arrived at destination!");
            return true;
        }

        // Progress-based check: never time out on long journeys as long as the bot is making forward progress!
        if (distSq < this.bestGoalDistSq - 1.0) {
            this.bestGoalDistSq = distSq;
            this.noProgressTicks = 0;
        } else {
            this.noProgressTicks++;
        }

        if (this.noProgressTicks > 600) {
            sayInChat("I'm completely blocked from making progress toward my destination.");
            stopMovement();
            return true;
        }
        moveTowards(targetVec, distSq > 16.0);
        return false;
    }

    private boolean tickGuardAction(BotAction action) {
        ServerPlayer guarded = findPlayer(action.getPrimaryTarget());
        BlockPos guardCenter = guarded != null ? guarded.blockPosition() : this.blockPosition();

        Monster threat = findNearestMonsterNear(guardCenter, 16.0);
        if (threat != null) {
            engageTarget(threat);
            return false;
        }

        if (guarded != null) {
            double distSq = this.distanceToSqr(guarded);
            if (distSq > 16.0) {
                moveTowards(guarded.position(), distSq > 36.0);
            } else {
                stopMovement();
                lookAtPos(guarded.getEyePosition());
            }
        } else {
            stopMovement();
        }
        return false;
    }

    private boolean tickMineAction(BotAction action) {
        if (this.actionProgressCount >= action.count) {
            stopMovement();
            sayInChat("Finished mining " + this.actionProgressCount + "x " + action.getPrimaryTarget() + "!");
            return true;
        }

        // First pick up any nearby dropped items
        ItemEntity drop = findNearestDrop(5.0);
        if (drop != null && this.miningTicks == 0) {
            moveTowards(drop.position(), false);
            if (this.distanceToSqr(drop) < 2.5) {
                drop.playerTouch(this);
            }
        }

        // Validate current targetBlockPos
        if (this.targetBlockPos != null) {
            BlockState state = this.level().getBlockState(this.targetBlockPos);
            if (state.isAir() || !matchesBlockQuery(state, action.getPrimaryTarget())) {
                this.level().destroyBlockProgress(this.getId(), this.targetBlockPos, -1);
                this.targetBlockPos = null;
                this.miningTicks = 0;
            }
        }

        // Find next matching block if needed
        if (this.targetBlockPos == null) {
            this.targetBlockPos = findNearestMatchingBlock(action.getPrimaryTarget(), BotConfig.SCAN_RADIUS.getAsInt());
            if (this.targetBlockPos == null) {
                stopMovement();
                if (this.actionProgressCount > 0) {
                    sayInChat("Mined " + this.actionProgressCount + "x " + action.getPrimaryTarget() + ", no more nearby.");
                } else {
                    sayInChat("I couldn't find any " + action.getPrimaryTarget() + " within " + BotConfig.SCAN_RADIUS.getAsInt() + " blocks.");
                }
                return true;
            }
        }

        Vec3 blockCenter = Vec3.atCenterOf(this.targetBlockPos);
        double distSq = this.getEyePosition().distanceToSqr(blockCenter);

        if (distSq > 20.0) {
            // Walk closer to the block
            moveTowards(blockCenter, distSq > 36.0);
            if (this.actionTicks > 500) {
                this.targetBlockPos = null;
                this.actionTicks = 0;
            }
            return false;
        }

        // Within mining reach!
        stopMovement();
        lookAtPos(blockCenter);
        BlockState state = this.level().getBlockState(this.targetBlockPos);
        equipBestToolFor(state);

        this.miningTicks++;
        if (this.miningTicks % 4 == 0) {
            broadcastSwing();
        }

        // Calculate realistic break time (clamped between 6 and 45 ticks for smooth gameplay)
        float destroySpeed = Math.max(1.0f, this.getMainHandItem().getDestroySpeed(state));
        float hardness = Math.max(0.5f, state.getDestroySpeed(this.level(), this.targetBlockPos));
        int ticksRequired = Mth.clamp((int) ((hardness * 20.0f) / destroySpeed), 6, 45);

        int stage = Mth.clamp((this.miningTicks * 10) / ticksRequired, 0, 9);
        this.level().destroyBlockProgress(this.getId(), this.targetBlockPos, stage);

        if (this.miningTicks >= ticksRequired) {
            this.level().destroyBlockProgress(this.getId(), this.targetBlockPos, -1);
            BlockPos brokenPos = this.targetBlockPos;
            boolean wasLog = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().contains("log");
            this.level().destroyBlock(brokenPos, true, this);
            // Lumberjack passive: auto-replant sapling if bottom of tree on dirt/grass
            if (wasLog && this.botRole.toLowerCase().contains("lumber")) {
                BlockState below = this.level().getBlockState(brokenPos.below());
                if (below.is(Blocks.DIRT) || below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.PODZOL)) {
                    placeSpecificBlockFromInventory("sapling", brokenPos);
                }
            }
            this.targetBlockPos = null;
            this.miningTicks = 0;
            this.actionTicks = 0;
            this.actionProgressCount++;

            // Immediately collect drops spawned at brokenPos
            AABB dropBox = new AABB(brokenPos).inflate(3.5);
            for (ItemEntity itemEntity : this.level().getEntitiesOfClass(ItemEntity.class, dropBox)) {
                if (itemEntity.isAlive()) {
                    ItemStack stack = itemEntity.getItem();
                    if (this.getInventory().add(stack)) {
                        itemEntity.discard();
                    }
                }
            }
        }
        return false;
    }

    private boolean executeCraftAction(BotAction action) {
        String query = action.getPrimaryTarget().toLowerCase().replace("minecraft:", "").trim();
        int desiredCount = Math.max(1, action.count);
        int craftedTotal = 0;

        for (int i = 0; i < desiredCount; i++) {
            if (craftSingleItemSmart(query)) {
                craftedTotal++;
            } else {
                break;
            }
        }

        if (craftedTotal > 0) {
            sayInChat("Crafted " + craftedTotal + "x " + query + "!");
        } else {
            sayInChat("I don't have enough materials in my inventory to craft " + query + ".");
        }
        return true;
    }

    private boolean craftSingleItemSmart(String query) {
        if (query.contains("plank")) {
            if (consumeMatching(s -> isLog(s), 1)) {
                giveToSelf(new ItemStack(Items.OAK_PLANKS, 4));
                return true;
            }
            return false;
        }
        if (query.contains("stick")) {
            ensurePlanks(2);
            if (consumeMatching(s -> isPlank(s), 2)) {
                giveToSelf(new ItemStack(Items.STICK, 4));
                return true;
            }
            return false;
        }
        if (query.contains("crafting_table") || query.equals("table")) {
            ensurePlanks(4);
            if (consumeMatching(s -> isPlank(s), 4)) {
                giveToSelf(new ItemStack(Items.CRAFTING_TABLE, 1));
                return true;
            }
            return false;
        }
        if (query.contains("torch")) {
            ensureSticks(1);
            if (countMatching(s -> s.is(Items.COAL) || s.is(Items.CHARCOAL)) >= 1 && countMatching(s -> s.is(Items.STICK)) >= 1) {
                consumeMatching(s -> s.is(Items.COAL) || s.is(Items.CHARCOAL), 1);
                consumeMatching(s -> s.is(Items.STICK), 1);
                giveToSelf(new ItemStack(Items.TORCH, 4));
                return true;
            }
            return false;
        }
        if (query.contains("chest")) {
            ensurePlanks(8);
            if (consumeMatching(s -> isPlank(s), 8)) {
                giveToSelf(new ItemStack(Items.CHEST, 1));
                return true;
            }
            return false;
        }
        if (query.contains("furnace")) {
            if (consumeMatching(s -> isCobble(s), 8)) {
                giveToSelf(new ItemStack(Items.FURNACE, 1));
                return true;
            }
            return false;
        }
        if (query.contains("bread")) {
            if (consumeMatching(s -> s.is(Items.WHEAT), 3)) {
                giveToSelf(new ItemStack(Items.BREAD, 1));
                return true;
            }
            return false;
        }
        if (query.contains("shield")) {
            ensurePlanks(6);
            ensureIronIngots(1);
            if (countMatching(this::isPlank) >= 6 && countMatching(s -> s.is(Items.IRON_INGOT)) >= 1) {
                consumeMatching(this::isPlank, 6);
                consumeMatching(s -> s.is(Items.IRON_INGOT), 1);
                giveToSelf(new ItemStack(Items.SHIELD, 1));
                return true;
            }
            return false;
        }

        // Tools & Weapons: wooden, stone, iron, diamond
        if (query.contains("pickaxe") || query.contains("axe") || query.contains("sword") || query.contains("shovel") || query.contains("hoe")) {
            int sticksNeeded = query.contains("sword") ? 1 : 2;
            int matNeeded = query.contains("pickaxe") || query.contains("axe") ? 3
                    : (query.contains("sword") || query.contains("hoe") ? 2 : 1);

            ensureSticks(sticksNeeded);
            if (countMatching(s -> s.is(Items.STICK)) < sticksNeeded) {
                return false;
            }

            if (query.startsWith("wood")) {
                ensurePlanks(matNeeded);
                if (countMatching(this::isPlank) >= matNeeded) {
                    consumeMatching(this::isPlank, matNeeded);
                    consumeMatching(s -> s.is(Items.STICK), sticksNeeded);
                    Item out = query.contains("pickaxe") ? Items.WOODEN_PICKAXE
                            : query.contains("axe") ? Items.WOODEN_AXE
                            : query.contains("sword") ? Items.WOODEN_SWORD
                            : query.contains("shovel") ? Items.WOODEN_SHOVEL : Items.WOODEN_HOE;
                    giveToSelf(new ItemStack(out, 1));
                    return true;
                }
            } else if (query.startsWith("stone")) {
                if (countMatching(this::isCobble) >= matNeeded) {
                    consumeMatching(this::isCobble, matNeeded);
                    consumeMatching(s -> s.is(Items.STICK), sticksNeeded);
                    Item out = query.contains("pickaxe") ? Items.STONE_PICKAXE
                            : query.contains("axe") ? Items.STONE_AXE
                            : query.contains("sword") ? Items.STONE_SWORD
                            : query.contains("shovel") ? Items.STONE_SHOVEL : Items.STONE_HOE;
                    giveToSelf(new ItemStack(out, 1));
                    return true;
                }
            } else if (query.startsWith("iron")) {
                ensureIronIngots(matNeeded);
                if (countMatching(s -> s.is(Items.IRON_INGOT)) >= matNeeded) {
                    consumeMatching(s -> s.is(Items.IRON_INGOT), matNeeded);
                    consumeMatching(s -> s.is(Items.STICK), sticksNeeded);
                    Item out = query.contains("pickaxe") ? Items.IRON_PICKAXE
                            : query.contains("axe") ? Items.IRON_AXE
                            : query.contains("sword") ? Items.IRON_SWORD
                            : query.contains("shovel") ? Items.IRON_SHOVEL : Items.IRON_HOE;
                    giveToSelf(new ItemStack(out, 1));
                    return true;
                }
            } else if (query.startsWith("diamond")) {
                if (countMatching(s -> s.is(Items.DIAMOND)) >= matNeeded) {
                    consumeMatching(s -> s.is(Items.DIAMOND), matNeeded);
                    consumeMatching(s -> s.is(Items.STICK), sticksNeeded);
                    Item out = query.contains("pickaxe") ? Items.DIAMOND_PICKAXE
                            : query.contains("axe") ? Items.DIAMOND_AXE
                            : query.contains("sword") ? Items.DIAMOND_SWORD
                            : query.contains("shovel") ? Items.DIAMOND_SHOVEL : Items.DIAMOND_HOE;
                    giveToSelf(new ItemStack(out, 1));
                    return true;
                }
            }
        }

        // Armor crafting (iron / diamond)
        if (query.contains("helmet") || query.contains("chestplate") || query.contains("leggings") || query.contains("boots")) {
            int needed = query.contains("helmet") ? 5 : query.contains("chestplate") ? 8 : query.contains("leggings") ? 7 : 4;
            if (query.startsWith("iron")) {
                ensureIronIngots(needed);
                if (countMatching(s -> s.is(Items.IRON_INGOT)) >= needed) {
                    consumeMatching(s -> s.is(Items.IRON_INGOT), needed);
                    Item out = query.contains("helmet") ? Items.IRON_HELMET
                            : query.contains("chestplate") ? Items.IRON_CHESTPLATE
                            : query.contains("leggings") ? Items.IRON_LEGGINGS : Items.IRON_BOOTS;
                    giveToSelf(new ItemStack(out, 1));
                    autoEquipArmor();
                    return true;
                }
            } else if (query.startsWith("diamond")) {
                if (countMatching(s -> s.is(Items.DIAMOND)) >= needed) {
                    consumeMatching(s -> s.is(Items.DIAMOND), needed);
                    Item out = query.contains("helmet") ? Items.DIAMOND_HELMET
                            : query.contains("chestplate") ? Items.DIAMOND_CHESTPLATE
                            : query.contains("leggings") ? Items.DIAMOND_LEGGINGS : Items.DIAMOND_BOOTS;
                    giveToSelf(new ItemStack(out, 1));
                    autoEquipArmor();
                    return true;
                }
            }
        }

        return false;
    }

    private void ensurePlanks(int needed) {
        while (countMatching(this::isPlank) < needed && countMatching(this::isLog) > 0) {
            if (consumeMatching(this::isLog, 1)) {
                giveToSelf(new ItemStack(Items.OAK_PLANKS, 4));
            } else {
                break;
            }
        }
    }

    private void ensureSticks(int needed) {
        while (countMatching(s -> s.is(Items.STICK)) < needed) {
            ensurePlanks(2);
            if (consumeMatching(this::isPlank, 2)) {
                giveToSelf(new ItemStack(Items.STICK, 4));
            } else {
                break;
            }
        }
    }

    private void ensureIronIngots(int needed) {
        while (countMatching(s -> s.is(Items.IRON_INGOT)) < needed
                && countMatching(s -> s.is(Items.RAW_IRON) || s.is(Items.IRON_ORE) || s.is(Items.DEEPSLATE_IRON_ORE)) > 0) {
            if (consumeMatching(s -> s.is(Items.RAW_IRON) || s.is(Items.IRON_ORE) || s.is(Items.DEEPSLATE_IRON_ORE), 1)) {
                giveToSelf(new ItemStack(Items.IRON_INGOT, 1));
            } else {
                break;
            }
        }
    }

    private boolean tickGiveAction(BotAction action) {
        ServerPlayer recipient = findPlayer(action.target);
        if (recipient == null) {
            // Fallback to nearest human player
            Player nearest = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), 32.0, p -> p != this && !(p instanceof AiBotPlayer));
            if (nearest instanceof ServerPlayer sp) {
                recipient = sp;
            }
        }
        if (recipient == null) {
            sayInChat("I can't find anyone nearby to give items to.");
            return true;
        }

        double distSq = this.distanceToSqr(recipient);
        if (distSq > 9.0) {
            moveTowards(recipient.position(), distSq > 25.0);
            if (this.actionTicks > 400) {
                sayInChat("Couldn't reach " + recipient.getPlainTextName() + " to give items.");
                stopMovement();
                return true;
            }
            return false;
        }

        stopMovement();
        lookAtPos(recipient.getEyePosition());

        String itemQuery = (action.item != null && !action.item.isBlank()) ? action.item : action.target;
        String cleanQuery = itemQuery.toLowerCase().replace("minecraft:", "").trim();
        int remaining = Math.max(1, action.count);
        int droppedTotal = 0;

        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack stack = this.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase();
            if ("all".equals(cleanQuery) || id.contains(cleanQuery)) {
                int toTake = "all".equals(cleanQuery) ? stack.getCount() : Math.min(remaining, stack.getCount());
                ItemStack split = this.getInventory().removeItem(i, toTake);
                if (!split.isEmpty()) {
                    tossStackTowards(split, recipient.position());
                    droppedTotal += split.getCount();
                    remaining -= split.getCount();
                    if (!"all".equals(cleanQuery) && remaining <= 0) {
                        break;
                    }
                }
            }
        }

        if (droppedTotal > 0) {
            broadcastSwing();
            sayInChat("Here you go, " + recipient.getPlainTextName() + "! Dropped " + droppedTotal + "x " + cleanQuery + ".");
        } else {
            sayInChat("I don't have any " + cleanQuery + " in my inventory.");
        }
        return true;
    }

    private void tossStackTowards(ItemStack stack, Vec3 targetPos) {
        ItemEntity itemEntity = this.createItemStackToDrop(stack, false, true);
        if (itemEntity != null) {
            Vec3 dir = targetPos.add(0, 0.5, 0).subtract(this.getEyePosition()).normalize().scale(0.32);
            itemEntity.setDeltaMovement(dir);
            itemEntity.setDefaultPickUpDelay();
            this.level().addFreshEntity(itemEntity);
        }
    }

    private boolean tickAttackAction(BotAction action) {
        if (this.actionProgressCount >= action.count) {
            stopMovement();
            sayInChat("Finished eliminating " + this.actionProgressCount + "x " + action.getPrimaryTarget() + "!");
            return true;
        }

        String query = action.getPrimaryTarget().toLowerCase().replace("minecraft:", "").trim();
        LivingEntity targetMob = findNearestTargetMob(query, 24.0);
        if (targetMob == null) {
            stopMovement();
            if (this.actionProgressCount > 0) {
                sayInChat("Cleared " + this.actionProgressCount + "x " + query + ", area is clear!");
            } else {
                sayInChat("No " + (query.isEmpty() ? "hostile mobs" : query) + " found nearby.");
            }
            return true;
        }

        boolean wasAlive = targetMob.isAlive();
        engageTarget(targetMob);
        if (wasAlive && !targetMob.isAlive()) {
            this.actionProgressCount++;
        }
        return false;
    }

    private void engageTarget(LivingEntity target) {
        equipBestWeapon();
        lookAtPos(target.getEyePosition());
        double distSq = this.distanceToSqr(target);
        if (distSq > 9.5) {
            moveTowards(target.position(), true);
        } else {
            stopMovement();
            if (this.attackCooldown <= 0) {
                broadcastSwing();
                this.attack(target);
                this.resetAttackStrengthTicker();
                this.attackCooldown = 11;
            }
        }
    }

    private boolean executePlaceAction(BotAction action) {
        String target = action.getPrimaryTarget().toLowerCase().replace("minecraft:", "").trim();
        BlockPos origin = this.blockPosition();

        if (target.contains("shelter") || target.contains("house") || target.contains("hut")) {
            int placed = buildQuickShelter(origin);
            if (placed > 0) {
                sayInChat("Built a protective shelter (" + placed + " blocks placed)!");
            } else {
                sayInChat("I need building blocks (cobblestone, planks, or dirt) in my inventory to build a shelter.");
            }
            return true;
        }

        if (target.contains("wall")) {
            int placed = 0;
            BlockPos front = origin.relative(this.getDirection(), 2);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = 0; dy <= 2; dy++) {
                    BlockPos p = this.getDirection().getAxis() == net.minecraft.core.Direction.Axis.Z
                            ? front.offset(dx, dy, 0)
                            : front.offset(0, dy, dx);
                    if (placeBuildingBlockAt(p)) placed++;
                }
            }
            sayInChat(placed > 0 ? "Built a defensive wall!" : "Out of building blocks.");
            return true;
        }

        if (target.contains("pillar")) {
            BlockPos below = origin;
            this.setPos(this.getX(), this.getY() + 1.1, this.getZ());
            if (placeBuildingBlockAt(below)) {
                sayInChat("Pillared up!");
            }
            return true;
        }

        // Single block placement (torch, crafting_table, chest, furnace, etc.)
        BlockPos placePos = origin.relative(this.getDirection(), 1);
        if (!this.level().getBlockState(placePos).isAir()) {
            placePos = origin.above();
        }
        if (placeSpecificBlockFromInventory(target, placePos)) {
            broadcastSwing();
            sayInChat("Placed " + target + " at (" + placePos.getX() + ", " + placePos.getY() + ", " + placePos.getZ() + ").");
        } else {
            sayInChat("I couldn't place " + target + " (check my inventory or space around me).");
        }
        return true;
    }

    private int buildQuickShelter(BlockPos center) {
        int placed = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 3; dy++) {
                    boolean isWall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                    boolean isRoof = dy == 3;
                    boolean isDoorway = (dx == 0 && dz == 2 && (dy == 0 || dy == 1));
                    if ((isWall || isRoof) && !isDoorway) {
                        if (placeBuildingBlockAt(center.offset(dx, dy, dz))) {
                            placed++;
                        }
                    }
                }
            }
        }
        placeSpecificBlockFromInventory("torch", center.offset(1, 0, 1));
        return placed;
    }

    private boolean placeBuildingBlockAt(BlockPos pos) {
        if (!this.level().getBlockState(pos).isAir() && !this.level().getBlockState(pos).canBeReplaced()) {
            return false;
        }
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi) {
                Block b = bi.getBlock();
                String id = BuiltInRegistries.BLOCK.getKey(b).toString();
                if (id.contains("planks") || id.contains("cobblestone") || id.contains("stone") || id.contains("dirt") || id.contains("log") || id.contains("bricks")) {
                    this.level().setBlockAndUpdate(pos, b.defaultBlockState());
                    s.shrink(1);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean placeSpecificBlockFromInventory(String query, BlockPos pos) {
        if (!this.level().getBlockState(pos).isAir() && !this.level().getBlockState(pos).canBeReplaced()) {
            return false;
        }
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi) {
                String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString().toLowerCase();
                if (id.contains(query)) {
                    this.level().setBlockAndUpdate(pos, bi.getBlock().defaultBlockState());
                    s.shrink(1);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean tickChestAction(BotAction action, boolean store) {
        BlockPos chestPos = findNearestContainerBlock(16);
        if (chestPos == null) {
            sayInChat("I couldn't find a chest or barrel nearby.");
            return true;
        }

        double distSq = this.position().distanceToSqr(Vec3.atCenterOf(chestPos));
        if (distSq > 16.0) {
            moveTowards(Vec3.atCenterOf(chestPos), false);
            if (this.actionTicks > 300) {
                stopMovement();
                sayInChat("Couldn't reach the chest.");
                return true;
            }
            return false;
        }

        stopMovement();
        lookAtPos(Vec3.atCenterOf(chestPos));
        BlockEntity be = this.level().getBlockEntity(chestPos);
        if (!(be instanceof Container container)) {
            sayInChat("That block isn't a valid container.");
            return true;
        }

        String query = action.getPrimaryTarget().toLowerCase().replace("minecraft:", "").trim();
        int moved = 0;

        if (store) {
            for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
                ItemStack s = this.getInventory().getItem(i);
                if (s.isEmpty()) continue;
                String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString().toLowerCase();
                // Keep main tools/weapons unless storing a specific item
                if (("all".equals(query) || query.isEmpty()) && (id.contains("pickaxe") || id.contains("sword") || id.contains("axe"))) {
                    continue;
                }
                if ("all".equals(query) || query.isEmpty() || id.contains(query)) {
                    int countBefore = s.getCount();
                    ItemStack remainder = insertIntoContainer(container, s.copy());
                    int inserted = countBefore - remainder.getCount();
                    if (inserted > 0) {
                        s.shrink(inserted);
                        moved += inserted;
                    }
                }
            }
            sayInChat("Stored " + moved + " items into the chest.");
        } else {
            int remaining = Math.max(1, action.count);
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack s = container.getItem(i);
                if (s.isEmpty()) continue;
                String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString().toLowerCase();
                if ("all".equals(query) || id.contains(query)) {
                    int take = Math.min(remaining, s.getCount());
                    ItemStack taken = container.removeItem(i, take);
                    if (this.getInventory().add(taken)) {
                        moved += take;
                        remaining -= take;
                        if (remaining <= 0) break;
                    }
                }
            }
            container.setChanged();
            sayInChat("Took " + moved + "x " + query + " from the chest.");
        }
        return true;
    }

    private ItemStack insertIntoContainer(Container container, ItemStack stack) {
        for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack slot = container.getItem(i);
            if (slot.isEmpty()) {
                container.setItem(i, stack.copy());
                stack.setCount(0);
                break;
            } else if (ItemStack.isSameItemSameComponents(slot, stack) && slot.getCount() < slot.getMaxStackSize()) {
                int space = slot.getMaxStackSize() - slot.getCount();
                int transfer = Math.min(space, stack.getCount());
                slot.grow(transfer);
                stack.shrink(transfer);
            }
        }
        container.setChanged();
        return stack;
    }

    // === MOVEMENT, NAVIGATION & UNSTUCK ENGINE ===

    private void moveTowards(Vec3 target, boolean sprint) {
        lookAtPos(new Vec3(target.x, target.y + this.getEyeHeight(), target.z));
        this.setSprinting(sprint);
        this.zza = sprint ? 1.0f : 0.8f;
        this.xxa = 0.0f;

        // Open wooden doors in front of bot
        BlockPos frontPos = this.blockPosition().relative(this.getDirection());
        BlockState frontState = this.level().getBlockState(frontPos);
        if (frontState.getBlock() instanceof DoorBlock door && !frontState.getValue(DoorBlock.OPEN)) {
            door.setOpen(this, this.level(), frontState, frontPos, true);
        }

        // Jump over ledges or swim in water/lava
        boolean shouldJump = this.horizontalCollision || this.isInWater() || this.isInLava();
        if (!shouldJump && target.y > this.getY() + 0.5 && !frontState.isAir()) {
            shouldJump = true;
        }
        this.setJumping(shouldJump);

        // Unstuck detection
        if (this.position().distanceToSqr(this.lastCheckedPos) < 0.02) {
            this.stuckTicks++;
            if (this.stuckTicks > 35) {
                // Break obstructing head block if stuck
                BlockPos headFront = frontPos.above();
                BlockState headState = this.level().getBlockState(headFront);
                if (!headState.isAir() && headState.getDestroySpeed(this.level(), headFront) >= 0 && headState.getDestroySpeed(this.level(), headFront) < 10.0f) {
                    this.level().destroyBlock(headFront, true, this);
                } else if (!frontState.isAir() && frontState.getDestroySpeed(this.level(), frontPos) >= 0 && frontState.getDestroySpeed(this.level(), frontPos) < 10.0f) {
                    this.level().destroyBlock(frontPos, true, this);
                }
                this.stuckTicks = 0;
            }
        } else {
            this.lastCheckedPos = this.position();
            this.stuckTicks = 0;
        }
    }

    private void stopMovement() {
        this.zza = 0.0f;
        this.xxa = 0.0f;
        this.setJumping(false);
        this.setSprinting(false);
    }

    private void lookAtPos(Vec3 target) {
        Vec3 eye = this.getEyePosition();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        float pitch = (float) (-(Mth.atan2(dy, horiz) * (180.0 / Math.PI)));
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setXRot(pitch);
    }

    private void broadcastSwing() {
        this.level().getChunkSource().sendToTrackingPlayers(this, new ClientboundAnimatePacket(this, 0));
    }

    // === INVENTORY & EQUIPMENT HELPERS ===

    private void equipBestToolFor(BlockState state) {
        int bestSlot = -1;
        float bestSpeed = this.getMainHandItem().getDestroySpeed(state);

        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (!s.isEmpty()) {
                float speed = s.getDestroySpeed(state);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = i;
                }
            }
        }

        if (bestSlot != -1) {
            swapToMainHand(bestSlot);
        }
    }

    private void equipBestWeapon() {
        int bestSlot = -1;
        int bestScore = getWeaponScore(this.getMainHandItem());

        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            int score = getWeaponScore(s);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }

        if (bestSlot != -1) {
            swapToMainHand(bestSlot);
        }
    }

    private int getWeaponScore(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase();
        int tier = id.contains("netherite") ? 50 : id.contains("diamond") ? 40 : id.contains("iron") ? 30 : id.contains("stone") ? 20 : id.contains("wooden") ? 10 : 5;
        if (id.contains("sword")) return tier + 8;
        if (id.contains("axe") && !id.contains("pickaxe")) return tier + 6;
        if (id.contains("trident") || id.contains("mace")) return 45;
        return 1;
    }

    private void equipBestGear(String targetQuery) {
        autoEquipArmor();
        if (targetQuery == null || targetQuery.isBlank() || "best".equalsIgnoreCase(targetQuery)) {
            equipBestWeapon();
            return;
        }
        String q = targetQuery.toLowerCase().replace("minecraft:", "").trim();
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (!s.isEmpty() && BuiltInRegistries.ITEM.getKey(s.getItem()).toString().toLowerCase().contains(q)) {
                swapToMainHand(i);
                return;
            }
        }
    }

    private void autoEquipArmor() {
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (s.isEmpty()) continue;
            Equippable eq = s.get(DataComponents.EQUIPPABLE);
            if (eq != null) {
                EquipmentSlot slot = eq.slot();
                if (slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) {
                    ItemStack current = this.getItemBySlot(slot);
                    if (current.isEmpty() || getArmorTierScore(s) > getArmorTierScore(current)) {
                        ItemStack toEquip = s.split(1);
                        this.setItemSlot(slot, toEquip);
                        if (!current.isEmpty()) {
                            this.getInventory().add(current);
                        }
                    }
                }
            } else if (s.is(Items.SHIELD) && this.getOffhandItem().isEmpty()) {
                this.setItemSlot(EquipmentSlot.OFFHAND, s.split(1));
            }
        }
    }

    private int getArmorTierScore(ItemStack s) {
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString().toLowerCase();
        if (id.contains("netherite")) return 6;
        if (id.contains("diamond")) return 5;
        if (id.contains("iron")) return 4;
        if (id.contains("chainmail") || id.contains("gold")) return 3;
        if (id.contains("copper")) return 2;
        return 1;
    }

    private void swapToMainHand(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < 9) {
            this.getInventory().setSelectedSlot(slotIndex);
        } else if (slotIndex >= 9 && slotIndex < this.getInventory().getContainerSize()) {
            int hotbarSlot = this.getInventory().getSelectedSlot();
            ItemStack inHotbar = this.getInventory().getItem(hotbarSlot);
            ItemStack inInv = this.getInventory().getItem(slotIndex);
            this.getInventory().setItem(hotbarSlot, inInv);
            this.getInventory().setItem(slotIndex, inHotbar);
        }
    }

    private void tryAutoEat() {
        if (this.getFoodData().needsFood() || (this.getHealth() < this.getMaxHealth() - 2.0f && this.getFoodData().getFoodLevel() < 20)) {
            forceEatFromInventory();
        }
    }

    private void forceEatFromInventory() {
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (!s.isEmpty()) {
                FoodProperties food = s.get(DataComponents.FOOD);
                if (food != null) {
                    this.getFoodData().eat(food);
                    this.heal(2.0f);
                    s.shrink(1);
                    broadcastSwing();
                    return;
                }
            }
        }
    }

    private void vacuumNearbyDrops() {
        AABB box = this.getBoundingBox().inflate(2.5, 1.5, 2.5);
        for (ItemEntity item : this.level().getEntitiesOfClass(ItemEntity.class, box)) {
            if (item.isAlive() && !item.hasPickUpDelay()) {
                ItemStack stack = item.getItem();
                if (this.getInventory().add(stack)) {
                    item.discard();
                }
            }
        }
    }

    private boolean isLog(ItemStack s) {
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString().toLowerCase();
        return id.contains("_log") || id.contains("_stem") || id.contains("_wood");
    }

    private boolean isPlank(ItemStack s) {
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString().toLowerCase();
        return id.contains("_planks");
    }

    private boolean isCobble(ItemStack s) {
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).toString().toLowerCase();
        return id.contains("cobblestone") || id.contains("cobbled_deepslate") || id.contains("blackstone");
    }

    private int countMatching(Predicate<ItemStack> predicate) {
        int total = 0;
        for (int i = 0; i < this.getInventory().getContainerSize(); i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (!s.isEmpty() && predicate.test(s)) {
                total += s.getCount();
            }
        }
        return total;
    }

    private boolean consumeMatching(Predicate<ItemStack> predicate, int amount) {
        if (countMatching(predicate) < amount) return false;
        int remaining = amount;
        for (int i = 0; i < this.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack s = this.getInventory().getItem(i);
            if (!s.isEmpty() && predicate.test(s)) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
        return true;
    }

    private void giveToSelf(ItemStack stack) {
        if (!this.getInventory().add(stack)) {
            tossStackTowards(stack, this.position());
        }
    }

    // === WORLD QUERIES ===

    private @Nullable ServerPlayer findPlayer(String name) {
        if (name == null || name.isBlank()) return null;
        for (ServerPlayer p : this.level().players()) {
            if (p != this && p.getPlainTextName().equalsIgnoreCase(name.trim())) {
                return p;
            }
        }
        for (ServerPlayer p : this.level().players()) {
            if (p != this && p.getPlainTextName().toLowerCase().contains(name.trim().toLowerCase())) {
                return p;
            }
        }
        return null;
    }

    private @Nullable BlockPos findNearestMatchingBlock(String query, int radius) {
        BlockPos origin = this.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -6; dy <= 10; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    BlockState state = this.level().getBlockState(pos);
                    if (!state.isAir() && matchesBlockQuery(state, query)) {
                        double dist = origin.distSqr(pos);
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = pos.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    private boolean matchesBlockQuery(BlockState state, String query) {
        if (query == null || query.isBlank()) return false;
        String q = query.toLowerCase().replace("minecraft:", "").trim();
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().toLowerCase();
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().toLowerCase();

        if (blockId.equals(q) || path.equals(q) || path.contains(q)) {
            return true;
        }
        if ((q.equals("wood") || q.equals("tree") || q.equals("trees") || q.equals("logs")) && (path.contains("_log") || path.contains("_stem"))) {
            return true;
        }
        if ((q.equals("cobblestone") || q.equals("stone")) && (path.equals("stone") || path.equals("cobblestone") || path.equals("deepslate"))) {
            return true;
        }
        if (q.equals("iron") && path.contains("iron_ore")) return true;
        if (q.equals("coal") && path.contains("coal_ore")) return true;
        if (q.equals("diamond") && path.contains("diamond_ore")) return true;
        if (q.equals("gold") && path.contains("gold_ore")) return true;
        if (q.equals("copper") && path.contains("copper_ore")) return true;
        return false;
    }

    private @Nullable BlockPos findNearestContainerBlock(int radius) {
        BlockPos origin = this.blockPosition();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (this.level().getBlockEntity(pos) instanceof Container) {
                        double d = origin.distSqr(pos);
                        if (d < bestDist) {
                            bestDist = d;
                            best = pos.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    private @Nullable Monster findNearestMonsterNear(BlockPos center, double radius) {
        AABB box = new AABB(center).inflate(radius);
        List<Monster> monsters = this.level().getEntitiesOfClass(Monster.class, box, LivingEntity::isAlive);
        monsters.sort(Comparator.comparingDouble(this::distanceToSqr));
        return monsters.isEmpty() ? null : monsters.get(0);
    }

    private @Nullable LivingEntity findNearestTargetMob(String query, double radius) {
        AABB box = this.getBoundingBox().inflate(radius);
        List<LivingEntity> list = this.level().getEntitiesOfClass(LivingEntity.class, box, e -> {
            if (e == this || e instanceof Player || !e.isAlive()) return false;
            if (query.isEmpty() || "hostile".equals(query) || "monsters".equals(query) || "mobs".equals(query)) {
                return e instanceof Monster;
            }
            String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().toLowerCase();
            return typeId.contains(query);
        });
        list.sort(Comparator.comparingDouble(this::distanceToSqr));
        return list.isEmpty() ? null : list.get(0);
    }

    private @Nullable ItemEntity findNearestDrop(double radius) {
        AABB box = this.getBoundingBox().inflate(radius);
        List<ItemEntity> drops = this.level().getEntitiesOfClass(ItemEntity.class, box, Entity::isAlive);
        drops.sort(Comparator.comparingDouble(this::distanceToSqr));
        return drops.isEmpty() ? null : drops.get(0);
    }

    // === SAFE SERVER PLAYER OVERRIDES ===

    @Override
    public void sendSystemMessage(Component chatComponent, boolean actionBar) {
    }

    @Override
    public void awardStat(Stat<?> stat, int amount) {
    }

    @Override
    public void updateOptions(ClientInformation information) {
    }

    @Override
    public OptionalInt openMenu(@Nullable MenuProvider menuProvider, @Nullable Consumer<RegistryFriendlyByteBuf> extraDataWriter) {
        return OptionalInt.empty();
    }

    @Override
    public void openHorseInventory(AbstractHorse horse, Container container) {
    }

    @Override
    public void die(DamageSource source) {
        this.clearActions();
        this.dropAllDeathLoot(this.level(), source);
        sayInChat("I died! Respawning near players...");
        this.setHealth(this.getMaxHealth());
        this.getFoodData().setFoodLevel(20);
        Player nearest = this.level().getNearestPlayer(this.getX(), this.getY(), this.getZ(), 256.0, p -> p != this && !(p instanceof AiBotPlayer));
        if (nearest != null) {
            this.teleportTo(nearest.getX() + 1.5, nearest.getY(), nearest.getZ() + 1.5);
        }
    }

    private static class BotNetHandler extends ServerGamePacketListenerImpl {
        private static final net.minecraft.network.Connection DUMMY_CONNECTION = new BotDummyConnection();

        public BotNetHandler(MinecraftServer server, ServerPlayer player) {
            super(server, DUMMY_CONNECTION, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
        }

        @Override public void tick() {}
        @Override public void resetPosition() {}
        @Override public void disconnect(Component message) {}
        @Override public void onDisconnect(DisconnectionDetails details) {}
        @Override public void send(Packet<?> packet) {}
        @Override public void send(Packet<?> packet, @Nullable ChannelFutureListener sendListener) {}
        @Override public void teleport(double x, double y, double z, float yaw, float pitch) {}
        @Override public void teleport(PositionMoveRotation posMoveRot, Set<Relative> relatives) {}
        @Override public void ackBlockChangesUpTo(int sequence) {}
        @Override public void sendPlayerChatMessage(PlayerChatMessage message, ChatType.Bound boundChatType) {}
        @Override public void sendDisguisedChatMessage(Component content, ChatType.Bound boundChatType) {}
        @Override public void handleChatSessionUpdate(ServerboundChatSessionUpdatePacket packet) {}
        @Override public boolean hasChannel(Identifier payloadId) { return false; }
        @Override public void handlePlayerInput(ServerboundPlayerInputPacket packet) {}
        @Override public void handleMoveVehicle(ServerboundMoveVehiclePacket packet) {}
        @Override public void handleAcceptTeleportPacket(ServerboundAcceptTeleportationPacket packet) {}
        @Override public void handleRecipeBookSeenRecipePacket(ServerboundRecipeBookSeenRecipePacket packet) {}
        @Override public void handleRecipeBookChangeSettingsPacket(ServerboundRecipeBookChangeSettingsPacket packet) {}
        @Override public void handleSeenAdvancements(ServerboundSeenAdvancementsPacket packet) {}
        @Override public void handleCustomCommandSuggestions(ServerboundCommandSuggestionPacket packet) {}
        @Override public void handleSetCommandBlock(ServerboundSetCommandBlockPacket packet) {}
        @Override public void handleSetCommandMinecart(ServerboundSetCommandMinecartPacket packet) {}
        @Override public void handleRenameItem(ServerboundRenameItemPacket packet) {}
        @Override public void handleSetBeaconPacket(ServerboundSetBeaconPacket packet) {}
        @Override public void handleSetStructureBlock(ServerboundSetStructureBlockPacket packet) {}
        @Override public void handleSetJigsawBlock(ServerboundSetJigsawBlockPacket packet) {}
        @Override public void handleJigsawGenerate(ServerboundJigsawGeneratePacket packet) {}
        @Override public void handleSelectTrade(ServerboundSelectTradePacket packet) {}
        @Override public void handleEditBook(ServerboundEditBookPacket packet) {}
        @Override public void handleEntityTagQuery(ServerboundEntityTagQueryPacket packet) {}
        @Override public void handleBlockEntityTagQuery(ServerboundBlockEntityTagQueryPacket packet) {}
        @Override public void handleMovePlayer(ServerboundMovePlayerPacket packet) {}
        @Override public void handlePlayerAction(ServerboundPlayerActionPacket packet) {}
        @Override public void handleUseItemOn(ServerboundUseItemOnPacket packet) {}
        @Override public void handleUseItem(ServerboundUseItemPacket packet) {}
        @Override public void handleTeleportToEntityPacket(ServerboundTeleportToEntityPacket packet) {}
        @Override public void handleResourcePackResponse(ServerboundResourcePackPacket packet) {}
        @Override public void handlePaddleBoat(ServerboundPaddleBoatPacket packet) {}
        @Override public void handleSetCarriedItem(ServerboundSetCarriedItemPacket packet) {}
        @Override public void handleChat(ServerboundChatPacket packet) {}
        @Override public void handlePlayerCommand(ServerboundPlayerCommandPacket packet) {}
        @Override public void handleInteract(ServerboundInteractPacket packet) {}
        @Override public void handleClientCommand(ServerboundClientCommandPacket packet) {}
        @Override public void handleContainerClose(ServerboundContainerClosePacket packet) {}
        @Override public void handleContainerClick(ServerboundContainerClickPacket packet) {}
        @Override public void handlePlaceRecipe(ServerboundPlaceRecipePacket packet) {}
        @Override public void handleContainerButtonClick(ServerboundContainerButtonClickPacket packet) {}
        @Override public void handleSetCreativeModeSlot(ServerboundSetCreativeModeSlotPacket packet) {}
        @Override public void handleSignUpdate(ServerboundSignUpdatePacket packet) {}
        @Override public void handleKeepAlive(ServerboundKeepAlivePacket packet) {}
        @Override public void handleCustomPayload(ServerboundCustomPayloadPacket packet) {}
        @Override public void handleClientInformation(ServerboundClientInformationPacket packet) {}
        @Override public void handlePlayerAbilities(ServerboundPlayerAbilitiesPacket packet) {}
        @Override public void handleChangeDifficulty(ServerboundChangeDifficultyPacket packet) {}
        @Override public void handleLockDifficulty(ServerboundLockDifficultyPacket packet) {}
        @Override public void handleChatCommand(ServerboundChatCommandPacket packet) {}
    }

    private static final class BotDummyConnection extends net.minecraft.network.Connection {
        public BotDummyConnection() {
            super(PacketFlow.SERVERBOUND);
        }

        @Override
        public void setListenerForServerboundHandshake(PacketListener listener) {
        }
    }
}
