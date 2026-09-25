package com.techguns.techguns3.turret;

import com.techguns.techguns3.registry.TGBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.Component;

public final class TurretBaseBlock extends BaseEntityBlock {
    public TurretBaseBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurretBaseBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, TGBlockEntities.TURRET_BASE.get(),
                (world, pos, current, base) -> TurretBaseBlockEntity.serverTick((ServerLevel) world, pos, current, base));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide() && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof TurretBaseBlockEntity base) {
            base.setOwner(player.getUUID());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TurretBaseBlockEntity base)) return InteractionResult.PASS;
        if (level instanceof ServerLevel && base.canConfigure(player)) {
            if (player.isShiftKeyDown()) {
                base.togglePvp();
                player.sendSystemMessage(Component.translatable("message.techguns3.turret.pvp", base.pvp()));
            }
            else player.openMenu(base);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, net.minecraft.world.InteractionHand hand,
                                          BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TurretBaseBlockEntity base) || !base.canConfigure(player)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server && stack.is(Items.FEATHER)) {
            base.toggleAnimals();
            player.sendSystemMessage(Component.translatable("message.techguns3.turret.animals", base.attackAnimals()));
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel server && stack.is(Items.IRON_INGOT)) {
            for (TurretHeadEntity head : server.getEntitiesOfClass(TurretHeadEntity.class,
                    new net.minecraft.world.phys.AABB(pos).inflate(2), entity -> entity.anchor().equals(pos))) {
                if (head.getHealth() < head.getMaxHealth()) {
                    head.heal(5.0f);
                    if (!player.isCreative()) stack.shrink(1);
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof TurretBaseBlockEntity base) {
            base.removeHead(server);
            net.minecraft.world.Containers.dropContents(server, pos, base);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
