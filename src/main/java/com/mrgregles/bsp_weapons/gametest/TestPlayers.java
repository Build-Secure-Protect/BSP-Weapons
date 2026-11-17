package com.mrgregles.bsp_weapons.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.util.UUID;

/**
 * Server players for tests, as in the other BSP mods. {@link GameTestHelper#makeMockServerPlayerInLevel()} gives its
 * connection no channel, and Forge's packet listener touches the channel in its constructor, so this builds the player
 * on an in-memory netty channel instead. Packets sent to the player go nowhere. A mock player is not in the server's
 * player list, so nothing broadcast reaches it and {@code getPlayer(uuid)} does not find it; add it with
 * {@code level.addNewPlayer} when a test needs it in {@code level.players()}.
 */
public final class TestPlayers {
    private static int counter;

    private TestPlayers() {
    }

    public static ServerPlayer mockServerPlayer(GameTestHelper h) {
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection); // registers and activates the handler, which gives the connection its channel
        String name = "wp_test_" + (++counter);
        UUID uuid = UUID.randomUUID();
        ServerPlayer player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), new GameProfile(uuid, name));
        player.connection = new ServerGamePacketListenerImpl(h.getLevel().getServer(), connection, player);
        return player;
    }
}
