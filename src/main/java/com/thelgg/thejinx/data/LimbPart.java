package com.thelgg.thejinx.data;

public enum LimbPart {
    HEAD("head"),
    LEFT_ARM("left_arm"),
    RIGHT_ARM("right_arm"),
    LEFT_LEG("left_leg"),
    RIGHT_LEG("right_leg");

    private final String id;

    LimbPart(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static LimbPart fromId(String id) {
        for (LimbPart part : values()) {
            if (part.id.equals(id)) {
                return part;
            }
        }
        return null;
    }
}
