package com.mrgregles.bsp_weapons.gametest;

import com.mrgregles.bsp_weapons.BspWeapons;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Proves the GameTest wiring: the mod is loaded in the test server with its required mods (BSP-Core and GeckoLib, as
 * dev runs load them) and a mock player can be made. Every server-side rule added later gets its own test class next
 * to this one.
 */
@GameTestHolder(BspWeapons.MODID)
@PrefixGameTestTemplate(false)
public class SetupGameTests {
    /** The empty 24 x 10 x 24 template every test fills itself (tools/gen_gametest_structures.py). */
    public static final String EMPTY = "empty";

    @GameTest(template = EMPTY)
    public void mod_is_loaded_with_its_required_mods_and_a_mock_player_can_be_made(GameTestHelper h) {
        if (!ModList.get().isLoaded(BspWeapons.MODID)) {
            h.fail("bsp_weapons is not loaded");
            return;
        }
        if (!ModList.get().isLoaded("bsp_core")) {
            h.fail("bsp_core is not loaded in the dev run");
            return;
        }
        if (!ModList.get().isLoaded("geckolib")) {
            h.fail("geckolib is not loaded in the dev run");
            return;
        }
        ServerPlayer player = TestPlayers.mockServerPlayer(h);
        if (player.getGameProfile().getName().isEmpty()) {
            h.fail("mock player has no name");
            return;
        }
        h.succeed();
    }
}
