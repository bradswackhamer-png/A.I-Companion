package com.aicompanion.bot.area;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;

public class SurveyorWandItem extends Item {

    public SurveyorWandItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Level level = context.getLevel();

        if (player.isShiftKeyDown()) {
            // Sneak + right click on a block also allows opening the Save GUI
            if (level.isClientSide() && FMLEnvironment.getDist().isClient()) {
                com.aicompanion.bot.client.ClientAreaOpener.openScreen();
            }
            return InteractionResult.SUCCESS;
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            AreaManager.addPoint(serverPlayer, context.getClickedPos());
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            if (level.isClientSide() && FMLEnvironment.getDist().isClient()) {
                com.aicompanion.bot.client.ClientAreaOpener.openScreen();
            } else if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                int count = AreaManager.getSelectedPoints(serverPlayer.getUUID()).size();
                if (count < 3) {
                    serverPlayer.sendSystemMessage(Component.literal("§c[Surveyor Wand] You need at least 3 corner points to form an area! Right-click blocks to place corners.")
                            .withStyle(ChatFormatting.RED));
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.literal("Used to survey and map global compound and zone perimeters.").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.literal("• Right-Click Block: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Mark boundary corner").withStyle(ChatFormatting.WHITE)));
        tooltip.accept(Component.literal("• Sneak + Right-Click: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Open Name & Save Area GUI").withStyle(ChatFormatting.WHITE)));
        tooltip.accept(Component.literal("• /bot area clear: ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("Clear current corner points").withStyle(ChatFormatting.WHITE)));
    }
}
