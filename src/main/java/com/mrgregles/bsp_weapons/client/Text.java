package com.mrgregles.bsp_weapons.client;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/** Text that fits its box: cut with "..." when too long. Worked out once per change, never every frame. */
public final class Text {
    private Text() {
    }

    public static String cut(Font font, String text, int width) {
        if (font.width(text) <= width) return text;
        String dots = "...";
        int room = width - font.width(dots);
        if (room <= 0) return dots;
        return font.plainSubstrByWidth(text, room) + dots;
    }

    public static String cut(Font font, Component text, int width) {
        return cut(font, text.getString(), width);
    }
}
