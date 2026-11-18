package com.mrgregles.bsp_weapons.network;

import com.mrgregles.bsp_weapons.enhancer.Enhancing;
import com.mrgregles.bsp_weapons.enhancer.WeaponEnhancerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * A click on FIT, REMOVE or a swap in the enhancer's screen, with the chosen socket (R21); the server checks the player
 * has that enhancer's screen open and runs the operation. A refusal, or a notice such as a chip burning out, goes back
 * as the action-bar message.
 */
public record EnhancerActionPacket(BlockPos pos, int action, int socket) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(action);
        buf.writeByte(socket);
    }

    public static EnhancerActionPacket decode(FriendlyByteBuf buf) {
        return new EnhancerActionPacket(buf.readBlockPos(), buf.readByte(), buf.readByte());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !(player.containerMenu instanceof WeaponEnhancerMenu menu) || !menu.pos().equals(pos) || !menu.stillValid(player)) return;
            Enhancing.Report report = Enhancing.run(action, menu.blockEntity().items(), menu.blockEntity().energy(), socket);
            if (report.message() != null) player.displayClientMessage(report.message(), true);
            if (report.ok()) menu.blockEntity().startWorking(action);
        });
        ctx.get().setPacketHandled(true);
    }
}
