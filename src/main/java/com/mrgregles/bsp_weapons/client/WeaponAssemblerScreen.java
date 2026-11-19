package com.mrgregles.bsp_weapons.client;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.assembler.Assembly;
import com.mrgregles.bsp_weapons.assembler.WeaponAssemblerMenu;
import com.mrgregles.bsp_weapons.item.WeaponItem;
import com.mrgregles.bsp_weapons.network.AssemblerActionPacket;
import com.mrgregles.bsp_weapons.network.WeaponsNetwork;
import com.mrgregles.bsp_weapons.parts.Catalyst;
import com.mrgregles.bsp_weapons.parts.PartKind;
import com.mrgregles.bsp_weapons.parts.Rarity;
import com.mrgregles.bsp_weapons.parts.Sheet;
import com.mrgregles.bsp_weapons.parts.SheetCalculator;
import com.mrgregles.bsp_weapons.parts.Stat;
import com.mrgregles.bsp_weapons.parts.WeaponSpec;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

/**
 * The assembler's screen, the Rail layout (D52): slots where the parts go, the weapon's sheet as bars (D38), the roll
 * odds, ASSEMBLE and TAKE APART drawn as the mod's own keys (no vanilla button), BUILD and PLAN tabs (PLAN comes later).
 * 256 x 232, which fits a 320 x 240 window at GUI scale 1. Text is cut to its box when it changes, not every frame.
 */
public class WeaponAssemblerScreen extends AbstractContainerScreen<WeaponAssemblerMenu> {
    private static final int PANEL = 0xFF10151C, SLOT = 0xFF0C0E12, DIM = 0xFF2A2F3A, MUTED = 0xFF9AA3B5, TEXT = 0xFFE6EBF2, TEAL = 0xFF19D3B0, GOLD = 0xFFFFD23A, VIOLET = 0xFFB58CFF, RED = 0xFFFF6B5C, FAINT = 0xFF3A4150;
    private static final Stat[] BARS = {Stat.DAMAGE, Stat.PIERCING, Stat.RANGE, Stat.FIRE_RATE, Stat.HEAT_CAP, Stat.DURABILITY};
    private static final double[] BAR_MAX = {20, 75, 160, 12, 200, 2000};
    private static final int ASSEMBLE_X = 8, ASSEMBLE_Y = 104, ASSEMBLE_W = 60, TAKE_X = 8, TAKE_Y = 122, TAKE_W = 130, KEY_H = 14, STATUS_W = 160;
    private static final int BARS_X = 180, BARS_Y = 106, BARS_W = 110, BAR_ROW = 7;
    private final ItemStackHandler items;
    private String status = "", odds = "", assembleLabel = "", takeLabel = "", title = "", tabBuild = "", tabPlan = "";
    private final String[] slotLabels = new String[Assembly.SLOTS], statNames = new String[BARS.length], barText = new String[BARS.length];
    private final int[] barFill = new int[BARS.length];
    private final Object[] lastTags = new Object[Assembly.SLOTS];
    private final int[] lastCounts = new int[Assembly.SLOTS];
    private final net.minecraft.world.item.Item[] lastItems = new net.minecraft.world.item.Item[Assembly.SLOTS];
    private boolean first = true, canAssemble, canTake;
    private Sheet sheet = new Sheet();
    private boolean sheetFromOutput;

