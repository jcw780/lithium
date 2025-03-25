package net.caffeinemc.mods.lithium.fabric.gametest;


import java.nio.file.Paths;

import net.minecraft.gametest.framework.StructureUtils;

public class LithiumFabricGameTest {

    public static final String LITHIUM_GAMETEST_SNBT_PATH = System.getenv("LITHIUM_GAMETEST_RESOURCES");

    static {
        StructureUtils.testStructuresDir = Paths.get(LITHIUM_GAMETEST_SNBT_PATH);
    }
}

