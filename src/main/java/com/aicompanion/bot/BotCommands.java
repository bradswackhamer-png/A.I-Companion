package com.aicompanion.bot;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

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

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(buildCommandTree("bot"));
        dispatcher.register(buildCommandTree("aibot"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCommandTree(String rootLiteral) {
        return Commands.literal(rootLiteral)
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
                        .then(Commands.literal("assign")
                                .then(Commands.argument("botName", StringArgumentType.word())
                                        .suggests(BOT_NAME_SUGGESTIONS)
                                        .then(Commands.argument("areaName", StringArgumentType.word())
                                                .suggests(AREA_SUGGESTIONS)
                                                .executes(ctx -> assignAreaCmd(ctx,
                                                        StringArgumentType.getString(ctx, "botName"),
                                                        StringArgumentType.getString(ctx, "areaName")))))));
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
        area.spawnPerimeterParticles(ctx.getSource().getLevel(), net.minecraft.core.particles.ParticleTypes.END_ROD);
        ctx.getSource().sendSuccess(() -> Component.literal("Illuminated boundary for area '" + area.name + "' with glowing perimeter particles!").withStyle(ChatFormatting.AQUA), false);
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
            extra.append(String.format("Farm Zone: (%d, %d, %d) %dx%d | Crops: %s\n",
                    bot.getFarmOriginPos().getX(), bot.getFarmOriginPos().getY(), bot.getFarmOriginPos().getZ(),
                    bot.getFarmRadiusX() * 2, bot.getFarmRadiusZ() * 2,
                    bot.getFarmAllowedCrops().isEmpty() ? "all" : String.join(", ", bot.getFarmAllowedCrops())));
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

    private static int tpBot(CommandContext<CommandSourceStack> ctx, String name) {
        AiBotPlayer bot = BotManager.getBot(name);
        if (bot == null) {
            ctx.getSource().sendFailure(Component.literal("No active bot named '" + name + "' found."));
            return 0;
        }
        Vec3 pos = ctx.getSource().getPosition();
        bot.teleportTo(pos.x + 1.0, pos.y, pos.z + 1.0);
        bot.sayInChat("Teleported to your location!");
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
}
