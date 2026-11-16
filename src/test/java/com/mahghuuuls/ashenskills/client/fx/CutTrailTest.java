package com.mahghuuuls.ashenskills.client.fx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mahghuuuls.ashenskills.api.SkillPhase;
import com.mahghuuuls.ashenskills.network.PhaseUpdateMessage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

class CutTrailTest {

    private static final double EXACT = 1e-9;

    /** A flat half circle from the user's left to the user's right, at chest height. */
    private static CutTrail halfCircle(String extra) {
        return CutTrail.parse(new StringReader("{'startAngle': -90, 'endAngle': 90, 'innerRadius': 1, 'outerRadius': 3,"
                + " 'height': 1.2, 'sweep': 0.2, 'fade': 0.3, 'color': [255, 0, 51]" + extra + "}"));
    }

    private static void assertPoint(double x, double y, double z, double[] point) {
        assertEquals(x, point[0], EXACT, "x");
        assertEquals(y, point[1], EXACT, "y");
        assertEquals(z, point[2], EXACT, "z");
    }

    @Test
    void theCutGoesFromTheUsersLeftAcrossTheFrontToTheRight() {
        CutTrail trail = halfCircle("");
        // The game's yaw 0 looks along +z; the user's left is then +x and the right is -x.
        assertPoint(3, 1.2, 0, trail.pointAt(0, 1, 0));
        assertPoint(0, 1.2, 3, trail.pointAt(0.5, 1, 0));
        assertPoint(-3, 1.2, 0, trail.pointAt(1, 1, 0));
        assertPoint(0, 1.2, 1, trail.pointAt(0.5, 0, 0));
        assertPoint(0, 1.2, 2, trail.pointAt(0.5, 0.5, 0));
    }

    @Test
    void theTrailTurnsWithTheDirectionTheUserFaced() {
        CutTrail trail = halfCircle("");
        // Yaw 90 looks along -x: straight ahead of the user is then -x.
        assertPoint(-3, 1.2, 0, trail.pointAt(0.5, 1, 90));
        // Yaw 180 looks along -z.
        assertPoint(0, 1.2, -3, trail.pointAt(0.5, 1, 180));
    }

    @Test
    void aTiltLiftsTheRightEndAndLowersTheLeftEnd() {
        CutTrail tilted = halfCircle(", 'tilt': 30");
        double[] left = tilted.pointAt(0, 1, 0);
        double[] right = tilted.pointAt(1, 1, 0);
        assertEquals(1.2 - 1.5, left[1], EXACT);
        assertEquals(1.2 + 1.5, right[1], EXACT);
        // Straight ahead is on the line the crescent rolls around: its height does not change.
        assertEquals(1.2, tilted.pointAt(0.5, 1, 0)[1], EXACT);
    }

    @Test
    void theBladeRevealsTheTrailAndTheTrailFadesBehindIt() {
        CutTrail trail = halfCircle("");
        // At the start nothing is lit yet; a place lights up over 0.03 seconds once the blade is there.
        assertEquals(0.0, trail.strengthAt(0, 0), EXACT);
        assertEquals(0.0, trail.strengthAt(0.5, 0), EXACT);
        assertEquals(0.5 * (1 - 0.015 / 0.3), trail.strengthAt(0, 0.015), EXACT);
        assertEquals(1 - 0.03 / 0.3, trail.strengthAt(0, 0.03), EXACT);
        // Half way through the sweep the blade is at the middle; the start has faded for 0.1 of its 0.3 seconds.
        assertEquals(0.0, trail.strengthAt(0.5, 0.1), EXACT);
        assertEquals(1 - 0.03 / 0.3, trail.strengthAt(0.5, 0.13), EXACT);
        assertEquals(1 - 0.1 / 0.3, trail.strengthAt(0, 0.1), EXACT);
        assertEquals(0.0, trail.strengthAt(1, 0.1), EXACT);
        // The end lights up when the sweep ends and is the last place to go dark.
        assertEquals(0.0, trail.strengthAt(1, 0.2), EXACT);
        assertEquals(1 - 0.03 / 0.3, trail.strengthAt(1, 0.23), EXACT);
        assertEquals(0.0, trail.strengthAt(0, 0.3), EXACT);
        assertFalse(trail.isOver(0.49));
        assertTrue(trail.isOver(0.5));
        assertEquals(0.0, trail.strengthAt(1, 0.5), EXACT);
    }

    @Test
    void theColorAndTheSparksComeFromTheFile() {
        CutTrail trail = halfCircle(", 'sparks': 7");
        assertEquals(1.0F, trail.red());
        assertEquals(0.0F, trail.green());
        assertEquals(0.2F, trail.blue());
        assertEquals(7, trail.sparks());
        assertEquals(0, halfCircle("").sparks());
    }

