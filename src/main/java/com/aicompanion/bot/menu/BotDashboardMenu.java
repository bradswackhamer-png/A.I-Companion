package com.aicompanion.bot.menu;

import com.aicompanion.bot.AiBotPlayer;
import com.aicompanion.bot.AiCompanionMod;
import com.aicompanion.bot.BotAction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Container menu for Companion Bot status and direct inventory access.
 * Displays real-time health/hunger data, armor slots, offhand, and 36 inventory slots.
 */
public class BotDashboardMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 8;
    private final Container botInventory;
    private final ContainerData data;
    private final @Nullable AiBotPlayer bot;
    private final String botName;
    private final String botRole;

    // Client-side constructor
    public BotDashboardMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(41), new SimpleContainerData(DATA_COUNT), null);
    }

    // Server-side constructor
    public BotDashboardMenu(int containerId, Inventory playerInventory, Container botInventory, ContainerData data, @Nullable AiBotPlayer bot) {
        super(AiCompanionMod.BOT_DASHBOARD_MENU.get(), containerId);
        this.botInventory = botInventory;
        this.data = data;
        this.bot = bot;
        this.botName = bot != null ? bot.getBotName() : "Companion";
        if (bot != null) {
            String roleStr = bot.getBotRole();
            if (roleStr.equalsIgnoreCase("villager") && bot.getBotSubRole() != null && !bot.getBotSubRole().isBlank() && !bot.getBotSubRole().equalsIgnoreCase("unemployed")) {
                roleStr = "Villager (" + AiBotPlayer.capitalize(bot.getBotSubRole()) + ")";
            }
            this.botRole = roleStr;
        } else {
            this.botRole = "Helper";
        }

        checkContainerDataCount(data, DATA_COUNT);
        this.addDataSlots(data);

        // 1. Bot Armor & Offhand Slots (column at x = 8)
        // y: 62 (head), 80 (chest), 98 (legs), 116 (feet), 134 (offhand)
        for (int i = 0; i < 4; i++) {
            this.addSlot(new Slot(botInventory, 36 + (3 - i), 8, 62 + i * 18));
        }
        this.addSlot(new Slot(botInventory, 40, 8, 134)); // offhand slot

        // 2. Bot Main Inventory: 3 rows of 9 (slots 9-35) at x = 30, y = 62
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(botInventory, 9 + col + row * 9, 30 + col * 18, 62 + row * 18));
            }
        }
        // Bot Hotbar: 1 row of 9 (slots 0-8) at x = 30, y = 116
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(botInventory, col, 30 + col * 18, 116));
        }

        // 3. Human Player Inventory at x = 19, y = 154
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, 9 + col + row * 9, 19 + col * 18, 154 + row * 18));
            }
        }
        // Human Player Hotbar at x = 19, y = 212
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 19 + col * 18, 212));
        }
    }

    public float getHealth() {
        return this.data.get(0) / 10.0f;
    }

    public float getMaxHealth() {
        float max = this.data.get(1) / 10.0f;
        return max <= 0 ? 20.0f : max;
    }

    public int getFoodLevel() {
        return this.data.get(2);
    }

    public String getBotName() {
        return this.botName;
    }

    public String getBotRole() {
        return this.botRole;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.bot == null) return false;

        switch (id) {
            case 0 -> { // Follow / Hold Toggle
                if (this.bot.getCurrentAction() != null && "FOLLOW".equalsIgnoreCase(this.bot.getCurrentAction().getNormalizedType())) {
                    this.bot.clearActions();
                    this.bot.sayInChat("Holding position.");
                } else {
                    this.bot.clearActions();
                    this.bot.setActionPlan(List.of(new BotAction("FOLLOW", player.getPlainTextName(), null, 1)));
                    this.bot.sayInChat("Following you!");
                }
                return true;
            }
            case 1 -> { // Patrol / Guard
                this.bot.clearActions();
                this.bot.setActionPlan(List.of(new BotAction("GUARD", "area", null, 1)));
                this.bot.sayInChat("On patrol duty! Guarding this area.");
                return true;
            }
            case 2 -> { // Build Cottage
                this.bot.startHouseConstruction(this.bot.blockPosition().relative(this.bot.getDirection(), 2), this.bot.getDirection());
                return true;
            }
            case 3 -> { // Deposit to Chest
                this.bot.clearActions();
                this.bot.setActionPlan(List.of(new BotAction("CHEST_STORE", "all", null, 1)));
                this.bot.sayInChat("Storing collected resources in nearby chests.");
                return true;
            }
            case 4 -> { // Eat / Ration
                this.bot.forceEatFromInventory();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            itemstack = stackInSlot.copy();

            int totalBotSlots = 5 + 36; // 4 armor + 1 offhand + 36 inventory = 41 slots
            if (index < totalBotSlots) {
                // Move from bot to human player
                if (!this.moveItemStackTo(stackInSlot, totalBotSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move from human player to bot
                if (!this.moveItemStackTo(stackInSlot, 5, totalBotSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.bot == null || (this.bot.isAlive() && player.distanceToSqr(this.bot) < 64.0);
    }
}