    public WeaponAssemblerScreen(WeaponAssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.items = menu.blockEntity().items();
        imageWidth = 300;
        imageHeight = 232;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = -1000; // the title is drawn by renderBg in the mod's own style
        inventoryLabelX = -1000;
        assembleLabel = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".assembler.assemble"), ASSEMBLE_W - 8);
        takeLabel = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".assembler.take_apart"), TAKE_W - 8);
        title = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".assembler.title"), 150);
        tabBuild = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".assembler.build"), 34);
        tabPlan = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".assembler.plan"), 34);
        for (int i = 0; i < Assembly.SLOTS; i++) slotLabels[i] = slotLabel(i);
        for (int i = 0; i < BARS.length; i++) statNames[i] = Component.translatable("stat." + BspWeapons.MODID + "." + BARS[i].id()).getString();
        first = true;
        refresh();
    }

    /** Recomputes the sheet, the status line, the odds and the bar strings when a slot's item, count or tag object changes. */
    private void refresh() {
        boolean changed = first;
        for (int i = 0; i < Assembly.SLOTS; i++) {
            ItemStack s = items.getStackInSlot(i);
            if (s.getItem() != lastItems[i] || s.getCount() != lastCounts[i] || s.getTag() != lastTags[i]) {
                changed = true;
                lastItems[i] = s.getItem(); lastCounts[i] = s.getCount(); lastTags[i] = s.getTag();
            }
        }
        if (!changed) return;
        first = false;
        WeaponSpec out = WeaponItem.specOf(items.getStackInSlot(Assembly.OUTPUT));
        WeaponSpec spec = out != null ? out : Assembly.preview(items);
        sheetFromOutput = out != null;
        sheet = spec == null ? new Sheet() : SheetCalculator.compute(spec);
        String why = Assembly.cannotAssemble(items);
        status = why == null || out != null ? "" : Text.cut(font, why, STATUS_W);
        Catalyst c = Assembly.rollCatalyst(items);
        if (c == null) odds = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".assembler.no_catalyst"), STATUS_W);
        else {
            int total = 0;
            for (Rarity r : Rarity.values()) total += c.weight(r);
            odds = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".assembler.odds", pct(c, Rarity.NORMAL, total), pct(c, Rarity.RARE, total), pct(c, Rarity.EPIC, total), pct(c, Rarity.S, total)), STATUS_W);
        }
        canAssemble = status.isEmpty() && !sheetFromOutput && items.getStackInSlot(Assembly.OUTPUT).isEmpty();
        canTake = Assembly.cannotTakeApart(items) == null;
        int bw = BARS_W - 40 - 22;
        for (int i = 0; i < BARS.length; i++) {
            double v = sheet.get(BARS[i]);
            barFill[i] = (int) Math.round((bw - 2) * Math.min(1, v / BAR_MAX[i]));
            barText[i] = WeaponItem.number(BARS[i], v);
        }
    }

    private static String pct(Catalyst c, Rarity r, int total) {
        return String.valueOf(Math.round(100.0 * c.weight(r) / total));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        refresh();
        renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        frame(g, x, y, imageWidth, imageHeight, DIM);
        g.drawString(font, title, x + 6, y + 6, GOLD, false);
        g.fill(x + 6, y + 18, x + 294, y + 19, DIM);
        // tabs: BUILD on, PLAN not yet
        tab(g, x + 214, y + 4, tabBuild, true);
        tab(g, x + 256, y + 4, tabPlan, false);
        // the rail
        g.fill(x + 24, y + 55, x + 276, y + 59, 0xFF2E343D);
        g.fill(x + 24, y + 55, x + 276, y + 56, 0xFF4A5363);
        g.fill(x + 26, y + 57, x + 274, y + 58, 0xFFD9B23A);
        // the slots, with faint names where empty
        for (int i = 0; i < Assembly.SLOTS; i++) {
            int sx = x + WeaponAssemblerMenu.SLOT_XY[i][0], sy = y + WeaponAssemblerMenu.SLOT_XY[i][1];
            boolean filled = !items.getStackInSlot(i).isEmpty();
            g.fill(sx, sy, sx + 18, sy + 18, SLOT);
            frame(g, sx, sy, 18, 18, filled ? DIM : 0xFF1A1F27);
            g.drawString(font, slotLabels[i], sx, sy + 19, filled ? MUTED : FAINT, false);
        }
        // player inventory
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) slotBox(g, x + WeaponAssemblerMenu.INVENTORY_X + col * 18, y + WeaponAssemblerMenu.INVENTORY_Y + row * 18);
        for (int col = 0; col < 9; col++) slotBox(g, x + WeaponAssemblerMenu.INVENTORY_X + col * 18, y + WeaponAssemblerMenu.HOTBAR_Y);
        // keys
        key(g, x + ASSEMBLE_X, y + ASSEMBLE_Y, ASSEMBLE_W, assembleLabel, canAssemble ? TEAL : FAINT, hovered(mouseX, mouseY, ASSEMBLE_X, ASSEMBLE_Y, ASSEMBLE_W, KEY_H) && canAssemble);
        key(g, x + TAKE_X, y + TAKE_Y, TAKE_W, takeLabel, canTake ? MUTED : FAINT, hovered(mouseX, mouseY, TAKE_X, TAKE_Y, TAKE_W, KEY_H) && canTake);
        // one line under the keys: the problem in red while there is one, else the roll odds in violet
        if (!status.isEmpty()) g.drawString(font, status, x + 8, y + 139, RED, false);
        else g.drawString(font, odds, x + 8, y + 139, VIOLET, false);
        // the bars, from strings worked out in refresh()
        for (int i = 0; i < BARS.length; i++) {
            int by = y + BARS_Y + i * BAR_ROW;
            g.pose().pushPose();
            g.pose().translate(x + BARS_X, by, 0);
            g.pose().scale(0.5f, 0.5f, 1);
            g.drawString(font, statNames[i], 0, 0, MUTED, false);
            g.pose().popPose();
            int bx = x + BARS_X + 40, bw = BARS_W - 40 - 22;
            g.fill(bx, by + 1, bx + bw, by + 6, SLOT);
            frame(g, bx, by + 1, bw, 5, DIM);
            g.fill(bx + 1, by + 2, bx + 1 + barFill[i], by + 5, sheetFromOutput ? GOLD : TEAL);
            g.pose().pushPose();
            g.pose().translate(bx + bw + 3, by, 0);
            g.pose().scale(0.5f, 0.5f, 1);
            g.drawString(font, barText[i], 0, 0, TEXT, false);
            g.pose().popPose();
        }
    }

    private String slotLabel(int slot) {
        String key;
        if (slot == Assembly.ENGRAM) key = "engram";
        else if (slot == Assembly.CORE) key = "core";
        else if (slot == Assembly.ROLL) key = "roll";
        else if (slot == Assembly.OUTPUT) key = "weapon";
        else {
            PartKind k = Assembly.kindOf(slot);
            key = k == null ? "part" : k.id();
        }
        return Text.cut(font, Component.translatable("slot." + BspWeapons.MODID + "." + key), 40);
    }

    private boolean hovered(int mouseX, int mouseY, int rx, int ry, int w, int h) {
        int mx = mouseX - leftPos, my = mouseY - topPos;
        return mx >= rx && mx < rx + w && my >= ry && my < ry + h;
    }

    private void key(GuiGraphics g, int x, int y, int w, String label, int colour, boolean hover) {
        g.fill(x, y, x + w, y + KEY_H, hover ? 0xFF1A2129 : SLOT);
        frame(g, x, y, w, KEY_H, colour);
        g.drawString(font, label, x + 4, y + 3, colour, false);
    }

    private void tab(GuiGraphics g, int x, int y, String label, boolean on) {
        if (on) g.fill(x, y, x + 40, y + 12, SLOT);
        frame(g, x, y, 40, 12, on ? TEAL : DIM);
        g.drawString(font, Text.cut(font, label, 34), x + 4, y + 2, on ? TEAL : FAINT, false);
    }

    private static void slotBox(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, SLOT);
        frame(g, x, y, 18, 18, 0xFF1A1F27);
    }

    private static void frame(GuiGraphics g, int x, int y, int w, int h, int colour) {
        g.fill(x, y, x + w, y + 1, colour);
        g.fill(x, y + h - 1, x + w, y + h, colour);
        g.fill(x, y, x + 1, y + h, colour);
        g.fill(x + w - 1, y, x + w, y + h, colour);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // drawn in renderBg
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (hovered((int) mouseX, (int) mouseY, ASSEMBLE_X, ASSEMBLE_Y, ASSEMBLE_W, KEY_H) && canAssemble) {
                WeaponsNetwork.CHANNEL.sendToServer(new AssemblerActionPacket(menu.pos(), AssemblerActionPacket.ASSEMBLE));
                return true;
            }
            if (hovered((int) mouseX, (int) mouseY, TAKE_X, TAKE_Y, TAKE_W, KEY_H) && canTake) {
                WeaponsNetwork.CHANNEL.sendToServer(new AssemblerActionPacket(menu.pos(), AssemblerActionPacket.TAKE_APART));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
