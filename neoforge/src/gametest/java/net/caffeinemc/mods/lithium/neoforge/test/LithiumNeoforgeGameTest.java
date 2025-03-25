package net.caffeinemc.mods.lithium.neoforge.test;


import java.nio.file.Paths;

import net.minecraft.gametest.framework.StructureUtils;

//TODO this does nothing, neoforge has no tests atm
public class LithiumNeoforgeGameTest {

    public static final String LITHIUM_GAMETEST_SNBT_PATH = System.getenv("LITHIUM_GAMETEST_RESOURCES");

    static {
        StructureUtils.testStructuresDir = Paths.get(LITHIUM_GAMETEST_SNBT_PATH);
    }
}