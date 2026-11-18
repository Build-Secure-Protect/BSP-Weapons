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

## 2026-11-18: WP-6b, augment chips (unreleased)

Headless: 8 GameTests in the `enhancement` batch: the augment data carries a colour, points and whether a chip is available (seven are; Breacher and Ricochet are not); a chip remembers its augment, roll and points in three tag keys, is named and tinted by the augment, a bare chip is unknown, grey and plain; the chip tooltip shows the roll with its multiplier, each effect scaled (Overcharge at S reads +23 % damage, -23 % heat capacity), the points and what it fits, and a Breacher chip says it cannot be fitted yet; a chip's roll scales only its own effect by the part multipliers and its points change nothing; fitting spends a point and is refused for a stick, a bare chip, Breacher, a doubled augment, a chip with one point or none, a full weapon and an invalid weapon, while an S roll's extra socket takes one more; removing returns the chip with its points and swapping returns the old chip and seats the new one a point down in the same socket; wear takes a point from every chip and burns out the one at zero; the weapon tooltip counts the chips and lists each with its points. The `parts` and `part_items` batches now carry chips with rolls and points, clamp forged points, ignore the old string chip list, and check the augments arrive with the part data.

Not tested in game or headless: no screen or block exists yet, so FIT, REMOVE and swap have only been called as code; a chip refused for the weapon's kind (no second kind of weapon exists); the look of the chip icon and the tints in the inventory.

In game:
- [ ] The BSP-Weapons creative tab shows seven Augment Chips after the Energy Round: Piercing (gold), Knockback (orange), Quick-draw (teal), Stabiliser (blue), Overcharge (red), Coolant (ice), Extended Cell (violet); no Breacher or Ricochet chip. Each icon is the Shard Key: a gunmetal clasp with gold teeth and band, a tapering crystal in the augment's colour with brighter edges, read at GUI scale 2 and 3.
- [ ] A chip's tooltip reads "Piercing Chip", then "Normal · effect ×1" in violet, then "Armour piercing +10 +15%" in teal, "Points 10 / 10", "Fits any weapon", and the wear line in dark grey. `/give @s bsp_weapons:augment_chip{Augment:"bsp_weapons:overcharge",Rarity:"s",Durability:8}`: "S Tier · effect ×1.15", "Damage +23%" teal, "Heat capacity -23%" red, "Points 8 / 8".
- [ ] `/give @s bsp_weapons:augment_chip{Augment:"bsp_weapons:breacher"}`: a copper crystal and "Cannot be fitted yet" in red. A bare `/give @s bsp_weapons:augment_chip`: grey, named "Augment Chip", one red line.
- [ ] Hold a chip: it draws in the hand as the Shard Key; drop it: it lies on the ground as one.
- [ ] A Laser Pistol's tooltip now shows "Chips 0 / 2" (a Tier III core) under its roll line; give one with a chip in its tag and the chip is listed in its colour with "9 / 10".
- [ ] On a dedicated server, a client on the old jar is refused for the protocol (2); a matching client sees the chip names and colours after joining and after `/reload`.

## 2026-11-18: WP-6c, the Weapon Enhancer block (unreleased)

Headless: 6 GameTests in the `enhancer` batch: placing the machine puts its companion above facing the same way and breaking either removes both and drops exactly one machine, and a lower block with no room above cannot survive; the slots take a weapon, a chip, a Core and an ingot or gem and nothing else; FIT through the machine is refused without charge, a chip or a weapon and passes the rules' refusals through, and a fit takes one chip from the stack, burns the fit charge and keeps the weapon's own charge; REMOVE puts the chip in CHIP IN with its points for no charge and waits for CHIP IN to be emptied, and a swap needs the fit charge and one chip, seats the new chip a point down and returns the old one whole; the energy capability is on every side, intake only, takes at most the intake a tick, refuses when full, and the charge survives a save and reaches the update tag; the action packet round-trips. 47 required tests pass in all.

