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

## 2026-11-18: WP-3, the Weapon Assembler (unreleased)

Headless: 6 GameTests in the `assembler` batch: placing the machine puts its companion on the viewer's right (facing south, east; facing east, north) and breaking either block removes both and drops exactly one machine; assembling makes the weapon with the right spec, uses the Core, the parts and one catalyst, keeps the Engram, and rolls Normal without a catalyst; assembly is refused with an empty slot, a missing Engram or Core, and the slot rules take only the right items; taking apart costs a catalyst, returns the Core and parts with their own rolls, is refused without a catalyst or with a needed slot full; the weapon item tints each slot from its material and the Core from its tier and shows its sheet; patterns and catalysts reach the client. No menu, screen or packet-handling code runs headless.

In game:
- [ ] Place a Weapon Assembler while facing north: the pod (glass dome) is on your left, the console on your right, and the machine is two blocks wide, a block and a half tall, one deep; the second space is solid but draws nothing of its own. Place one facing each way; the companion never overwrites a block, and placing against a wall is refused.
- [ ] Break the companion space: the whole machine goes and exactly one Weapon Assembler drops. Break the master: the same. Items left inside drop.
- [ ] Right-click either space: the WEAPON ASSEMBLER screen opens, 256 x 232, dark panel, the rail across the middle, the empty slots named faintly (ENGRAM, OPTIC, CORE, BARREL, GRIP, CELL, ROLL, WEAPON), the bars at the right, BUILD lit and PLAN dim. At GUI scale 1 in a 320 x 240 window the whole screen shows.
- [ ] Put in a Laser Pistol Engram, a Weapon Core, a barrel, cell, grip and optic (any materials from `/give`, see WP-2): the status line names what is missing until all are in, the bars grow as parts go in (teal), ASSEMBLE lights teal. Click it: the pistol appears in the WEAPON slot with gold bars, the Core and parts are gone, the Engram stays. The pistol's tooltip shows its rarity and Core tier, seven stats and four part lines; its icon is the pistol with each part in its material's colour.
- [ ] With a Lapis Catalyst in ROLL, the odds line reads "roll: Normal 70 · Rare 25 · Epic 5 · S 0" and one catalyst is spent per assembly; with an Illyrium Catalyst every weapon comes out Epic or S Tier; the Illyrium Catalyst's motes are turquoise.
- [ ] TAKE APART is grey until a weapon you have just assembled sits in WEAPON and a catalyst in ROLL (the WEAPON slot takes no weapon from your inventory); then it spends the catalyst and puts the Core and parts back in their slots. With no catalyst it stays grey and does nothing.
- [ ] Hold the Laser Pistol: it draws in the hand as the voxel pistol, about two thirds of a block long, barrel forward, grip down (the first-person transform is a guess until seen: note which way it points). On the ground and in an item frame it draws as the pistol too.
- [ ] Shift-click moves parts between the inventory and their slots; wrong items are refused by the slots.
- [ ] On a dedicated server the screen's numbers and odds match the server's (the pattern and catalyst data arrive with the rest).

## 2026-11-18: WP-4, firing the Laser Pistol (unreleased)

Headless: 6 GameTests in the `firing`, `firing_hit` and `firing_range` batches: a shot spends a charge, adds heat and wear, spawns one lance that is the shooter's and flies along the look at the config's speed, and the next shot waits for the cooldown; a shot at the heat capacity locks the weapon and costs the overheat durability, an empty weapon starts its reload for the sheet's Recharge and is full after it; the lance hurts the zombie in its path, stops at a wall and is gone after the hit; a lance fades at its range; piercing lets its share through armour whole and never above 100 %, and the Region Guard adapter allows everything when the mod is absent; a weapon takes Unbreaking and Mending and refuses bow and sword enchantments. Nothing client-side runs headless: the lance's look, the rings, the HUD and the held weapon's red tells are unseen.

