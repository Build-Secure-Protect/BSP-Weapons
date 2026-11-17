# Testing

Headless checks run with `./gradlew runGameTestServer`. The sections below are the in-game checks, one per task, newest at the bottom.

## 2026-11-16: WP-0, the project (unreleased)

Headless: 1 GameTest passes (`mod_is_loaded_with_its_required_mods_and_a_mock_player_can_be_made`).

In game:
- [ ] Put `bsp-weapons-0.1.0.jar` in a client's and a server's `mods` folder beside BSP-Core 1.17.1 and GeckoLib 4.7.3. The game and the server start, and the mod list shows "BSP-Weapons 0.1.0".
- [ ] Remove BSP-Core or GeckoLib from a test server: Forge refuses to start with a message naming the missing mod (both are required).
- [ ] On a Mohist server, the log shows "BSP-Weapons loaded".
- [ ] In the pack's client (295 mods), the game reaches the title screen with the mod in the list.

## 2026-11-17: WP-1, the part system's data model (unreleased)

Headless: 7 GameTests in the `parts` batch: the data files load and every material keys to a forge ingot or gem tag (gems for optics only); the sheet follows the layers in order (base, core tier the right way for every stat, parts, the weapon's roll, chips); a part's own roll scales only that part's change; piercing and range stop at their caps and nothing goes negative; the spec survives its tag, ignores forged numbers, caps forged lists and turns junk into something validation refuses; validation names an empty slot, a type in the wrong slot, a gem on a barrel, unknown ids, a bad tier, too many chips and a doubled chip; rolls follow the catalysts' odds over 4,000 draws.

In game (nothing is visible yet: no items exist until WP-2):
- [ ] Start a world with the mod: the log shows "BSP-Weapons: 1 patterns loaded", "105 materials loaded", "5 bands loaded", "14 part_types loaded", "9 augments loaded", "4 catalysts loaded".
- [ ] `serverconfig/bsp_weapons-server.toml` appears in the world with the `[sheet]` section (tier and rarity multipliers, augment slots, caps).
- [ ] Add a datapack with a broken file at `data/bsp_weapons/weapons/materials/test.json` (for example `{"tag": 5}`): the log shows one "skipped" line naming it and the server still starts; `/reload` with the file fixed loads it.

## 2026-11-17: WP-2, the part items (unreleased)

Headless: 6 GameTests in the `part_items` batch: every part type in the data is an item of its kind and the Weapon Core is registered; a part remembers only its material and roll (two tag keys), a bare stack is an unknown material tinted grey; a core keeps its tier within the config's tiers and its tooltip names the tier; a part's tooltip shows rarity and material first and then its own changes, one line for an unknown material; the part data packet round-trips every material, band and part type under 64 KB; a pistol assembled from items is the same weapon as its spec.

In game (the first things to see):
- [ ] The BSP-Weapons creative tab shows five Weapon Cores (Tier I to V, each glowing its colour: grey, teal, blue, violet, gold) and fourteen parts in iron. Every icon is a small three-quarter view of the part's voxel model with pixel shading (D48); nothing is a flat picture or a missing-texture square.
- [ ] `/give @s bsp_weapons:focused_barrel{Material:"bsp_weapons:illyrium",Rarity:"epic"}`: the barrel's metal turns pale gold; the tooltip reads "Focused Barrel", then "Epic · Illyrium" in violet, then grey stat names with teal numbers (Damage, Range, Spread, Durability, Heat capacity). Try `bsp_weapons:magnatite` (blue-violet) and `bsp_weapons:steel` (grey).
- [ ] `/give @s bsp_weapons:scope{Material:"bsp_weapons:diamond"}`: tinted cyan. `/give @s bsp_weapons:focused_barrel{Material:"bsp_weapons:diamond"}`: the barrel tints, but the tooltip and the assembler (later) refuse a gem barrel; a bare `/give @s bsp_weapons:focused_barrel` shows "Normal · Unknown material" and a grey tint.
- [ ] The icons read at GUI scale 2 and 3 in a hotbar and a chest; note any part that turns to mush at scale 1, and whether the gui scale per model (a long barrel draws smaller than a cell) looks right.
- [ ] Hold a part: it draws in the hand as a small voxel object; drop it: it lies on the ground as one.
- [ ] On a dedicated server, join with a client: the log shows the join and the client's tooltips show real material names and numbers (the part data packet arrived); `/reload` on the server keeps them.
- [ ] With Oculus and a shader pack on, the tinted parts still show their colours in the inventory.
