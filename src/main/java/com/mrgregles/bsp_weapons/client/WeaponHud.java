package com.mrgregles.bsp_weapons.client;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.item.WeaponItem;
import com.mrgregles.bsp_weapons.parts.Sheet;
import com.mrgregles.bsp_weapons.parts.Stat;
import com.mrgregles.bsp_weapons.weapon.Firing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;

/**
 * The weapon HUD (D47): while a BSP weapon is held, a heat bar at the bottom right and the charge at the bottom left,
 * above the hotbar's corners and clear of the vanilla bars (placed from Forge's own heights). OVERHEATED or RELOADING
 * shows over the heat bar while it applies. Nothing is drawn with no weapon in hand.
 */
public final class WeaponHud {
    private static final int TEAL = 0xFF19D3B0, RED = 0xFFFF6B5C, DIM = 0xFF2A2F3A, SLOT = 0xCC0C0E12, MUTED = 0xFF9AA3B5, TEXT = 0xFFE6EBF2;

    private static Object lastTag;
    private static long lastTick = Long.MIN_VALUE;
    private static double share;
    private static boolean locked, reloading, energy, empty;
    private static int capacity, charge;
    private static String HEAT, OVERHEATED, RELOADING, NO_CHARGE, NO_ROUNDS;

    private WeaponHud() {
    }

    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.EXPERIENCE_BAR.id(), "weapon", WeaponHud::draw); // after health, armour, food, air and mount bars, so Forge's heights are final
    }

    private static void draw(ForgeGui gui, GuiGraphics g, float partial, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof WeaponItem)) stack = mc.player.getOffhandItem();
        if (!(stack.getItem() instanceof WeaponItem)) return;
        long now = mc.level.getGameTime();
        if (stack.getTag() != lastTag || now != lastTick) { // the sheet and the weapon's state once a tick, not once a frame
            lastTag = stack.getTag(); lastTick = now;
            Sheet sheet = Firing.sheetOf(stack);
            if (sheet == null) { lastTag = null; return; }
            double cap = Math.max(1, sheet.get(Stat.HEAT_CAP));
            share = Math.min(1, Firing.heat(stack, now) / cap);
            locked = Firing.locked(stack, now); reloading = Firing.reloading(stack, now);
            capacity = (int) Math.round(sheet.get(Stat.CAPACITY)); charge = reloading ? 0 : Firing.shots(stack);
            energy = com.mrgregles.bsp_weapons.weapon.WeaponEnergy.holdsEnergy(stack);
            empty = charge == 0 && !reloading && (energy || Firing.roundsCarried(mc.player) == 0);
            if (HEAT == null) {
                HEAT = Component.translatable("hud." + BspWeapons.MODID + ".heat").getString();
                OVERHEATED = Component.translatable("hud." + BspWeapons.MODID + ".overheated").getString();
                RELOADING = Component.translatable("hud." + BspWeapons.MODID + ".reloading").getString();
                NO_CHARGE = Component.translatable("hud." + BspWeapons.MODID + ".no_charge").getString();
                NO_ROUNDS = Component.translatable("hud." + BspWeapons.MODID + ".no_rounds").getString();
            }
        }
        int bottom = height - Math.max(gui.rightHeight, gui.leftHeight) - 4;
        // heat, bottom right, kept inside a narrow window
        int bw = 80, bx = Math.min(width / 2 + 95, width - bw - 4), by = bottom - 10;
        g.drawString(mc.font, HEAT, bx, by - 10, MUTED, false);
        g.fill(bx, by, bx + bw, by + 6, SLOT);
        frame(g, bx, by, bw, 6, DIM);
        g.fill(bx + 1, by + 1, bx + 1 + (int) Math.round((bw - 2) * share), by + 5, share > 0.75 || locked ? RED : TEAL);
        if (locked || reloading || empty) {
            String word = locked ? OVERHEATED : reloading ? RELOADING : energy ? NO_CHARGE : NO_ROUNDS;
            if ((now / 4) % 2 == 0 || reloading || empty) g.drawString(mc.font, word, bx + bw - mc.font.width(word), by - 20, locked || empty ? RED : TEAL, false);
        }
        // charge, bottom left
        String count = String.valueOf(charge), of = " / " + capacity;
        int cx = Math.max(4, width / 2 - 95 - mc.font.width(count + of));
        g.drawString(mc.font, count, cx, by - 4, charge == 0 ? RED : TEXT, false);
        g.drawString(mc.font, of, cx + mc.font.width(count), by - 4, MUTED, false);
    }

    private static void frame(GuiGraphics g, int x, int y, int w, int h, int colour) {
        g.fill(x, y, x + w, y + 1, colour);
        g.fill(x, y + h - 1, x + w, y + h, colour);
        g.fill(x, y, x + 1, y + h, colour);
        g.fill(x + w - 1, y, x + w, y + h, colour);
    }
}
