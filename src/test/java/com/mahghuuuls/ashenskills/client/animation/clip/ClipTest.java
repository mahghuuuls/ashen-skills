package com.mahghuuuls.ashenskills.client.animation.clip;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillNumber;
import com.mahghuuuls.ashenskills.config.DefaultsData;
import com.mahghuuuls.ashenskills.skill.BuiltInSkills;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ClipTest {

    private static final double EXACT = 1.0e-9;

    private static final String SIMPLE = "{ \"length\": 1.0, \"bones\": {"
            + " \"right_arm\": { \"rotation\": ["
            + "   { \"t\": 0.0, \"v\": [10, 20, 30] },"
            + "   { \"t\": 0.5, \"v\": [20, 40, 60] },"
            + "   { \"t\": 1.0, \"v\": [20, 40, -60], \"ease\": \"ease_in\" } ],"
            + "  \"position\": [ { \"t\": 0.25, \"v\": [1, 2, 3] } ] },"
            + " \"body\": { \"rotation\": [ { \"t\": 0.0, \"v\": [0, 90, 0] } ] } } }";

    private static Clip simple() throws ClipFormatException {
        return ClipParser.parse(SIMPLE);
    }

    private static String refusal(String json) {
        return assertThrows(ClipFormatException.class, () -> ClipParser.parse(json)).getMessage();
    }

    private static String shipped(String name) throws IOException {
        String path = "/assets/ashenskills/ashen_clips/powerful_slash/" + name + ".json";
        try (InputStream stream = ClipTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "missing resource " + path);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = stream.read(buffer)) > 0) {
                bytes.write(buffer, 0, read);
            }
            return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void aKeyGivesItsOwnValueAndTheAxesAreNotSwapped() throws ClipFormatException {
        Pose pose = ClipSampler.pose(simple(), 0.0);

        assertArrayEquals(new double[] {10, 20, 30}, pose.rotationDegrees(Bone.RIGHT_ARM), EXACT);
        assertArrayEquals(new double[] {0, 90, 0}, pose.rotationDegrees(Bone.BODY), EXACT);
        float[] radians = pose.rotationRadians(Bone.RIGHT_ARM);
        assertEquals((float) Math.toRadians(10), radians[0], 1.0e-6F);
        assertEquals((float) Math.toRadians(20), radians[1], 1.0e-6F);
        assertEquals((float) Math.toRadians(30), radians[2], 1.0e-6F);
        assertEquals((float) (Math.PI / 2), pose.rotationRadians(Bone.BODY)[1], 1.0e-6F);
    }

    @Test
    void aLeftHandedPoseSwapsTheSidesAndTurnsTheOtherWay() throws ClipFormatException {
        Pose mirror = ClipSampler.pose(simple(), 0.0).mirrored();

        assertArrayEquals(new double[] {10, -20, -30}, mirror.rotationDegrees(Bone.LEFT_ARM), EXACT);
        assertNull(mirror.rotationDegrees(Bone.RIGHT_ARM));
        assertArrayEquals(new double[] {0, -90, 0}, mirror.rotationDegrees(Bone.BODY), EXACT);
        assertArrayEquals(new double[] {-1, 2, 3}, mirror.position(Bone.LEFT_ARM), EXACT);
    }

    @Test
    void aClipIsStretchedToTheLengthOfItsPhase() throws ClipFormatException {
        Clip oneSecond = simple();

        assertEquals(0.5, ClipSampler.clipTime(oneSecond, 0.5, 1.0), EXACT);
        assertEquals(0.5, ClipSampler.clipTime(oneSecond, 1.5, 3.0), EXACT);
        assertEquals(0.5, ClipSampler.clipTime(oneSecond, 0.1, 0.2), EXACT);
        assertEquals(1.0, ClipSampler.clipTime(oneSecond, 9.0, 3.0), EXACT);
        assertEquals(0.0, ClipSampler.clipTime(oneSecond, -1.0, 3.0), EXACT);
        assertEquals(1.0, ClipSampler.clipTime(oneSecond, 0.2, 0.0), EXACT);
    }

    @Test
    void betweenTwoKeysAValueMovesInAStraightLineByDefault() throws ClipFormatException {
        Pose quarter = ClipSampler.pose(simple(), 0.25);

        assertArrayEquals(new double[] {15, 30, 45}, quarter.rotationDegrees(Bone.RIGHT_ARM), EXACT);
    }

    @Test
    void easeInCoversAnEighthOfTheWayAtHalfTime() throws ClipFormatException {
        Pose pose = ClipSampler.pose(simple(), 0.75);

        // From 60 to -60 with ease_in: 0.5 cubed = 0.125 of the way.
        assertArrayEquals(new double[] {20, 40, 60 - 120 * 0.125}, pose.rotationDegrees(Bone.RIGHT_ARM), EXACT);
    }

    @Test
    void easingsStartAtZeroEndAtOneAndDifferInTheMiddle() {
        for (Easing easing : Easing.values()) {
            assertEquals(0.0, easing.apply(0.0), EXACT, easing.name());
            assertEquals(1.0, easing.apply(1.0), EXACT, easing.name());
        }
        assertEquals(0.25, Easing.LINEAR.apply(0.25), EXACT);
        assertEquals(0.015625, Easing.EASE_IN.apply(0.25), EXACT);
        assertEquals(0.578125, Easing.EASE_OUT.apply(0.25), EXACT);
        assertEquals(0.0625, Easing.EASE_IN_OUT.apply(0.25), EXACT);
        assertEquals(0.9375, Easing.EASE_IN_OUT.apply(0.75), EXACT);
    }

    @Test
    void timeOutsideTheClipHoldsTheFirstOrLastPose() throws ClipFormatException {
        Clip clip = simple();

        assertArrayEquals(new double[] {10, 20, 30}, ClipSampler.pose(clip, -3).rotationDegrees(Bone.RIGHT_ARM), EXACT);
        assertArrayEquals(new double[] {20, 40, -60}, ClipSampler.pose(clip, 9).rotationDegrees(Bone.RIGHT_ARM), EXACT);
        // A track whose only key is at 0.25 has that value at every time.
        assertArrayEquals(new double[] {1, 2, 3}, ClipSampler.pose(clip, 0.0).position(Bone.RIGHT_ARM), EXACT);
    }

    @Test
    void aBoneTheClipDoesNotNameIsLeftAlone() throws ClipFormatException {
        Pose pose = ClipSampler.pose(simple(), 0.5);

        assertNull(pose.rotationDegrees(Bone.HEAD));
        assertNull(pose.rotationRadians(Bone.LEFT_LEG));
        assertNull(pose.position(Bone.BODY));
    }

    @Test
    void thePhaseTimeIsTheReportedTicksPlusTheTimeSinceTheReport() {
        assertEquals(0.0, ClipSampler.secondsIntoPhase(0, 0), EXACT);
        assertEquals(0.30, ClipSampler.secondsIntoPhase(6, 0), EXACT);
        assertEquals(0.425, ClipSampler.secondsIntoPhase(6, 125), EXACT);
        assertEquals(0.30, ClipSampler.secondsIntoPhase(6, -40), EXACT);
    }

    @Test
    void aBrokenClipFileIsRefusedWithThePlaceOfTheProblem() {
        assertTrue(refusal("[1]").contains("not a JSON object"));
        assertTrue(refusal("{ \"bones\": {} }").contains("'length'"));
        assertTrue(refusal("{ \"length\": 0, \"bones\": {} }").contains("above 0"));
        assertTrue(refusal("{ \"length\": 1 }").contains("'bones'"));
        assertTrue(refusal("{ \"length\": 1, \"bones\": {} }").contains("moves no bone"));
        assertTrue(refusal(clipWith("\"tail\": { \"rotation\": [ { \"t\": 0, \"v\": [0,0,0] } ] }")).contains("unknown bone 'tail'"));
        assertTrue(refusal(clipWith("\"head\": { \"scale\": [ { \"t\": 0, \"v\": [0,0,0] } ] }")).contains("head.scale"));
        assertTrue(refusal(clipWith("\"head\": { \"rotation\": [] }")).contains("at least one key"));
        assertTrue(refusal(clipWith("\"head\": { \"rotation\": [ { \"t\": 0, \"v\": [0,0] } ] }"))
                .contains("head.rotation[0].v"));
        assertTrue(refusal(clipWith("\"head\": { \"rotation\": [ { \"t\": 2, \"v\": [0,0,0] } ] }"))
                .contains("outside 0 to the clip length"));
        assertTrue(refusal(clipWith(
                "\"head\": { \"rotation\": [ { \"t\": 0.5, \"v\": [0,0,0] }, { \"t\": 0.5, \"v\": [1,1,1] } ] }"))
                .contains("head.rotation[1].t is not after"));
        assertTrue(refusal(clipWith("\"head\": { \"rotation\": [ { \"t\": 0, \"v\": [0,0,0], \"ease\": \"bounce\" } ] }"))
                .contains("unknown easing 'bounce'"));
        assertTrue(refusal(clipWith("\"head\": { \"rotation\": [ { \"t\": 0, \"v\": [0,\"x\",0] } ] }"))
                .contains("not a number"));
    }

    private static String clipWith(String bone) {
        return "{ \"length\": 1, \"bones\": { " + bone + " } }";
    }

    @Test
    void theShippedSlashClipsLoadAndLastAsLongAsTheirDefaultPhases() throws Exception {
        SkillDefinition slash = BuiltInSkills.definitionOf("powerful_slash", DefaultsData.loadBundled());

        assertEquals(slash.number(SkillNumber.EXECUTION_LENGTH),
                ClipParser.parse(shipped("third_execution")).lengthSeconds(), EXACT);
        assertEquals(slash.number(SkillNumber.RECOVERY),
                ClipParser.parse(shipped("third_recovery")).lengthSeconds(), EXACT);
        assertEquals(slash.number(SkillNumber.INTERRUPTED_RECOVERY),
                ClipParser.parse(shipped("third_interrupted_recovery")).lengthSeconds(), EXACT);
    }

    @Test
    void theShippedSlashHasItsStrikePoseAtTheHitTime() throws Exception {
        SkillDefinition slash = BuiltInSkills.definitionOf("powerful_slash", DefaultsData.loadBundled());
        double hitTime = slash.hitWindowSeconds().get(0);
        Pose strike = ClipSampler.pose(ClipParser.parse(shipped("third_execution")), hitTime);

        assertArrayEquals(new double[] {-35, 0, 90}, strike.rotationDegrees(Bone.RIGHT_ARM), EXACT);
        assertArrayEquals(new double[] {20, 0, -22}, strike.rotationDegrees(Bone.LEFT_ARM), EXACT);
        assertArrayEquals(new double[] {4, -20, 0}, strike.rotationDegrees(Bone.BODY), EXACT);
        assertEquals("body.rot=[4.00,-20.00,0.00] right_arm.rot=[-35.00,0.00,90.00] left_arm.rot=[20.00,0.00,-22.00]",
                strike.describe());
    }

    @Test
    void theFirstPersonSlashClipsLoadFitTheirPhasesAndJoin() throws Exception {
        SkillDefinition slash = BuiltInSkills.definitionOf("powerful_slash", DefaultsData.loadBundled());
        Clip execution = ClipParser.parse(shipped("first_execution"));
        Clip recovery = ClipParser.parse(shipped("first_recovery"));
        Clip interrupted = ClipParser.parse(shipped("first_interrupted_recovery"));

        assertEquals(slash.number(SkillNumber.EXECUTION_LENGTH), execution.lengthSeconds(), EXACT);
        assertEquals(slash.number(SkillNumber.RECOVERY), recovery.lengthSeconds(), EXACT);
        assertEquals(slash.number(SkillNumber.INTERRUPTED_RECOVERY), interrupted.lengthSeconds(), EXACT);
        Pose end = ClipSampler.pose(execution, execution.lengthSeconds());
        Pose next = ClipSampler.pose(recovery, 0.0);
        assertArrayEquals(end.rotationDegrees(Bone.RIGHT_ARM), next.rotationDegrees(Bone.RIGHT_ARM), EXACT);
        assertArrayEquals(end.position(Bone.RIGHT_ARM), next.position(Bone.RIGHT_ARM), EXACT);
        assertArrayEquals(end.rotationDegrees(Bone.CAMERA), next.rotationDegrees(Bone.CAMERA), EXACT);
        // The first-person view draws nothing without both a place and a rotation for the weapon arm.
        for (Clip clip : new Clip[] {execution, recovery, interrupted}) {
            Pose start = ClipSampler.pose(clip, 0.0);
            assertNotNull(start.rotationDegrees(Bone.RIGHT_ARM));
            assertNotNull(start.position(Bone.RIGHT_ARM));
        }
        // The camera is level when each clip ends, so the view does not jump when the game takes over.
        assertArrayEquals(new double[] {0, 0, 0},
                ClipSampler.pose(recovery, recovery.lengthSeconds()).rotationDegrees(Bone.CAMERA), EXACT);
        assertArrayEquals(new double[] {0, 0, 0},
                ClipSampler.pose(interrupted, interrupted.lengthSeconds()).rotationDegrees(Bone.CAMERA), EXACT);
    }

    @Test
    void theRecoveryStartsWhereTheExecutionEndsAndEndsAtRest() throws Exception {
        Clip execution = ClipParser.parse(shipped("third_execution"));
        Clip recovery = ClipParser.parse(shipped("third_recovery"));

        for (Bone bone : new Bone[] {Bone.RIGHT_ARM, Bone.LEFT_ARM, Bone.BODY}) {
            assertArrayEquals(
                    ClipSampler.pose(execution, 0.5).rotationDegrees(bone),
                    ClipSampler.pose(recovery, 0.0).rotationDegrees(bone), EXACT, bone.name());
        }
        assertArrayEquals(new double[] {0, 0, 0}, ClipSampler.pose(recovery, 0.6).rotationDegrees(Bone.BODY), EXACT);
    }
}