Not tested in game or headless: the menu opening and the stand-in screen's keys (no menu or packet handling runs headless); the Spire model in the world and in hand; charging from a real cable; the drops of the machine's contents when broken.

In game:
- [ ] Place a Weapon Enhancer facing north: the Spire stands one block wide and two high with its charge window and socket toward you, the spine at the back, the drum on top. Try to place it with a block directly above the target space: nothing is placed. Break the upper or the lower block: both go and one Weapon Enhancer drops, with anything that was in it.
- [ ] Right-click either block: a plain WEAPON ENHANCER panel opens with WEAPON, CHIP IN, CORE and REPAIR slots, "0 / 20,000 FE", and FIT and REMOVE keys (the Board comes with WP-6d). Shift-click a Laser Pistol into WEAPON and a chip into CHIP IN; a stick is refused by every slot; an iron ingot or a diamond goes into REPAIR only.
- [ ] Press FIT with no charge: the action bar reads "Not enough charge: 0 of 400 FE." Connect a Mekanism cable, a Flux Plug or any FE source to any side of the lower block: the number climbs by up to 1,000 a tick and stops at 20,000. Press FIT: the chip is gone from CHIP IN, the weapon's tooltip lists it with "9 / 10", and the number drops by 400.
- [ ] Press REMOVE: the chip comes back into CHIP IN with 9 points and the weapon's tooltip reads "Chips 0 / 2"; press it again with CHIP IN full: "Empty the CHIP IN slot first."
- [ ] Leave the world and come back: the charge and the slots' contents are still there.
- [ ] `serverconfig/bsp_weapons-server.toml` gains an `[enhancer]` section (enhancerCapacity, enhancerMaxReceive, fitCharge).

## 2026-11-18: WP-6d, the Board screen (unreleased)

Headless: 1 GameTest added to the `enhancer` batch (7 there now, 48 required in all): the checks the screen shows before a key is pressed name the same reasons as the moves (a doubled augment, CHIP IN full, no chip in the socket, no charge, no chip in CHIP IN) and move nothing. No screen code runs headless.

Not tested in game or headless: everything drawn. In particular the weapon rendered side on with the item-frame transform (R20), whose orientation and size are a guess until seen; the half-size text's legibility at GUI scale 1 and 2; four sockets on a Tier V S-tier weapon.

In game:
- [ ] Open a Weapon Enhancer: the WEAPON ENHANCER panel, 256 x 232, the gold charge gauge beside the title with "0 / 20,000 FE" under it in red, the rail across, three faint sockets numbered 1 to 3 above it wired down to the rail, WEAPON at the rail's end, CHIP IN, REPAIR and CORE in a row under it, FIT and REMOVE grey, UPGRADE and REPAIR grey, the status "Put a weapon in the WEAPON slot.", the right column "no weapon" and empty bars.
- [ ] Put a Laser Pistol (Tier III) in WEAPON: it draws side on over the rail, about 60 px long, barrel one way and grip down; the right column reads its name, "Core III  Normal  2 sockets" in violet, "CHIPS 0 / 2", two "empty socket" rows, and the bars fill teal and gold. The third socket stays faint. A Tier V S-tier pistol shows four sockets, closer together.
- [ ] Put a Piercing chip in CHIP IN: FIT turns teal and the status reads "FIT: 400 FE and a point of the chip." in grey; with no charge it reads "Not enough charge: 0 of 400 FE." in red and FIT stays grey. Charge the machine and press FIT: the chip appears in socket 1 as its Shard Key icon with ten pips over it (nine lit), the chip row names it in gold with "9/10 Rare", the bars gain a violet share on ARMOUR PIERCING, the gauge drops.
- [ ] Put an Overcharge chip in and FIT it: HEAT CAP shows a red mark and a red line where the chip costs; DAMAGE gains violet.
- [ ] Click socket 1: it frames teal and the status reads "Socket 1 chosen: REMOVE takes Piercing."; press REMOVE: the chip is in CHIP IN with 9 points, the socket is empty again. Click a socket twice: it lets go.
- [ ] With socket 2 chosen and a Coolant chip in CHIP IN, the FIT key reads SWAP; press it: Coolant sits in socket 2 and Overcharge is in CHIP IN with its points. With two chips in CHIP IN the status reads "Swap one chip at a time." in red.
- [ ] Hover a socket with a chip: the chip's tooltip. Put a second Piercing chip in CHIP IN with one already fitted: "Piercing is already fitted." in red and FIT grey.
- [ ] The whole panel fits a 320 x 240 window at GUI scale 1 and reads at scale 2.

