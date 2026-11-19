package com.mrgregles.bsp_weapons.fabricator;

import com.google.gson.JsonObject;
import com.mrgregles.bsp_weapons.parts.Need;
import com.mrgregles.bsp_weapons.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * A fixed product of the Weapon Fabricator (D112): an engram, a Weapon Core, a catalyst or rounds. Read from
 * {@code data/<namespace>/recipes/} as recipe type {@code bsp_weapons:fabricating}, so datapacks, KubeJS, CraftTweaker
 * and JEI see it like any machine's recipe. {@code kind} names the charge the job draws (D114): {@code engram},
 * {@code core} (per tier of the Core made), {@code catalyst}, {@code round}, or anything else for the plain charge.
 * {@code material} goes in the machine's MATERIAL slot and {@code extra}, if any, in its second slot (D113). Parts and
 * chips are not recipes: they come from the part-type and augment data.
 */
public record FabricatingRecipe(ResourceLocation id, String kind, Need material, @Nullable Need extra, ItemStack result) implements Recipe<Container> {
    public static final String ENGRAM = "engram", CORE = "core", CATALYST = "catalyst", ROUND = "round";

    /** Whether this server has the items the recipe asks for and makes (D122). */
    public boolean obtainable() {
        return material.obtainable() && (extra == null || extra.obtainable()) && !result.isEmpty();
    }

    /** The container's MATERIAL slot holds enough material and its second slot enough of the extra, if the recipe has one. */
    @Override
    public boolean matches(Container container, Level level) {
        if (!material.satisfiedBy(container.getItem(Fabricating.MATERIAL))) return false;
        return extra == null || extra.satisfiedBy(container.getItem(Fabricating.EXTRA));
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess access) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(material.ingredient());
        if (extra != null) list.add(extra.ingredient());
        return list;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.FABRICATING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.FABRICATING.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static final class Serializer implements RecipeSerializer<FabricatingRecipe> {
        @Override
        public FabricatingRecipe fromJson(ResourceLocation id, JsonObject json) {
            String kind = GsonHelper.getAsString(json, "kind");
            if (!kind.matches("[a-z_]{1,32}")) throw new IllegalArgumentException(id + ": kind must be a short lower-case word, not '" + kind + "'");
            Need material = Need.fromJson(GsonHelper.getAsJsonObject(json, "material"));
            Need extra = json.has("extra") ? Need.fromJson(GsonHelper.getAsJsonObject(json, "extra")) : null;
            ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            if (result.isEmpty()) throw new IllegalArgumentException(id + ": the result is empty");
            return new FabricatingRecipe(id, kind, material, extra, result);
        }

        @Override
        public FabricatingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            String kind = buf.readUtf(32);
            Need material = Need.read(buf);
            Need extra = buf.readBoolean() ? Need.read(buf) : null;
            ItemStack result = buf.readItem();
            return new FabricatingRecipe(id, kind, material, extra, result);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, FabricatingRecipe recipe) {
            buf.writeUtf(recipe.kind, 32);
            recipe.material.write(buf);
            buf.writeBoolean(recipe.extra != null);
            if (recipe.extra != null) recipe.extra.write(buf);
            buf.writeItem(recipe.result);
        }
    }
}
