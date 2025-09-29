_ReleaseTag_ is automatically replaced with the release tag, e.g. mc1.21.4-0.14.5
_MCVersion_ is automatically replaced with the minecraft version, e.g. 1.21.4
_LithiumVersion_ is automatically replaced with the lithium version, e.g. 0.14.5
Everything above the line is ignored and not included in the changelog. Everything below will be in the
changelog on GitHub, Modrinth and CurseForge.
----------
Lithium _LithiumVersion_ for Minecraft _MCVersion_ adds several new optimizations.

Make sure to take a backup of your world before using the mod and please report any bugs and mod compatibility issues at the [issue tracker](https://github.com/CaffeineMC/lithium-fabric/issues). You can check the [description of each optimization](https://github.com/CaffeineMC/lithium/blob/_ReleaseTag_/lithium-mixin-config.md) and how to disable it when encountering a problem.

## Additions
- Optimize piglin/hoglin repellent and turtle egg searches (Thanks to jcw780)
- Optimize random ticking
- Optimize precipitation

## Changes
- Re-enable previously broken inline world height optimization

## Fixes
- Fix missing initialization of block counting in empty sections
- Fix compatibility issue that convert client side mobs to nbt
- Fix incorrect section y coordinates used in experimental optimization (Thanks to jcw780)