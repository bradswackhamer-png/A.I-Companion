package com.aicompanion.bot;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import com.aicompanion.bot.area.SavedArea;
import com.aicompanion.bot.memory.BotMemoryManager;
import com.aicompanion.bot.memory.LearnedRecipe;
import com.aicompanion.bot.village.CustomBlueprint;

public class BotCommands {

    private static final SuggestionProvider<CommandSourceStack> BOT_NAME_SUGGESTIONS = (ctx, builder) -> {
        var names = BotManager.getActiveBots().stream().map(AiBotPlayer::getBotName).toList();
        return SharedSuggestionProvider.suggest(names, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> ROLE_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(java.util.List.of("miner", "guard", "lumberjack", "builder", "courier", "farmer", "husbandry", "rancher", "conductor"), builder);

    private static final SuggestionProvider<CommandSourceStack> AREA_SUGGESTIONS = (ctx, builder) -> {
        var names = BotPersistence.getAllAreas().values().stream().map(a -> a.name).toList();
        return SharedSuggestionProvider.suggest(names, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> AREA_TYPE_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(java.util.List.of("COMPOUND", "FARM", "RANCH", "SAFE_ZONE", "RESTRICTED", "GENERAL"), builder);

    private static final SuggestionProvider<CommandSourceStack> BLUEPRINT_SUGGESTIONS = (ctx, builder) -> {
        var names = BotPersistence.getAllBlueprints().values().stream().map(b -> b.name).toList();
        return SharedSuggestionProvider.suggest(names, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> SKIN_SUGGESTIONS = (ctx, builder) -> {
        var skins = com.aicompanion.bot.skin.BotSkinManager.getAvailableSkinNames();
        return SharedSuggestionProvider.suggest(skins, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(buildCommandTree("bot"));
        dispatcher.register(buildCommandTree("aibot"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCommandTree(String rootLiteral) {
        return Commands.literal(rootLiteral)
                .executes(BotCommands::showGeneralHelp)
                .then(Commands.literal("help")
                        .executes(BotCommands::showGeneralHelp))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> spawnBot(ctx, StringArgumentType.getString(ctx, "name"), "All-Around Survival Companion"))
                                .then(Commands.argument("role", StringArgumentType.greedyString())
                                        .suggests(ROLE_SUGGESTIONS)
                                        .executes(ctx -> spawnBot(ctx,
                                                StringArgumentType.getString(ctx, "name"),
                                                StringArgumentType.getString(ctx, "role"))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(BOT_NAME_SUGGESTIONS)
                                .executes(ctx -> removeBot(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("list")
                        .executes(BotCommands::listBots))
                .then(Commands.literal("status")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(BOT_NAME_SUGGESTIONS)
                                .executes(ctx -> showStatus(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("stop")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(BOT_NAME_SUGGESTIONS)
                                .executes(ctx -> stopBot(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("tp")
                        .executes(BotCommands::tpNearestBot)
                        .then(Commands.literal("all")
                                .executes(BotCommands::tpAllBots))
                        .then(Commands.literal("to")
                                .then(Commands.argument("botName", StringArgumentType.word())
                                        .suggests(BOT_NAME_SUGGESTIONS)
                                        .executes(ctx -> tpPlayerToBot(ctx, StringArgumentType.getString(ctx, "botName")))))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(BOT_NAME_SUGGESTIONS)
                                .executes(ctx -> tpBot(ctx, StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("ask")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(BOT_NAME_SUGGESTIONS)
                                .then(Commands.argument("request", StringArgumentType.greedyString())
                                        .executes(ctx -> askBot(ctx,
                                                StringArgumentType.getString(ctx, "name"),
                                                StringArgumentType.getString(ctx, "request"))))))
                .then(Commands.literal("apikey")
                        .executes(BotCommands::showApiKeyHelp)
                        .then(Commands.argument("key", StringArgumentType.greedyString())
                                .executes(ctx -> setApiKey(ctx, StringArgumentType.getString(ctx, "key")))))
                .then(Commands.literal("model")
                        .then(Commands.argument("modelName", StringArgumentType.greedyString())
                                .executes(ctx -> setModel(ctx, StringArgumentType.getString(ctx, "modelName")))))
                .then(Commands.literal("waypoint")
                        .then(Commands.argument("locationName", StringArgumentType.word())
                                .executes(ctx -> saveWaypoint(ctx, StringArgumentType.getString(ctx, "locationName")))))
                .then(Commands.literal("nickname")
                        .then(Commands.argument("nickname", StringArgumentType.word())
                                .executes(ctx -> setBotNicknameNearest(ctx, StringArgumentType.getString(ctx, "nickname"))))
                        .then(Commands.argument("botName", StringArgumentType.word())
                                .suggests(BOT_NAME_SUGGESTIONS)
                                .then(Commands.argument("nickname", StringArgumentType.word())
                                        .executes(ctx -> setBotNickname(ctx,
                                                StringArgumentType.getString(ctx, "botName"),
                                                StringArgumentType.getString(ctx, "nickname"))))))
                .then(Commands.literal("rename")
                        .then(Commands.argument("nickname", StringArgumentType.word())
                                .executes(ctx -> setBotNicknameNearest(ctx, StringArgumentType.getString(ctx, "nickname"))))
                        .then(Commands.argument("botName", StringArgumentType.word())
                                .suggests(BOT_NAME_SUGGESTIONS)
                                .then(Commands.argument("nickname", StringArgumentType.word())
                                        .executes(ctx -> setBotNickname(ctx,
                                                StringArgumentType.getString(ctx, "botName"),
                                                StringArgumentType.getString(ctx, "nickname"))))))
                .then(Commands.literal("wand")
                        .executes(BotCommands::giveWand))
                .then(Commands.literal("area")
                        .then(Commands.literal("list")
                                .executes(BotCommands::listAreas))
                        .then(Commands.literal("clear")
                                .executes(BotCommands::clearAreaPoints))
                        .then(Commands.literal("save")
                                .then(Commands.argument("areaName", StringArgumentType.word())
                                        .executes(ctx -> saveAreaCmd(ctx, StringArgumentType.getString(ctx, "areaName"), "COMPOUND"))
                                        .then(Commands.argument("areaType", StringArgumentType.word())
                                                .suggests(AREA_TYPE_SUGGESTIONS)
                                                .executes(ctx -> saveAreaCmd(ctx,
                                                        StringArgumentType.getString(ctx, "areaName"),
                                                        StringArgumentType.getString(ctx, "areaType"))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("areaName", StringArgumentType.word())
                                        .suggests(AREA_SUGGESTIONS)
                                        .executes(ctx -> removeAreaCmd(ctx, StringArgumentType.getString(ctx, "areaName")))))
                        .then(Commands.literal("show")
                                .then(Commands.argument("areaName", StringArgumentType.word())
                                        .suggests(AREA_SUGGESTIONS)
                                        .executes(ctx -> showAreaCmd(ctx, StringArgumentType.getString(ctx, "areaName")))))
                        .then(Commands.literal("height")
                                .then(Commands.argument("areaName", StringArgumentType.word())
                                        .suggests(AREA_SUGGESTIONS)
                                        .then(Commands.argument("minY", IntegerArgumentType.integer(-384, 384))
                                                .executes(ctx -> setAreaHeightCmd(ctx,
                                                        StringArgumentType.getString(ctx, "areaName"),
                                                        IntegerArgumentType.getInteger(ctx, "minY"),
                                                        null))
                                                .then(Commands.argument("maxY", IntegerArgumentType.integer(-384, 384))
                                                        .executes(ctx -> setAreaHeightCmd(ctx,
                                                                StringArgumentType.getString(ctx, "areaName"),
                                                                IntegerArgumentType.getInteger(ctx, "minY"),
                                                                IntegerArgumentType.getInteger(ctx, "maxY")))))))
                        .then(Commands.literal("heigh")
                                .then(Commands.argument("areaName", StringArgumentType.word())
                                        .suggests(AREA_SUGGESTIONS)
                                        .then(Commands.argument("minY", IntegerArgumentType.integer(-384, 384))
                                                .executes(ctx -> setAreaHeightCmd(ctx,
                                                        StringArgumentType.getString(ctx, "areaName"),
                                                        IntegerArgumentType.getInteger(ctx, "minY"),
                                                        null))
                                                .then(Commands.argument("maxY", IntegerArgumentType.integer(-384, 384))
                                                        .executes(ctx -> setAreaHeightCmd(ctx,
                                                                StringArgumentType.getString(ctx, "areaName"),
                                                                IntegerArgumentType.getInteger(ctx, "minY"),
                                                                IntegerArgumentType.getInteger(ctx, "maxY")))))))
                        .then(Commands.literal("cube")
                                .then(Commands.argument("areaName", StringArgumentType.word())
                                        .suggests(AREA_SUGGESTIONS)
                                        .then(Commands.argument("minY", IntegerArgumentType.integer(-384, 384))
                                                .executes(ctx -> setAreaHeightCmd(ctx,
                                                        StringArgumentType.getString(ctx, "areaName"),
                                                        IntegerArgumentType.getInteger(ctx, "minY"),
                                                        null))
                                                .then(Commands.argument("maxY", IntegerArgumentType.integer(-384, 384))
                                                        .executes(ctx -> setAreaHeightCmd(ctx,
                                                                StringArgumentType.getString(ctx, "areaName"),
                                                                IntegerArgumentType.getInteger(ctx, "minY"),
                                                                IntegerArgumentType.getInteger(ctx, "maxY")))))))
                        .then(Commands.literal("assign")
                                .then(Commands.argument("botName", StringArgumentType.word())
                                        .suggests(BOT_NAME_SUGGESTIONS)
                                        .then(Commands.argument("areaName", StringArgumentType.word())
                                                .suggests(AREA_SUGGESTIONS)
                                                .executes(ctx -> assignAreaCmd(ctx,
                                                        StringArgumentType.getString(ctx, "botName"),
                                                        StringArgumentType.getString(ctx, "areaName")))))))
                .then(Commands.literal("blueprint")
                        .then(Commands.literal("capture")
                                .then(Commands.argument("areaName", StringArgumentType.word())
                                        .suggests(AREA_SUGGESTIONS)
                                        .then(Commands.argument("blueprintName", StringArgumentType.word())
                                                .executes(ctx -> captureBlueprintCmd(ctx,
                                                        StringArgumentType.getString(ctx, "areaName"),
                                                        StringArgumentType.getString(ctx, "blueprintName"))))))
                        .then(Commands.literal("scan")
                                .then(Commands.argument("areaName", StringArgumentType.word())
                                        .suggests(AREA_SUGGESTIONS)
                                        .then(Commands.argument("blueprintName", StringArgumentType.word())
                                                .executes(ctx -> captureBlueprintCmd(ctx,
                                                        StringArgumentType.getString(ctx, "areaName"),
                                                        StringArgumentType.getString(ctx, "blueprintName"))))))
                        .then(Commands.literal("list")
                                .executes(BotCommands::listBlueprintsCmd))
                        .then(Commands.literal("info")
                                .then(Commands.argument("blueprintName", StringArgumentType.word())
                                        .suggests(BLUEPRINT_SUGGESTIONS)
                                        .executes(ctx -> infoBlueprintCmd(ctx, StringArgumentType.getString(ctx, "blueprintName")))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("blueprintName", StringArgumentType.word())
                                        .suggests(BLUEPRINT_SUGGESTIONS)
                                        .executes(ctx -> removeBlueprintCmd(ctx, StringArgumentType.getString(ctx, "blueprintName")))))
                        .then(Commands.literal("build")
                                .then(Commands.argument("botName", StringArgumentType.word())
                                        .suggests(BOT_NAME_SUGGESTIONS)
                                        .then(Commands.argument("blueprintName", StringArgumentType.word())
                                                .suggests(BLUEPRINT_SUGGESTIONS)
                                                .executes(ctx -> buildBlueprintCmd(ctx,
                                                        StringArgumentType.getString(ctx, "botName"),
                                                        StringArgumentType.getString(ctx, "blueprintName")))))))
                .then(Commands.literal("build")
                        .then(Commands.argument("botName", StringArgumentType.word())
                                .suggests(BOT_NAME_SUGGESTIONS)
                                .then(Commands.argument("blueprintName", StringArgumentType.word())
                                        .suggests(BLUEPRINT_SUGGESTIONS)
                                        .executes(ctx -> buildBlueprintCmd(ctx,
                                                StringArgumentType.getString(ctx, "botName"),
                                                StringArgumentType.getString(ctx, "blueprintName"))))))
                .then(Commands.literal("gui")
                        .then(Commands.argument("botName", StringArgumentType.word())
                                .suggests(BOT_NAME_SUGGESTIONS)
                                .executes(ctx -> openBotGuiCmd(ctx, StringArgumentType.getString(ctx, "botName"))))
                        .executes(BotCommands::openNearestBotGuiCmd))
                .then(Commands.literal("skin")
                        .then(Commands.literal("list")
                                .executes(BotCommands::listSkinsCmd))
                        .then(Commands.literal("reload")
                                .executes(BotCommands::reloadSkinsCmd))
                        .then(Commands.literal("set")
                                .then(Commands.argument("botName", StringArgumentType.word())
                                        .suggests(BOT_NAME_SUGGESTIONS)
                                        .then(Commands.argument("skinName", StringArgumentType.string())
                                                .suggests(SKIN_SUGGESTIONS)
                                                .executes(ctx -> setBotSkinCmd(ctx,
                                                        StringArgumentType.getString(ctx, "botName"),
                                                        StringArgumentType.getString(ctx, "skinName")))))))
                .then(Commands.literal("train")
                        .then(Commands.literal("book")
                                .executes(ctx -> trainFromHeldBook(ctx, null, false))
                                .then(Commands.literal("global")
                                        .executes(ctx -> trainFromHeldBook(ctx, null, true)))
                                .then(Commands.argument("botName", StringArgumentType.word())
                                        .suggests(BOT_NAME_SUGGESTIONS)
                                        .executes(ctx -> trainFromHeldBook(ctx, StringArgumentType.getString(ctx, "botName"), false))))
                        .then(Commands.literal("global")
                                .then(Commands.argument("instruction", StringArgumentType.greedyString())
                                        .executes(ctx -> trainGlobal(ctx, StringArgumentType.getString(ctx, "instruction")))))
                        .then(Commands.argument("botName", StringArgumentType.word())
                                .suggests(BOT_NAME_SUGGESTIONS)
                                .then(Commands.argument("instruction", StringArgumentType.greedyString())
                                        .executes(ctx -> trainBot(ctx, StringArgumentType.getString(ctx, "botName"), StringArgumentType.getString(ctx, "instruction"))))))
                .then(Commands.literal("memory")
                        .then(Commands.literal("list")
                                .executes(ctx -> listMemory(ctx, null))
                                .then(Commands.literal("global")
                                        .executes(ctx -> listMemory(ctx, "global")))
                                .then(Commands.argument("botName", StringArgumentType.word())
                                        .suggests(BOT_NAME_SUGGESTIONS)
                                        .executes(ctx -> listMemory(ctx, StringArgumentType.getString(ctx, "botName")))))
                        .then(Commands.literal("book")
                                .executes(ctx -> giveMemoryBook(ctx, null))
                                .then(Commands.literal("global")
                                        .executes(ctx -> giveMemoryBook(ctx, "global")))
                                .then(Commands.argument("botName", StringArgumentType.word())
                                        .suggests(BOT_NAME_SUGGESTIONS)
                                        .executes(ctx -> giveMemoryBook(ctx, StringArgumentType.getString(ctx, "botName")))))
                        .then(Commands.literal("clear")
                                .then(Commands.literal("global")
                                        .executes(ctx -> clearMemory(ctx, "global")))
                                .then(Commands.argument("botName", StringArgumentType.word())
                                        .suggests(BOT_NAME_SUGGESTIONS)
                                        .executes(ctx -> clearMemory(ctx, StringArgumentType.getString(ctx, "botName"))))));
    }

    private static int openBotGuiCmd(CommandContext<CommandSourceStack> ctx, String botName) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Only players can open bot dashboards."));
            return 0;
        }
        AiBotPlayer bot = BotManager.getBot(botName);
        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("Bot '" + botName + "' not found."));
            return 0;
        }
        bot.openCompanionGui(player);
        return 1;
    }

    private static int openNearestBotGuiCmd(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Only players can open bot dashboards."));
            return 0;
        }
        AiBotPlayer nearest = null;
        double bestDist = 64.0;
        for (AiBotPlayer bot : BotManager.getActiveBots()) {
            if (bot.level() == player.level()) {
                double d = bot.distanceToSqr(player);
                if (d < bestDist) {
                    bestDist = d;
                    nearest = bot;
                }
            }
        }
        if (nearest == null) {
            ctx.getSource().sendFailure(Component.literal("No companion bot nearby. Right-click a bot or stand closer."));
            return 0;
        }
        nearest.openCompanionGui(player);
        return 1;
    }

    private static int giveWand(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only in-game players can hold the Surveyor Wand."));
            return 0;
        }
        ItemStack wand = AiCompanionMod.SURVEYOR_WAND.toStack();
        player.getInventory().add(wand);
        source.sendSuccess(() -> Component.literal("§aGiven §6Surveyor Wand§a! Right-click blocks to set corners, then Sneak + Right-Click to save the boundary.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int listAreas(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        var areas = BotPersistence.getAllAreas();
        if (areas.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No global areas saved yet. Use /bot wand to mark corners and save an area!").withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("=== Global Companion Areas (" + areas.size() + ") ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (var a : areas.values()) {
            source.sendSuccess(() -> Component.literal("• §f" + a.name + " §7[§e" + a.type + "§7] §a" + a.points.size() + " corners §7(~" + (int) a.getAreaSqMeters() + " m², Dim: " + a.dimension + ")"), false);
        }
        return 1;
    }

    private static int clearAreaPoints(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player != null) {
            com.aicompanion.bot.area.AreaManager.clearSelection(player);
        }
        return 1;
    }

    private static int saveAreaCmd(CommandContext<CommandSourceStack> ctx, String name, String type) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Only players can save areas from active wand selections."));
            return 0;
        }
        boolean ok = com.aicompanion.bot.area.AreaManager.saveActiveSelection(player, name, type);
        return ok ? 1 : 0;
    }

    private static int removeAreaCmd(CommandContext<CommandSourceStack> ctx, String name) {
        boolean removed = BotPersistence.removeArea(name);
        if (removed) {
            BotPersistence.saveAll(ctx.getSource().getServer());
            ctx.getSource().sendSuccess(() -> Component.literal("Removed global area '" + name + "'.").withStyle(ChatFormatting.GREEN), true);
            return 1;
        } else {
            ctx.getSource().sendFailure(Component.literal("No global area named '" + name + "' found."));
            return 0;
        }
    }

    private static int showAreaCmd(CommandContext<CommandSourceStack> ctx, String name) {
        var area = BotPersistence.getArea(name);
        if (area == null) {
            ctx.getSource().sendFailure(Component.literal("Area '" + name + "' not found."));
            return 0;
        }
        if (area.isCubed || (area.maxY - area.minY < 100 && area.maxY - area.minY > 0)) {
            area.spawnCubeParticles(ctx.getSource().getLevel(), net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK);
            ctx.getSource().sendSuccess(() -> Component.literal("★ Illuminated 3D Cubed Volume for area '" + area.name + "' (Height: " + area.getHeight() + " blocks, Y: " + area.minY + " to " + area.maxY + ")!").withStyle(ChatFormatting.AQUA), false);
        } else {
            area.spawnPerimeterParticles(ctx.getSource().getLevel(), net.minecraft.core.particles.ParticleTypes.END_ROD);
            ctx.getSource().sendSuccess(() -> Component.literal("Illuminated boundary for area '" + area.name + "' with glowing perimeter particles!").withStyle(ChatFormatting.AQUA), false);
        }
        return 1;
    }

    private static int setAreaHeightCmd(CommandContext<CommandSourceStack> ctx, String areaName, int y1, Integer y2) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        var area = BotPersistence.getArea(areaName);
        if (area == null) {
            if (player != null && !com.aicompanion.bot.area.AreaManager.getSelectedPoints(player.getUUID()).isEmpty()) {
                boolean ok = com.aicompanion.bot.area.AreaManager.saveActiveSelection(player, areaName, "COMPOUND", y1, y2);
                return ok ? 1 : 0;
            }
            source.sendFailure(Component.literal("Area '" + areaName + "' not found. Right-click corners with the Surveyor Wand to select an area first, or specify an existing area name."));
            return 0;
        }

        if (y2 != null) {
            area.setCubedRange(y1, y2);
        } else {
            area.setCubedHeight(y1);
        }
        BotPersistence.saveArea(area);
        BotPersistence.saveAll(source.getServer());
        area.spawnCubeParticles(source.getLevel(), net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK);

        source.sendSuccess(() -> Component.literal("§a★ Area '§f" + area.name + "§a' converted to 3D Cuboid! Y-Range: §e" + area.minY + " to " + area.maxY + " §a(Height: §e" + area.getHeight() + "§a blocks, Volume: ~" + area.getVolume() + " m³)")
                .withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    private static int captureBlueprintCmd(CommandContext<CommandSourceStack> ctx, String areaName, String blueprintName) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();
        var area = BotPersistence.getArea(areaName);
        if (area == null) {
            source.sendFailure(Component.literal("Area '" + areaName + "' not found. Define it with your Surveyor Wand and set its height with /bot area height " + areaName + " <minY> <maxY>."));
            return 0;
        }

        if (!area.isCubed && (area.maxY - area.minY > 100)) {
            area.setCubedHeight(16);
            BotPersistence.saveArea(area);
        }

        net.minecraft.core.Direction facing = player != null ? player.getDirection() : net.minecraft.core.Direction.NORTH;
        String author = player != null ? player.getName().getString() : "Server";

        CustomBlueprint bp = CustomBlueprint.capture(source.getLevel(), area, blueprintName, facing, author);
        if (bp.totalBlocks == 0 && bp.groundOffsetY >= 0) {
            source.sendFailure(Component.literal("No blocks found inside area '" + areaName + "'. Make sure the area encloses blocks and has a valid Y range!"));
            return 0;
        }

        BotPersistence.saveBlueprint(bp);
        BotPersistence.saveAll(source.getServer());

        area.spawnCubeParticles(source.getLevel(), net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER);

        source.sendSuccess(() -> Component.literal("§a★ Blueprint '§6" + bp.name + "§a' created! Captured §e" + bp.totalBlocks + "§a blocks from '§f" + area.name + "§a'.")
                .withStyle(ChatFormatting.GREEN), true);
        source.sendSuccess(() -> Component.literal("§7• Dimensions: §e" + bp.sizeX + "w × " + bp.sizeY + "h × " + bp.sizeZ + "d §7| Facing: §b" + bp.capturedFacing)
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("§a✔ Added to Builder Catalog! Tell any builder §f\"builder, build " + bp.name + "\"§a or type §e/bot build <bot> " + bp.name + "§a to construct it!")
                .withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private static int listBlueprintsCmd(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        var blueprints = BotPersistence.getAllBlueprints();
        if (blueprints.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No custom blueprints saved yet. Use /bot wand to mark an area, /bot area height <name> <height>, then /bot blueprint capture <area> <name>!").withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("=== Saved Architectural Blueprints (" + blueprints.size() + ") ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (var bp : blueprints.values()) {
            source.sendSuccess(() -> Component.literal("• §6" + bp.name + " §7(" + bp.sizeX + "x" + bp.sizeY + "x" + bp.sizeZ + ", §e" + bp.totalBlocks + " blocks§7, by " + bp.author + ")"), false);
        }
        return 1;
    }

    private static int infoBlueprintCmd(CommandContext<CommandSourceStack> ctx, String blueprintName) {
        CommandSourceStack source = ctx.getSource();
        var bp = BotPersistence.getBlueprint(blueprintName);
        if (bp == null) {
            source.sendFailure(Component.literal("Blueprint '" + blueprintName + "' not found."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("=== Blueprint: " + bp.name + " ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal("• Dimensions: " + bp.sizeX + "x" + bp.sizeY + "x" + bp.sizeZ + " (" + bp.totalBlocks + " total blocks)"), false);
        source.sendSuccess(() -> Component.literal("• Captured Facing: " + bp.capturedFacing + " | Creator: " + bp.author), false);
        source.sendSuccess(() -> Component.literal("• Required Materials:").withStyle(ChatFormatting.YELLOW), false);
        int shown = 0;
        for (var entry : bp.materialSummary.entrySet()) {
            if (shown++ >= 10) {
                source.sendSuccess(() -> Component.literal("  ... and " + (bp.materialSummary.size() - 10) + " more material types."), false);
                break;
            }
            source.sendSuccess(() -> Component.literal("  - §f" + entry.getKey() + ": §e" + entry.getValue()), false);
        }
        return 1;
    }

    private static int removeBlueprintCmd(CommandContext<CommandSourceStack> ctx, String blueprintName) {
        CommandSourceStack source = ctx.getSource();
        boolean removed = BotPersistence.removeBlueprint(blueprintName);
        if (removed) {
            BotPersistence.saveAll(source.getServer());
            source.sendSuccess(() -> Component.literal("Removed blueprint '" + blueprintName + "' from catalog.").withStyle(ChatFormatting.GREEN), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Blueprint '" + blueprintName + "' not found."));
            return 0;
        }
    }

    private static int buildBlueprintCmd(CommandContext<CommandSourceStack> ctx, String botName, String blueprintName) {
        CommandSourceStack source = ctx.getSource();
        AiBotPlayer bot = BotManager.getBot(botName);
        if (bot == null) {
            source.sendFailure(Component.literal("Bot '" + botName + "' not found."));
            return 0;
        }
        var bp = BotPersistence.getBlueprint(blueprintName);
        if (bp == null) {
            source.sendFailure(Component.literal("Blueprint '" + blueprintName + "' not found. Available blueprints: "
                    + String.join(", ", BotPersistence.getAllBlueprints().keySet())));
            return 0;
        }
        net.minecraft.core.BlockPos site = bot.blockPosition().relative(bot.getDirection(), 2);
        bot.startCustomBlueprintConstruction(bp, site, bot.getDirection());
        source.sendSuccess(() -> Component.literal("Ordered " + bot.getBotName() + " to construct blueprint '" + bp.name + "'!").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int assignAreaCmd(CommandContext<CommandSourceStack> ctx, String botName, String areaName) {
        AiBotPlayer bot = BotManager.getBot(botName);
        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("Bot '" + botName + "' not found."));
            return 0;
        }
        var area = BotPersistence.getArea(areaName);
        if (area == null) {
            ctx.getSource().sendFailure(Component.literal("Area '" + areaName + "' not found."));
            return 0;
        }
        bot.setAssignedArea(area.name);
        BotPersistence.saveAll(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.literal("Assigned " + bot.getBotName() + " to global area '" + area.name + "' (" + area.type + "). Bot will strictly patrol and operate inside this perimeter!").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int saveWaypoint(CommandContext<CommandSourceStack> ctx, String locationName) {
        CommandSourceStack source = ctx.getSource();
        net.minecraft.core.BlockPos pos = net.minecraft.core.BlockPos.containing(source.getPosition());
        String dim = source.getLevel().dimension().identifier().toString();
        BotPersistence.saveLocation(locationName, pos, dim);
        BotPersistence.saveAll(source.getServer());
        source.sendSuccess(() -> Component.literal("Saved persistent bot waypoint '" + locationName + "' at ("
                + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")! Bots can now walk to '" + locationName + "' anytime.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int spawnBot(CommandContext<CommandSourceStack> ctx, String name, String role) {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        Vec3 pos = source.getPosition();
        float yaw = source.getRotation().y;
        float pitch = source.getRotation().x;

        AiBotPlayer bot = BotManager.spawnBot(level, name, role, pos.x + 1.2, pos.y, pos.z + 1.2, yaw, pitch);
        source.sendSuccess(() -> Component.literal("Spawned AI Companion ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(bot.getBotName()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
                .append(Component.literal(" (" + bot.getBotRole() + ")!").withStyle(ChatFormatting.GREEN)), true);
        return 1;
    }

    private static int removeBot(CommandContext<CommandSourceStack> ctx, String name) {
        CommandSourceStack source = ctx.getSource();
        if ("all".equalsIgnoreCase(name)) {
            int removed = BotManager.removeAllBots();
            source.sendSuccess(() -> Component.literal("Removed " + removed + " AI bot(s).").withStyle(ChatFormatting.YELLOW), true);
            return removed;
        }
        if (BotManager.removeBot(name)) {
            source.sendSuccess(() -> Component.literal("Removed AI bot " + name + ".").withStyle(ChatFormatting.YELLOW), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("No active bot named '" + name + "' found."));
            return 0;
        }
    }

    private static int listBots(CommandContext<CommandSourceStack> ctx) {
        Collection<AiBotPlayer> bots = BotManager.getActiveBots();
        if (bots.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("No AI bots are currently spawned. Use /bot spawn <name> to summon one!")
                    .withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("=== Active AI Companion Bots (" + bots.size() + ") ===")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (AiBotPlayer bot : bots) {
            String line = String.format("- %s [%s] | HP: %.0f/%.0f | Food: %d/20 | Pos: (%d, %d, %d) | Task: %s",
                    bot.getChatTag(), bot.getBotRole(), bot.getHealth(), bot.getMaxHealth(),
                    bot.getFoodData().getFoodLevel(), bot.getBlockX(), bot.getBlockY(), bot.getBlockZ(),
                    bot.getCurrentTaskSummary());
            ctx.getSource().sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.AQUA), false);
        }
        return bots.size();
    }

    private static int showStatus(CommandContext<CommandSourceStack> ctx, String name) {
        AiBotPlayer bot = BotManager.getBot(name);
        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("No active bot named '" + name + "' found."));
            return 0;
        }
        StringBuilder inv = new StringBuilder();
        for (int i = 0; i < bot.getInventory().getContainerSize(); i++) {
            ItemStack s = bot.getInventory().getItem(i);
            if (!s.isEmpty()) {
                if (inv.length() > 0) inv.append(", ");
                inv.append(BuiltInRegistries.ITEM.getKey(s.getItem()).getPath()).append(" x").append(s.getCount());
            }
        }
        String invStr = inv.length() == 0 ? "Empty" : inv.toString();
        StringBuilder extra = new StringBuilder();
        if (bot.getFarmOriginPos() != null) {
            extra.append(String.format("Farm Zone: (%d, %d, %d) %dx%d | Crops: %s | AutoHay: %b | SeedsOnHand: %d | ChestSeeds: %d | CompostExcess: %b\n",
                    bot.getFarmOriginPos().getX(), bot.getFarmOriginPos().getY(), bot.getFarmOriginPos().getZ(),
                    bot.getFarmRadiusX() * 2, bot.getFarmRadiusZ() * 2,
                    bot.getFarmAllowedCrops().isEmpty() ? "all" : String.join(", ", bot.getFarmAllowedCrops()),
                    bot.isAutoCompactWheatToHay(), bot.getMaxSeedsToKeepOnHand(), bot.getMaxSeedsInChest(), bot.isAutoCompostExcessSeeds()));
        }
        if (bot.getRanchOriginPos() != null || bot.getBotRole().toLowerCase().contains("husbandry") || bot.getBotRole().toLowerCase().contains("ranch")) {
            extra.append(String.format("Livestock: %s (Food: %s, Cap: %d, Min: %d, CullAdults: %b)\n",
                    bot.getRanchAnimal(), bot.getRanchFood(), bot.getRanchMaxPop(), bot.getRanchMinPop(), bot.isRanchAutoCull()));
        }

        ctx.getSource().sendSuccess(() -> Component.literal("=== " + bot.getChatTag() + " Status ===\n")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("Role: " + bot.getBotRole() + "\n").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(String.format("HP: %.1f/%.1f | Hunger: %d/20 | Pos: (%d, %d, %d)\n",
                        bot.getHealth(), bot.getMaxHealth(), bot.getFoodData().getFoodLevel(),
                        bot.getBlockX(), bot.getBlockY(), bot.getBlockZ())).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(extra.toString()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("Task: " + bot.getCurrentTaskSummary() + "\n").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("Inventory: " + invStr).withStyle(ChatFormatting.GRAY)), false);
        return 1;
    }

    private static int stopBot(CommandContext<CommandSourceStack> ctx, String name) {
        if ("all".equalsIgnoreCase(name)) {
            for (AiBotPlayer b : BotManager.getActiveBots()) {
                b.clearActions();
                b.sayInChat("Stopped all tasks.");
            }
            return 1;
        }
        AiBotPlayer bot = BotManager.getBot(name);
        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("No active bot named '" + name + "' found."));
            return 0;
        }
        bot.clearActions();
        bot.sayInChat("Stopped my current task and standing by.");
        return 1;
    }

    private static void teleportBotToSource(AiBotPlayer bot, CommandSourceStack source) {
        ServerLevel targetLevel = source.getLevel();
        Vec3 pos = source.getPosition();
        if (bot.level() != targetLevel) {
            bot.teleportTo(targetLevel, pos.x + 0.5, pos.y + 0.1, pos.z + 0.5, java.util.Set.of(), bot.getYRot(), bot.getXRot(), true);
        } else {
            bot.teleportTo(pos.x + 0.5, pos.y + 0.1, pos.z + 0.5);
        }
    }

    private static int tpNearestBot(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        AiBotPlayer nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        Vec3 pos = ctx.getSource().getPosition();

        for (AiBotPlayer bot : BotManager.getActiveBots()) {
            if (player != null && bot.level() == player.level()) {
                double distSq = bot.distanceToSqr(pos.x, pos.y, pos.z);
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = bot;
                }
            } else if (nearest == null) {
                nearest = bot;
            }
        }

        if (nearest == null) {
            ctx.getSource().sendFailure(Component.literal("No active bots found to teleport."));
            return 0;
        }

        teleportBotToSource(nearest, ctx.getSource());
        nearest.sayInChat("Teleported to your location!");
        return 1;
    }

    private static int tpAllBots(CommandContext<CommandSourceStack> ctx) {
        var bots = BotManager.getActiveBots();
        if (bots.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No active bots found."));
            return 0;
        }
        for (AiBotPlayer bot : bots) {
            teleportBotToSource(bot, ctx.getSource());
            bot.sayInChat("Teleported to your location!");
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Teleported " + bots.size() + " bots to your location!").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int tpPlayerToBot(CommandContext<CommandSourceStack> ctx, String botName) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Only players can teleport to a bot."));
            return 0;
        }
        AiBotPlayer bot = BotManager.getBot(botName);
        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("No active bot named '" + botName + "' found."));
            return 0;
        }
        ServerLevel botLevel = (ServerLevel) bot.level();
        if (player.level() != botLevel) {
            player.teleportTo(botLevel, bot.getX(), bot.getY(), bot.getZ(), java.util.Set.of(), player.getYRot(), player.getXRot(), true);
        } else {
            player.teleportTo(bot.getX(), bot.getY(), bot.getZ());
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Teleported to bot '" + bot.getBotName() + "'!").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int tpBot(CommandContext<CommandSourceStack> ctx, String name) {
        if ("all".equalsIgnoreCase(name)) {
            return tpAllBots(ctx);
        }
        AiBotPlayer bot = BotManager.getBot(name);
        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("No active bot named '" + name + "' found."));
            return 0;
        }
        teleportBotToSource(bot, ctx.getSource());
        bot.sayInChat("Teleported to your location!");
        return 1;
    }

    private static int trainGlobal(CommandContext<CommandSourceStack> ctx, String instruction) {
        LearnedRecipe recipe = BotMemoryManager.parseRecipeFromText(instruction);
        if (recipe != null) {
            BotMemoryManager.addGlobalRecipe(recipe);
            BotPersistence.saveAll(ctx.getSource().getServer());
            ctx.getSource().sendSuccess(() -> Component.literal("[Global Training] Learned new recipe: " + recipe.getSummary())
                    .withStyle(ChatFormatting.GREEN), true);
            for (AiBotPlayer bot : BotManager.getActiveBots()) {
                bot.sayInChat("I have updated my global memory with the new recipe: " + recipe.getSummary());
            }
            return 1;
        }

        BotMemoryManager.addGlobalMemory(instruction);
        BotPersistence.saveAll(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.literal("[Global Training] Added global memory rule: " + instruction)
                .withStyle(ChatFormatting.GREEN), true);
        for (AiBotPlayer bot : BotManager.getActiveBots()) {
            bot.sayInChat("Understood! I have adopted this team rule into my memory: \"" + instruction + "\"");
        }
        return 1;
    }

    private static int trainBot(CommandContext<CommandSourceStack> ctx, @Nullable String botName, String instruction) {
        ServerPlayer player = ctx.getSource().getPlayer();
        AiBotPlayer target = null;
        if (botName != null && !botName.isEmpty()) {
            target = BotManager.getBot(botName);
            if (target == null) {
                ctx.getSource().sendFailure(Component.literal("No active bot named '" + botName + "' found."));
                return 0;
            }
        } else if (player != null) {
            target = BotManager.findNearestBot(player, BotManager.getActiveBots());
        }

        if (target == null) {
            ctx.getSource().sendFailure(Component.literal("No bot found to train."));
            return 0;
        }

        boolean handled = target.handleTrainingChatCommand(instruction, player);
        if (!handled) {
            target.addLearnedMemory(instruction);
            target.sayInChat("Understood! I have recorded this personal instruction in my memory: \"" + instruction + "\"");
        }
        BotPersistence.saveAll(ctx.getSource().getServer());
        return 1;
    }

    private static int trainFromHeldBook(CommandContext<CommandSourceStack> ctx, @Nullable String botName, boolean isGlobal) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Only players can train bots from a held book."));
            return 0;
        }

        ItemStack held = player.getMainHandItem();
        if (!held.is(Items.WRITTEN_BOOK) && !held.is(Items.WRITABLE_BOOK)) {
            held = player.getOffhandItem();
        }
        if (!held.is(Items.WRITTEN_BOOK) && !held.is(Items.WRITABLE_BOOK)) {
            ctx.getSource().sendFailure(Component.literal("Please hold a Written Book or Book & Quill in your hand to train.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        AiBotPlayer targetBot = null;
        if (!isGlobal) {
            if (botName != null && !botName.isEmpty()) {
                targetBot = BotManager.getBot(botName);
                if (targetBot == null) {
                    ctx.getSource().sendFailure(Component.literal("No active bot named '" + botName + "' found."));
                    return 0;
                }
            } else {
                targetBot = BotManager.findNearestBot(player, BotManager.getActiveBots());
            }
        }

        BotMemoryManager.StudyResult res = BotMemoryManager.studyTrainingBook(held, targetBot, isGlobal);
        BotPersistence.saveAll(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.literal("Studied training book '" + res.title() + "'! Learned "
                + res.newGuidelines() + " guidelines and " + res.newRecipes() + " recipes (" + (res.global() ? "Global" : "Personal") + ").")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int listMemory(CommandContext<CommandSourceStack> ctx, @Nullable String targetName) {
        if ("global".equalsIgnoreCase(targetName)) {
            List<String> mems = BotMemoryManager.getGlobalMemories();
            List<LearnedRecipe> recipes = BotMemoryManager.getGlobalRecipes();
            ctx.getSource().sendSuccess(() -> Component.literal("=== Global Team Memories (" + mems.size() + ") ===")
                    .withStyle(ChatFormatting.GOLD), false);
            for (int i = 0; i < mems.size(); i++) {
                final int idx = i + 1;
                final String m = mems.get(i);
                ctx.getSource().sendSuccess(() -> Component.literal(idx + ". " + m).withStyle(ChatFormatting.YELLOW), false);
            }
            ctx.getSource().sendSuccess(() -> Component.literal("=== Global Custom Recipes (" + recipes.size() + ") ===")
                    .withStyle(ChatFormatting.GOLD), false);
            for (LearnedRecipe r : recipes) {
                ctx.getSource().sendSuccess(() -> Component.literal("- " + r.getSummary()).withStyle(ChatFormatting.AQUA), false);
            }
            return 1;
        }

        AiBotPlayer bot = null;
        if (targetName != null && !targetName.isEmpty()) {
            bot = BotManager.getBot(targetName);
            if (bot == null) {
                ctx.getSource().sendFailure(Component.literal("No active bot named '" + targetName + "' found."));
                return 0;
            }
        } else {
            ServerPlayer player = ctx.getSource().getPlayer();
            if (player != null) {
                bot = BotManager.findNearestBot(player, BotManager.getActiveBots());
            }
        }

        if (bot != null) {
            final AiBotPlayer targetBot = bot;
            List<String> mems = targetBot.getLearnedMemories();
            List<LearnedRecipe> recipes = targetBot.getLearnedRecipes();
            ctx.getSource().sendSuccess(() -> Component.literal("=== " + targetBot.getBotName() + "'s Personal Memories (" + mems.size() + ") ===")
                    .withStyle(ChatFormatting.GREEN), false);
            for (int i = 0; i < mems.size(); i++) {
                final int idx = i + 1;
                final String m = mems.get(i);
                ctx.getSource().sendSuccess(() -> Component.literal(idx + ". " + m).withStyle(ChatFormatting.WHITE), false);
            }
            ctx.getSource().sendSuccess(() -> Component.literal("=== " + targetBot.getBotName() + "'s Custom Recipes (" + recipes.size() + ") ===")
                    .withStyle(ChatFormatting.GREEN), false);
            for (LearnedRecipe r : recipes) {
                ctx.getSource().sendSuccess(() -> Component.literal("- " + r.getSummary()).withStyle(ChatFormatting.AQUA), false);
            }
        }

        List<String> gMems = BotMemoryManager.getGlobalMemories();
        List<LearnedRecipe> gRecipes = BotMemoryManager.getGlobalRecipes();
        ctx.getSource().sendSuccess(() -> Component.literal("=== Global Team Memories (" + gMems.size() + ") & Recipes (" + gRecipes.size() + ") ===")
                .withStyle(ChatFormatting.GOLD), false);
        for (LearnedRecipe r : gRecipes) {
            ctx.getSource().sendSuccess(() -> Component.literal("- " + r.getSummary()).withStyle(ChatFormatting.AQUA), false);
        }
        return 1;
    }

    private static int giveMemoryBook(CommandContext<CommandSourceStack> ctx, @Nullable String targetName) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Only players can receive memory books."));
            return 0;
        }

        ItemStack book;
        if ("global".equalsIgnoreCase(targetName)) {
            book = BotMemoryManager.createPhysicalMemoryBook(null, "Global Training Manual", "Team Admin");
        } else {
            AiBotPlayer bot = null;
            if (targetName != null && !targetName.isEmpty()) {
                bot = BotManager.getBot(targetName);
                if (bot == null) {
                    ctx.getSource().sendFailure(Component.literal("No active bot named '" + targetName + "' found."));
                    return 0;
                }
            } else {
                bot = BotManager.findNearestBot(player, BotManager.getActiveBots());
            }

            if (bot == null) {
                book = BotMemoryManager.createPhysicalMemoryBook(null, "Global Training Manual", "Team Admin");
            } else {
                book = BotMemoryManager.createPhysicalMemoryBook(bot, bot.getBotName() + "'s Memory Book", bot.getBotName());
            }
        }

        if (!player.getInventory().add(book)) {
            ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), book);
            drop.setDefaultPickUpDelay();
            player.level().addFreshEntity(drop);
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Given Memory Book into your inventory! Read or edit it anytime.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int clearMemory(CommandContext<CommandSourceStack> ctx, String targetName) {
        if ("global".equalsIgnoreCase(targetName)) {
            BotMemoryManager.getGlobalMemories().clear();
            BotMemoryManager.getGlobalRecipes().clear();
            BotPersistence.saveAll(ctx.getSource().getServer());
            ctx.getSource().sendSuccess(() -> Component.literal("Cleared all global memories and custom recipes.")
                    .withStyle(ChatFormatting.YELLOW), true);
            return 1;
        }

        AiBotPlayer bot = BotManager.getBot(targetName);
        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("No active bot named '" + targetName + "' found."));
            return 0;
        }

        bot.getLearnedMemories().clear();
        bot.getLearnedRecipes().clear();
        BotPersistence.saveAll(ctx.getSource().getServer());
        bot.sayInChat("My personal memory and custom recipes have been reset.");
        ctx.getSource().sendSuccess(() -> Component.literal("Cleared personal memory for '" + targetName + "'.")
                .withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int askBot(CommandContext<CommandSourceStack> ctx, String name, String request) {
        ServerPlayer speaker = ctx.getSource().getPlayer();
        if ("all".equalsIgnoreCase(name)) {
            for (AiBotPlayer b : BotManager.getActiveBots()) {
                BotManager.dispatchRequestToBot(b, speaker, request);
            }
            return 1;
        }
        AiBotPlayer bot = BotManager.getBot(name);
        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("No active bot named '" + name + "' found."));
            return 0;
        }
        BotManager.dispatchRequestToBot(bot, speaker, request);
        return 1;
    }

    private static int setApiKey(CommandContext<CommandSourceStack> ctx, String key) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player != null) {
            BotConfig.setPlayerApiKey(player.getPlainTextName(), key);
            ctx.getSource().sendSuccess(() -> Component.literal("Saved personal Gemini API Key for " + player.getPlainTextName() + " (and added to server pool)!")
                    .withStyle(ChatFormatting.GREEN), false);
        } else {
            BotConfig.setApiKey(key);
            ctx.getSource().sendSuccess(() -> Component.literal("Server default Gemini API Key saved!")
                    .withStyle(ChatFormatting.GREEN), false);
        }
        return 1;
    }

    private static int setModel(CommandContext<CommandSourceStack> ctx, String modelName) {
        BotConfig.setModel(modelName);
        ctx.getSource().sendSuccess(() -> Component.literal("Gemini model updated to: " + BotConfig.getModel())
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int showGeneralHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> Component.literal("==================== AI COMPANION COMMANDS ====================").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        src.sendSuccess(() -> Component.literal("/bot spawn <name> [role]")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Spawn bot (farmer, lumberjack, miner, guard, builder, conductor, husbandry, courier)").withStyle(ChatFormatting.GRAY)), false);
        src.sendSuccess(() -> Component.literal("/bot list")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - List all active bots and current tasks").withStyle(ChatFormatting.GRAY)), false);
        src.sendSuccess(() -> Component.literal("/bot status [name]")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - View bot health, hunger, inventory, farm/ranch zones").withStyle(ChatFormatting.GRAY)), false);
        src.sendSuccess(() -> Component.literal("/bot gui [name]")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Open interactive Speedometer Dashboard & Inventory").withStyle(ChatFormatting.GRAY)), false);
        src.sendSuccess(() -> Component.literal("/bot stop <name|all>")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Stop bot's current task and stand by").withStyle(ChatFormatting.GRAY)), false);
        src.sendSuccess(() -> Component.literal("/bot tp [all|<name>|to <name>]")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Teleport nearest, all, or specific bot (cross-dimension)").withStyle(ChatFormatting.GRAY)), false);
        src.sendSuccess(() -> Component.literal("/bot rename [name] <nickname>")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Set companion nickname or alias").withStyle(ChatFormatting.GRAY)), false);
        src.sendSuccess(() -> Component.literal("/bot train [name] <instruction>")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Teach bot a custom recipe, rule, or guideline").withStyle(ChatFormatting.GRAY)), false);
        src.sendSuccess(() -> Component.literal("/bot memory book [global|<name>]")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Export an in-game Written Book of recorded recipes & rules").withStyle(ChatFormatting.GRAY)), false);
        src.sendSuccess(() -> Component.literal("/bot wand")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Obtain Surveyor Wand to map farm, ranch, or base boundaries").withStyle(ChatFormatting.GRAY)), false);
        src.sendSuccess(() -> Component.literal("/bot area <list|height|show|remove>")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Manage surveyed 3D compound areas").withStyle(ChatFormatting.GRAY)), false);
        src.sendSuccess(() -> Component.literal("/bot blueprint <capture|list|info|build>")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Scan in-world buildings and order builders to remake them").withStyle(ChatFormatting.GRAY)), false);
        src.sendSuccess(() -> Component.literal("/bot skin <list|reload|set>")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - Manage custom player skins in /aicompanion/skins").withStyle(ChatFormatting.GRAY)), false);
        src.sendSuccess(() -> Component.literal("/bot apikey [key]")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(" - View setup guide or configure personal Gemini API key").withStyle(ChatFormatting.GRAY)), false);
        src.sendSuccess(() -> Component.literal("Voice Tip: ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal("Speak directly to bots in public chat! e.g. \"lumber chop all trees in this 100x100 area\", \"goku dig 10x10x2 box around us starting y81 go up 2\", \"explorer find ocean\".").withStyle(ChatFormatting.AQUA)), false);
        return 1;
    }

    private static int showApiKeyHelp(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        String pName = player != null ? player.getPlainTextName() : "Server";
        boolean hasKey = !BotConfig.getCandidateApiKeys(pName).isEmpty();

        ctx.getSource().sendSuccess(() -> Component.literal("=== AI Companion: API Key Setup ===\n")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                .append(Component.literal("AI Engine Status: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(hasKey ? "Connected to Cloud Gemini (Full AI Personality)\n" : "Offline Tactical Mode (Zero Setup Needed)\n")
                        .withStyle(hasKey ? ChatFormatting.GREEN : ChatFormatting.YELLOW))
                .append(Component.literal("\nGet a 100% FREE Gemini API Key in 30 seconds (no credit card needed):\n").withStyle(ChatFormatting.WHITE))
                .append(Component.literal("1. Visit: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("https://aistudio.google.com/app/apikey\n").withStyle(ChatFormatting.AQUA, ChatFormatting.UNDERLINE))
                .append(Component.literal("2. Sign in with Google and click 'Create API Key'.\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("3. In chat, run: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("/bot apikey <your_key_here>\n\n").withStyle(ChatFormatting.GREEN))
                .append(Component.literal("Note: Even with NO key, companions will still farm, breed, mine, guard, craft, and build offline!")
                        .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC)), false);
        return 1;
    }

    private static int listSkinsCmd(CommandContext<CommandSourceStack> ctx) {
        com.aicompanion.bot.skin.BotSkinManager.ensureDirectoriesExist();
        var skins = com.aicompanion.bot.skin.BotSkinManager.getAvailableSkinNames();
        if (skins.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("No custom skins found in /aicompanion/skins.\n")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal("Drop any 64x64 .png skin files into: " + com.aicompanion.bot.skin.BotSkinManager.getSkinsDirectory())
                            .withStyle(ChatFormatting.GRAY)), false);
            return 0;
        }

        ctx.getSource().sendSuccess(() -> Component.literal("=== AI Companion Custom Skins (" + skins.size() + " detected) ===\n")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (String s : skins) {
            boolean isSlim = s.contains("slim") || s.contains("alex");
            String model = isSlim ? "Slim (Alex 3px)" : "Wide (Steve 4px)";
            ctx.getSource().sendSuccess(() -> Component.literal("• ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(s).withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" [" + model + "]").withStyle(ChatFormatting.GRAY)), false);
        }
        return skins.size();
    }

    private static int reloadSkinsCmd(CommandContext<CommandSourceStack> ctx) {
        com.aicompanion.bot.skin.BotSkinManager.ensureDirectoriesExist();
        com.aicompanion.bot.skin.BotSkinManager.scanSkins();
        int count = com.aicompanion.bot.skin.BotSkinManager.getAvailableSkinNames().size();

        if (net.neoforged.fml.loading.FMLEnvironment.getDist().isClient()) {
            com.aicompanion.bot.client.BotClientSkinManager.loadAllSkins();
        }

        for (AiBotPlayer bot : BotManager.getActiveBots()) {
            if (bot.getCustomSkinName().isBlank()) {
                String assigned = com.aicompanion.bot.skin.BotSkinManager.determineSkinForBot(bot.getBotName(), bot.getBotRole(), bot.getAliases(), bot.getUUID());
                bot.setCustomSkinName(assigned);
            }
            if (!bot.getCustomSkinName().isBlank()) {
                net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(new com.aicompanion.bot.skin.SyncBotSkinPayload(bot.getUUID(), bot.getCustomSkinName()));
            }
        }

        ctx.getSource().sendSuccess(() -> Component.literal("Reloaded custom companion skins! Found " + count + " skins across role categories.")
                .withStyle(ChatFormatting.GREEN), true);
        return count;
    }

    private static int setBotSkinCmd(CommandContext<CommandSourceStack> ctx, String botName, String skinName) {
        AiBotPlayer bot = BotManager.getBot(botName);
        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("Companion '" + botName + "' not found."));
            return 0;
        }

        bot.setCustomSkinName(skinName);
        BotPersistence.saveAll(ctx.getSource().getServer());
        net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(new com.aicompanion.bot.skin.SyncBotSkinPayload(bot.getUUID(), skinName));

        ctx.getSource().sendSuccess(() -> Component.literal("Updated " + bot.getBotName() + "'s skin to '" + skinName + "'!")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int setBotNicknameNearest(CommandContext<CommandSourceStack> ctx, String nickname) {
        ServerPlayer player = ctx.getSource().getPlayer();
        AiBotPlayer nearest = null;
        if (player != null) {
            nearest = BotManager.findNearestBot(player, BotManager.getActiveBots());
        } else if (!BotManager.getActiveBots().isEmpty()) {
            nearest = BotManager.getActiveBots().iterator().next();
        }
        if (nearest == null) {
            ctx.getSource().sendFailure(Component.literal("No active companion bots found nearby."));
            return 0;
        }
        return setBotNicknameDirect(ctx.getSource(), nearest, nickname);
    }

    private static int setBotNickname(CommandContext<CommandSourceStack> ctx, String botName, String nickname) {
        AiBotPlayer bot = BotManager.getBot(botName);
        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("No active bot named '" + botName + "' found."));
            return 0;
        }
        return setBotNicknameDirect(ctx.getSource(), bot, nickname);
    }

    private static int setBotNicknameDirect(CommandSourceStack source, AiBotPlayer bot, String nickname) {
        String clean = nickname.trim();
        bot.addAlias(clean);
        bot.sayInChat("My nickname is now '" + clean + "'! I will respond to this name.");
        BotPersistence.saveAll(bot.level().getServer());
        source.sendSuccess(() -> Component.literal("Set nickname for " + bot.getBotName() + " to '" + clean + "'!")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }
}
