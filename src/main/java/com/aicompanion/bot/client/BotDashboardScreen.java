package com.aicompanion.bot.client;

import com.aicompanion.bot.menu.BotDashboardMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BotDashboardScreen extends AbstractContainerScreen<BotDashboardMenu> {

    public BotDashboardScreen(BotDashboardMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, 200, 240);
        this.inventoryLabelY = 142;
    }

    @Override
    protected void init() {
        super.init();

        int xo = this.leftPos;
        int yo = this.topPos;

        // Quick Command Buttons Row at y = 34
        this.addRenderableWidget(Button.builder(Component.literal("Follow"), btn -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            }
        }).bounds(xo + 6, yo + 34, 40, 16).build());

        this.addRenderableWidget(Button.builder(Component.literal("Patrol"), btn -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
            }
        }).bounds(xo + 48, yo + 34, 38, 16).build());

        this.addRenderableWidget(Button.builder(Component.literal("Build"), btn -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2);
            }
        }).bounds(xo + 88, yo + 34, 34, 16).build());

        this.addRenderableWidget(Button.builder(Component.literal("Store"), btn -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 3);
            }
        }).bounds(xo + 124, yo + 34, 34, 16).build());

        this.addRenderableWidget(Button.builder(Component.literal("Eat"), btn -> {
            if (this.minecraft != null && this.minecraft.gameMode != null) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 4);
            }
        }).bounds(xo + 160, yo + 34, 34, 16).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);

        int xo = this.leftPos;
        int yo = this.topPos;

        // 1. Sleek Dashboard Body (Dark Slate with Gold Border)
        graphics.fill(xo, yo, xo + this.imageWidth, yo + this.imageHeight, 0xF0181A1E);
        graphics.fill(xo, yo, xo + this.imageWidth, yo + 2, 0xFFFFB800); // top gold border
        graphics.fill(xo, yo + this.imageHeight - 2, xo + this.imageWidth, yo + this.imageHeight, 0xFFFFB800); // bottom gold border
        graphics.fill(xo, yo, xo + 2, yo + this.imageHeight, 0xFFFFB800); // left gold border
        graphics.fill(xo + this.imageWidth - 2, yo, xo + this.imageWidth, yo + this.imageHeight, 0xFFFFB800); // right gold border

        // 2. Speedometer-style Health Meter
        int healthX = xo + 8;
        int healthY = yo + 18;
        int healthW = 88;
        int healthH = 10;
        graphics.fill(healthX - 1, healthY - 1, healthX + healthW + 1, healthY + healthH + 1, 0xFF444444);
        graphics.fill(healthX, healthY, healthX + healthW, healthY + healthH, 0xFF222222);

        float hpPct = Math.max(0.0f, Math.min(1.0f, this.menu.getHealth() / this.menu.getMaxHealth()));
        int barFillW = (int) (healthW * hpPct);
        int hpColor = hpPct > 0.5f ? 0xFF2ECC71 : (hpPct > 0.25f ? 0xFFF39C12 : 0xFFE74C3C);
        graphics.fill(healthX, healthY, healthX + barFillW, healthY + healthH, hpColor);

        // 3. Speedometer-style Hunger Meter
        int foodX = xo + 104;
        int foodY = yo + 18;
        int foodW = 88;
        int foodH = 10;
        graphics.fill(foodX - 1, foodY - 1, foodX + foodW + 1, foodY + foodH + 1, 0xFF444444);
        graphics.fill(foodX, foodY, foodX + foodW, foodY + foodH, 0xFF222222);

        float foodPct = Math.max(0.0f, Math.min(1.0f, this.menu.getFoodLevel() / 20.0f));
        int foodFillW = (int) (foodW * foodPct);
        graphics.fill(foodX, foodY, foodX + foodFillW, foodY + foodH, 0xFFE67E22);

        // 4. Clean High-Contrast Slot Backgrounds
        for (Slot slot : this.menu.slots) {
            int sx = xo + slot.x;
            int sy = yo + slot.y;
            graphics.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF3A3E45);
            graphics.fill(sx, sy, sx + 16, sy + 16, 0xFF282C34);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        // Bot Title & Role
        graphics.text(this.font, Component.literal("★ " + this.menu.getBotName() + " ★").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), 8, 6, 0xFFFFAA00, false);
        graphics.text(this.font, Component.literal("[" + this.menu.getBotRole() + "]").withStyle(ChatFormatting.AQUA), 120, 6, 0xFF55FFFF, false);

        // Meter numerical text
        String hpStr = "HP " + (int) this.menu.getHealth() + "/" + (int) this.menu.getMaxHealth();
        graphics.text(this.font, Component.literal(hpStr), 12, 19, 0xFFFFFFFF, true);

        String foodStr = "Food " + this.menu.getFoodLevel() + "/20";
        graphics.text(this.font, Component.literal(foodStr), 108, 19, 0xFFFFFFFF, true);

        // Section labels
        graphics.text(this.font, Component.literal("Bot Gear & Inventory").withStyle(ChatFormatting.GRAY), 8, 52, 0xFFAAAAAA, false);
        graphics.text(this.font, Component.literal("Your Inventory").withStyle(ChatFormatting.GRAY), 19, 143, 0xFFAAAAAA, false);
    }
}
