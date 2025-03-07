_ReleaseTag_ is automatically replaced with the release tag, e.g. mc1.21.4-0.14.5
_MCVersion_ is automatically replaced with the minecraft version, e.g. 1.21.4
_LithiumVersion_ is automatically replaced with the lithium version, e.g. 0.14.5
Everything above the line is ignored and not included in the changelog. Everything below will be in the
changelog on GitHub, Modrinth and CurseForge.
----------
Lithium _LithiumVersion_ for Minecraft _MCVersion_ includes new optimizations and mod compatibility improvements.

Make sure to take a backup of your world before using the mod and please report any bugs and mod compatibility issues at the [issue tracker](https://github.com/CaffeineMC/lithium-fabric/issues). You can check the [description of each optimization](https://github.com/CaffeineMC/lithium/blob/_ReleaseTag_/lithium-mixin-config.md) and how to disable it when encountering a problem.

## Additions
- explosion entity raycast optimizations (Thanks to RacoonDog)
- Optimize projectile entity collisions attempts with uncollidable entities

## Fixes
- Fix crash with Create Pondering when entities are pushed by fluids
- Fix crash when placing a block at build limit in the end and attempting to spawn a dragon
- Fix hoppers sleeping even though interaction with neoforge API block inventories is possible
- Fix sleeping hoppers woken up by moving item entities even if blocked with a full block
- Fix broken state of ChunkAwareBlockCollisionSweeper in entity nether portal positioning optimization and with TIS-Carpet