package core;

import net.caffeinemc.mods.lithium.common.util.change_tracking.ChangePublisher;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CoreTest {

    @BeforeAll
    static void beforeAll() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void testMixinApply() {
        Assertions.assertTrue(ChangePublisher.class.isAssignableFrom(ItemStack.class));
    }
}
