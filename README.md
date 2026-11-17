# BSP-Weapons

Sci-fi weapons for the Build Secure Protect (BSP) modpack: Minecraft 1.20.1, Forge 47.4.x. MIT.

Weapons are built from interchangeable parts. A **pattern** names a weapon class and the parts it takes; a **core** sets the weapon's tier; **barrels, cells, grips, optics** and the other parts are made of the pack's metals, alloys and gems, each with its own strengths; an assembled weapon **rolls** a rarity, and **augment chips** are fitted afterwards. The same system covers hand-held guns, grenades and turrets.

Planned contents:

- Hand-held and throwable: laser pistol, rapid-fire laser rifle, rail gun, projectile launcher, sniper rifle, EMP rifle, plasma grenade, EMP grenade.
- Turrets: a mounted turret built from blocks, a remote-controlled turret with a view down the barrel, an automatic turret with per-turret target settings.
- Machines: parts manufacturing, weapon assembly, weapon enhancement, turret base, turret remote, turret AI.
- A test dummy and a target block that show the damage per hit and per second of any weapon.

The mod is in development: nothing is playable yet.

## Requirements

- Minecraft 1.20.1 with Forge 47.4.10 or newer.
- BSP-Core 1.17.1 or newer and GeckoLib 4.7.3 or newer (both required).
- Optional: BSP-Region Guard 0.7.0 (region rules for firing, block damage and turrets), BSP-RankUp 0.6.0 (kills count for ranks), BSP-InventorySync 0.8.0 (weapons follow players between servers), Mekanism 10.4 (gas and infusion recipes for parts).

## Building

`./gradlew build` writes `build/libs/bsp-weapons-<version>.jar`. The BSP mods are compiled against the jars in the sibling repositories' `build/libs` folders (`../BSP-Core`, `../BSP-Region Guard`, `../BSP-RankUp`, `../BSP-InventorySync`), so build those first. GeckoLib and Mekanism come from their own Maven repositories.

`./gradlew runGameTestServer` runs the headless GameTests. `-PwithBsp` also loads the optional BSP mods, `-PwithMek` loads Mekanism.

## Documentation

- `CHANGELOG.md`: what changed in each version.
- `docs/TESTING.md`: the in-game checks for each change.
