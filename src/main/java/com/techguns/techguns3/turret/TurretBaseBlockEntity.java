package com.techguns.techguns3.turret;

import com.techguns.techguns3.registry.TGBlockEntities;
import com.techguns.techguns3.registry.TGEntities;
import com.techguns.techguns3.item.GenericGunItem;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/** Persistent base and ammunition inventory. The linked head is recreated only when absent. */
public final class TurretBaseBlockEntity extends BaseContainerBlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    public static final int INPUT_END = 9;
    public static final int OUTPUT_END = 18;
    public static final int GUN_SLOT = 18;
    public static final int ARMOR_SLOT = 19;
    private NonNullList<ItemStack> items = NonNullList.withSize(20, ItemStack.EMPTY);
    private UUID owner;
    private UUID headId;
    private boolean pvp;
    private boolean attackAnimals;
    private int loadedRounds;
    private int reloadTicks;
    private int fireTicks;
    private int checkTicks;
    private int headHealth = 50;

    public TurretBaseBlockEntity(BlockPos pos, BlockState state) {
        super(TGBlockEntities.TURRET_BASE.get(), pos, state);
    }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return animationCache; }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, TurretBaseBlockEntity base) {
        if (++base.checkTicks < 40) return;
        base.checkTicks = 0;
        AABB area = new AABB(pos).inflate(1.5);
        var heads = level.getEntitiesOfClass(TurretHeadEntity.class, area,
                head -> head.anchor().equals(pos));
        TurretHeadEntity linked = null;
        for (TurretHeadEntity head : heads) {
            if (base.headId != null && base.headId.equals(head.getUUID())) {
                linked = head;
                break;
            }
        }
        if (linked == null && !heads.isEmpty()) linked = heads.getFirst();
        for (TurretHeadEntity head : heads) if (head != linked) head.discard();
        if (linked == null) {
            linked = TGEntities.TURRET_HEAD.get().create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if (linked != null) {
                linked.setAnchor(pos);
                linked.setPos(pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5);
                level.addFreshEntity(linked);
            }
        }
        if (linked != null && !linked.getUUID().equals(base.headId)) {
            base.headId = linked.getUUID();
            base.setChanged();
        }
        if (linked != null) linked.setItemSlot(EquipmentSlot.MAINHAND, base.getItem(GUN_SLOT).copy());
        if (linked != null) base.headHealth = Math.round(linked.getHealth());
    }

    public void removeHead(ServerLevel level) {
        for (TurretHeadEntity head : level.getEntitiesOfClass(TurretHeadEntity.class,
                new AABB(worldPosition).inflate(2), head -> head.anchor().equals(worldPosition))) {
            head.discard();
        }
        headId = null;
    }

    public void setOwner(UUID id) { owner = id; setChanged(); }
    public UUID owner() { return owner; }
    public boolean pvp() { return pvp; }
    public boolean attackAnimals() { return attackAnimals; }
    public boolean canConfigure(Player player) { return owner == null || owner.equals(player.getUUID()); }
    public void togglePvp() { pvp = !pvp; setChanged(); }
    public void toggleAnimals() { attackAnimals = !attackAnimals; setChanged(); }
    public int loadedRounds() { return loadedRounds; }
    public void setLoadedRounds(int value) { loadedRounds = value; setChanged(); }
    public int reloadTicks() { return reloadTicks; }
    public void setReloadTicks(int value) { reloadTicks = value; setChanged(); }
    public int fireTicks() { return fireTicks; }
    public void setFireTicks(int value) { fireTicks = value; setChanged(); }
    public GenericGunItem gun() {
        return items.get(GUN_SLOT).getItem() instanceof GenericGunItem gun ? gun : null;
    }

    public ContainerData menuData() {
        return new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> pvp ? 1 : 0;
                    case 1 -> attackAnimals ? 1 : 0;
                    case 2 -> loadedRounds;
                    case 3 -> headHealth;
                    case 4 -> level != null && level.hasNeighborSignal(worldPosition) ? 1 : 0;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 5; }
        };
    }

    public boolean canStoreOutput(Item item) {
        for (int i = INPUT_END; i < OUTPUT_END; i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty() || (stack.is(item) && stack.getCount() < stack.getMaxStackSize())) return true;
        }
        return false;
    }

    public void storeOutput(Item item) {
        for (int i = INPUT_END; i < OUTPUT_END; i++) {
            ItemStack stack = items.get(i);
            if (stack.is(item) && stack.getCount() < stack.getMaxStackSize()) {
                stack.grow(1);
                setChanged();
                return;
            }
        }
        for (int i = INPUT_END; i < OUTPUT_END; i++) {
            if (items.get(i).isEmpty()) {
                setItem(i, new ItemStack(item));
                return;
            }
        }
    }

    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> replacement) { items = replacement; }
    @Override public void setItem(int slot, ItemStack stack, boolean notify) {
        Item previous = slot == GUN_SLOT ? getItem(slot).getItem() : null;
        super.setItem(slot, stack, notify);
        if (slot == GUN_SLOT && previous != stack.getItem()) {
            loadedRounds = 0;
            reloadTicks = 0;
            fireTicks = 0;
            setChanged();
        }
    }
    @Override public int getContainerSize() { return items.size(); }
    @Override protected Component getDefaultName() { return Component.translatable("container.techguns3.turret_base"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new TurretMenu(id, inventory, this);
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        if (owner != null) output.putString("Owner", owner.toString());
        if (headId != null) output.putString("Head", headId.toString());
        output.putBoolean("PvP", pvp);
        output.putBoolean("Animals", attackAnimals);
        output.putInt("Loaded", loadedRounds);
        output.putInt("Reload", reloadTicks);
        output.putInt("Fire", fireTicks);
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(20, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        owner = parseUuid(input.getStringOr("Owner", ""));
        headId = parseUuid(input.getStringOr("Head", ""));
        pvp = input.getBooleanOr("PvP", false);
        attackAnimals = input.getBooleanOr("Animals", false);
        loadedRounds = input.getIntOr("Loaded", 0);
        reloadTicks = input.getIntOr("Reload", 0);
        fireTicks = input.getIntOr("Fire", 0);
    }

    private static UUID parseUuid(String text) {
        try { return UUID.fromString(text); }
        catch (IllegalArgumentException ignored) { return null; }
    }
}
