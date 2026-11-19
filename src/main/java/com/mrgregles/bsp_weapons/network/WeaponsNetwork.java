package com.mrgregles.bsp_weapons.network;

import com.mrgregles.bsp_weapons.BspWeapons;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** The mod's channel. Protocol 3: the part data to the client (2 added the augments, 3 the fabricator's costs, ingredients and signatures). Players need the same protocol as the server. */
public final class WeaponsNetwork {
    public static final String PROTOCOL = "3";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(BspWeapons.MODID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private WeaponsNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(WeaponDataPacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(WeaponDataPacket::encode).decoder(WeaponDataPacket::decode).consumerNetworkThread(WeaponDataPacket::handle).add();
        CHANNEL.messageBuilder(AssemblerActionPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AssemblerActionPacket::encode).decoder(AssemblerActionPacket::decode).consumerNetworkThread(AssemblerActionPacket::handle).add();
        CHANNEL.messageBuilder(EnhancerActionPacket.class, 2, NetworkDirection.PLAY_TO_SERVER)
                .encoder(EnhancerActionPacket::encode).decoder(EnhancerActionPacket::decode).consumerNetworkThread(EnhancerActionPacket::handle).add();
        CHANNEL.messageBuilder(FabricatorActionPacket.class, 3, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FabricatorActionPacket::encode).decoder(FabricatorActionPacket::decode).consumerNetworkThread(FabricatorActionPacket::handle).add();
    }

    /** On a join (one player) or a reload (everyone): the part data the client draws and reads. */
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        WeaponDataPacket packet = WeaponDataPacket.current();
        ServerPlayer player = event.getPlayer();
        if (player != null) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        else CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
    }
}
