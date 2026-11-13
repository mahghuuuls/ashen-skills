package com.mahghuuuls.ashenskills.ashenforge;

import com.mahghuuuls.ashenskills.AshenSkillsMod;
import com.mahghuuuls.ashenskills.Tags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The Ashen Forge block: an anvil shape, not a full cube. Using it opens the Forge screens;
 * breaking it drops itself, every stored Ash, and the weapon it holds.
 */
public final class AshenForgeBlock extends Block {

    public static final PropertyDirection FACING = BlockHorizontal.FACING;

    /** The long head lies across the direction the block faces. */
    private static final AxisAlignedBB HEAD_ALONG_X = new AxisAlignedBB(0.0, 0.0, 2 / 16.0, 1.0, 1.0, 14 / 16.0);
    private static final AxisAlignedBB HEAD_ALONG_Z = new AxisAlignedBB(2 / 16.0, 0.0, 0.0, 14 / 16.0, 1.0, 1.0);

    public AshenForgeBlock() {
        super(Material.IRON);
        setRegistryName(new ResourceLocation(Tags.MOD_ID, "ashen_forge"));
        setTranslationKey(Tags.MOD_ID + ".ashen_forge");
        setHardness(5.0F);
        setResistance(2000.0F);
        setSoundType(SoundType.ANVIL);
        setHarvestLevel("pickaxe", 0);
        setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
    }

    /** A Forge broken by hand still drops itself; a pickaxe is only faster. */
    @Override
    public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
        return true;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new AshenForgeTile();
    }

    @Override
    public boolean onBlockActivated(
            World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side,
            float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            player.openGui(AshenSkillsMod.instance, ForgeGuiHandler.FORGE_SCREENS, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof AshenForgeTile) {
            ((AshenForgeTile) tile).dropContents(world, pos);
        }
        // Removes the tile entity. Every open Forge screen then closes.
        super.breakBlock(world, pos, state);
    }

    @Override
    public IBlockState getStateForPlacement(
            World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, int meta,
            EntityLivingBase placer, EnumHand hand) {
        return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return state.getValue(FACING).getAxis() == EnumFacing.Axis.Z ? HEAD_ALONG_X : HEAD_ALONG_Z;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex();
    }

    @Override
    public IBlockState withRotation(IBlockState state, Rotation rotation) {
        return state.withProperty(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING);
    }
}
