package com.aicompanion.bot.memory;

import com.aicompanion.bot.AiBotPlayer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import org.jspecify.annotations.Nullable;

public class BotMemoryManager {
    private static final List<String> GLOBAL_MEMORIES = Collections.synchronizedList(new ArrayList<>());
    private static final List<LearnedRecipe> GLOBAL_RECIPES = Collections.synchronizedList(new ArrayList<>());

    public static List<String> getGlobalMemories() {
        synchronized (GLOBAL_MEMORIES) {
            return new ArrayList<>(GLOBAL_MEMORIES);
        }
    }

    public static void setGlobalMemories(List<String> list) {
        synchronized (GLOBAL_MEMORIES) {
            GLOBAL_MEMORIES.clear();
            if (list != null) {
                for (String s : list) {
                    if (s != null && !s.isBlank()) GLOBAL_MEMORIES.add(s.trim());
                }
            }
        }
    }

    public static boolean addGlobalMemory(String memory) {
        if (memory == null || memory.isBlank()) return false;
        String clean = memory.trim();
        synchronized (GLOBAL_MEMORIES) {
            if (!GLOBAL_MEMORIES.contains(clean)) {
                GLOBAL_MEMORIES.add(clean);
                return true;
            }
        }
        return false;
    }

    public static boolean removeGlobalMemory(int index) {
        synchronized (GLOBAL_MEMORIES) {
            if (index >= 0 && index < GLOBAL_MEMORIES.size()) {
                GLOBAL_MEMORIES.remove(index);
                return true;
            }
        }
        return false;
    }

    public static void clearGlobalMemories() {
        synchronized (GLOBAL_MEMORIES) {
            GLOBAL_MEMORIES.clear();
        }
    }

    public static List<LearnedRecipe> getGlobalRecipes() {
        synchronized (GLOBAL_RECIPES) {
            return new ArrayList<>(GLOBAL_RECIPES);
        }
    }

    public static void setGlobalRecipes(List<LearnedRecipe> list) {
        synchronized (GLOBAL_RECIPES) {
            GLOBAL_RECIPES.clear();
            if (list != null) {
                GLOBAL_RECIPES.addAll(list);
            }
        }
    }

    public static void addGlobalRecipe(LearnedRecipe recipe) {
        if (recipe == null || recipe.resultItemId.isBlank()) return;
        synchronized (GLOBAL_RECIPES) {
            GLOBAL_RECIPES.removeIf(r -> r.matches(recipe.resultItemId) || r.name.equalsIgnoreCase(recipe.name));
            GLOBAL_RECIPES.add(recipe);
        }
    }

    public static boolean removeGlobalRecipe(String nameOrItem) {
        if (nameOrItem == null || nameOrItem.isBlank()) return false;
        synchronized (GLOBAL_RECIPES) {
            return GLOBAL_RECIPES.removeIf(r -> r.matches(nameOrItem));
        }
    }

    public static void clearGlobalRecipes() {
        synchronized (GLOBAL_RECIPES) {
            GLOBAL_RECIPES.clear();
        }
    }

    public static @Nullable LearnedRecipe parseRecipeFromText(String text) {
        return parseRecipeFromText(text, true);
    }

