package com.aicompanion.bot.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AreaConfigScreen extends Screen {
    private static final String[] AREA_TYPES = {"COMPOUND", "FARM", "RANCH", "SAFE_ZONE", "RESTRICTED", "GENERAL"};
    private int typeIndex = 0;

    private EditBox nameEdit;
    private Button typeButton;
    private Button saveButton;
    private Button cancelButton;

    public AreaConfigScreen() {
        super(Component.literal("Define Global Area"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.nameEdit = new EditBox(this.font, centerX - 100, centerY - 35, 200, 20, Component.literal("Area Name"));
        this.nameEdit.setMaxLength(32);
        this.nameEdit.setValue("Compound");
        this.addRenderableWidget(this.nameEdit);

        this.typeButton = Button.builder(
                Component.literal("Zone Type: " + AREA_TYPES[typeIndex]),
                btn -> {
                    typeIndex = (typeIndex + 1) % AREA_TYPES.length;
                    btn.setMessage(Component.literal("Zone Type: " + AREA_TYPES[typeIndex]));
                })
                .bounds(centerX - 100, centerY - 5, 200, 20)
                .build();
        this.addRenderableWidget(this.typeButton);

        this.saveButton = Button.builder(
                Component.literal("Save Area"),
                btn -> {
                    String name = nameEdit.getValue().trim();
                    if (!name.isEmpty()) {
                        String cleanName = name.replace(" ", "_");
                        if (minecraft != null && minecraft.player != null) {
                            minecraft.player.connection.sendCommand("bot area save " + cleanName + " " + AREA_TYPES[typeIndex]);
                        }
                    }
                    this.onClose();
                })
                .bounds(centerX - 100, centerY + 30, 95, 20)
                .build();
        this.addRenderableWidget(this.saveButton);

        this.cancelButton = Button.builder(
                Component.literal("Cancel"),
                btn -> this.onClose())
                .bounds(centerX + 5, centerY + 30, 95, 20)
                .build();
        this.addRenderableWidget(this.cancelButton);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        this.extractTransparentBackground(extractor);
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        extractor.centeredText(this.font, Component.literal("★ Define Global Area Boundary ★"), centerX, centerY - 70, 0xFFFFAA00);
        extractor.centeredText(this.font, Component.literal("All companion bots will recognize and stay inside this perimeter"), centerX, centerY - 55, 0xFFAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
