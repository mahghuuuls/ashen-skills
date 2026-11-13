package com.mahghuuuls.ashenskills.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import net.minecraft.entity.player.EntityPlayer;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

class IntegrationGateTest {

    /** Stands for the code that talks to another mod. */
    public static final class LoadablePool implements ResourcePool {

        @Override
        public boolean isInstalled() {
            return true;
        }

        @Override
        public Claim claim(EntityPlayer player, double amount) {
            return Claim.FREE;
        }
    }

    @Test
    void theGateLoadsAnImplementationOnlyForAnInstalledModAndFallsBackWhenItCannotLoad() {
        org.apache.logging.log4j.Logger logger = LogManager.getLogger("test");
        String loadable = LoadablePool.class.getName();

        assertSame(ResourcePool.ABSENT,
                IntegrationGate.implementationOr(false, loadable, ResourcePool.class, ResourcePool.ABSENT, logger));
        assertEquals(LoadablePool.class,
                IntegrationGate.implementationOr(true, loadable, ResourcePool.class, ResourcePool.ABSENT, logger).getClass());
        assertSame(ResourcePool.ABSENT,
                IntegrationGate.implementationOr(true, "no.such.Class", ResourcePool.class, ResourcePool.ABSENT, logger));
        assertSame(ResourcePool.ABSENT,
                IntegrationGate.implementationOr(true, "java.lang.Object", ResourcePool.class, ResourcePool.ABSENT, logger));
    }
}
