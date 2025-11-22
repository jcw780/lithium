package net.caffeinemc.mods.lithium.fabric.gametest.mixin.ai.poi;


import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.fabricmc.fabric.api.gametest.v1.CustomTestMethodInvoker;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class POI_Search implements CustomTestMethodInvoker {

    @GameTest(manualOnly = true)
    public void test(GameTestHelper context, BlockPos center, RandomSource randomSource) {
        PoiManager poiManager = context.getLevel().getPoiManager();
        Predicate<Holder<PoiType>> predicate = holder -> holder.is(PoiTypes.SHEPHERD);

        long countInRange = poiManager.getCountInRange(predicate, center, 128, PoiManager.Occupancy.ANY);
        LongArrayList getInSquare = LongArrayList.of(poiManager.getInSquare(predicate, center, 128, PoiManager.Occupancy.ANY).mapToLong(poiRecord -> poiRecord.getPos().asLong()).toArray());
        LongArrayList getInRange = LongArrayList.of(poiManager.getInRange(predicate, center, 128, PoiManager.Occupancy.ANY).mapToLong(poiRecord -> poiRecord.getPos().asLong()).toArray());
        LongArrayList getInChunk = LongArrayList.of(poiManager.getInChunk(predicate, new ChunkPos(center), PoiManager.Occupancy.ANY).mapToLong(poiRecord -> poiRecord.getPos().asLong()).toArray());
        LongArrayList findAll = LongArrayList.of(poiManager.findAll(predicate, blockPos -> true, center, 128, PoiManager.Occupancy.ANY).mapToLong(BlockPos::asLong).toArray());
        LongArrayList findAllWithType = LongArrayList.of(poiManager.findAllWithType(predicate, blockPos -> true, center, 128, PoiManager.Occupancy.ANY).mapToLong(pair -> pair.getSecond().asLong()).toArray());
        LongArrayList findAllClosestFirstWithType = LongArrayList.of(poiManager.findAllClosestFirstWithType(predicate, blockPos -> true, center, 128, PoiManager.Occupancy.ANY).mapToLong(pair -> pair.getSecond().asLong()).toArray());
        Optional<Long> find = poiManager.find(predicate, blockPos -> true, center, 128, PoiManager.Occupancy.ANY).map(BlockPos::asLong);
        Optional<Long> findClosest = poiManager.findClosest(predicate, blockPos -> true, center, 128, PoiManager.Occupancy.ANY).map(BlockPos::asLong);
        Optional<Long> findClosestWithType = poiManager.findClosestWithType(predicate, center, 128, PoiManager.Occupancy.ANY).map(pair -> pair.getSecond().asLong());
        Optional<Long> findClosest2 = poiManager.findClosest(predicate, center, 128, PoiManager.Occupancy.ANY).map(BlockPos::asLong);
        Optional<Long> getRandom = poiManager.getRandom(predicate, blockPos -> true, PoiManager.Occupancy.ANY, center, 128, randomSource).map(BlockPos::asLong);


        // Print to file if it does not exist yet, otherwise compare with existing file.
        File outputFile = new File("poi_search_output/" + context.getLevel().getSeed() + "_" + center.getX() + "_" + center.getY() + "_" + center.getZ() + ".txt");
        File newOutputFile = outputFile.exists() ? new File("poi_search_output/" + context.getLevel().getSeed() + "_" + center.getX() + "_" + center.getY() + "_" + center.getZ() + "_new.txt") : outputFile;

        StringBuilder sb = new StringBuilder();
        sb.append("countInRange:").append(countInRange).append("\n");
        sb.append("getInSquare: [");
        sb.append(getInSquare.longStream().mapToObj(a -> BlockPos.of(a).toString()).collect(Collectors.joining(",")));
        sb.append("]\n");
        sb.append("getInRange: [");
        sb.append(getInRange.longStream().mapToObj(a -> BlockPos.of(a).toString()).collect(Collectors.joining(",")));
        sb.append("]\n");
        sb.append("getInChunk: [");
        sb.append(getInChunk.longStream().mapToObj(a -> BlockPos.of(a).toString()).collect(Collectors.joining(",")));
        sb.append("]\n");
        sb.append("findAll: [");
        sb.append(findAll.longStream().mapToObj(a -> BlockPos.of(a).toString()).collect(Collectors.joining(",")));
        sb.append("]\n");
        sb.append("findAllWithType: [");
        sb.append(findAllWithType.longStream().mapToObj(a -> BlockPos.of(a).toString()).collect(Collectors.joining(",")));
        sb.append("]\n");
        sb.append("findAllClosestFirstWithType: [");
        sb.append(findAllClosestFirstWithType.longStream().mapToObj(a -> BlockPos.of(a).toString()).collect(Collectors.joining(",")));
        sb.append("]\n");
        sb.append("find:").append(find.map(a -> BlockPos.of(a).toString()).orElse("empty")).append("\n");
        sb.append("findClosest:").append(findClosest.map(a -> BlockPos.of(a).toString()).orElse("empty")).append("\n");
        sb.append("findClosestWithType:").append(findClosestWithType.map(a -> BlockPos.of(a).toString()).orElse("empty")).append("\n");
        sb.append("findClosest2:").append(findClosest2.map(a -> BlockPos.of(a).toString()).orElse("empty")).append("\n");
        sb.append("getRandom:").append(getRandom.map(a -> BlockPos.of(a).toString()).orElse("empty")).append("\n");

        if (!outputFile.exists()) {
            outputFile.getParentFile().mkdirs();
            try (java.io.FileWriter writer = new java.io.FileWriter(newOutputFile)) {
                writer.write(sb.toString());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            String existingContent;
            try (java.io.FileReader reader = new java.io.FileReader(outputFile);
                 java.io.BufferedReader br = new java.io.BufferedReader(reader)) {
                StringBuilder existingSb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    existingSb.append(line).append("\n");
                }
                existingContent = existingSb.toString();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (!existingContent.equals(sb.toString())) {
                try (java.io.FileWriter writer = new java.io.FileWriter(newOutputFile)) {
                    writer.write(sb.toString());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                throw new AssertionError("POI search results differ from expected output. See " + newOutputFile.getAbsolutePath());
            }
        }

        context.succeed();
    }

    @Override
    public void invokeTestMethod(GameTestHelper context, Method method) throws ReflectiveOperationException {
        ServerLevel level = context.getLevel();
        RandomSource random = RandomSource.create(level.getSeed()); //TODO avoid hardcoding seed

        int x = random.nextInt(60000000) - 30000000;
        int z = random.nextInt(60000000) - 30000000;
        int y = random.nextInt(level.getHeight()) - level.getMinY();

        int chosen = random.nextInt(20);
        double randomPct = chosen == 19 ? 0.0 : 1.0 / Math.pow(2, chosen);

        int radius = 128;

        int ceilRadius = radius + 16;
//        for(BlockPos blockPos : BlockPos.betweenClosed(x - ceilRadius, y - ceilRadius, z - ceilRadius, x + ceilRadius, y + ceilRadius, z + ceilRadius)) {
//            if (level.isInWorldBounds(blockPos)) {
//                level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 0);
//            }
//        }

        long poiCount = 0;
        for (BlockPos blockPos : BlockPos.betweenClosed(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius)) {
            if (random.nextFloat() < randomPct && level.isInWorldBounds(blockPos)) {
                poiCount++;
                level.setBlock(blockPos, Blocks.LOOM.defaultBlockState(), 0);
            }
        }

        System.out.println("Placed " + poiCount + " POIs around (" + x + ", " + y + ", " + z + ") with randomPct=" + randomPct + " and radius=" + radius);

        method.invoke(this, context, new BlockPos(x, y, z), random);
    }
}