    public static @Nullable LearnedRecipe parseRecipeFromText(String text, boolean isGlobal) {
        if (text == null || text.isBlank()) return null;
        String s = text.toLowerCase(Locale.ROOT).trim();

        String resultItem = null;
        int resultCount = 1;

        // 1. Result Item Extraction
        Pattern resultPattern = Pattern.compile(
                "(?:craft|make|create|produce)\\s+(?:(?:a|an)\\s+)?(?:(\\d+)\\s+)?([a-zA-Z0-9_]+(?:\\s+[a-zA-Z0-9_]+)?)");
        Matcher rm = resultPattern.matcher(s);
        if (rm.find()) {
            if (rm.group(1) != null) {
                try { resultCount = Integer.parseInt(rm.group(1)); } catch (Exception ignored) {}
            }
            resultItem = rm.group(2).trim();
        } else {
            Pattern altPattern = Pattern.compile("(?:recipe for|to craft|to make)\\s+(?:(?:a|an)\\s+)?(?:(\\d+)\\s+)?([a-zA-Z0-9_]+(?:\\s+[a-zA-Z0-9_]+)?)");
            Matcher am = altPattern.matcher(s);
            if (am.find()) {
                if (am.group(1) != null) {
                    try { resultCount = Integer.parseInt(am.group(1)); } catch (Exception ignored) {}
                }
                resultItem = am.group(2).trim();
            }
        }

        if (resultItem == null) return null;

        // Clean up resultItem name
        resultItem = normalizeItemName(resultItem);

        // 2. Ingredients Extraction (e.g. "9 wheat", "3 iron ingots and 2 sticks", "1 in each spot")
        Map<String, Integer> ingredients = new LinkedHashMap<>();

        // Match "<count> <item>"
        Pattern ingPattern = Pattern.compile("(\\d+)\\s*(?:x|of)?\\s+([a-zA-Z0-9_]+(?:\\s+[a-zA-Z0-9_]+)?)");
        Matcher im = ingPattern.matcher(s);
        while (im.find()) {
            int count;
            try {
                count = Integer.parseInt(im.group(1));
            } catch (Exception e) {
                continue;
            }
            String rawItem = im.group(2).trim();
            String normItem = normalizeItemName(rawItem);

            // Skip words like "spots", "blocks", "times", or the result itself if matched
            if (normItem.equalsIgnoreCase("spot") || normItem.equalsIgnoreCase("spots")
                    || normItem.equalsIgnoreCase("each_spot") || normItem.equalsIgnoreCase("slot")
                    || normItem.equalsIgnoreCase("block") || normItem.equalsIgnoreCase("blocks")
                    || normItem.equalsIgnoreCase(resultItem)) {
                continue;
            }

            if (isValidIngredientCandidate(normItem)) {
                ingredients.put(normItem, ingredients.getOrDefault(normItem, 0) + count);
            }
        }

        // Special case: "place 9 wheat in the crafting table, 1 in each spot. Craft a hay bay"
        if (ingredients.isEmpty() && s.contains("wheat")) {
            Pattern wheatPattern = Pattern.compile("(\\d+)\\s+wheat");
            Matcher wm = wheatPattern.matcher(s);
            if (wm.find()) {
                ingredients.put("wheat", Integer.parseInt(wm.group(1)));
            }
        }

        if (ingredients.isEmpty()) {
            return null;
        }

        return new LearnedRecipe(resultItem, resultItem, resultCount, ingredients, isGlobal, text.trim());
    }

    public static String normalizeItemName(String name) {
        String clean = name.trim().toLowerCase(Locale.ROOT).replace("minecraft:", "").replace(" ", "_");
        if (clean.endsWith("s") && !clean.endsWith("ss") && !clean.equals("glass")) {
            String singular = clean.substring(0, clean.length() - 1);
            if (BuiltInRegistries.ITEM.containsKey(Identifier.tryParse("minecraft:" + singular))) {
                clean = singular;
            }
        }
        if (clean.equals("hay") || clean.equals("hay_bale") || clean.equals("hay_bay") || clean.equals("hay_block")) {
            return "hay_block";
        }
        if (clean.equals("plank") || clean.equals("planks") || clean.equals("wood")) {
            return "oak_planks";
        }
        if (clean.equals("table") || clean.equals("crafting_table")) {
            return "crafting_table";
        }
        if (clean.equals("bread")) {
            return "bread";
        }
        return clean;
    }

    private static boolean isValidIngredientCandidate(String item) {
        if (item.isBlank()) return false;
        if (item.equals("crafting_table") || item.equals("table") || item.equals("the_crafting_table")) return false;
        if (item.equals("inventory") || item.equals("spot") || item.equals("spots") || item.equals("each")) return false;
        return true;
    }

    public static List<String> extractTextFromBook(ItemStack bookStack) {
        List<String> lines = new ArrayList<>();
        if (bookStack.has(DataComponents.WRITTEN_BOOK_CONTENT)) {
            WrittenBookContent content = bookStack.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (content != null) {
                for (Component page : content.getPages(false)) {
                    String text = page.getString();
                    for (String l : text.split("\\r?\\n")) {
                        if (!l.isBlank()) lines.add(l.trim());
                    }
                }
            }
        } else if (bookStack.has(DataComponents.WRITABLE_BOOK_CONTENT)) {
            WritableBookContent content = bookStack.get(DataComponents.WRITABLE_BOOK_CONTENT);
            if (content != null) {
                content.getPages(false).forEach(page -> {
                    for (String l : page.split("\\r?\\n")) {
                        if (!l.isBlank()) lines.add(l.trim());
                    }
                });
            }
        }
        return lines;
    }

