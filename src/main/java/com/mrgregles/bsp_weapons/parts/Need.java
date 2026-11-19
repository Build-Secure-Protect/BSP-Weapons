package com.mrgregles.bsp_weapons.parts;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.ITag;

/**
 * Something the Weapon Fabricator asks for (D101, D102, D112 to D118): so many of an item, or so many of anything in an
 * item tag. Written in JSON as {@code {"tag": "forge:gems/diamond", "count": 1}} or {@code {"item": "minecraft:redstone",
 * "count": 2}}. A need whose tag has no item on this server cannot be met, and the job that asks for it is left out of the
 * catalogue (D122).
 */
public record Need(ResourceLocation id, boolean tag, int count) {
    public static final int MAX_COUNT = 64;

    public Need {
        if (count < 1 || count > MAX_COUNT) throw new IllegalArgumentException(id + ": count must be 1 to " + MAX_COUNT + ", not " + count);
    }

    public static Need ofTag(String tag, int count) {
        return new Need(new ResourceLocation(tag), true, count);
    }

    public static Need ofItem(String item, int count) {
        return new Need(new ResourceLocation(item), false, count);
    }

    public static Need fromJson(JsonObject o) {
        int count = o.has("count") ? o.get("count").getAsInt() : 1;
        if (o.has("tag") && o.has("item")) throw new IllegalArgumentException("a need names a tag or an item, not both");
        if (o.has("tag")) return new Need(new ResourceLocation(o.get("tag").getAsString()), true, count);
        if (o.has("item")) return new Need(new ResourceLocation(o.get("item").getAsString()), false, count);
        throw new IllegalArgumentException("a need names a tag or an item");
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty(tag ? "tag" : "item", id.toString());
        o.addProperty("count", count);
        return o;
    }

    public TagKey<Item> tagKey() {
        return TagKey.create(Registries.ITEM, id);
    }

    /** Whether the stack is the right thing, whatever its count. */
    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return tag ? stack.is(tagKey()) : ForgeRegistries.ITEMS.getKey(stack.getItem()).equals(id);
    }

    /** Whether the stack is the right thing and holds enough. */
    public boolean satisfiedBy(ItemStack stack) {
        return matches(stack) && stack.getCount() >= count;
    }

    /** Whether this server has any item that meets the need: a tag with members, or a registered item. */
    public boolean obtainable() {
        if (!tag) return ForgeRegistries.ITEMS.containsKey(id);
        ITag<Item> t = ForgeRegistries.ITEMS.tags() == null ? null : ForgeRegistries.ITEMS.tags().getTag(tagKey());
        return t != null && !t.isEmpty();
    }

    /** The need as a recipe ingredient (for JEI and the recipe's ingredient list). */
    public Ingredient ingredient() {
        return tag ? Ingredient.of(tagKey()) : Ingredient.of(ForgeRegistries.ITEMS.getValue(id));
    }

    /** A name for a sentence: the item's, or the tag's first item's, or the tag itself when nothing on this server is in it. */
    public Component name() {
        if (!tag) {
            Item item = ForgeRegistries.ITEMS.getValue(id);
            return item == null ? Component.literal(id.toString()) : new ItemStack(item).getHoverName();
        }
        ITag<Item> t = ForgeRegistries.ITEMS.tags() == null ? null : ForgeRegistries.ITEMS.tags().getTag(tagKey());
        if (t == null || t.isEmpty()) return Component.literal("#" + id);
        return new ItemStack(t.iterator().next()).getHoverName();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeBoolean(tag);
        buf.writeVarInt(count);
    }

    public static Need read(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readResourceLocation();
        boolean tag = buf.readBoolean();
        int count = Math.max(1, Math.min(MAX_COUNT, buf.readVarInt()));
        return new Need(id, tag, count);
    }
}
