package com.mahghuuuls.ashenskills.client.render;

import com.mahghuuuls.ashenskills.Tags;
import com.mahghuuuls.ashenskills.ashenforge.AshenForgeBlock;
import com.mahghuuuls.ashenskills.ashenforge.AshenForgeTile;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

/**
 * What an Ashen Forge shows beyond its block model. Its runes and the weapon-shaped groove on its
 * top are drawn at full brightness, so they look lit in the dark without the block giving light to
 * the world. The weapon it holds turns above the block; a Forge that holds no weapon shows none.
 */
public final class ForgeRenderer extends TileEntitySpecialRenderer<AshenForgeTile> {

    /** The lit pixels of the rune texture, at the same places; every other pixel is clear. */
    private static final ResourceLocation RUNE_GLOW = new ResourceLocation(Tags.MOD_ID, "textures/blocks/forge_runes_glow.png");
    /** The lit pixels of the top plate texture (the groove and its four studs), at the same places. */
    private static final ResourceLocation PLATE_GLOW = new ResourceLocation(Tags.MOD_ID, "textures/blocks/forge_plate_glow.png");

    // The rune waist of the block model, in blocks: 5 to 11 sixteenths wide, 5 to 10 high.
    private static final double WAIST_LOW = 5 / 16.0;
    private static final double WAIST_HIGH = 11 / 16.0;
    private static final double WAIST_BOTTOM = 5 / 16.0;
    private static final double WAIST_TOP = 10 / 16.0;
    // The part of the 16 by 16 glow texture one waist face shows: 6 wide, 5 high.
    private static final double WAIST_U_END = 6 / 16.0;
    private static final double WAIST_V_END = 5 / 16.0;
    // The top of the head in the model as it faces north: the full width, 3 to 13 sixteenths deep.
    // Its texture part has the same numbers: the model maps the plate straight onto it.
    private static final double HEAD_NORTH = 3 / 16.0;
    private static final double HEAD_SOUTH = 13 / 16.0;
    /** How far the lit pixels lie in front of the model's face, so the two do not flicker. */
    private static final double IN_FRONT = 0.002;
    private static final float FULL_BRIGHT = 240.0F;

    private static final double WEAPON_HEIGHT = 1.2;
    private static final float WEAPON_SCALE = 1.4F;
    /** One full turn and one full rise and fall, in ticks. Whole numbers, so the motion repeats exactly. */
    private static final long TURN_TICKS = 180;
    private static final long BOB_TICKS = 80;
    private static final double BOB_HEIGHT = 0.03;

    @Override
    public void render(
            AshenForgeTile forge, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        float lightX = OpenGlHelper.lastBrightnessX;
        float lightY = OpenGlHelper.lastBrightnessY;
        try {
            drawGlow(forge, x, y, z);
            ItemStack weapon = forge.weapon();
            if (!weapon.isEmpty()) {
                drawWeapon(forge, weapon, x, y, z, partialTicks);
            }
        } finally {
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightX, lightY);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private void drawGlow(AshenForgeTile forge, double x, double y, double z) {
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        try {
            GlStateManager.translate(x, y, z);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, FULL_BRIGHT, FULL_BRIGHT);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            drawRuneGlow();
            drawPlateGlow(forge);
        } finally {
            // Tile entities are drawn with culling and lighting on; leave both as they were.
            GlStateManager.enableCull();
            GlStateManager.enableLighting();
            GlStateManager.popMatrix();
        }
    }

    /** The four faces of the waist. They are equal, so the block's facing does not matter here. */
    private void drawRuneGlow() {
        bindTexture(RUNE_GLOW);
        double low = WAIST_LOW - IN_FRONT;
        double high = WAIST_HIGH + IN_FRONT;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        // Each face as its viewer sees it: the texture's left edge at the viewer's left.
        waistFace(buffer, WAIST_HIGH, low, WAIST_LOW, low);     // north, seen from the north
        waistFace(buffer, WAIST_LOW, high, WAIST_HIGH, high);   // south
        waistFace(buffer, high, WAIST_HIGH, high, WAIST_LOW);   // east
        waistFace(buffer, low, WAIST_LOW, low, WAIST_HIGH);     // west
        tessellator.draw();
    }

