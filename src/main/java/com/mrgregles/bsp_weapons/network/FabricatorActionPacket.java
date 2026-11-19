package com.mrgregles.bsp_weapons.network;

import com.mrgregles.bsp_weapons.fabricator.Fabricating;
import com.mrgregles.bsp_weapons.fabricator.Job;
import com.mrgregles.bsp_weapons.fabricator.WeaponFabricatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * A click in the fabricator's screen: CHOOSE a catalogue row by its index (R27), or MAKE the chosen one (D109). The
 * server checks the player has that fabricator's screen open; a refusal goes back as the action-bar message.
 */
public record FabricatorActionPacket(BlockPos pos, int action, int index) {
    public static final int CHOOSE = 0, MAKE = 1;

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(action);
        buf.writeShort(index);
    }

    public static FabricatorActionPacket decode(FriendlyByteBuf buf) {
        return new FabricatorActionPacket(buf.readBlockPos(), buf.readByte(), buf.readShort());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            Component message = apply(player, pos, action, index);
            if (message != null) player.displayClientMessage(message, true);
        });
        ctx.get().setPacketHandled(true);
    }

    /** Runs the action for a player with the fabricator's screen open at {@code pos}; returns the message to show, or null. */
    @Nullable
    public static Component apply(ServerPlayer player, BlockPos pos, int action, int index) {
        if (!(player.containerMenu instanceof WeaponFabricatorMenu menu) || !menu.pos().equals(pos) || !menu.stillValid(player)) return null;
        switch (action) {
            case CHOOSE -> {
                List<Job> jobs = Fabricating.catalogue(player.level());
                menu.blockEntity().choose(index >= 0 && index < jobs.size() ? jobs.get(index) : null);
                return null;
            }
            case MAKE -> {
                return menu.blockEntity().make();
            }
            default -> {
                return null;
            }
        }
    }
}
