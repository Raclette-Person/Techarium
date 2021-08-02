package software.bernie.techarium.block.base;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import software.bernie.techarium.trait.Traits;
import software.bernie.techarium.trait.behaviour.IHasBehaviour;
import software.bernie.techarium.trait.block.BlockBehaviour;
import software.bernie.techarium.trait.block.BlockTraits;
import software.bernie.techarium.trait.block.MasterBlockTrait;
import software.bernie.techarium.trait.block.SlaveBlockTrait;
import software.bernie.techarium.util.Utils;

public abstract class TechariumBlock<T extends TileEntity> extends Block implements IHasBehaviour {

    private final BlockBehaviour behaviour;

    public TechariumBlock(BlockBehaviour behaviour, Properties properties) {
        super(configure(properties, behaviour));
        this.behaviour = behaviour;

        //Copied from the Block constructor. This is hacky but I can't figure out a better way to do this as createBlockStateDefinition() needs this block's behaviour, which hasn't been set yet.
        StateContainer.Builder<Block, BlockState> builder = new StateContainer.Builder<>(this);
        this.createBlockStateDefinition(builder);
        this.stateDefinition = builder.create(Block::defaultBlockState, BlockState::new);
        this.registerDefaultState(this.stateDefinition.any());

        behaviour.tweak(this);
    }

    public static Properties configure(Properties properties, BlockBehaviour behaviour) {
        behaviour.tweak(properties);
        return properties;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return behaviour.has(BlockTraits.TileEntityTrait.class);
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return behaviour.get(BlockTraits.TileEntityTrait.class).map(BlockTraits.TileEntityTrait::createTileEntity)
                .orElse(null);
    }

    @Override
    public boolean addDestroyEffects(BlockState state, World world, BlockPos pos, ParticleManager manager) {
        // god knows why but you have to return true to stop the particles
        return !behaviour.getRequired(BlockTraits.ParticlesTrait.class).isShowBreakParticles();
    }

    @Override
    public boolean addHitEffects(BlockState state, World world, RayTraceResult target, ParticleManager manager) {
        // god knows why but you have to return true to stop the particles
        return !behaviour.getRequired(BlockTraits.ParticlesTrait.class).isShowBreakParticles();
    }

    @Override
    public boolean addLandingEffects(BlockState state1, ServerWorld worldserver, BlockPos pos, BlockState state2, LivingEntity entity, int numberOfParticles) {
        return !behaviour.getRequired(BlockTraits.ParticlesTrait.class).isShowBreakParticles();
    }

    @Override
    public boolean addRunningEffects(BlockState state, World world, BlockPos pos, Entity entity) {
        return !behaviour.getRequired(BlockTraits.ParticlesTrait.class).isShowBreakParticles();
    }

    public Optional<Traits.DescriptionTrait> getDescription() {
        return behaviour.get(Traits.DescriptionTrait.class);
    }

    public DirectionProperty getDirectionProperty() {
        return getBehaviour().getRequired(BlockTraits.BlockRotationTrait.class).getDirectionProperty();
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext useContext) {
        return getBehaviour().getRequired(BlockTraits.BlockRotationTrait.class).getStateForPlacement(this, useContext);
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        if (this.behaviour != null)
            getBehaviour().getRequired(BlockTraits.BlockRotationTrait.class).createBlockStateDefinition(builder);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader p_220071_2_, BlockPos p_220071_3_, ISelectionContext p_220071_4_) {
        BlockTraits.VoxelShapeTrait voxelShapeTrait = behaviour.getRequired(BlockTraits.VoxelShapeTrait.class);
        if(state.hasProperty(HorizontalBlock.FACING)){
            return Utils.rotateVoxelShape(voxelShapeTrait.getCollisionBox(), state.getValue(HorizontalBlock.FACING));
        }
        return voxelShapeTrait.getCollisionBox();
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader p_220053_2_, BlockPos p_220053_3_, ISelectionContext p_220053_4_) {
        BlockTraits.VoxelShapeTrait voxelShapeTrait = behaviour.getRequired(BlockTraits.VoxelShapeTrait.class);
        if(state.hasProperty(HorizontalBlock.FACING)){
            return Utils.rotateVoxelShape(voxelShapeTrait.getBoundingBox(), state.getValue(HorizontalBlock.FACING));
        }
        return voxelShapeTrait.getBoundingBox();
    }

    @Override
    public BlockBehaviour getBehaviour() {
        return this.behaviour;
    }

    @Deprecated
    @Override
    @SuppressWarnings("deprecared")
    public void onRemove(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (world != null && state.getBlock() != newState.getBlock()) {
            this.getBehaviour().get(SlaveBlockTrait.class).ifPresent(trait -> trait.handleDestruction(world, pos, false));
            this.getBehaviour().get(MasterBlockTrait.class).ifPresent(trait -> trait.handleDestruction(world, pos, state, false));
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    public boolean removedByPlayer(BlockState blockState, World world, BlockPos pos, PlayerEntity player, boolean willHarvest, FluidState fluid) {
        if (world != null && willHarvest) {
            this.getBehaviour().get(SlaveBlockTrait.class).ifPresent(trait -> trait.drop(world, pos, player));
        }
        return super.removedByPlayer(blockState, world, pos, player, willHarvest, fluid);
    }

    @Override
    public void setPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (world != null) {
            this.getBehaviour().get(SlaveBlockTrait.class).ifPresent(trait -> trait.placeSlaves(world, pos));
            this.getBehaviour().get(MasterBlockTrait.class).ifPresent(trait -> trait.placeSlaves(world, pos));
        }
        super.setPlacedBy(world, pos, state, placer, stack);
    }
}
