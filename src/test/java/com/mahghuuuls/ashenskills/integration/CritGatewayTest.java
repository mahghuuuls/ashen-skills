package com.mahghuuuls.ashenskills.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.Test;

class CritGatewayTest {

    /** Writes down what it is told, in order. */
    private static final class Recorder implements CritGateway {

        final List<String> calls = new ArrayList<>();

        @Override
        public void beginStrike(EntityPlayer user, ItemStack weapon) {
            calls.add("begin");
        }

        @Override
        public void endStrike(EntityPlayer user) {
            calls.add("end");
        }
    }

    @Test
    void aStrikeRunsOnceBetweenOneBeginAndOneEnd() {
        final Recorder recorder = new Recorder();

        CritGateway.around(recorder, null, null, new Runnable() {
            @Override
            public void run() {
                // Three targets of one hit window: still one begin and one end around all of them.
                recorder.calls.add("target");
                recorder.calls.add("target");
                recorder.calls.add("target");
            }
        });

        assertEquals(Arrays.asList("begin", "target", "target", "target", "end"), recorder.calls);
    }

    @Test
    void theEndIsToldWhenTheStrikeThrowsAndTheProblemIsPassedOn() {
        final Recorder recorder = new Recorder();
        final IllegalStateException problem = new IllegalStateException("a target broke the strike");

        IllegalStateException thrown = assertThrows(IllegalStateException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                CritGateway.around(recorder, null, null, new Runnable() {
                    @Override
                    public void run() {
                        recorder.calls.add("target");
                        throw problem;
                    }
                });
            }
        });

        assertSame(problem, thrown);
        assertEquals(Arrays.asList("begin", "target", "end"), recorder.calls);
    }
}
