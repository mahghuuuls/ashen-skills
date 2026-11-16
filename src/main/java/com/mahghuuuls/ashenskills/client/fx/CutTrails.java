package com.mahghuuuls.ashenskills.client.fx;

import com.mahghuuuls.ashenskills.AshenSkillsMod;
import com.mahghuuuls.ashenskills.api.SkillPhase;
import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.config.Settings;
import com.mahghuuuls.ashenskills.diagnostics.Diagnostics;
import com.mahghuuuls.ashenskills.network.PhaseUpdateMessage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

/**
 * Draws the trails of cuts. The skill {@code <namespace>:<name>} has a trail when the file
 * {@code assets/<namespace>/ashen_trails/<name>.json} exists; a skill without it leaves none. A
 * trail starts when the skill's execution starts, stays around its user while the user moves, and
 * is only light in the air: it changes nothing in the world.
 */
public final class CutTrails implements IResourceManagerReloadListener {

    private static final String FOLDER = "ashen_trails/";
    private static final byte EXECUTION = PhaseUpdateMessage.codeOf(SkillPhase.EXECUTION);
    private static final byte INTERRUPTED_RECOVERY = PhaseUpdateMessage.codeOf(SkillPhase.INTERRUPTED_RECOVERY);
    /** How many pieces the crescent is drawn in. A look, not a game rule. */
    private static final int PIECES = 28;
    /** The push a spark gets away from the user, for each block it starts away; the game slows a spark down at once. A look, not a game rule. */
    private static final double SPARK_SPEED = 0.12;
    /** How strong the middle of a crescent is drawn at its brightest, 0 to 1. A look, not a game rule. */
    private static final float CORE_STRENGTH = 0.8F;
    /** The part of a crescent's length over which each end runs out. A look, not a game rule. */
    private static final double END_PART = 0.18;
    /** Half the height of the upright band along the middle of a crescent, in blocks. A look, not a game rule. */
    private static final double UPRIGHT_HALF_HEIGHT = 0.25;

    /** Skill id text to its trail, or to nothing when the skill has none; emptied when resources load again. */
    private static final Map<String, Optional<CutTrail>> KNOWN = new HashMap<>();
    private static final List<Running> RUNNING = new ArrayList<>();

    /** One trail in the air. */
    private static final class Running {
        final CutTrail trail;
        final int userEntityId;
        final float yaw;
        final long startedAtNanos;

        Running(CutTrail trail, int userEntityId, float yaw, long startedAtNanos) {
            this.trail = trail;
            this.userEntityId = userEntityId;
            this.yaw = yaw;
            this.startedAtNanos = startedAtNanos;
        }
    }

    private CutTrails() {
    }

