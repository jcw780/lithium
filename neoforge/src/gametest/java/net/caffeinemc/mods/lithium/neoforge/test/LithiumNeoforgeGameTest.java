package net.caffeinemc.mods.lithium.neoforge.test;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

@PrefixGameTestTemplate(value = false)
@GameTestHolder("lithium-gametest")
public class LithiumNeoforgeGameTest {

    public static final String LITHIUM_GAMETEST_SNBT_PATH = System.getenv("LITHIUM_GAMETEST_RESOURCES");

    //Some tests are excluded because Neoforge is breaking them, not lithium.
    //test_redstone.lava_push_speed broken by https://github.com/neoforged/NeoForge/issues/1575
    //test_redstone.comparator_update_collection broken by https://github.com/neoforged/NeoForge/issues/1750
    public static final Collection<String> NEOFORGE_EXLUDED_TESTS = List.of("test_redstone.lava_push_speed", "test_redstone.comparator_update_collection");

    static {
        StructureUtils.testStructuresDir = LITHIUM_GAMETEST_SNBT_PATH;
    }

    @Nullable
    public static String getTemplateName(String structureName) {
        int dotIndex = structureName.indexOf(".");
        if (dotIndex < 0) {
            return null;
        }
        return structureName.substring(0, dotIndex);
    }

    @GameTestGenerator
    public Collection<TestFunction> getAllRedstoneTests() {
        if (LITHIUM_GAMETEST_SNBT_PATH == null) {
            return List.of();
        }

        List<String> structureNames = null;
        try {
            structureNames = getLithiumSNBTFilenames();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ArrayList<TestFunction> testFunctions = new ArrayList<>();
        for (String structureName : structureNames) {
            String templateName = getTemplateName(structureName);

            if ("test_redstone".equals(templateName)) {
                testFunctions.add(new TestFunction(
                        "lithium_test_redstone",
                        structureName.substring(templateName.length() + 1),
                        "lithium-gametest:" + structureName, //This structure file location method is Fabric dependent?
                        400 /* Timeout ticks. 20 Seconds is fine for our redstone tests. */,
                        10 /* Setup ticks (between placing structure and test start). Prevents random redstone firing from activating success / failure condition immediately. */,
                        !NEOFORGE_EXLUDED_TESTS.contains(structureName) /* Test required */,
                        LithiumNeoforgeGameTest::test_redstone));
            }
        }

        return testFunctions;
    }

    private List<String> getLithiumSNBTFilenames() throws IOException {
        Path folderPath = Paths.get(LITHIUM_GAMETEST_SNBT_PATH);
        try (Stream<Path> paths = Files.walk(folderPath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.toFile().getName())
                    .filter(string -> string.endsWith(".snbt"))
                    .map(file -> file.substring(0, file.lastIndexOf('.'))).toList();
        }
    }
}