In game:
- [ ] Assemble a Laser Pistol (WP-3) and right-click: a teal lance leaves the gun and crosses the world at about two and a half blocks a tick, a ring of teal motes opening behind it as it goes; where it lands, a ring opens on the surface. It can be dodged at range (D65). The gun kicks and the vanilla cooldown shade shows on the hotbar between shots.
- [ ] The HUD: while a BSP weapon is held, a HEAT bar at the bottom right and the charge "24 / 24" at the bottom left, above the hotbar's corners and clear of the health and hunger bars (also clear of BSP-Core's Recall and Sentinel overlays, and the Steal overlay during a steal: note any overlap). Nothing with another item held.
- [ ] Fire fast: the bar fills and turns red past three quarters; the Cell's windows and the bore flash red on the held gun (D66); at the cap the bar is full, OVERHEATED blinks over it, the Core breathes red and the gun will not fire for a second and a half; the weapon loses extra durability (watch the durability bar on the item).
- [ ] Empty the charge: RELOADING shows over the bar, the count reads 0, and after the sheet's Recharge (3 s for the base pistol) the count is full again.
- [ ] Shoot a zombie: it takes damage and the death message, if it dies, reads "Zombie was cut down by <you>'s laser" in the log; shoot a friend in a PvP region and in a no-PvP region of Region Guard: hurt in the first, nothing in the second, and a region where only one of you stands in no-PvP also blocks it.
- [ ] Shoot a wall: the lance stops with its ring on the wall and nothing behind it is hurt. Shoot across a long open space: the lance fades at the sheet's Range.
- [ ] A kill with the pistol counts on BSP-RankUp as the player's kill (check the player's `player_kills` count or a rank-up).
- [ ] Enchant the pistol at a table: only Unbreaking (and Mending from a book) go on; Unbreaking cuts the wear per shot; a weapon worn to its last point refuses to fire.
- [ ] With Oculus and a shader on, the lance still draws bright (it uses the lightning render type).

## 2026-11-18: WP-5a, energy cells (unreleased)

Headless: 3 GameTests in the `energy` batch: a weapon with an energy cell offers a Forge Energy store of Capacity shots' worth (full from the assembler by config), gives nothing out, and a magazine weapon offers none; charging is limited to a full store per Recharge time, a simulation stores nothing, and a full store takes nothing; a shot costs one shot's energy, an empty store refuses without the free reload of a magazine, and a charged one fires again.

In game:
- [ ] Assemble a Laser Pistol with an Energy Cell: its tooltip shows "Energy 6,000 / 6,000 FE" (24 shots at 250 FE); fire it empty and the HUD reads 0 with NO CHARGE in red, and nothing refills on its own.
- [ ] Put the empty pistol in a Mekanism Energy Cube's charge slot, a Flux Plug's charger or a Thermal charger: it fills over about three seconds at a strong source (the base Recharge), slower at a weak one; the tooltip's FE count rises; it fires again.
- [ ] A pistol built with a Magazine cell shows no FE line and cannot be charged; it still reloads for free when empty (rounds come with WP-5b).

## 2026-11-18: WP-5b, magazines and rounds (unreleased)

Headless: 2 GameTests in the `rounds` batch: an empty magazine weapon starts its reload when the player carries rounds and at the end takes Capacity rounds from the inventory, or fewer when fewer are there; with no rounds it neither reloads nor fires until rounds arrive, a holder that is not a player refills freely, and rounds stack to 64.

In game:
- [ ] The Energy Round is in the BSP-Weapons creative tab as a small capsule icon (gunmetal, teal cap, gold band) and stacks to 64. No recipe yet (the parts machine, WP-7).
- [ ] Assemble a Laser Pistol with a Magazine: its tooltip reads "Rounds 29 / 29" (the magazine's +20 % capacity) and no FE line; fire it empty with rounds in your inventory: RELOADING shows, and after the Recharge the count is full and the rounds are gone from the inventory. With three rounds carried it loads three.
- [ ] With no rounds carried, the empty weapon shows NO ROUNDS in red and does nothing on right-click; pick up rounds and the next right-click starts the reload.