    public static void register() {
        CutTrails trails = new CutTrails();
        MinecraftForge.EVENT_BUS.register(trails);
        IResourceManager manager = Minecraft.getMinecraft().getResourceManager();
        if (manager instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) manager).registerReloadListener(trails);
        }
    }

    /** Where the trail file of this skill id is looked for. */
    static ResourceLocation fileOf(String skillId) {
        ResourceLocation id = new ResourceLocation(skillId);
        return new ResourceLocation(id.getNamespace(), FOLDER + id.getPath() + ".json");
    }

    /** What a reported skill phase means for the trail of its user. */
    enum Change { START, END, NONE }

    /**
     * A cut leaves its trail from its first moment, so a player who comes into view later sees
     * none. A user whose skill was broken off or has ended has no trail: a trail never outlives
     * its skill, also when a pack makes the trail long or the skill short.
     *
     * @param phaseCode a phase code of {@link PhaseUpdateMessage}
     */
    static Change changeFor(byte phaseCode, int elapsedTicks) {
        if (phaseCode == EXECUTION) {
            return elapsedTicks == 0 ? Change.START : Change.NONE;
        }
        return phaseCode == INTERRUPTED_RECOVERY || phaseCode == PhaseUpdateMessage.NO_PHASE ? Change.END : Change.NONE;
    }

    /** Told every skill phase this client learns of. */
    public static void onPhase(int userEntityId, String skillId, byte phaseCode, int elapsedTicks) {
        Change change = changeFor(phaseCode, elapsedTicks);
        if (change == Change.NONE) {
            return;
        }
        // A user has one trail at most: a start report that comes twice must not light two.
        for (Iterator<Running> all = RUNNING.iterator(); all.hasNext();) {
            if (all.next().userEntityId == userEntityId) {
                all.remove();
            }
        }
        if (change == Change.START) {
            start(userEntityId, skillId);
        }
    }

    /** Whether this skill leaves a trail. */
    public static boolean has(String skillId) {
        return KNOWN.computeIfAbsent(skillId, CutTrails::load).isPresent();
    }

    /**
     * Starts the trail of this skill around its user, when the skill has one. Its sparks are at
     * most the player's particle limit for one skill use.
     */
    private static void start(int userEntityId, String skillId) {
        Minecraft game = Minecraft.getMinecraft();
        Entity user = game.world == null ? null : game.world.getEntityByID(userEntityId);
        CutTrail trail = KNOWN.computeIfAbsent(skillId, CutTrails::load).orElse(null);
        if (user == null || trail == null) {
            return;
        }
        // This computer's own clock, as the poses use: the world's time is set back by the server now and then.
        // A trail so also runs out while a single-player game is paused.
        RUNNING.add(new Running(trail, userEntityId, user.rotationYaw, System.nanoTime()));
        int sparks = Math.min(trail.sparks(), Settings.general().getInt(SettingKey.MAX_IMPACT_PARTICLES_PER_SKILL));
        for (int i = 0; i < sparks; i++) {
            double[] at = trail.pointAt(game.world.rand.nextDouble(), 1.0, user.rotationYaw);
            game.world.spawnParticle(EnumParticleTypes.CRIT, user.posX + at[0], user.posY + at[1], user.posZ + at[2],
                    at[0] * SPARK_SPEED, game.world.rand.nextDouble() * SPARK_SPEED, at[2] * SPARK_SPEED);
        }
        Diagnostics.log("CLIENT_TRAIL", "state=started user={} skill={} yaw={} sparks={}",
                userEntityId, skillId, user.rotationYaw, sparks);
    }

    private static Optional<CutTrail> load(String skillId) {
        ResourceLocation file = fileOf(skillId);
        IResource resource;
        try {
            resource = Minecraft.getMinecraft().getResourceManager().getResource(file);
        } catch (IOException none) {
            return Optional.empty();
        }
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return Optional.of(CutTrail.parse(reader));
        } catch (IOException | IllegalArgumentException wrong) {
            AshenSkillsMod.LOGGER.error("Trail file {}: {}; the skill {} shows no trail", file, wrong.getMessage(), skillId);
            return Optional.empty();
        }
    }

    @SubscribeEvent
    public void afterWorld(RenderWorldLastEvent event) {
        Minecraft game = Minecraft.getMinecraft();
        if (game.world == null || RUNNING.isEmpty()) {
            return;
        }
        long now = System.nanoTime();
        Entity viewer = game.getRenderViewEntity();
        if (viewer == null) {
            return;
        }
        float partial = event.getPartialTicks();
        double viewX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partial;
        double viewY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partial;
        double viewZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partial;

        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        // Light added to what is behind it: a trail never darkens the world.
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.depthMask(false);
        // The game throws away nearly clear pixels here; a trail fades through them.
        GlStateManager.disableAlpha();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        for (Iterator<Running> all = RUNNING.iterator(); all.hasNext();) {
            Running running = all.next();
            double age = (now - running.startedAtNanos) / 1.0e9;
            Entity user = game.world.getEntityByID(running.userEntityId);
            if (user == null || running.trail.isOver(age)) {
                all.remove();
                continue;
            }
            draw(running.trail, age, running.yaw,
                    user.lastTickPosX + (user.posX - user.lastTickPosX) * partial - viewX,
                    user.lastTickPosY + (user.posY - user.lastTickPosY) * partial - viewY,
                    user.lastTickPosZ + (user.posZ - user.lastTickPosZ) * partial - viewZ);
        }
        // Back to what the game has at this point of a frame: lighting stays off.
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableAlpha();
        GlStateManager.depthMask(true);
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * One crescent: bright and white along its middle, in the trail's color and clear at both
     * edges. A flat crescent seen from its own height is only a line, so a low upright band along
     * its middle is drawn with it; one of the two always faces the viewer.
     */
    private static void draw(CutTrail trail, double age, float yaw, double x, double y, double z) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        // The middle is the trail's color made lighter, not plain white, so the color shows.
        float coreRed = (1.0F + trail.red()) / 2;
        float coreGreen = (1.0F + trail.green()) / 2;
        float coreBlue = (1.0F + trail.blue()) / 2;
        for (int side = 0; side < 2; side++) {
            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int piece = 0; piece <= PIECES; piece++) {
                double along = piece / (double) PIECES;
                float strength = (float) (trail.strengthAt(along, age) * thinAtTheEnds(along));
                double[] middle = trail.pointAt(along, 0.5, yaw);
                double lift = side == 0 ? UPRIGHT_HALF_HEIGHT : -UPRIGHT_HALF_HEIGHT;
                buffer.pos(x + middle[0], y + middle[1] + lift, z + middle[2])
                        .color(trail.red(), trail.green(), trail.blue(), 0.0F).endVertex();
                buffer.pos(x + middle[0], y + middle[1], z + middle[2])
                        .color(coreRed, coreGreen, coreBlue, strength * CORE_STRENGTH).endVertex();
            }
            tessellator.draw();
        }
        for (int half = 0; half < 2; half++) {
            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int piece = 0; piece <= PIECES; piece++) {
                double along = piece / (double) PIECES;
                float strength = (float) (trail.strengthAt(along, age) * thinAtTheEnds(along));
                double[] edge = trail.pointAt(along, half == 0 ? 0.0 : 1.0, yaw);
                double[] middle = trail.pointAt(along, 0.5, yaw);
                buffer.pos(x + edge[0], y + edge[1], z + edge[2])
                        .color(trail.red(), trail.green(), trail.blue(), 0.0F).endVertex();
                buffer.pos(x + middle[0], y + middle[1], z + middle[2])
                        .color(coreRed, coreGreen, coreBlue, strength * CORE_STRENGTH).endVertex();
            }
            tessellator.draw();
        }
    }

    /** The two ends of a crescent run out to nothing, so a trail has no cut-off end. */
    private static double thinAtTheEnds(double along) {
        return Math.min(1.0, Math.min(along, 1.0 - along) / END_PART);
    }

    @Override
    public void onResourceManagerReload(IResourceManager manager) {
        KNOWN.clear();
    }

    /** Forgets the trails in the air; for leaving a world. */
    public static void reset() {
        RUNNING.clear();
    }
}
