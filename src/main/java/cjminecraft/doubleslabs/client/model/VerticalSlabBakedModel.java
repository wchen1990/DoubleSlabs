package cjminecraft.doubleslabs.client.model;

import cjminecraft.doubleslabs.api.IBlockInfo;
import cjminecraft.doubleslabs.api.SlabSupport;
import cjminecraft.doubleslabs.api.support.IHorizontalSlabSupport;
import cjminecraft.doubleslabs.api.support.IVerticalSlabSupport;
import cjminecraft.doubleslabs.client.ClientConstants;
import cjminecraft.doubleslabs.client.util.ClientUtils;
import cjminecraft.doubleslabs.client.util.CullInfo;
import cjminecraft.doubleslabs.client.util.SlabCacheKey;
import cjminecraft.doubleslabs.client.util.vertex.VerticalSlabTransformer;
import cjminecraft.doubleslabs.common.blocks.VerticalSlabBlock;
import cjminecraft.doubleslabs.common.config.DSConfig;
import cjminecraft.doubleslabs.common.init.DSBlocks;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.FaceBakery;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static cjminecraft.doubleslabs.client.ClientConstants.getFallbackModel;

public class VerticalSlabBakedModel extends DynamicSlabBakedModel {

    public static final VerticalSlabBakedModel INSTANCE = new VerticalSlabBakedModel();

    public static final ModelProperty<Boolean> ROTATE_POSITIVE = new ModelProperty<>();
    public static final ModelProperty<Boolean> ROTATE_NEGATIVE = new ModelProperty<>();

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand, @Nonnull IModelData extraData) {
        if (state != null && extraData.hasProperty(POSITIVE_BLOCK) && extraData.hasProperty(NEGATIVE_BLOCK)) {
            IBlockInfo positiveBlock = extraData.getData(POSITIVE_BLOCK);
            IBlockInfo negativeBlock = extraData.getData(NEGATIVE_BLOCK);

            assert positiveBlock != null;
            assert negativeBlock != null;

            BlockState positiveState = positiveBlock.getBlockState();
            BlockState negativeState = negativeBlock.getBlockState();

            if (positiveState == null && negativeState == null)
                return getFallbackModel().getQuads(state, side, rand, extraData);

            boolean positiveTransparent = positiveState == null || ClientUtils.isTransparent(positiveState);
            boolean negativeTransparent = negativeState == null || ClientUtils.isTransparent(negativeState);
            boolean shouldCull = positiveState != null && negativeState != null && DSConfig.CLIENT.shouldCull(positiveState.getBlock()) && DSConfig.CLIENT.shouldCull(negativeState.getBlock()) && (!(positiveTransparent && negativeTransparent) || (positiveState.getBlock() == negativeState.getBlock() && positiveState.isIn(negativeState.getBlock())));

            Direction direction = state.get(VerticalSlabBlock.FACING);

            // If the top and bottom states are the same, use the combined block model where possible
            if (positiveState != null && negativeState != null && useDoubleSlabModel(positiveState, negativeState)) {
                IHorizontalSlabSupport horizontalSlabSupport = SlabSupport.isHorizontalSlab(positiveBlock.getWorld(), positiveBlock.getPos(), positiveState);
                if (horizontalSlabSupport != null && horizontalSlabSupport.useDoubleSlabModel(positiveState)) {
                    BlockState doubleState = horizontalSlabSupport.getStateForHalf(positiveBlock.getWorld(), positiveBlock.getPos(), positiveState, SlabType.DOUBLE);
                    if (RenderTypeLookup.canRenderInLayer(doubleState, MinecraftForgeClient.getRenderLayer()) || MinecraftForgeClient.getRenderLayer() == null) {
                        IBakedModel model = ClientConstants.getVerticalModel(doubleState, direction);
                        return model.getQuads(doubleState, side, rand, EmptyModelData.INSTANCE);
                    }
                    return Lists.newArrayList();
                }
            }

            List<BakedQuad> quads = Lists.newArrayList();

            if (positiveState != null && (RenderTypeLookup.canRenderInLayer(positiveState, MinecraftForgeClient.getRenderLayer()) || MinecraftForgeClient.getRenderLayer() == null)) {
                List<BakedQuad> positiveQuads = getQuadsForState(positiveBlock, ClientConstants.getVerticalModel(positiveState, direction), side, rand);
                if (shouldCull)
                    if ((!negativeTransparent && !positiveTransparent) || (positiveTransparent && !negativeTransparent) || (positiveTransparent && negativeTransparent))
                        positiveQuads.removeIf(bakedQuad -> bakedQuad.getFace() == direction.getOpposite());
                quads.addAll(positiveQuads);
            }
            if (negativeState != null && (RenderTypeLookup.canRenderInLayer(negativeState, MinecraftForgeClient.getRenderLayer()) || MinecraftForgeClient.getRenderLayer() == null)) {
                List<BakedQuad> negativeQuads = getQuadsForState(negativeBlock, ClientConstants.getVerticalModel(negativeState, direction), side, rand);
                if (shouldCull)
                    if ((!positiveTransparent && !negativeTransparent) || (negativeTransparent && !positiveTransparent) || (positiveTransparent && negativeTransparent))
                        negativeQuads.removeIf(bakedQuad -> bakedQuad.getFace() == direction);
                quads.addAll(negativeQuads);
            }
            return quads;
        } else if (MinecraftForgeClient.getRenderLayer() == null) {
            // Rendering the break block animation
            IBakedModel model = this.models.get(state);
            if (model != null)
                return model.getQuads(state, side, rand, extraData);
        }
        return getFallbackModel().getQuads(state, side, rand, extraData);
    }

    private final Map<BlockState, IBakedModel> models = new HashMap<>();

    public void addModel(IBakedModel model, BlockState state) {
        this.models.put(state, model);
    }

    public IBakedModel getModel(BlockState state) {
        return this.models.get(state);
    }

    private boolean rotateModel(IModelData modelData, ModelProperty<IBlockInfo> property, IBlockDisplayReader world, BlockPos pos) {
        if (modelData.hasProperty(property)) {
            IBlockInfo blockInfo = modelData.getData(property);
            if (blockInfo != null && blockInfo.getBlockState() != null) {
                IVerticalSlabSupport support = SlabSupport.getVerticalSlabSupport(world, pos, blockInfo.getBlockState());
                if (support != null)
                    return support.rotateModel(blockInfo.getWorld(), pos, blockInfo.getBlockState());
            }
        }
        return true;
    }

    @Nonnull
    @Override
    public IModelData getModelData(@Nonnull IBlockDisplayReader world, @Nonnull BlockPos pos, @Nonnull BlockState state, @Nonnull IModelData tileData) {
        IModelData modelData = super.getModelData(world, pos, state, tileData);
        modelData.setData(ROTATE_POSITIVE, rotateModel(modelData, POSITIVE_BLOCK, world, pos));
        modelData.setData(ROTATE_NEGATIVE, rotateModel(modelData, NEGATIVE_BLOCK, world, pos));
        return modelData;
    }
}
