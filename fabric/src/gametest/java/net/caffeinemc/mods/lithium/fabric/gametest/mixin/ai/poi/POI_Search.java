package net.caffeinemc.mods.lithium.fabric.gametest.mixin.ai.poi;


import com.mojang.datafixers.util.Pair;
import net.caffeinemc.mods.lithium.common.world.interests.PoiOrdering;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class POI_Search implements CustomTestMethodInvoker {

    @GameTest(manualOnly = true)
    public void test(GameTestHelper context, BlockPos center, RandomSource randomSource) {
        PoiManager poiManager = context.getLevel().getPoiManager();
        Predicate<Holder<PoiType>> predicate = holder -> holder.is(PoiTypes.SHEPHERD);

        long countInRange = poiManager.getCountInRange(predicate, center, 128, PoiManager.Occupancy.ANY);

        List<BlockPos> inSquarePositions = poiManager.getInSquare(predicate, center, 128, PoiManager.Occupancy.ANY).map(PoiRecord::getPos).collect(Collectors.toList());
        PoiOrdering.InSquare.INSTANCE.checkOrderOrThrow(center, poiManager, inSquarePositions);

        List<BlockPos> inRangePositions = poiManager.getInRange(predicate, center, 128, PoiManager.Occupancy.ANY).map(PoiRecord::getPos).collect(Collectors.toList());
        PoiOrdering.InSquare.INSTANCE.checkOrderOrThrow(center, poiManager, inRangePositions);

        List<BlockPos> inChunkPositions = poiManager.getInChunk(predicate, new ChunkPos(center), PoiManager.Occupancy.ANY).map(PoiRecord::getPos).collect(Collectors.toList());
        PoiOrdering.InChunk.INSTANCE.checkOrderOrThrow(center, poiManager, inChunkPositions);

        List<BlockPos> allPositions = poiManager.findAll(predicate, blockPos1 -> true, center, 128, PoiManager.Occupancy.ANY).collect(Collectors.toList());
        PoiOrdering.InSquare.INSTANCE.checkOrderOrThrow(center, poiManager, allPositions);

        List<BlockPos> allWithTypePositions = poiManager.findAllWithType(predicate, blockPos1 -> true, center, 128, PoiManager.Occupancy.ANY).map(Pair::getSecond).collect(Collectors.toList());
        PoiOrdering.InSquare.INSTANCE.checkOrderOrThrow(center, poiManager, allWithTypePositions);

        List<BlockPos> allClosestFirstWithTypePositions = poiManager.findAllClosestFirstWithType(predicate, blockPos1 -> true, center, 128, PoiManager.Occupancy.ANY).map(Pair::getSecond).collect(Collectors.toList());
        PoiOrdering.L2ThenInSquare.INSTANCE.checkOrderOrThrow(center, poiManager, allClosestFirstWithTypePositions);

        Optional<BlockPos> firstOfAll = poiManager.find(predicate, blockPos -> true, center, 128, PoiManager.Occupancy.ANY);
        if (firstOfAll.isEmpty() != inRangePositions.isEmpty()) {
            throw new IllegalStateException("find() result presence does not match getInRange() emptiness");
        } else if (firstOfAll.isPresent()) {
            BlockPos closestPos = firstOfAll.get();
            if (!inRangePositions.getFirst().equals(closestPos)) {
                throw new IllegalStateException("find() result is not the first of getInRange()");
            }
        }

        Optional<BlockPos> findClosest = poiManager.findClosest(predicate, blockPos -> true, center, 128, PoiManager.Occupancy.ANY);
        if (findClosest.isEmpty() != allClosestFirstWithTypePositions.isEmpty()) {
            throw new IllegalStateException("findClosest() result presence does not match findAllClosestFirstWithType() emptiness");
        } else if (findClosest.isPresent()) {
            BlockPos closestPos = findClosest.get();
            if (!allClosestFirstWithTypePositions.getFirst().equals(closestPos)) {
                throw new IllegalStateException("findClosest() result is not the first of findAllClosestFirstWithType()");
            }
        }

        Optional<BlockPos> findClosestWithType = poiManager.findClosestWithType(predicate, center, 128, PoiManager.Occupancy.ANY).map(Pair::getSecond);
        if (!findClosestWithType.equals(findClosest)) {
            throw new IllegalStateException("findClosest() result does not equal findClosestWithType()");
        }

        Optional<BlockPos> findClosest2 = poiManager.findClosest(predicate, center, 128, PoiManager.Occupancy.ANY);
        if (!findClosest2.equals(findClosest)) {
            throw new IllegalStateException("findClosest() result does not equal findClosest()");
        }

        Optional<BlockPos> getRandom = poiManager.getRandom(predicate, blockPos -> true, PoiManager.Occupancy.ANY, center, 128, randomSource);
        if (getRandom.isEmpty() != inRangePositions.isEmpty()) {
            throw new IllegalStateException("getRandom() emptiness does not match getInRange() emptiness");
        }
        if (!inRangePositions.isEmpty() && !inRangePositions.contains(getRandom.get())) {
            throw new IllegalStateException("getRandom() result is not contained in getInRange() results");
        }


//        // Print to file if it does not exist yet, otherwise compare with existing file.
//        File outputFile = new File("poi_search_output/" + context.getLevel().getSeed() + "_" + center.getX() + "_" + center.getY() + "_" + center.getZ() + ".txt");
//        File newOutputFile = outputFile.exists() ? new File("poi_search_output/" + context.getLevel().getSeed() + "_" + center.getX() + "_" + center.getY() + "_" + center.getZ() + "_new.txt") : outputFile;
//
//        StringBuilder sb = new StringBuilder();
//        sb.append("countInRange:").append(countInRange).append("\n");
//        sb.append("getInSquare: ");
//        sb.append(Arrays.toString(inSquarePositions.toArray()));
//        sb.append("\n");
//        sb.append("getInRange: ");
//        sb.append(Arrays.toString(inRangePositions.toArray()));
//        sb.append("\n");
//        sb.append("getInChunk: ");
//        sb.append(Arrays.toString(inChunkPositions.toArray()));
//        sb.append("\n");
//        sb.append("findAll: ");
//        sb.append(Arrays.toString(allPositions.toArray()));
//        sb.append("\n");
//        sb.append("findAllWithType: ");
//        sb.append(Arrays.toString(allWithTypePositions.toArray()));
//        sb.append("\n");
//        sb.append("findAllClosestFirstWithType: ");
//        sb.append(Arrays.toString(allClosestFirstWithTypePositions.toArray()));
//        sb.append("\n");
//        sb.append("find:").append(firstOfAll.map(Vec3i::toString).orElse("empty")).append("\n");
//        sb.append("findClosest:").append(findClosest.map(Vec3i::toString).orElse("empty")).append("\n");
//        sb.append("findClosestWithType:").append(findClosestWithType.map(Vec3i::toString).orElse("empty")).append("\n");
//        sb.append("findClosest2:").append(findClosest2.map(Vec3i::toString).orElse("empty")).append("\n");
//        sb.append("getRandom:").append(getRandom.map(Vec3i::toString).orElse("empty")).append("\n");
//
//        if (!outputFile.exists()) {
//            outputFile.getParentFile().mkdirs();
//            try (java.io.FileWriter writer = new java.io.FileWriter(newOutputFile)) {
//                writer.write(sb.toString());
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        } else {
//            String existingContent;
//            try (java.io.FileReader reader = new java.io.FileReader(outputFile);
//                 java.io.BufferedReader br = new java.io.BufferedReader(reader)) {
//                StringBuilder existingSb = new StringBuilder();
//                String line;
//                while ((line = br.readLine()) != null) {
//                    existingSb.append(line).append("\n");
//                }
//                existingContent = existingSb.toString();
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//
//            if (!existingContent.equals(sb.toString())) {
//                try (java.io.FileWriter writer = new java.io.FileWriter(newOutputFile)) {
//                    writer.write(sb.toString());
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//                throw new AssertionError("POI search results differ from expected output. See " + newOutputFile.getAbsolutePath());
//            }
//        }

        context.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        ServerLevel level = context.getLevel();
        RandomSource random = RandomSource.create(level.getSeed()); //TODO avoid hardcoding seed

        for (int i = 0; i < 100; i++) {

            int x = random.nextInt(60000000) - 30000000;
            int z = random.nextInt(60000000) - 30000000;
            int y = random.nextInt(level.getHeight()) - level.getMinY();

            int chosen = random.nextInt(20);
            double randomPct = chosen == 19 ? 0.0 : 1.0 / Math.pow(2, chosen);

            int radius = 128;

            long poiCount = 0;

            System.out.println("Placing POIs for iteration " + (i + 1) + "/100 : around (" + x + ", " + y + ", " + z + ") with randomPct=" + randomPct + " and radius=" + radius);
            for (BlockPos blockPos : BlockPos.betweenClosed(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius)) {
                if (random.nextFloat() < randomPct && level.isInWorldBounds(blockPos)) {
                    poiCount++;
                    level.setBlock(blockPos, Blocks.LOOM.defaultBlockState(), 0);
                }
            }

            System.out.println("Placed " + poiCount + " POIs, running test method...");

            method.invoke(this, context, new BlockPos(x, y, z), random);
        }
    }
}
