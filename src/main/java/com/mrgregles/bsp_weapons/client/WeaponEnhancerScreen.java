package com.mrgregles.bsp_weapons.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.data.WeaponData;
import com.mrgregles.bsp_weapons.enhancer.Enhancing;
import com.mrgregles.bsp_weapons.enhancer.MachineEnergy;
import com.mrgregles.bsp_weapons.enhancer.WeaponEnhancerMenu;
import com.mrgregles.bsp_weapons.item.AugmentChipItem;
import com.mrgregles.bsp_weapons.item.CoreItem;
import com.mrgregles.bsp_weapons.item.WeaponItem;
import com.mrgregles.bsp_weapons.network.EnhancerActionPacket;
import com.mrgregles.bsp_weapons.network.WeaponsNetwork;
import com.mrgregles.bsp_weapons.parts.Augment;
import com.mrgregles.bsp_weapons.parts.FittedChip;
import com.mrgregles.bsp_weapons.parts.Rarity;
import com.mrgregles.bsp_weapons.parts.Sheet;
import com.mrgregles.bsp_weapons.parts.SheetCalculator;
import com.mrgregles.bsp_weapons.parts.Stat;
import com.mrgregles.bsp_weapons.parts.WeaponSpec;
import com.mrgregles.bsp_weapons.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The enhancer's screen, the Board (D85, D87; R20, R21, R23): the weapon side on over its rail, the sockets above it
 * wired to the frame, each a Shard Key with its points, a click choosing one; CHIP IN, REPAIR and CORE under the rail;
 * the charge as a gauge beside the title; the weapon's name, Core line and chips on the right over the sheet as bars
 * (teal the parts' share, gold the Core and roll's, violet the chips', a red mark where a chip costs); FIT (SWAP when a
 * full socket is chosen and a chip waits), REMOVE, UPGRADE and REPAIR, each lit when its check passes; one status line. The
 * machine's part of the screen is 148 rows over the player's inventory; 300 x 232 (D123). Text is worked out when a slot or the
 * charge changes, not every frame.
 */
public class WeaponEnhancerScreen extends AbstractContainerScreen<WeaponEnhancerMenu> {
    private static final int PANEL = 0xFF10151C, SLOT = 0xFF0C0E12, DIM = 0xFF2A2F3A, MUTED = 0xFF9AA3B5, TEXT = 0xFFE6EBF2, TEAL = 0xFF19D3B0, GOLD = 0xFFFFD23A, VIOLET = 0xFFB58CFF, RED = 0xFFFF6B5C, FAINT = 0xFF3A4150, EMPTY = 0xFF1A1F27;
    private static final Stat[] BARS = {Stat.DAMAGE, Stat.PIERCING, Stat.RANGE, Stat.FIRE_RATE, Stat.HEAT_CAP, Stat.DURABILITY};
    private static final double[] BAR_MAX = {20, 75, 160, 12, 200, 2000};
    private static final int SOCKET_Y = 22, RAIL_Y = 60, RAIL_X0 = 24, RAIL_X1 = 162, KEY_H = 14, STATUS_Y = 139, STATUS_W = 286;
    private static final int FIT_X = 8, FIT_W = 38, REMOVE_X = 50, REMOVE_W = 52, KEYS_Y = 108, UPGRADE_X = 8, UPGRADE_W = 54, REPAIR_X = 66, REPAIR_W = 48, KEYS2_Y = 124;
    private static final int RIGHT_X = 170, RIGHT_W = 124, BARS_Y = 100, BAR_ROW = 6, GAUGE_X = 170, GAUGE_Y = 5, GAUGE_W = 124, GAUGE_H = 7;
    private static final String[] SLOT_KEYS = {"weapon", "chip_in", "core", "repair"};
    private final ItemStackHandler items;
    private final Object[] lastTags = new Object[Enhancing.SLOTS];
    private final int[] lastCounts = new int[Enhancing.SLOTS];
    private final net.minecraft.world.item.Item[] lastItems = new net.minecraft.world.item.Item[Enhancing.SLOTS];
    private int lastEnergy = -1, lastChosen = -2;
    private boolean first = true;
    private String title = "", fitLabel = "", swapLabel = "", removeLabel = "", upgradeLabel = "", repairLabel = "", feText = "", nameText = "", coreText = "", chipsText = "", status = "";
    private final String[] slotLabels = new String[Enhancing.SLOTS], statNames = new String[BARS.length], barText = new String[BARS.length];
    private final int[] barParts = new int[BARS.length], barCore = new int[BARS.length], barFinal = new int[BARS.length];
    private final List<ChipRow> chipRows = new ArrayList<>();
    private int chosen = -1, sockets = 0, gaugeFill = 0;
    private boolean statusBad, swapMode, canFit, canRemove, canUpgrade, canRepair, loaded;
    private WeaponSpec spec;

