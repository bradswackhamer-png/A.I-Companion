package com.aicompanion.bot;

import java.util.List;

public class BotAction {
    public String type = "";
    public String target = "";
    public String item = "";
    public int count = 1;
    public Integer x = null;
    public Integer y = null;
    public Integer z = null;
    public double distance = 3.0;

    public BotAction() {
    }

    public BotAction(String type, String target, String item, int count) {
        this.type = type != null ? type.toUpperCase().trim() : "";
        this.target = target != null ? target.trim() : "";
        this.item = item != null ? item.trim() : "";
        this.count = Math.max(1, count);
    }

    public String getNormalizedType() {
        return type != null ? type.toUpperCase().trim() : "";
    }

    public String getPrimaryTarget() {
        if (target != null && !target.isBlank()) {
            return target.trim();
        }
        if (item != null && !item.isBlank()) {
            return item.trim();
        }
        return "";
    }

    @Override
    public String toString() {
        String t = getNormalizedType();
        return switch (t) {
            case "GOTO" -> (x != null && y != null && z != null)
                    ? "Go to (" + x + ", " + y + ", " + z + ")"
                    : "Go to " + getPrimaryTarget();
            case "FOLLOW" -> "Follow " + getPrimaryTarget();
            case "GUARD" -> "Guard " + (getPrimaryTarget().isEmpty() ? "area" : getPrimaryTarget());
            case "MINE" -> "Mine " + count + "x " + getPrimaryTarget();
            case "ATTACK" -> "Attack " + count + "x " + (getPrimaryTarget().isEmpty() ? "hostiles" : getPrimaryTarget());
            case "CRAFT" -> "Craft " + count + "x " + getPrimaryTarget();
            case "GIVE" -> "Give " + count + "x " + (item != null && !item.isBlank() ? item : target) + " to " + target;
            case "EQUIP" -> "Equip " + (getPrimaryTarget().isEmpty() ? "best gear" : getPrimaryTarget());
            case "PLACE", "BUILD" -> "Build/Place " + getPrimaryTarget();
            case "CHEST_STORE" -> "Store items in chest";
            case "CHEST_TAKE" -> "Take " + count + "x " + getPrimaryTarget() + " from chest";
            case "FARM" -> "Farm " + (getPrimaryTarget().isEmpty() ? "mature crops" : getPrimaryTarget());
            case "EAT" -> "Eat food";
            case "STOP" -> "Stop";
            default -> t + " " + getPrimaryTarget();
        };
    }

    public static class GeminiPlanResponse {
        public String thought = "";
        public String reply = "";
        public List<BotAction> actions = List.of();
    }
}