    /** One upright face from its left edge (x, z) to its right edge (x, z). */
    private static void waistFace(BufferBuilder buffer, double leftX, double leftZ, double rightX, double rightZ) {
        buffer.pos(leftX, WAIST_TOP, leftZ).tex(0.0, 0.0).endVertex();
        buffer.pos(leftX, WAIST_BOTTOM, leftZ).tex(0.0, WAIST_V_END).endVertex();
        buffer.pos(rightX, WAIST_BOTTOM, rightZ).tex(WAIST_U_END, WAIST_V_END).endVertex();
        buffer.pos(rightX, WAIST_TOP, rightZ).tex(WAIST_U_END, 0.0).endVertex();
    }

    /** The groove on the top of the head, turned with the block as the block model is turned. */
    private void drawPlateGlow(AshenForgeTile forge) {
        bindTexture(PLATE_GLOW);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(0.5, 0.0, 0.5);
            GlStateManager.rotate(quarterTurnsFromNorth(forge) * -90.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(-0.5, 0.0, -0.5);
            double top = 1.0 + IN_FRONT;
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            buffer.pos(0.0, top, HEAD_NORTH).tex(0.0, HEAD_NORTH).endVertex();
            buffer.pos(0.0, top, HEAD_SOUTH).tex(0.0, HEAD_SOUTH).endVertex();
            buffer.pos(1.0, top, HEAD_SOUTH).tex(1.0, HEAD_SOUTH).endVertex();
            buffer.pos(1.0, top, HEAD_NORTH).tex(1.0, HEAD_NORTH).endVertex();
            tessellator.draw();
        } finally {
            GlStateManager.popMatrix();
        }
    }

    /**
     * How many quarter turns, clockwise seen from above, the block model is turned for the block's
     * facing: the same as the block state file says (north 0, east 1, south 2, west 3).
     */
    private static int quarterTurnsFromNorth(AshenForgeTile forge) {
        IBlockState state = forge.getWorld().getBlockState(forge.getPos());
        if (!(state.getBlock() instanceof AshenForgeBlock)) {
            return 0;
        }
        EnumFacing facing = state.getValue(AshenForgeBlock.FACING);
        return facing == EnumFacing.EAST ? 1 : facing == EnumFacing.SOUTH ? 2 : facing == EnumFacing.WEST ? 3 : 0;
    }

    private static void drawWeapon(
            AshenForgeTile forge, ItemStack weapon, double x, double y, double z, float partialTicks) {
        // The world's tick count is cut to one period before the part of a tick is added: a
        // large count as a float would lose that part, and the weapon would turn in steps.
        long ticks = forge.getWorld().getTotalWorldTime();
        float turn = ((ticks % TURN_TICKS) + partialTicks) / TURN_TICKS;
        float bob = ((ticks % BOB_TICKS) + partialTicks) / BOB_TICKS;
        // Lit like the air above the Forge, not like the inside of its block.
        int light = forge.getWorld().getCombinedLight(forge.getPos().up(), 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(
                    x + 0.5, y + WEAPON_HEIGHT + MathHelper.sin(bob * 2.0F * (float) Math.PI) * BOB_HEIGHT, z + 0.5);
            GlStateManager.rotate(turn * 360.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.scale(WEAPON_SCALE, WEAPON_SCALE, WEAPON_SCALE);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            // The game's own way to draw an item lying on the ground, with the settings it needs
            // and the item lighting the game set for tile entities.
            Minecraft.getMinecraft().getRenderItem().renderItem(weapon, ItemCameraTransforms.TransformType.GROUND);
        } finally {
            GlStateManager.popMatrix();
            // Set again outside the weapon's own turn, so the lights are as the game left them.
            RenderHelper.enableStandardItemLighting();
        }
    }
}
