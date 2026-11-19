package com.mrgregles.bsp_weapons.client;

import com.mrgregles.bsp_weapons.BspWeapons;
import com.mrgregles.bsp_weapons.data.WeaponData;
import com.mrgregles.bsp_weapons.enhancer.MachineEnergy;
import com.mrgregles.bsp_weapons.fabricator.Fabricating;
import com.mrgregles.bsp_weapons.fabricator.FabricatingRecipe;
import com.mrgregles.bsp_weapons.fabricator.Fabrication;
import com.mrgregles.bsp_weapons.fabricator.Job;
import com.mrgregles.bsp_weapons.fabricator.WeaponFabricatorMenu;
import com.mrgregles.bsp_weapons.item.CatalystItem;
import com.mrgregles.bsp_weapons.item.CoreItem;
import com.mrgregles.bsp_weapons.item.EngramItem;
import com.mrgregles.bsp_weapons.network.FabricatorActionPacket;
import com.mrgregles.bsp_weapons.network.WeaponsNetwork;
import com.mrgregles.bsp_weapons.parts.Augment;
import com.mrgregles.bsp_weapons.parts.Catalyst;
import com.mrgregles.bsp_weapons.parts.Need;
import com.mrgregles.bsp_weapons.parts.PartKind;
import com.mrgregles.bsp_weapons.parts.PartType;
import com.mrgregles.bsp_weapons.parts.Rarity;
import com.mrgregles.bsp_weapons.parts.WeaponMaterial;
import com.mrgregles.bsp_weapons.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The fabricator's screen, the Catalogue (D109; R27 to R30): tabs PARTS, CHIPS, ENGRAM and MORE over a scrolling list of
 * everything the machine makes, a click choosing a row that the machine keeps; on the right the chosen job: its name, what
 * it needs as ghosts with counts (a part's "any ingot" cycling through the materials each second), MATERIAL, ROLL and the
 * second slot (BLANK on the ENGRAM tab), OUT with a ghost of the product, the progress bar, the charge gauge, MAKE with its
 * price, the odds in violet when a catalyst sits in ROLL, and one status line: what MAKE will cost, why it is grey, or how
 * long the job has left. The machine's part of the screen is 148 rows over the player's inventory; 300 x 232 (D123). Text is
 * worked out when a slot, the charge, the row, the tab, the scroll or the progress changes, not every frame.
 */
public class WeaponFabricatorScreen extends AbstractContainerScreen<WeaponFabricatorMenu> {
    private static final int PANEL = 0xFF10151C, SLOT = 0xFF0C0E12, DIM = 0xFF2A2F3A, MUTED = 0xFF9AA3B5, TEXT = 0xFFE6EBF2, TEAL = 0xFF19D3B0, GOLD = 0xFFFFD23A, VIOLET = 0xFFB58CFF, RED = 0xFFFF6B5C, FAINT = 0xFF3A4150, EMPTY = 0xFF1A1F27, CHOSEN = 0xFF15202A;
    private static final int GLYPH_METAL = 0xFF7D97B0, GLYPH_GOLD = 0xFFD9B23A, GLYPH_GUNMETAL = 0xFF2E343D, GLYPH_DARK = 0xFF14181E, GLYPH_LIGHT = 0xFF5A6270, GLYPH_GLASS = 0xFF8AE8DC, GLYPH_LAPIS = 0xFF2A4AC8, GLYPH_RED = 0xFFE03A3A, GLYPH_GEM = 0xFF6AF0E8;
    private static final int TABS_X = 133, TABS_Y = 5, TAB_W = 40, TAB_STEP = 41, TAB_H = 12;
    private static final int LIST_X = 6, LIST_Y = 24, LIST_W = 124, LIST_H = 122, ROW_X = 10, ROW_Y = 29, ROW_W = 114, ROW_H = 11, ROWS = 10, BAR_X = 126, BAR_Y = 26, BAR_H = 118;
    private static final int RIGHT_X = 134, NAME_W = 150, NEEDS_Y = 38, GHOST_X = 166, NEED_TEXT_X = 202, NEED_W = 84;
    private static final int PROGRESS_X = 252, PROGRESS_Y = 100, PROGRESS_W = 44, PROGRESS_H = 14, GAUGE_X = 290, GAUGE_Y = 24, GAUGE_W = 8, GAUGE_H = 40;
    private static final int MAKE_X = 134, MAKE_Y = 100, MAKE_W = 40, KEY_H = 14, PRICE_X = 180, ODDS_Y = 116, STATUS_Y = 126, STATUS_ROW = 10, STATUS_ROWS = 2, STATUS_W = 162, LABEL_W = 46;
    private static final String[] SLOT_KEYS = {"material", "roll", "extra", "out"};
    private final ItemStackHandler items;
    private final Object[] lastTags = new Object[Fabricating.SLOTS];
    private final int[] lastCounts = new int[Fabricating.SLOTS];
    private final Item[] lastItems = new Item[Fabricating.SLOTS];
    private final String[] slotLabels = new String[Fabricating.SLOTS], tabLabels = new String[Job.Tab.values().length];
    private final List<Row> rows = new ArrayList<>();
    private List<Job> catalogue = List.of();
    private Job.Tab tab = Job.Tab.PARTS;
    private int scroll, lastEnergy = -1, lastChosen = -2, lastLeft = -1, lastTotal = -1, lastCycle = -1, lastScroll = -1, chosenRow = -1;
    private Job.Tab lastTab;
    private boolean first = true, canMake, statusBad, working, rolls;
    private String title = "", makeLabel = "", feLabel = "", needsLabel = "", nameText = "", priceText = "", oddsText = "", progressText = "", need1Text = "", need2Text = "", need1Count = "", need2Count = "";
    /** The status, wrapped to at most {@link #STATUS_ROWS} rows. */
    private List<net.minecraft.util.FormattedCharSequence> status = List.of();
    private int gaugeFill, progressFill, need1Tint = GLYPH_METAL, need2Tint = GLYPH_METAL;
    private String need1Glyph = "", need2Glyph = "", outGlyph = "";
    private int outTint = GLYPH_METAL;
    /** For a part, the materials the first need cycles through (R28); for a tag need, the tag's items. */
    private List<Cycle> cycle = List.of();

    private record Row(Job job, int index, String name, String glyph, int tint) {
    }

    private record Cycle(Component name, int tint) {
    }

    public WeaponFabricatorScreen(WeaponFabricatorMenu menu, Inventory inventory, Component title) {
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
        title = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".fabricator.title"), TABS_X - 10);
        makeLabel = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".fabricator.make"), MAKE_W - 6);
        feLabel = Component.translatable("gui." + BspWeapons.MODID + ".fabricator.fe").getString();
        needsLabel = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".fabricator.needs"), GHOST_X - RIGHT_X - 2);
        for (Job.Tab t : Job.Tab.values()) tabLabels[t.ordinal()] = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".fabricator.tab." + t.id()), TAB_W - 4);
        first = true;
        lastTab = null;
        refresh();
    }

    @Nullable
    private Level level() {
        return minecraft == null ? null : minecraft.level;
    }

    /** Recomputes the text when a slot's item, count or tag object, the charge, the chosen row, the tab, the scroll, the progress or the second's cycle changes. */
    private void refresh() {
        Level level = level();
        boolean changed = first;
        for (int i = 0; i < Fabricating.SLOTS; i++) {
            ItemStack s = items.getStackInSlot(i);
            if (s.getItem() != lastItems[i] || s.getCount() != lastCounts[i] || s.getTag() != lastTags[i]) {
                changed = true;
                lastItems[i] = s.getItem(); lastCounts[i] = s.getCount(); lastTags[i] = s.getTag();
            }
        }
        int stored = menu.energyStored(), capacity = Math.max(1, menu.energyCapacity()), chosen = menu.chosenIndex(), left = menu.ticksLeft(), total = menu.ticksTotal();
        int cycleIndex = level == null ? 0 : (int) ((level.getGameTime() / 20) % Math.max(1, cycle.size()));
        boolean jobChanged = first || chosen != lastChosen;
        if (stored != lastEnergy || jobChanged || left != lastLeft || total != lastTotal || tab != lastTab || scroll != lastScroll) {
            changed = true;
            lastEnergy = stored; lastChosen = chosen; lastLeft = left; lastTotal = total;
        }
        if (!changed) {
            if (cycleIndex != lastCycle) { lastCycle = cycleIndex; applyCycle(cycleIndex); }
            return;
        }
        first = false;
        if (level == null) return;
        catalogue = Fabricating.catalogue(level);
        Job job = chosen >= 0 && chosen < catalogue.size() ? catalogue.get(chosen) : null;
        if (jobChanged && job != null) tab = Fabricating.tabOf(job, level);
        if (tab != lastTab) { scroll = 0; lastTab = tab; }
        // the list
        rows.clear();
        chosenRow = -1;
        for (int i = 0; i < catalogue.size(); i++) {
            Job j = catalogue.get(i);
            if (Fabricating.tabOf(j, level) != tab) continue;
            if (j.equals(job)) chosenRow = rows.size();
            Glyph g = glyphOf(j, level);
            rows.add(new Row(j, i, Text.cut(font, Fabricating.name(j, level), ROW_W - 13), g.kind, g.tint));
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - ROWS)));
        lastScroll = scroll;
        // the charge
        gaugeFill = (int) Math.round((GAUGE_H - 2) * Math.min(1.0, (double) stored / capacity));
        // the job
        working = total > 0;
        progressFill = working ? (int) Math.round((PROGRESS_W - 2) * (1.0 - (double) left / Math.max(1, total))) : 0;
        progressText = working ? Math.round(100.0 * (1.0 - (double) left / Math.max(1, total))) + "%" : "";
        boolean engram = job != null && tab == Job.Tab.ENGRAM;
        for (int i = 0; i < Fabricating.SLOTS; i++) slotLabels[i] = Text.cut(font, Component.translatable("slot." + BspWeapons.MODID + "." + (i == Fabricating.EXTRA && engram ? "blank" : SLOT_KEYS[i])), LABEL_W);
        rolls = job != null && job.rolls();
        oddsText = "";
        if (job == null) {
            nameText = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".fabricator.nothing_chosen"), NAME_W);
            need1Glyph = need2Glyph = outGlyph = ""; need1Text = need2Text = need1Count = need2Count = ""; cycle = List.of(); priceText = "";
            canMake = false; statusBad = false;
            status = wrap(Component.translatable("gui." + BspWeapons.MODID + ".fabricator.no_job"));
        } else {
            nameText = Text.cut(font, Fabricating.name(job, level), NAME_W);
            needs(job, level);
            if (job.kind() == Job.Kind.PART) { outGlyph = partGlyph(WeaponData.partType(job.id())); outTint = GLYPH_METAL; }
            else if (job.kind() == Job.Kind.CHIP) { Augment a = WeaponData.augment(job.id()); outGlyph = "chip"; outTint = a == null ? GLYPH_METAL : 0xFF000000 | a.colour(); }
            else { FabricatingRecipe r = Fabricating.recipe(level, job.id()); Glyph g = r == null ? new Glyph("ingot", GLYPH_METAL) : glyphOfStack(r.result()); outGlyph = g.kind; outTint = g.tint; }
            int cost = Fabricating.cost(job, level);
            priceText = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".fabricator.price", String.format(Locale.ROOT, "%,d", cost), String.format(Locale.ROOT, "%.0f", Fabricating.ticks() / 20.0)), PROGRESS_X - 4 - PRICE_X);
            Catalyst c = rolls ? Fabricating.rollCatalyst(items) : null;
            if (c != null) {
                int totalOdds = 0;
                for (Rarity r : Rarity.values()) totalOdds += c.weight(r);
                final int t = Math.max(1, totalOdds);
                oddsText = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".fabricator.odds", pct(c, Rarity.NORMAL, t), pct(c, Rarity.RARE, t), pct(c, Rarity.EPIC, t), pct(c, Rarity.S, t)), STATUS_W);
            }
            if (working) {
                canMake = false; statusBad = false;
                status = wrap(Component.translatable("gui." + BspWeapons.MODID + ".fabricator.working", Fabricating.name(job, level), String.format(Locale.ROOT, "%.1f", left / 20.0)));
            } else {
                Component why = Fabricating.cannotMake(job, items, new MachineEnergy(capacity, 1, stored), new Fabrication(), level);
                canMake = why == null;
                statusBad = !canMake;
                status = wrap(canMake ? Component.translatable("gui." + BspWeapons.MODID + ".fabricator.cost", String.format(Locale.ROOT, "%,d", cost), spent(job, level)) : why);
            }
        }
        lastCycle = cycleIndex;
        applyCycle(cycleIndex);
    }

    /** The status wrapped to its rows; a third row is dropped. */
    private List<net.minecraft.util.FormattedCharSequence> wrap(Component text) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(text, STATUS_W);
        return lines.size() <= STATUS_ROWS ? lines : new ArrayList<>(lines.subList(0, STATUS_ROWS));
    }

    /** The needs line: the first need (cycling for a tag or a part's material), then the second need or the optional catalyst. */
    private void needs(Job job, Level level) {
        cycle = new ArrayList<>();
        need2Glyph = ""; need2Text = ""; need2Count = "";
        if (job.kind() == Job.Kind.PART) {
            PartType type = WeaponData.partType(job.id());
            PartKind kind = type == null ? PartKind.BARREL : type.kind();
            for (WeaponMaterial m : Fabricating.materialsFor(kind)) cycle.add(new Cycle(new Need(m.tag(), true, 1).name(), 0xFF000000 | m.colour()));
            need1Glyph = kind == PartKind.OPTIC ? "gem" : "ingot";
            need1Count = count(type == null ? 1 : type.cost());
        } else {
            Need n1 = Fabricating.materialNeed(job, level), n2 = Fabricating.extraNeed(job, level);
            if (n1 != null) {
                for (Cycle c : cyclesOf(n1)) cycle.add(c);
                need1Glyph = glyphOfNeed(n1); need1Count = count(n1.count());
            } else { need1Glyph = ""; need1Count = ""; }
            if (n2 != null) {
                need2Glyph = glyphOfNeed(n2); need2Count = count(n2.count()); need2Tint = tintOfNeed(n2);
                need2Text = Text.cut(font, n2.name(), NEED_W);
            }
        }
        if (need2Glyph.isEmpty() && job.rolls()) {
            need2Glyph = "catalyst"; need2Count = count(1); need2Tint = GLYPH_METAL;
            need2Text = Text.cut(font, Component.translatable("gui." + BspWeapons.MODID + ".fabricator.optional_roll"), NEED_W);
        }
    }

    private void applyCycle(int index) {
        if (cycle.isEmpty()) { need1Text = ""; need1Tint = GLYPH_METAL; return; }
        Cycle c = cycle.get(Math.max(0, Math.min(cycle.size() - 1, index)));
        need1Text = Text.cut(font, c.name(), NEED_W);
        need1Tint = c.tint();
    }

    /** The items a tag need may be met with, up to sixteen, each with the material's tint when a material claims the tag. */
    private static List<Cycle> cyclesOf(Need need) {
        List<Cycle> out = new ArrayList<>();
        int tint = tintOfNeed(need);
        if (!need.tag()) { out.add(new Cycle(need.name(), tint)); return out; }
        ITag<Item> t = ForgeRegistries.ITEMS.tags() == null ? null : ForgeRegistries.ITEMS.tags().getTag(need.tagKey());
        if (t != null) for (Item item : t) { out.add(new Cycle(new ItemStack(item).getHoverName(), tint)); if (out.size() >= 16) break; }
        if (out.isEmpty()) out.add(new Cycle(need.name(), tint));
        return out;
    }

    private static int tintOfNeed(Need need) {
        if (need.tag()) for (WeaponMaterial m : WeaponData.materials().values()) if (m.tag().equals(need.id())) return 0xFF000000 | m.colour();
        return need.id().getPath().startsWith("dusts/") ? GLYPH_RED : need.id().getPath().startsWith("gems/") ? GLYPH_GEM : GLYPH_METAL;
    }

    private static String glyphOfNeed(Need need) {
        String path = need.id().getPath();
        if (!need.tag() && path.equals("blank_engram")) return "slate";
        if (path.startsWith("gems/")) return "gem";
        if (path.startsWith("dusts/")) return "dust";
        return "ingot";
    }

    private String count(int n) {
        return Component.translatable("gui." + BspWeapons.MODID + ".fabricator.count", n, "").getString().trim();
    }

    /** "3 Iron Ingot, 2 Redstone Dust": what MAKE will take from the slots right now; the catalyst is left to the odds line (R29), so the line fits two rows. */
    private String spent(Job job, Level level) {
        List<String> parts = new ArrayList<>();
        ItemStack material = items.getStackInSlot(Fabricating.MATERIAL), extra = items.getStackInSlot(Fabricating.EXTRA);
        if (job.kind() == Job.Kind.PART) {
            PartType type = WeaponData.partType(job.id());
            parts.add(Component.translatable("gui." + BspWeapons.MODID + ".fabricator.count", type == null ? 1 : type.cost(), material.getHoverName()).getString());
        } else {
            Need n1 = Fabricating.materialNeed(job, level), n2 = Fabricating.extraNeed(job, level);
            if (n1 != null) parts.add(Component.translatable("gui." + BspWeapons.MODID + ".fabricator.count", n1.count(), material.getHoverName()).getString());
            if (n2 != null) parts.add(Component.translatable("gui." + BspWeapons.MODID + ".fabricator.count", n2.count(), extra.getHoverName()).getString());
        }
        return String.join(", ", parts);
    }

    private static String pct(Catalyst c, Rarity r, int total) {
        return String.valueOf(Math.round(100.0 * c.weight(r) / total));
    }

    private record Glyph(String kind, int tint) {
    }

    private static String partGlyph(@Nullable PartType type) {
        if (type == null) return "ingot";
        return switch (type.kind()) {
            case BARREL -> "barrel";
            case CELL -> "cell";
            case GRIP -> "grip";
            case OPTIC -> "optic";
            default -> "core";
        };
    }

    private static Glyph glyphOf(Job job, Level level) {
        return switch (job.kind()) {
            case PART -> new Glyph(partGlyph(WeaponData.partType(job.id())), GLYPH_METAL);
            case CHIP -> {
                Augment a = WeaponData.augment(job.id());
                yield new Glyph("chip", a == null ? GLYPH_METAL : 0xFF000000 | a.colour());
            }
            case RECIPE -> {
                FabricatingRecipe r = Fabricating.recipe(level, job.id());
                yield r == null ? new Glyph("ingot", GLYPH_METAL) : glyphOfStack(r.result());
            }
        };
    }

    private static Glyph glyphOfStack(ItemStack stack) {
        if (stack.getItem() instanceof EngramItem) return new Glyph("engram", GLYPH_METAL);
        if (stack.getItem() instanceof CoreItem) return new Glyph("core", 0xFF000000 | CoreItem.colourOf(stack));
        if (stack.getItem() instanceof CatalystItem) return new Glyph("catalyst", 0xFF000000 | CatalystItem.colourOf(stack));
        if (stack.is(ModItems.ENERGY_ROUND.get())) return new Glyph("round", GLYPH_METAL);
        return new Glyph("ingot", GLYPH_METAL);
    }

    // ---- drawing -----------------------------------------------------------------------------------------------------------------------

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
        g.fill(x + 6, y + 20, x + 294, y + 21, DIM);
        // the tabs
        for (Job.Tab t : Job.Tab.values()) {
            int tx = x + TABS_X + t.ordinal() * TAB_STEP;
            boolean on = t == tab;
            if (on) g.fill(tx, y + TABS_Y, tx + TAB_W, y + TABS_Y + TAB_H, SLOT);
            frame(g, tx, y + TABS_Y, TAB_W, TAB_H, on ? TEAL : DIM);
            g.drawString(font, tabLabels[t.ordinal()], tx + 2, y + TABS_Y + 2, on ? TEAL : FAINT, false);
        }
        // the list and its bar
        g.fill(x + LIST_X, y + LIST_Y, x + LIST_X + LIST_W, y + LIST_Y + LIST_H, SLOT);
        frame(g, x + LIST_X, y + LIST_Y, LIST_W, LIST_H, DIM);
        for (int i = 0; i < ROWS && scroll + i < rows.size(); i++) {
            Row row = rows.get(scroll + i);
            int rx = x + ROW_X, ry = y + ROW_Y + i * ROW_H;
            boolean on = scroll + i == chosenRow;
            if (on) {
                g.fill(rx - 2, ry - 1, rx - 2 + ROW_W, ry - 1 + ROW_H, CHOSEN);
                frame(g, rx - 2, ry - 1, ROW_W, ROW_H, TEAL);
            }
            glyph(g, row.glyph, rx, ry, row.tint);
            g.drawString(font, row.name, rx + 11, ry, on ? TEXT : MUTED, false);
        }
        g.fill(x + BAR_X, y + BAR_Y, x + BAR_X + 2, y + BAR_Y + BAR_H, EMPTY);
        if (rows.size() > ROWS) {
            int thumb = Math.max(8, BAR_H * ROWS / rows.size()), top = (BAR_H - thumb) * scroll / Math.max(1, rows.size() - ROWS);
            g.fill(x + BAR_X, y + BAR_Y + top, x + BAR_X + 2, y + BAR_Y + top + thumb, DIM);
        } else g.fill(x + BAR_X, y + BAR_Y, x + BAR_X + 2, y + BAR_Y + BAR_H, DIM);
        // the job's name and needs
        g.drawString(font, nameText, x + RIGHT_X, y + LIST_Y, nameText.isEmpty() || chosenRow < 0 && menu.chosenIndex() < 0 ? FAINT : TEXT, false);
        if (!need1Glyph.isEmpty() || !need2Glyph.isEmpty()) {
            g.drawString(font, needsLabel, x + RIGHT_X, y + NEEDS_Y, MUTED, false);
            if (!need1Glyph.isEmpty()) ghost(g, x + GHOST_X, y + NEEDS_Y - 1, need1Glyph, need1Tint, need1Count, need1Text);
            if (!need2Glyph.isEmpty()) ghost(g, x + GHOST_X, y + NEEDS_Y + 13, need2Glyph, need2Tint, need2Count, need2Text);
        }
        // the slots
        for (int i = 0; i < Fabricating.SLOTS; i++) {
            int sx = x + WeaponFabricatorMenu.SLOT_XY[i][0], sy = y + WeaponFabricatorMenu.SLOT_XY[i][1];
            boolean filled = !items.getStackInSlot(i).isEmpty();
            g.fill(sx, sy, sx + 18, sy + 18, SLOT);
            frame(g, sx, sy, 18, 18, filled ? DIM : EMPTY);
            if (i == Fabricating.OUT && !filled && !outGlyph.isEmpty()) glyphFaint(g, outGlyph, sx + 5, sy + 5, outTint);
            g.drawString(font, slotLabels[i], sx, sy + 19, filled || i == Fabricating.OUT && menu.chosenIndex() >= 0 ? MUTED : FAINT, false);
        }
        // the progress bar beside MAKE, its percentage inside, and the gauge
        g.fill(x + PROGRESS_X, y + PROGRESS_Y, x + PROGRESS_X + PROGRESS_W, y + PROGRESS_Y + PROGRESS_H, SLOT);
        frame(g, x + PROGRESS_X, y + PROGRESS_Y, PROGRESS_W, PROGRESS_H, working ? TEAL : DIM);
        if (progressFill > 0) g.fill(x + PROGRESS_X + 1, y + PROGRESS_Y + 1, x + PROGRESS_X + 1 + progressFill, y + PROGRESS_Y + PROGRESS_H - 1, 0x9019D3B0);
        if (!progressText.isEmpty()) g.drawString(font, progressText, x + PROGRESS_X + (PROGRESS_W - font.width(progressText)) / 2, y + PROGRESS_Y + 3, TEXT, false);
        g.fill(x + GAUGE_X, y + GAUGE_Y, x + GAUGE_X + GAUGE_W, y + GAUGE_Y + GAUGE_H, SLOT);
        frame(g, x + GAUGE_X, y + GAUGE_Y, GAUGE_W, GAUGE_H, DIM);
        if (gaugeFill > 0) g.fill(x + GAUGE_X + 1, y + GAUGE_Y + GAUGE_H - 1 - gaugeFill, x + GAUGE_X + GAUGE_W - 1, y + GAUGE_Y + GAUGE_H - 1, GOLD);
        for (int yy = y + GAUGE_Y + 8; yy < y + GAUGE_Y + GAUGE_H - 1; yy += 8) g.fill(x + GAUGE_X + 1, yy, x + GAUGE_X + GAUGE_W - 1, yy + 1, SLOT);
        g.drawString(font, feLabel, x + GAUGE_X - 1, y + GAUGE_Y + GAUGE_H + 2, gaugeFill > 0 ? GOLD : RED, false);
        // MAKE, its price, the odds, the status
        key(g, x + MAKE_X, y + MAKE_Y, MAKE_W, makeLabel, canMake);
        g.drawString(font, priceText, x + PRICE_X, y + MAKE_Y + 3, MUTED, false);
        if (!oddsText.isEmpty()) g.drawString(font, oddsText, x + RIGHT_X, y + ODDS_Y, VIOLET, false);
        for (int i = 0; i < status.size(); i++) g.drawString(font, status.get(i), x + RIGHT_X, y + STATUS_Y + i * STATUS_ROW, statusBad ? RED : MUTED, false);
    }

    private void ghost(GuiGraphics g, int x, int y, String kind, int tint, String count, String name) {
        g.fill(x, y, x + 12, y + 12, SLOT);
        frame(g, x, y, 12, 12, FAINT);
        glyphFaint(g, kind, x + 2, y + 2, tint);
        g.drawString(font, "×" + count, x + 14, y + 2, MUTED, false);
        g.drawString(font, name, x + NEED_TEXT_X - GHOST_X, y + 2, FAINT, false);
    }

    private void key(GuiGraphics g, int x, int y, int w, String label, boolean lit) {
        g.fill(x, y, x + w, y + KEY_H, SLOT);
        frame(g, x, y, w, KEY_H, lit ? TEAL : FAINT);
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + 3, lit ? TEAL : FAINT, false);
    }

    private static void frame(GuiGraphics g, int x, int y, int w, int h, int colour) {
        g.fill(x, y, x + w, y + 1, colour);
        g.fill(x, y + h - 1, x + w, y + h, colour);
        g.fill(x, y, x + 1, y + h, colour);
        g.fill(x + w - 1, y, x + w, y + h, colour);
    }

    /** A ghost: the glyph at about a third of its colour, over the slot's dark. */
    private static void glyphFaint(GuiGraphics g, String kind, int x, int y, int tint) {
        glyph(g, kind, x, y, (tint & 0xFFFFFF) | 0x60000000);
    }

    /** The eight-pixel glyphs the mock-up drew for each kind of thing; {@code m} tints the metal. */
    private static void glyph(GuiGraphics g, String kind, int x, int y, int m) {
        int a = m & 0xFF000000;
        int gold = a | (GLYPH_GOLD & 0xFFFFFF), teal = a | (TEAL & 0xFFFFFF), gun = a | (GLYPH_GUNMETAL & 0xFFFFFF), dark = a | (GLYPH_DARK & 0xFFFFFF), light = a | (GLYPH_LIGHT & 0xFFFFFF), glass = a | (GLYPH_GLASS & 0xFFFFFF), lapis = a | (GLYPH_LAPIS & 0xFFFFFF), red = a | (GLYPH_RED & 0xFFFFFF);
        switch (kind) {
            case "barrel" -> { rect(g, x, y + 3, 8, 2, m); rect(g, x, y + 2, 1, 4, gold); rect(g, x + 7, y + 3, 1, 2, teal); }
            case "cell" -> { rect(g, x + 1, y + 1, 6, 6, m); rect(g, x + 2, y + 3, 1, 2, teal); rect(g, x + 5, y + 3, 1, 2, teal); rect(g, x + 1, y, 6, 1, gold); }
            case "grip" -> { rect(g, x + 2, y, 4, 2, m); rect(g, x + 2, y + 2, 3, 2, m); rect(g, x + 1, y + 4, 3, 2, m); rect(g, x, y + 6, 3, 2, m); }
            case "optic" -> { rect(g, x, y + 3, 8, 3, m); rect(g, x + 7, y + 4, 1, 1, teal); rect(g, x + 3, y + 1, 2, 2, gold); }
            case "engram" -> { rect(g, x + 1, y, 6, 8, gun); rect(g, x + 2, y + 2, 4, 3, dark); rect(g, x + 3, y + 3, 2, 1, teal); rect(g, x + 2, y + 7, 1, 1, gold); rect(g, x + 4, y + 7, 1, 1, gold); rect(g, x + 6, y + 7, 1, 1, gold); }
            case "slate" -> { rect(g, x, y, 8, 8, gun); rect(g, x + 1, y + 1, 6, 6, dark); rect(g, x + 3, y + 1, 1, 6, light); rect(g, x + 1, y + 3, 6, 1, light); rect(g, x + 6, y + 6, 2, 2, gold); }
            case "catalyst" -> { rect(g, x + 2, y + 2, 4, 6, glass); rect(g, x + 3, y + 4, 2, 3, lapis); rect(g, x + 2, y, 4, 2, gold); rect(g, x + 4, y + 5, 1, 1, m == GLYPH_METAL ? red : m); }
            case "chip" -> { rect(g, x + 1, y + 5, 6, 3, gun); rect(g, x + 1, y + 4, 6, 1, gold); rect(g, x + 2, y + 1, 4, 3, m); rect(g, x + 3, y, 2, 1, m); }
            case "core" -> { rect(g, x + 1, y + 1, 6, 6, gun); rect(g, x + 2, y + 2, 4, 4, m); rect(g, x, y + 1, 1, 6, gold); rect(g, x + 7, y + 1, 1, 6, gold); }
            case "round" -> { rect(g, x + 1, y + 3, 5, 2, gun); rect(g, x + 6, y + 3, 2, 2, teal); rect(g, x + 2, y + 3, 1, 2, gold); }
            case "gem" -> { rect(g, x + 3, y, 2, 1, m); rect(g, x + 2, y + 1, 4, 2, m); rect(g, x + 1, y + 3, 6, 2, m); rect(g, x + 2, y + 5, 4, 1, m); rect(g, x + 3, y + 6, 2, 2, m); }
            case "dust" -> { rect(g, x + 1, y + 5, 6, 2, m); rect(g, x + 2, y + 3, 4, 2, m); rect(g, x + 3, y + 2, 2, 1, m); rect(g, x + 5, y + 1, 1, 1, m); }
            default -> { rect(g, x + 1, y + 3, 6, 3, m); rect(g, x + 2, y + 2, 4, 1, m); rect(g, x + 1, y + 3, 6, 1, a | 0x4DFFFFFF & 0x4DFFFFFF); }
        }
    }

    private static void rect(GuiGraphics g, int x, int y, int w, int h, int colour) {
        g.fill(x, y, x + w, y + h, colour);
    }

    // ---- the mouse -----------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int x = (int) mx - leftPos, y = (int) my - topPos;
            if (y >= TABS_Y && y < TABS_Y + TAB_H && x >= TABS_X) {
                int i = (x - TABS_X) / TAB_STEP;
                if (i < Job.Tab.values().length && (x - TABS_X) % TAB_STEP < TAB_W) {
                    tab = Job.Tab.values()[i];
                    return true;
                }
            }
            if (x >= ROW_X - 2 && x < ROW_X - 2 + ROW_W && y >= ROW_Y - 1 && y < ROW_Y - 1 + ROWS * ROW_H) {
                int i = scroll + (y - (ROW_Y - 1)) / ROW_H;
                if (i >= 0 && i < rows.size()) {
                    send(FabricatorActionPacket.CHOOSE, rows.get(i).index);
                    return true;
                }
            }
            if (x >= MAKE_X && x < MAKE_X + MAKE_W && y >= MAKE_Y && y < MAKE_Y + KEY_H && canMake) {
                send(FabricatorActionPacket.MAKE, 0);
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int x = (int) mx - leftPos, y = (int) my - topPos;
        if (x >= LIST_X && x < LIST_X + LIST_W && y >= LIST_Y && y < LIST_Y + LIST_H && rows.size() > ROWS) {
            scroll = Math.max(0, Math.min(rows.size() - ROWS, scroll - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    private void send(int action, int index) {
        WeaponsNetwork.CHANNEL.sendToServer(new FabricatorActionPacket(menu.pos(), action, index));
    }
}
