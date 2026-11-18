package com.mrgregles.bsp_weapons.network;

import com.mrgregles.bsp_weapons.assembler.Assembly;
import com.mrgregles.bsp_weapons.assembler.WeaponAssemblerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** A click on ASSEMBLE or TAKE APART; the server checks the player has that assembler's screen open and runs the rule. */
public record AssemblerActionPacket(BlockPos pos, int action) {
    public static final int ASSEMBLE = 0, TAKE_APART = 1;

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(action);
    }

    public static AssemblerActionPacket decode(FriendlyByteBuf buf) {
        return new AssemblerActionPacket(buf.readBlockPos(), buf.readByte());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || !(player.containerMenu instanceof WeaponAssemblerMenu menu) || !menu.pos().equals(pos) || !menu.stillValid(player)) return;
            String why = action == ASSEMBLE ? Assembly.assemble(menu.blockEntity().items(), player.getRandom()) : Assembly.takeApart(menu.blockEntity().items());
            if (why != null) player.displayClientMessage(Component.literal(why), true);
        });
        ctx.get().setPacketHandled(true);
    }
}
