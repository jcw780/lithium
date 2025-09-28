package core;

import net.caffeinemc.mods.lithium.common.util.change_tracking.ChangePublisher;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    void testItemStackInitialized() {
        ItemStack diamondStack = new ItemStack(Items.DIAMOND, 4);

        Assertions.assertTrue(diamondStack.is(Items.DIAMOND));
        Assertions.assertEquals(4, diamondStack.getCount());

    }

    @Test
    void testMixinApply() {
        ItemStack diamondStack = new ItemStack(Items.DIRT, 4);

        //noinspection ConstantValue,SimplifiableAssertion,DataFlowIssue
        Assertions.assertTrue(((Object) diamondStack) instanceof ChangePublisher);
    }
}
