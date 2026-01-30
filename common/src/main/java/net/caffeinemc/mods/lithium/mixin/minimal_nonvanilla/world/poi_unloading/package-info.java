@MixinConfigOption(
        description = "Unload non-portal forced POIs. " +
                "Note: This will cause extra chunk loading when portals run ensureLoadedAndValid into unload areas. " +
                "However, this will only be detectable during the first portal load into an area with valid POI " +
                "sections that were loaded before by non-portal means and then unloaded. No known use is affected by this.",
        depends = {
                @MixinConfigDependency(dependencyPath = "mixin.ai.poi"),
                @MixinConfigDependency(dependencyPath = "mixin.ai.poi.reduce_poi_memory"),
        }
)

package net.caffeinemc.mods.lithium.mixin.minimal_nonvanilla.world.poi_unloading;

import net.caffeinemc.gradle.MixinConfigDependency;
import net.caffeinemc.gradle.MixinConfigOption;