    private record ChipRow(ItemStack icon, String name, String points, String rarity, int colour, int dura, int max) {
    }

    public WeaponEnhancerScreen(WeaponEnhancerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.items = menu.blockEntity().items();
        imageWidth = 300;
        imageHeight = 232;
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = -1000;
        inventoryLabelX = -1000;
        title = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".enhancer.title"), 140);
        fitLabel = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".enhancer.fit"), FIT_W - 8);
        swapLabel = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".enhancer.swap"), FIT_W - 8);
        removeLabel = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".enhancer.remove"), REMOVE_W - 8);
        upgradeLabel = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".enhancer.upgrade"), UPGRADE_W - 8);
        repairLabel = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".enhancer.repair"), REPAIR_W - 8);
        for (int i = 0; i < Enhancing.SLOTS; i++) slotLabels[i] = Text.cut(font, Component.translatable("slot." + BspWeapons.MODID + "." + SLOT_KEYS[i]), 36);
        for (int i = 0; i < BARS.length; i++) statNames[i] = Component.translatable("stat." + BspWeapons.MODID + "." + BARS[i].id()).getString();
        first = true;
        refresh();
    }

    /** Recomputes everything drawn from text when a slot's item, count or tag object, the charge or the chosen socket changes. */
    private void refresh() {
        boolean changed = first;
        for (int i = 0; i < Enhancing.SLOTS; i++) {
            ItemStack s = items.getStackInSlot(i);
            if (s.getItem() != lastItems[i] || s.getCount() != lastCounts[i] || s.getTag() != lastTags[i]) {
                changed = true;
                lastItems[i] = s.getItem(); lastCounts[i] = s.getCount(); lastTags[i] = s.getTag();
            }
        }
        int stored = menu.energyStored(), capacity = Math.max(1, menu.energyCapacity());
        if (stored != lastEnergy || chosen != lastChosen) { changed = true; lastEnergy = stored; lastChosen = chosen; }
        if (!changed) return;
        first = false;
        feText = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".enhancer.fe", String.format(Locale.ROOT, "%,d", stored), String.format(Locale.ROOT, "%,d", capacity)), GAUGE_W);
        gaugeFill = (int) Math.round((GAUGE_W - 2) * Math.min(1.0, (double) stored / capacity));
        ItemStack gun = items.getStackInSlot(Enhancing.WEAPON);
        spec = WeaponItem.specOf(gun);
        loaded = spec != null;
        chipRows.clear();
        if (!loaded) {
            sockets = 0; chosen = -1; lastChosen = -1;
            nameText = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".enhancer.no_weapon"), RIGHT_W);
            coreText = ""; chipsText = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".enhancer.chips", "-", "-"), RIGHT_W);
            for (int i = 0; i < BARS.length; i++) { barParts[i] = barCore[i] = barFinal[i] = 0; barText[i] = ""; }
        } else {
            sockets = Math.min(4, SheetCalculator.augmentSlots(spec));
            if (chosen >= sockets) { chosen = -1; lastChosen = -1; }
            nameText = Text.cut(font, gun.getHoverName(), RIGHT_W);
            coreText = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".enhancer.core_line", CoreItem.roman(spec.coreTier()), Component.translatable("rarity." + BspWeapons.MODID + "." + spec.roll().id()), sockets), RIGHT_W * 2);
            chipsText = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".enhancer.chips", spec.augments().size(), sockets), RIGHT_W * 2);
            for (FittedChip c : spec.augments()) {
                Augment a = WeaponData.augment(c.augment());
                ItemStack icon = ModItems.AUGMENT_CHIP.get().of(c);
                chipRows.add(new ChipRow(icon, Text.cut(font, a == null ? Component.translatable("tooltip." + BspWeapons.MODID + ".chip_unknown") : Component.translatable(a.nameKey()), 70),
                        c.durability() + "/" + (a == null ? "?" : String.valueOf(a.durability())), Component.translatable("rarity." + BspWeapons.MODID + "." + c.rarity().id()).getString(),
                        0xFF000000 | (a == null ? AugmentChipItem.UNKNOWN_COLOUR : a.colour()), c.durability(), a == null ? 10 : a.durability()));
            }
            // the three layers of the sheet (D38, D87): parts alone, then the Core and roll, then the chips
            Sheet parts = SheetCalculator.compute(new WeaponSpec(spec.pattern(), 1, spec.parts(), Rarity.NORMAL, List.of()));
            Sheet core = SheetCalculator.compute(new WeaponSpec(spec.pattern(), spec.coreTier(), spec.parts(), spec.roll(), List.of()));
            Sheet all = SheetCalculator.compute(spec);
            int bw = barWidth();
            for (int i = 0; i < BARS.length; i++) {
                barParts[i] = fill(bw, parts.get(BARS[i]), BAR_MAX[i]);
                barCore[i] = fill(bw, core.get(BARS[i]), BAR_MAX[i]);
                barFinal[i] = fill(bw, all.get(BARS[i]), BAR_MAX[i]);
                barText[i] = WeaponItem.number(BARS[i], all.get(BARS[i]));
            }
        }
        // the keys and the status line, from the same checks the server runs
        MachineEnergy energy = new MachineEnergy(capacity, 1, stored);
        ItemStack chipIn = items.getStackInSlot(Enhancing.CHIP_IN);
        boolean chipWaiting = chipIn.getItem() instanceof AugmentChipItem;
        swapMode = loaded && chipWaiting && chosen >= 0 && chosen < spec.augments().size();
        Component fitWhy = swapMode ? Enhancing.cannotSwap(items, energy, chosen) : Enhancing.cannotFit(items, energy);
        canFit = fitWhy == null;
        Component removeWhy = chosen < 0 ? null : Enhancing.cannotRemove(items, chosen);
        canRemove = chosen >= 0 && removeWhy == null;
        Component upgradeWhy = Enhancing.cannotUpgrade(items, energy), repairWhy = Enhancing.cannotRepair(items, energy);
        canUpgrade = upgradeWhy == null;
        canRepair = repairWhy == null;
        boolean coreWaiting = items.getStackInSlot(Enhancing.CORE).getItem() instanceof CoreItem, ingotWaiting = !items.getStackInSlot(Enhancing.REPAIR).isEmpty();
        Component line;
        if (!loaded) { line = Component.translatable("enhance." + BspWeapons.MODID + ".no_weapon"); statusBad = false; }
        else if (chipWaiting) {
            if (fitWhy != null) { line = fitWhy; statusBad = true; }
            else { line = Component.translatable("gui." + BspWeapons.MODID + ".enhancer.fit_cost", swapMode ? swapLabel : fitLabel, String.format(Locale.ROOT, "%,d", Enhancing.fitCost())); statusBad = false; }
        } else if (chosen >= 0) {
            if (removeWhy != null) { line = removeWhy; statusBad = true; }
            else { line = Component.translatable("gui." + BspWeapons.MODID + ".enhancer.chosen", chosen + 1, chipRows.get(chosen).name()); statusBad = false; }
        } else if (coreWaiting) {
            if (upgradeWhy != null) { line = upgradeWhy; statusBad = true; }
            else { line = Component.translatable("gui." + BspWeapons.MODID + ".enhancer.upgrade_cost", upgradeLabel, String.format(Locale.ROOT, "%,d", Enhancing.upgradeCost())); statusBad = false; }
        } else if (ingotWaiting) {
            if (repairWhy != null) { line = repairWhy; statusBad = true; }
            else { line = Component.translatable("gui." + BspWeapons.MODID + ".enhancer.upgrade_cost", repairLabel, String.format(Locale.ROOT, "%,d", Enhancing.repairCost())); statusBad = false; }
        } else { line = Component.translatable("gui." + BspWeapons.MODID + ".enhancer.idle"); statusBad = false; }
        status = Text.cut(font, line, STATUS_W);
    }

    private static int barWidth() {
        return RIGHT_W - 36 - 22;
    }

    private static int fill(int bw, double v, double max) {
        return (int) Math.round((bw - 2) * Math.max(0, Math.min(1, v / max)));
    }

    private int socketX(int i) {
        return sockets <= 3 ? 84 + i * 28 : 72 + i * 24;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        refresh();
        renderBackground(g);
        super.render(g, mouseX, mouseY, partial);
        renderTooltip(g, mouseX, mouseY);
        // the fitted chips' tooltips, over their sockets
        if (loaded) for (int i = 0; i < chipRows.size() && i < sockets; i++) {
            if (hovered(mouseX, mouseY, socketX(i), SOCKET_Y, 18, 18)) g.renderTooltip(font, chipRows.get(i).icon(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        frame(g, x, y, imageWidth, imageHeight, DIM);
        g.drawString(font, title, x + 6, y + 6, GOLD, false);
        g.fill(x + 6, y + 18, x + 294, y + 19, DIM);
        // the charge: a gold gauge beside the title with the numbers under it
        g.fill(x + GAUGE_X, y + GAUGE_Y, x + GAUGE_X + GAUGE_W, y + GAUGE_Y + GAUGE_H, SLOT);
        frame(g, x + GAUGE_X, y + GAUGE_Y, GAUGE_W, GAUGE_H, DIM);
        if (gaugeFill > 0) g.fill(x + GAUGE_X + 1, y + GAUGE_Y + 1, x + GAUGE_X + 1 + gaugeFill, y + GAUGE_Y + GAUGE_H - 1, GOLD);
        for (int gx = GAUGE_X + 8; gx < GAUGE_X + GAUGE_W - 1; gx += 8) g.fill(x + gx, y + GAUGE_Y + 1, x + gx + 1, y + GAUGE_Y + GAUGE_H - 1, SLOT);
        small(g, feText, x + GAUGE_X, y + GAUGE_Y + GAUGE_H + 1, gaugeFill > 0 ? GOLD : RED);
        // the rail and the weapon over it, side on
        g.fill(x + RAIL_X0, y + RAIL_Y, x + RAIL_X1, y + RAIL_Y + 4, 0xFF2E343D);
        g.fill(x + RAIL_X0, y + RAIL_Y, x + RAIL_X1, y + RAIL_Y + 1, 0xFF4A5363);
        g.fill(x + RAIL_X0 + 2, y + RAIL_Y + 2, x + RAIL_X1 - 2, y + RAIL_Y + 3, 0xFFD9B23A);
        if (loaded) drawWeapon(g, items.getStackInSlot(Enhancing.WEAPON), x + (RAIL_X0 + RAIL_X1) / 2 + 4, y + RAIL_Y - 4);
        // the sockets above the rail, wired to it; the chosen one framed teal
        for (int i = 0; i < Math.max(sockets, 3); i++) {
            int sx = x + socketX(i), sy = y + SOCKET_Y;
            boolean exists = i < sockets, has = exists && i < chipRows.size();
            g.fill(sx, sy, sx + 18, sy + 18, SLOT);
            frame(g, sx, sy, 18, 18, i == chosen ? TEAL : (exists ? DIM : EMPTY));
            small(g, String.valueOf(i + 1), sx - 5, sy + 1, exists ? MUTED : FAINT);
            if (has) {
                ChipRow row = chipRows.get(i);
                g.renderItem(row.icon(), sx + 1, sy + 1);
                for (int p = 0; p < row.max(); p++) g.fill(sx + p * 2, sy - 4, sx + p * 2 + 1, sy - 2, p < row.dura() ? row.colour() : DIM);
            } else if (exists) g.fill(sx + 3, sy + 8, sx + 15, sy + 9, FAINT);
            int trace = has ? TEAL : FAINT;
            g.fill(sx + 9, sy + 18, sx + 10, y + RAIL_Y, trace);
            g.fill(sx + 8, y + RAIL_Y - 1, sx + 11, y + RAIL_Y, trace);
        }
        // the slots, with names; CORE and REPAIR wired up to the rail
        for (int i = 0; i < Enhancing.SLOTS; i++) {
            int sx = x + WeaponEnhancerMenu.SLOT_XY[i][0], sy = y + WeaponEnhancerMenu.SLOT_XY[i][1];
            boolean filled = !items.getStackInSlot(i).isEmpty();
            g.fill(sx, sy, sx + 18, sy + 18, SLOT);
            frame(g, sx, sy, 18, 18, filled ? DIM : EMPTY);
            g.drawString(font, slotLabels[i], sx, sy + 19, filled ? MUTED : FAINT, false);
            if (i == Enhancing.CORE || i == Enhancing.REPAIR) g.fill(sx + 9, y + RAIL_Y + 4, sx + 10, sy, filled ? (i == Enhancing.CORE ? GOLD : MUTED) : FAINT);
        }
        // the right column: name, Core line, chips
        g.drawString(font, nameText, x + RIGHT_X, y + 24, loaded ? TEXT : FAINT, false);
        small(g, coreText, x + RIGHT_X, y + 34, VIOLET);
        small(g, chipsText, x + RIGHT_X, y + 42, TEXT);
        for (int i = 0; i < Math.max(sockets, 1); i++) {
            int ry = y + 50 + i * 10;
            if (!loaded) { small(g, "-", x + RIGHT_X, ry, FAINT); break; }
            if (i >= chipRows.size()) { small(g, Component.translatable("gui." + BspWeapons.MODID + ".enhancer.empty_socket").getString(), x + RIGHT_X, ry, FAINT); continue; }
            ChipRow row = chipRows.get(i);
            small(g, row.name(), x + RIGHT_X, ry, row.colour());
            for (int p = 0; p < row.max(); p++) g.fill(x + RIGHT_X + p * 2, ry + 6, x + RIGHT_X + p * 2 + 1, ry + 8, p < row.dura() ? row.colour() : DIM);
            small(g, row.points(), x + RIGHT_X + 24, ry + 5, MUTED);
            small(g, row.rarity(), x + RIGHT_X + 48, ry + 5, VIOLET);
        }
        // the bars (D38, D87)
        int bw = barWidth();
        for (int i = 0; i < BARS.length; i++) {
            int by = y + BARS_Y + i * BAR_ROW, bx = x + RIGHT_X + 36;
            small(g, statNames[i], x + RIGHT_X, by, MUTED);
            g.fill(bx, by + 1, bx + bw, by + 6, SLOT);
            frame(g, bx, by + 1, bw, 5, DIM);
            if (loaded) {
                g.fill(bx + 1, by + 2, bx + 1 + barParts[i], by + 5, TEAL);
                if (barCore[i] > barParts[i]) g.fill(bx + 1 + barParts[i], by + 2, bx + 1 + barCore[i], by + 5, GOLD);
                if (barFinal[i] >= barCore[i]) { if (barFinal[i] > barCore[i]) g.fill(bx + 1 + barCore[i], by + 2, bx + 1 + barFinal[i], by + 5, VIOLET); }
                else {
                    g.fill(bx + 1 + barFinal[i], by + 2, bx + 1 + barCore[i], by + 5, SLOT);
                    g.fill(bx + 1 + barFinal[i], by + 2, bx + 2 + barFinal[i], by + 5, RED);
                    g.fill(bx + 1 + barFinal[i], by, bx + 1 + barCore[i], by + 1, RED);
                }
                small(g, barText[i], bx + bw + 3, by, TEXT);
            }
        }
        // player inventory
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) slotBox(g, x + WeaponEnhancerMenu.INVENTORY_X + col * 18, y + WeaponEnhancerMenu.INVENTORY_Y + row * 18);
        for (int col = 0; col < 9; col++) slotBox(g, x + WeaponEnhancerMenu.INVENTORY_X + col * 18, y + WeaponEnhancerMenu.HOTBAR_Y);
        // keys and the status line
        key(g, x + FIT_X, y + KEYS_Y, FIT_W, swapMode ? swapLabel : fitLabel, canFit ? TEAL : FAINT, canFit && hovered(mouseX, mouseY, FIT_X, KEYS_Y, FIT_W, KEY_H));
        key(g, x + REMOVE_X, y + KEYS_Y, REMOVE_W, removeLabel, canRemove ? TEAL : FAINT, canRemove && hovered(mouseX, mouseY, REMOVE_X, KEYS_Y, REMOVE_W, KEY_H));
        key(g, x + UPGRADE_X, y + KEYS2_Y, UPGRADE_W, upgradeLabel, canUpgrade ? TEAL : FAINT, canUpgrade && hovered(mouseX, mouseY, UPGRADE_X, KEYS2_Y, UPGRADE_W, KEY_H));
        key(g, x + REPAIR_X, y + KEYS2_Y, REPAIR_W, repairLabel, canRepair ? TEAL : FAINT, canRepair && hovered(mouseX, mouseY, REPAIR_X, KEYS2_Y, REPAIR_W, KEY_H));
        g.drawString(font, status, x + 8, y + STATUS_Y, statusBad ? RED : MUTED, false);
    }

    /** The weapon as an item frame shows it: side on, still (R20), over the rail. */
    private void drawWeapon(GuiGraphics g, ItemStack stack, int cx, int cy) {
        var renderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = renderer.getModel(stack, null, null, 0);
        g.pose().pushPose();
        g.pose().translate(cx, cy, 150);
        g.pose().scale(56, -56, 56);
        Lighting.setupFor3DItems();
        renderer.render(stack, ItemDisplayContext.FIXED, false, g.pose(), g.bufferSource(), 0xF000F0, OverlayTexture.NO_OVERLAY, model);
        g.flush();
        g.pose().popPose();
    }

    /** Half-size text, as the assembler's bars use. */
    private void small(GuiGraphics g, String s, int x, int y, int colour) {
        if (s == null || s.isEmpty()) return;
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.5f, 0.5f, 1);
        g.drawString(font, s, 0, 0, colour, false);
        g.pose().popPose();
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

    private static void slotBox(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, SLOT);
        frame(g, x, y, 18, 18, EMPTY);
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
            int mx = (int) mouseX, my = (int) mouseY;
            for (int i = 0; i < sockets; i++) {
                if (hovered(mx, my, socketX(i), SOCKET_Y, 18, 18)) { chosen = chosen == i ? -1 : i; return true; } // a second click lets go
            }
            if (canFit && hovered(mx, my, FIT_X, KEYS_Y, FIT_W, KEY_H)) {
                WeaponsNetwork.CHANNEL.sendToServer(new EnhancerActionPacket(menu.pos(), swapMode ? Enhancing.SWAP : Enhancing.FIT, Math.max(0, chosen)));
                return true;
            }
            if (canRemove && hovered(mx, my, REMOVE_X, KEYS_Y, REMOVE_W, KEY_H)) {
                WeaponsNetwork.CHANNEL.sendToServer(new EnhancerActionPacket(menu.pos(), Enhancing.REMOVE, chosen));
                return true;
            }
            if (canUpgrade && hovered(mx, my, UPGRADE_X, KEYS2_Y, UPGRADE_W, KEY_H)) {
                WeaponsNetwork.CHANNEL.sendToServer(new EnhancerActionPacket(menu.pos(), Enhancing.OP_UPGRADE, 0));
                return true;
            }
            if (canRepair && hovered(mx, my, REPAIR_X, KEYS2_Y, REPAIR_W, KEY_H)) {
                WeaponsNetwork.CHANNEL.sendToServer(new EnhancerActionPacket(menu.pos(), Enhancing.OP_REPAIR, 0));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