## 2026-11-18: WP-6e, core upgrade and repair (unreleased)

Headless: 2 GameTests added to the `enhancer` batch (9 there now, 50 required in all): UPGRADE is refused with an empty CORE slot, a Core of the same tier, two Cores at once or too little charge, then gives the weapon the new tier and its sockets, wears every chip a point and burns out the one at zero with a notice naming it, leaves the old Core in the slot, burns the upgrade charge, and the returned Core cannot upgrade it again; REPAIR is refused with an empty REPAIR slot, an ingot of a material on no part, an unworn weapon or too little charge, then takes as many ingots as the wear needs (two for two steps and ten points), restores a step each, wears every chip, burns the repair charge, and the last step takes one ingot and stops at full.

Not tested in game or headless: the two keys on the Board and the burnt-out notice on the action bar; a gem repairing an optic; the weapon's tooltip and bars after an upgrade (durability grows, the damage value stays).

In game:
- [ ] Put a worn Tier II Laser Pistol with a chip at 1 point in WEAPON and a Tier IV Weapon Core in CORE: UPGRADE turns teal and the status reads "UPGRADE: 2,000 FE and a point of every chip."; a second Tier II Core instead reads "The Core must be higher than Tier II." in red. Press UPGRADE: the tooltip reads Core IV with its sockets, the chip at 1 is gone and the action bar reads "<Augment> burnt out.", a Tier II Core sits in CORE, the gauge drops by 2,000.
- [ ] Fire a pistol until its durability bar is well down. Put it in WEAPON and an ingot of one of its parts' materials in REPAIR (iron for an iron grip; a Tetrium ingot for a Tetrium barrel): REPAIR turns teal, "REPAIR: 1,000 FE and a point of every chip."; a copper ingot reads "Copper Ingot is not a material on this weapon." Press REPAIR with a stack of ingots: the bar fills by a quarter per ingot taken, the stack shrinks by the number used, the rest stays; at full the key greys with "The weapon is not worn."
- [ ] `serverconfig/bsp_weapons-server.toml` `[enhancer]` now has upgradeCharge, repairCharge and repairShare.

## 2026-11-18: WP-6f, Knockback's push (unreleased)

Headless: 1 GameTest in the `firing_push` batch (51 required in all): a plain pistol and a Knockback pistol fire down two lanes at zombies that cannot walk; after three seconds both are hurt, the Knockback zombie has been thrown back along the shot more than half a block and further than the plain hit moved its zombie; `Firing.pushOf` gives the chip's block scaled by the config. The `enhancement` data test checks Knockback's push is one block and Piercing's none.

Not tested in game or headless: how far "about a block" is on real ground against a walking mob and against a player; the push on a mob with knockback resistance; the Knockback chip's tooltip line.

In game:
- [ ] Fit a Knockback chip (Normal) to a Laser Pistol; its chip tooltip reads "Hits throw the target back about 1.0 blocks" in teal under "Damage -5%" in red. Shoot a zombie on flat ground: it slides back about a block further than a shot from a chipless pistol throws it. An S Knockback chip reads "about 1.2 blocks".
- [ ] Shoot an iron golem: it barely moves (knockback resistance applies to the push as to any knockback).
- [ ] `serverconfig/bsp_weapons-server.toml` `[firing]` gains `pushScale`; at 0 the Knockback chip throws nothing extra, at 3 a zombie flies several blocks.

