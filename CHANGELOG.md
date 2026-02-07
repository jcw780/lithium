_ReleaseTag_ is automatically replaced with the release tag, e.g. mc1.21.4-0.14.5
_MCVersion_ is automatically replaced with the minecraft version, e.g. 1.21.4
_LithiumVersion_ is automatically replaced with the lithium version, e.g. 0.14.5
Everything above the line is ignored and not included in the changelog. Everything below will be in the
changelog on GitHub, Modrinth and CurseForge.
----------
Lithium _LithiumVersion_ for Minecraft _MCVersion_ adds several optimizations and fixes.

Make sure to take a backup of your world before using the mod and please report any bugs and mod compatibility issues at the [issue tracker](https://github.com/CaffeineMC/lithium-fabric/issues). You can check the [description of each optimization](https://github.com/CaffeineMC/lithium/blob/_ReleaseTag_/lithium-mixin-config.md) and how to disable it when encountering a problem.

## Additions
- Optimize item frames with maps lag when many players are online
- Use chunk aware block search for mob supporting block (Thanks to jcw780)
- Update chunk serialization optimization (Thanks to ishland)
- Allow hoppers to sleep when empty input inventory has comparators
- Sculk sensor, catalyst and shrieker sleeping

## Fixes
- Use correct height limit and chunk loading order in portal POI optimization (Thanks to jcw780)
- Wake sleeping hoppers when double chests above them have comparators placed nearby