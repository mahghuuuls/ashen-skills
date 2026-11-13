package com.mahghuuuls.ashenskills.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.entity.player.EntityPlayer;
import org.junit.jupiter.api.Test;

class ResourceGatewayTest {

    /** A resource with a fixed amount; spending more than it holds is not allowed. */
    private static final class FakePool implements ResourcePool {

        private double held;
        private double spent;

        FakePool(double held) {
            this.held = held;
        }

        @Override
        public boolean isInstalled() {
            return true;
        }

        @Override
        public boolean allowsSpending(EntityPlayer player, double amount) {
            return amount <= held;
        }

        @Override
        public void spend(EntityPlayer player, double amount) {
            held -= amount;
            spent += amount;
        }
    }

    private static ResourceGateway gateway(ResourcePool stamina, ResourcePool mana, boolean waive) {
        return new ResourceGateway(stamina, mana, () -> waive);
    }

    @Test
    void bothCostsAreTakenWhenBothCanBePaid() {
        FakePool stamina = new FakePool(20);
        FakePool mana = new FakePool(20);
        ResourceGateway gateway = gateway(stamina, mana, true);

        assertEquals(PaymentResult.PAID, gateway.pay(null, gateway.quote(15, 5)));
        assertEquals(15, stamina.spent);
        assertEquals(5, mana.spent);
    }

    @Test
    void nothingIsTakenWhenOneResourceRefuses() {
        FakePool stamina = new FakePool(20);
        FakePool mana = new FakePool(3);
        ResourceGateway gateway = gateway(stamina, mana, true);

        assertEquals(PaymentResult.NOT_ENOUGH_MANA, gateway.pay(null, gateway.quote(15, 5)));
        assertEquals(0, stamina.spent);
        assertEquals(0, mana.spent);

        FakePool emptyStamina = new FakePool(1);
        FakePool fullMana = new FakePool(20);
        ResourceGateway other = gateway(emptyStamina, fullMana, true);
        assertEquals(PaymentResult.NOT_ENOUGH_STAMINA, other.pay(null, other.quote(15, 5)));
        assertEquals(0, fullMana.spent);
    }

    @Test
    void anAbsentResourceIsFreeWhenTheServerWaivesIt() {
        FakePool mana = new FakePool(20);
        ResourceGateway gateway = gateway(ResourcePool.ABSENT, mana, true);
        PaymentPlan plan = gateway.quote(15, 5);

        assertNull(plan.blockedBy());
        assertEquals(0, plan.stamina());
        assertEquals(PaymentResult.PAID, gateway.pay(null, plan));
        assertEquals(5, mana.spent);
    }

    @Test
    void anAbsentResourceBlocksASkillThatCostsItWhenTheServerDoesNotWaive() {
        FakePool mana = new FakePool(20);
        ResourceGateway noStamina = gateway(ResourcePool.ABSENT, mana, false);

        assertEquals(PaymentResult.STAMINA_MOD_MISSING, noStamina.pay(null, noStamina.quote(15, 5)));
        assertEquals(0, mana.spent);
        // A skill that costs none of the absent resource is not blocked.
        assertEquals(PaymentResult.PAID, noStamina.pay(null, noStamina.quote(0, 5)));
        assertEquals(5, mana.spent);

        ResourceGateway noMana = gateway(new FakePool(20), ResourcePool.ABSENT, false);
        assertEquals(PaymentResult.MANA_MOD_MISSING, noMana.pay(null, noMana.quote(15, 5)));
        assertEquals(PaymentResult.PAID, noMana.pay(null, noMana.quote(15, 0)));
    }

    @Test
    void withBothResourcesAbsentAndNotWaivedTheStaminaModIsNamedFirst() {
        ResourceGateway gateway = gateway(ResourcePool.ABSENT, ResourcePool.ABSENT, false);

        assertEquals(PaymentResult.STAMINA_MOD_MISSING, gateway.pay(null, gateway.quote(15, 5)));
        assertEquals(PaymentResult.MANA_MOD_MISSING, gateway.pay(null, gateway.quote(0, 5)));
    }

    @Test
    void aSkillWithNoCostsIsPaidWithNothingInstalled() {
        ResourceGateway gateway = gateway(ResourcePool.ABSENT, ResourcePool.ABSENT, false);

        assertEquals(PaymentResult.PAID, gateway.pay(null, gateway.quote(0, 0)));
    }
}
