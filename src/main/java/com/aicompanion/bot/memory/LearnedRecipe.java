package com.aicompanion.bot.memory;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import com.aicompanion.bot.BotPersistence;

public class LearnedRecipe {
    public String name = "";
    public String resultItemId = "";
    public int resultCount = 1;
    public Map<String, Integer> ingredients = new LinkedHashMap<>();
    public boolean global = true;
    public String description = "";

    public LearnedRecipe() {}

    public LearnedRecipe(String name, String resultItemId, int resultCount, Map<String, Integer> ingredients, boolean global, String description) {
        this.name = name != null ? name.trim().toLowerCase(Locale.ROOT) : "";
        this.resultItemId = resultItemId != null ? resultItemId.trim().toLowerCase(Locale.ROOT) : "";
        this.resultCount = Math.max(1, resultCount);
        if (ingredients != null) {
            this.ingredients.putAll(ingredients);
        }
        this.global = global;
        this.description = description != null ? description.trim() : "";
    }

    public boolean matches(String query) {
        if (query == null || query.isBlank()) return false;
        String q = query.toLowerCase(Locale.ROOT).trim().replace("minecraft:", "").replace(" ", "_");
        String r = resultItemId.toLowerCase(Locale.ROOT).replace("minecraft:", "").replace(" ", "_");
        String n = name.toLowerCase(Locale.ROOT).replace(" ", "_");
        return q.equals(r) || q.equals(n) || r.contains(q) || n.contains(q) || q.contains(r);
    }

    public @Nullable ItemStack createResult() {
        String cleanId = resultItemId.contains(":") ? resultItemId : "minecraft:" + resultItemId;
        ItemStack stack = BotPersistence.resolveStack(cleanId, resultCount);
        if (!stack.isEmpty()) {
            return stack;
        }
        for (Item it : BuiltInRegistries.ITEM) {
            String path = BuiltInRegistries.ITEM.getKey(it).getPath();
            if (path.equalsIgnoreCase(resultItemId) || path.equalsIgnoreCase(name)) {
                return new ItemStack(it, resultCount);
            }
        }
        return null;
    }

    public String getSummary() {
        return toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(resultCount).append("x ").append(resultItemId).append(" <= (");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : ingredients.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getValue()).append("x ").append(entry.getKey());
            first = false;
        }
        sb.append(")");
        if (!description.isBlank()) sb.append(" [").append(description).append("]");
        return sb.toString();
    }
}
