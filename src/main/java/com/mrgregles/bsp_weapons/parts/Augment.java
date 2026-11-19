package com.mrgregles.bsp_weapons.parts;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

/**
 * A chip fitted at the Weapon Enhancer (D9, D25, D71, D73, D83): the last layer of the sheet. Empty {@code kinds} or
 * {@code classes} means any weapon. {@code colour} tints the chip item; {@code durability} is the points a fresh chip
 * has, one spent on every fit, upgrade and repair; an augment that is not {@code available} stays in the data but has
 * no chip in the creative tab and is refused by the machine until the effect it names exists. {@code push} is the blocks a hit
 * throws the target back (D74; the Knockback chip), 0 for most. {@code ingredients} is what the Weapon Fabricator takes
 * for one chip (D101, D118): the first goes in its MATERIAL slot, the second in its second slot; none means no recipe.
 */
public record Augment(ResourceLocation id, Modifiers modifiers, Set<WeaponKind> kinds, Set<ResourceLocation> classes, int colour, int durability, boolean available, double push,
                      List<Need> ingredients) {
    public static final int DEFAULT_COLOUR = 0x9AA3B5;
    public static final int DEFAULT_DURABILITY = 10;
    public static final int MAX_INGREDIENTS = 2;

    public Augment {
        if (ingredients.size() > MAX_INGREDIENTS) throw new IllegalArgumentException(id + ": at most " + MAX_INGREDIENTS + " ingredients (the material and the second slot)");
        ingredients = List.copyOf(ingredients);
    }

    public Augment(ResourceLocation id, Modifiers modifiers, Set<WeaponKind> kinds, Set<ResourceLocation> classes, int colour, int durability, boolean available, double push) {
        this(id, modifiers, kinds, classes, colour, durability, available, push, List.of());
    }

    public boolean allowedOn(Pattern pattern) {
        return (kinds.isEmpty() || kinds.contains(pattern.kind())) && (classes.isEmpty() || classes.contains(pattern.id()));
    }

    /** The lang key of the augment's name: {@code augment.<namespace>.<path>}. */
    public String nameKey() {
        return "augment." + id.getNamespace() + "." + id.getPath();
    }
}