    @Test
    void aFileThatCannotMakeATrailIsRefusedWithItsReason() {
        assertRefused("'outerRadius'", "{'startAngle': 0, 'endAngle': 90, 'innerRadius': 1, 'height': 1, 'sweep': 0.2, 'fade': 0.3, 'color': [1, 2, 3]}");
        assertRefused("outerRadius must be above innerRadius", trailWith("'innerRadius': 3, 'outerRadius': 3"));
        assertRefused("sweep and fade", trailWith("'innerRadius': 1, 'outerRadius': 3").replace("'sweep': 0.2", "'sweep': 0"));
        assertRefused("must differ", trailWith("'innerRadius': 1, 'outerRadius': 3").replace("'endAngle': 90", "'endAngle': -90"));
        assertRefused("color", trailWith("'innerRadius': 1, 'outerRadius': 3").replace("[255, 0, 51]", "[255, 0]"));
        assertRefused("0 to 255", trailWith("'innerRadius': 1, 'outerRadius': 3").replace("[255, 0, 51]", "[300, 0, 0]"));
        assertRefused("not a JSON object", "[1, 2]");
        String good = trailWith("'innerRadius': 1, 'outerRadius': 3");
        assertRefused("not be above 16", trailWith("'innerRadius': 1, 'outerRadius': 16.5"));
        assertRefused("sweep and fade", good.replace("'fade': 0.3", "'fade': 5.5"));
        assertRefused("by 360 at most", good.replace("'endAngle': 90", "'endAngle': 300"));
        assertRefused("'height'", good.replace("'height': 1.2, ", ""));
        assertRefused("sparks must be a whole number", good.replace("}", ", 'sparks': -1}"));
        assertRefused("sparks must be a whole number", good.replace("}", ", 'sparks': 2.7}"));
        assertRefused("sparks must be a whole number", good.replace("}", ", 'sparks': 1001}"));
    }

    private static String trailWith(String radii) {
        return "{'startAngle': -90, 'endAngle': 90, " + radii + ", 'height': 1.2, 'sweep': 0.2, 'fade': 0.3, 'color': [255, 0, 51]}";
    }

    private static void assertRefused(String reasonPart, String file) {
        IllegalArgumentException refused =
                assertThrows(IllegalArgumentException.class, () -> CutTrail.parse(new StringReader(file)));
        assertTrue(refused.getMessage().contains(reasonPart), refused.getMessage());
    }

    @Test
    void aTrailStartsWithTheFirstMomentOfACutAndEndsWhenTheSkillIsBrokenOffOrOver() {
        byte execution = PhaseUpdateMessage.codeOf(SkillPhase.EXECUTION);
        assertEquals(CutTrails.Change.START, CutTrails.changeFor(execution, 0));
        // A player who comes into view during the cut gets its phase with time already gone: no trail.
        assertEquals(CutTrails.Change.NONE, CutTrails.changeFor(execution, 3));
        assertEquals(CutTrails.Change.NONE, CutTrails.changeFor(PhaseUpdateMessage.codeOf(SkillPhase.RECOVERY), 0));
        assertEquals(CutTrails.Change.NONE, CutTrails.changeFor(PhaseUpdateMessage.codeOf(SkillPhase.PREPARATION), 0));
        assertEquals(CutTrails.Change.END, CutTrails.changeFor(PhaseUpdateMessage.codeOf(SkillPhase.INTERRUPTED_RECOVERY), 0));
        assertEquals(CutTrails.Change.END, CutTrails.changeFor(PhaseUpdateMessage.NO_PHASE, 0));
    }

    @Test
    void theTrailOfASkillIsLookedForUnderItsNamespaceAndName() {
        assertEquals(new ResourceLocation("ashenskills", "ashen_trails/unsheathe.json"), CutTrails.fileOf("ashenskills:unsheathe"));
        assertEquals(new ResourceLocation("addon", "ashen_trails/whirl.json"), CutTrails.fileOf("addon:whirl"));
    }

    @Test
    void theShippedUnsheatheTrailLoadsAndIsGoneWithinHalfASecond() throws Exception {
        ResourceLocation file = CutTrails.fileOf("ashenskills:unsheathe");
        try (InputStream in = CutTrailTest.class.getResourceAsStream("/assets/" + file.getNamespace() + "/" + file.getPath())) {
            assertNotNull(in, file.toString());
            Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
            CutTrail trail = CutTrail.parse(reader);
            // Nothing of it is left half a second after the cut began.
            assertTrue(trail.isOver(0.5));
            assertFalse(trail.isOver(0.2));
        }
    }
}
