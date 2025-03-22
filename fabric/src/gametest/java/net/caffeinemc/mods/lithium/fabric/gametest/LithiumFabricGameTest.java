package net.caffeinemc.mods.lithium.fabric.gametest;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

public class LithiumFabricGameTest {

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
        List<String> structureNames = null;
        try {
            structureNames = getLithiumSNBTFilenames();
            System.out.println("Found " + structureNames.size() + " lithium test structures!");
        } catch (IOException e) {
            System.out.println("Current directory: " + Paths.get(".").toAbsolutePath());
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
                        true /* Test required */,
                        LithiumFabricGameTest::test_redstone));
            }
        }

        return testFunctions;
    }

    private List<String> getLithiumSNBTFilenames() throws IOException {
        Path folderPath = Paths.get("../resources/gametest/data/lithium-gametest/gametest/structure");
        try (Stream<Path> paths = Files.walk(folderPath)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.toFile().getName())
                    .filter(string -> string.endsWith(".snbt"))
                    .map(file -> file.substring(0, file.lastIndexOf('.'))).toList();
        }
    }
}