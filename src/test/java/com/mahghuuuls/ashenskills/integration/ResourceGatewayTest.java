package com.mahghuuuls.ashenskills.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import org.junit.jupiter.api.Test;

class ResourceGatewayTest {

    /**
     * A resource with a fixed amount. It refuses a claim above what it holds, and writes down, in a
     * list it may share with the other resource, everything it is asked to do.
     */
    private static class FakePool implements ResourcePool {

        final String name;
        final List<String> calls;
        double held;
        double spent;
        int openClaims;

        FakePool(String name, double held, List<String> calls) {
            this.name = name;
            this.held = held;
            this.calls = calls;
        }

        FakePool(double held) {
            this("pool", held, new ArrayList<String>());
        }

        @Override
        public boolean isInstalled() {
            return true;
        }

        /** Whether taking an accepted claim really takes it. */
        boolean takeSucceeds() {
            return true;
        }

        @Override
        public Claim claim(EntityPlayer player, final double amount) {
            calls.add(name + " claim");
            if (amount > held) {
                return null;
            }
            openClaims++;
            return new Claim() {
                @Override
                public boolean take() {
                    calls.add(name + " take");
                    openClaims--;
                    if (!takeSucceeds()) {
                        return false;
                    }
                    held -= amount;
                    spent += amount;
                    return true;
                }

                @Override
                public void release() {
                    calls.add(name + " release");
                    openClaims--;
                }
            };
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
        assertEquals(0, stamina.openClaims);
        assertEquals(0, mana.openClaims);
    }

    @Test
    void theStaminaIsHeldBeforeAnyManaIsTakenAndTakenOnlyAfterIt() {
        List<String> calls = new ArrayList<>();
        ResourceGateway gateway = gateway(new FakePool("stamina", 20, calls), new FakePool("mana", 20, calls), true);

        gateway.pay(null, gateway.quote(15, 5));

        assertEquals(Arrays.asList("stamina claim", "mana claim", "mana take", "stamina take"), calls);
    }

    @Test
    void whenTheStaminaModRefusesTheManaModIsNotAsked() {
        List<String> calls = new ArrayList<>();
        FakePool mana = new FakePool("mana", 20, calls);
        ResourceGateway gateway = gateway(new FakePool("stamina", 1, calls), mana, true);

        assertEquals(PaymentResult.NOT_ENOUGH_STAMINA, gateway.pay(null, gateway.quote(15, 5)));
        assertEquals(Collections.singletonList("stamina claim"), calls);
        assertEquals(0, mana.spent);
    }

    @Test
    void whenTheManaModRefusesTheHeldStaminaIsGivenBack() {
        List<String> calls = new ArrayList<>();
        FakePool stamina = new FakePool("stamina", 20, calls);
        FakePool mana = new FakePool("mana", 3, calls);
        ResourceGateway gateway = gateway(stamina, mana, true);

        assertEquals(PaymentResult.NOT_ENOUGH_MANA, gateway.pay(null, gateway.quote(15, 5)));
        assertEquals(Arrays.asList("stamina claim", "mana claim", "stamina release"), calls);
        assertEquals(0, stamina.spent);
        assertEquals(0, mana.spent);
        assertEquals(0, stamina.openClaims);
    }

    @Test
    void whenTheManaIsNotTakenAfterAllTheHeldStaminaIsGivenBack() {
        List<String> calls = new ArrayList<>();
        FakePool stamina = new FakePool("stamina", 20, calls);
        FakePool mana = new FakePool("mana", 20, calls) {
            @Override
            boolean takeSucceeds() {
                return false;
            }
        };
        ResourceGateway gateway = gateway(stamina, mana, true);

        assertEquals(PaymentResult.NOT_ENOUGH_MANA, gateway.pay(null, gateway.quote(15, 5)));
        assertEquals(Arrays.asList("stamina claim", "mana claim", "mana take", "stamina release"), calls);
        assertEquals(0, stamina.spent);
        assertEquals(0, stamina.openClaims);
    }

    @Test
    void whenTheManaModThrowsTheHeldStaminaIsGivenBackAndTheProblemIsPassedOn() {
        FakePool stamina = new FakePool(20);
        ResourcePool brokenMana = new FakePool(20) {
            @Override
            public Claim claim(EntityPlayer player, double amount) {
                throw new IllegalStateException("the mana mod broke");
            }
        };
        ResourceGateway gateway = gateway(stamina, brokenMana, true);

        assertThrows(IllegalStateException.class, () -> gateway.pay(null, gateway.quote(15, 5)));
        assertEquals(0, stamina.spent);
        assertEquals(0, stamina.openClaims);
    }

    @Test
    void whenTheStaminaModTakesNothingAfterTheManaIsGoneTheUseStillCountsAsPaid() {
        FakePool stamina = new FakePool(20) {
            @Override
            boolean takeSucceeds() {
                return false;
            }
        };
        FakePool mana = new FakePool(20);
        ResourceGateway gateway = gateway(stamina, mana, true);

        assertEquals(PaymentResult.PAID, gateway.pay(null, gateway.quote(15, 5)));
        assertEquals(5, mana.spent);
        assertEquals(0, stamina.openClaims);
    }

    @Test
    void aPreparationMultiplierTakesAwayTheRestOfTheRegeneration() {
        assertEquals(1.0, PreparationRegen.reductionFor(0.0));
        assertEquals(0.75, PreparationRegen.reductionFor(0.25));
        assertEquals(0.0, PreparationRegen.reductionFor(1.0));
        // Outside 0 to 1 counts as the nearer end.
        assertEquals(1.0, PreparationRegen.reductionFor(-2.0));
        assertEquals(0.0, PreparationRegen.reductionFor(3.0));
    }

    @Test
    void aCostOfZeroDoesNotAskTheMod() {
        List<String> calls = new ArrayList<>();
        ResourceGateway gateway = gateway(new FakePool("stamina", 20, calls), new FakePool("mana", 20, calls), true);

        assertEquals(PaymentResult.PAID, gateway.pay(null, gateway.quote(15, 0)));
        assertEquals(Arrays.asList("stamina claim", "stamina take"), calls);
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
