package com.mrgregles.bsp_weapons.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

/**
 * BSP-Region Guard, when it is loaded: a weapon asks before it hurts a player (the {@code pvp} flag where either stands),
 * as Region Guard does for arrows. Without the mod everything is allowed. Every call to its api stays in the nested
 * class, so an older or missing Region Guard costs a check, never a crash.
 */
public final class RegionGuard {
    private static final boolean LOADED = ModList.get().isLoaded("bsp_region_guard");
    private static boolean broken;

    private RegionGuard() {
    }

    public static boolean isLoaded() {
        return LOADED && !broken;
    }

    /** Whether {@code attacker} may hurt a player standing at {@code victim} from {@code from}. */
    public static boolean pvpAllowed(ServerLevel level, BlockPos from, BlockPos victim, Player attacker) {
        if (!isLoaded()) return true;
        try {
            return Api.pvp(level, from, attacker) && Api.pvp(level, victim, attacker);
        } catch (LinkageError e) {
            // an older or newer Region Guard without this api: say so once and stop asking (its regions cannot be read)
            broken = true;
            com.mrgregles.bsp_weapons.BspWeapons.LOGGER.error("BSP-Weapons: Region Guard's api is not the one this build knows; weapons no longer ask it about PvP ({})", e.toString());
            return true;
        } catch (RuntimeException e) {
            // Region Guard is there but this call failed: never let a shot through a region on an error
            com.mrgregles.bsp_weapons.BspWeapons.LOGGER.warn("BSP-Weapons: Region Guard refused a PvP check, the shot is stopped ({})", e.toString());
            return false;
        }
    }

    private static final class Api {
        static boolean pvp(ServerLevel level, BlockPos pos, Player player) {
            return com.mrgregles.bsp_region_guard.api.RegionGuardApi.get().regions().allows(level, pos, com.mrgregles.bsp_region_guard.flag.Flags.PVP, player);
        }
    }
}
