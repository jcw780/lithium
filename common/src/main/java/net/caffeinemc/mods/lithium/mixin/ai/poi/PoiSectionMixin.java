package net.caffeinemc.mods.lithium.mixin.ai.poi;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMaps;
import net.caffeinemc.mods.lithium.common.world.interests.PointOfInterestSetExtended;
import net.caffeinemc.mods.lithium.common.world.interests.iterator.SinglePointOfInterestTypeFilter;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiSection;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Mixin(PoiSection.class)
public class PoiSectionMixin implements PointOfInterestSetExtended {
    @Mutable
    @Shadow
    @Final
    private Map<Holder<PoiType>, Set<PoiRecord>> byType;

    // Must not replace the HashMap and HashSet with optimized collections because iteration order is detectable ingame.

    private static <K, V> Iterable<? extends Map.Entry<K, V>> getPointsByTypeIterator(Map<K, V> map) {
        if (map instanceof Reference2ReferenceMap) {
            return Reference2ReferenceMaps.fastIterable((Reference2ReferenceMap<K, V>) map);
        } else {
            return map.entrySet();
        }
    }

    @Override
    public void lithium$collectMatchingPoints(Predicate<Holder<PoiType>> type, PoiManager.Occupancy status, Consumer<PoiRecord> consumer) {
        if (type instanceof SinglePointOfInterestTypeFilter) {
            this.getWithSingleTypeFilter(((SinglePointOfInterestTypeFilter) type).getType(), status, consumer);
        } else {
            this.getWithDynamicTypeFilter(type, status, consumer);
        }
    }

    private void getWithDynamicTypeFilter(Predicate<Holder<PoiType>> type, PoiManager.Occupancy status, Consumer<PoiRecord> consumer) {
        for (Map.Entry<Holder<PoiType>, Set<PoiRecord>> entry : getPointsByTypeIterator(this.byType)) {
            if (!type.test(entry.getKey())) {
                continue;
            }

            if (!entry.getValue().isEmpty()) {
                for (PoiRecord poi : entry.getValue()) {
                    if (status.getTest().test(poi)) {
                        consumer.accept(poi);
                    }
                }
            }
        }
    }

    private void getWithSingleTypeFilter(Holder<PoiType> type, PoiManager.Occupancy status, Consumer<PoiRecord> consumer) {
        Set<PoiRecord> entries = this.byType.get(type);

        if (entries == null || entries.isEmpty()) {
            return;
        }

        for (PoiRecord poi : entries) {
            if (status.getTest().test(poi)) {
                consumer.accept(poi);
            }
        }
    }
}
