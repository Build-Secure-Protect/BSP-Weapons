# Changelog

All notable changes to this project are documented in this file. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- The project: a Forge 1.20.1 mod (`bsp_weapons`) that requires BSP-Core 1.17.1 and GeckoLib 4.7.3 and works with BSP-Region Guard, BSP-RankUp, BSP-InventorySync and Mekanism when they are present. Headless GameTests run with `./gradlew runGameTestServer`.
- The part system's data model. A weapon is a pattern (its class, slots and base numbers), a core tier (I to V), parts (each a kind, a type and a material, with its own roll), the weapon's roll (Normal, Rare, Epic, S) and augment chips; its sheet (damage, armour piercing, range, fire rate, charge time, capacity, recharge, spread, zoom, heat capacity, block damage, ricochets, durability) is worked out on the server from those alone. Patterns, materials, bands, part types, augments and catalysts are data files under `data/bsp_weapons/weapons/`, so a datapack can add or change any of them: 105 materials keyed to `forge:ingots/` and `forge:gems/` tags in five bands, 14 part types, 9 augments, 4 catalysts and the laser pistol pattern. Server config `bsp_weapons-server.toml`: the tier and rarity multipliers, augment slots per tier, the piercing and range caps.
- The part items: fourteen parts (Focused, Scatter, Thermal and Pulse Barrels; Energy, Overcharge and EMP Cells and the Magazine; Standard, Light and Heavy Grips; Iron Sight, Scope and Holo Sight) and the Weapon Core (Tier I to V). A part's icon is its own voxel model, tinted by the material it is made of; the tooltip shows its rarity and material and its own changes to the sheet. A BSP-Weapons creative tab. The part data (materials, bands, part types) is sent to every client on joining and on `/reload`, so icons and tooltips match the server's data; players need the same network protocol (1) as the server.