    public record StudyResult(String title, int newGuidelines, int newRecipes, boolean global) {}

    public static StudyResult studyTrainingBook(ItemStack bookStack, @Nullable AiBotPlayer targetBot, boolean forceGlobal) {
        String title = "Training Manual";
        if (bookStack.has(DataComponents.WRITTEN_BOOK_CONTENT)) {
            WrittenBookContent c = bookStack.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (c != null && c.title() != null) {
                title = c.title().raw();
            }
        }

        boolean isGlobal = forceGlobal || title.toLowerCase(Locale.ROOT).contains("global");
        List<String> lines = extractTextFromBook(bookStack);
        int learnedGuidelines = 0;
        int learnedRecipes = 0;

        for (String line : lines) {
            String clean = line.trim();
            if (clean.startsWith("•") || clean.startsWith("-") || clean.startsWith("*")) {
                clean = clean.substring(1).trim();
            }
            if (clean.isBlank()) continue;

            LearnedRecipe recipe = parseRecipeFromText(clean, isGlobal);
            if (recipe != null) {
                if (isGlobal || targetBot == null) {
                    addGlobalRecipe(recipe);
                } else {
                    targetBot.addLearnedRecipe(recipe);
                }
                learnedRecipes++;
            } else {
                if (isGlobal || targetBot == null) {
                    if (addGlobalMemory(clean)) learnedGuidelines++;
                } else {
                    if (targetBot.addLearnedMemory(clean)) learnedGuidelines++;
                }
            }
        }

        return new StudyResult(title, learnedGuidelines, learnedRecipes, isGlobal);
    }

    public static ItemStack createPhysicalMemoryBook(@Nullable AiBotPlayer bot, String title, String author) {
        List<String> guidelines = bot != null ? bot.getLearnedMemories() : getGlobalMemories();
        List<LearnedRecipe> recipes = bot != null ? bot.getLearnedRecipes() : getGlobalRecipes();
        return createPhysicalMemoryBook(title, author, guidelines, recipes);
    }

    public static ItemStack createPhysicalMemoryBook(String title, String author, List<String> guidelines, List<LearnedRecipe> recipes) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> pages = new ArrayList<>();

        StringBuilder page = new StringBuilder();
        page.append("=== ").append(title.toUpperCase(Locale.ROOT)).append(" ===\n\n");
        page.append("Author: ").append(author).append("\n\n");
        page.append("Guidelines & Trained Recipes recorded for autonomous companion operation.\n");
        pages.add(Filterable.passThrough(Component.literal(page.toString())));
        page.setLength(0);

        if (!recipes.isEmpty()) {
            page.append("--- LEARNED RECIPES ---\n\n");
            for (LearnedRecipe r : recipes) {
                page.append("• ").append(r.resultCount).append("x ").append(r.resultItemId).append(":\n  ");
                boolean first = true;
                for (Map.Entry<String, Integer> ing : r.ingredients.entrySet()) {
                    if (!first) page.append(", ");
                    page.append(ing.getValue()).append("x ").append(ing.getKey());
                    first = false;
                }
                page.append("\n\n");
                if (page.length() > 200) {
                    pages.add(Filterable.passThrough(Component.literal(page.toString())));
                    page.setLength(0);
                }
            }
            if (page.length() > 0) {
                pages.add(Filterable.passThrough(Component.literal(page.toString())));
                page.setLength(0);
            }
        }

        if (!guidelines.isEmpty()) {
            page.append("--- GUIDELINES & RULES ---\n\n");
            for (String g : guidelines) {
                page.append("• ").append(g).append("\n\n");
                if (page.length() > 200) {
                    pages.add(Filterable.passThrough(Component.literal(page.toString())));
                    page.setLength(0);
                }
            }
            if (page.length() > 0) {
                pages.add(Filterable.passThrough(Component.literal(page.toString())));
                page.setLength(0);
            }
        }

        if (pages.isEmpty()) {
            pages.add(Filterable.passThrough(Component.literal("No memory guidelines or custom recipes recorded yet.")));
        }

        WrittenBookContent content = new WrittenBookContent(
                Filterable.passThrough(title),
                author,
                0,
                pages,
                true
        );
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        return book;
    }
}
