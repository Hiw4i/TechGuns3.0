package com.techguns.techguns3.turret;

import com.techguns.techguns3.item.GenericGunItem;
import com.techguns.techguns3.registry.TGItems;
import com.techguns.techguns3.registry.TGMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Server-backed turret inventory and controls. Slot positions match the original GUI texture. */
public final class TurretMenu extends AbstractContainerMenu {
    private static final int BASE_SLOTS = 20;
    private final Container container;
    private final ContainerData data;
    private final TurretBaseBlockEntity base;

    public TurretMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(BASE_SLOTS), new SimpleContainerData(5), null);
    }

    public TurretMenu(int id, Inventory playerInventory, TurretBaseBlockEntity base) {
        this(id, playerInventory, base, base.menuData(), base);
    }

    private TurretMenu(int id, Inventory playerInventory, Container container, ContainerData data,
                       TurretBaseBlockEntity base) {
        super(TGMenus.TURRET.get(), id);
        checkContainerSize(container, BASE_SLOTS);
        checkContainerDataCount(data, 5);
        this.container = container;
        this.data = data;
        this.base = base;
        container.startOpen(playerInventory.player);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int index = row * 3 + column;
                addSlot(new Slot(container, index, 17 + column * 18, 17 + row * 18));
                addSlot(new Slot(container, 9 + index, 115 + column * 18, 17 + row * 18) {
                    @Override public boolean mayPlace(ItemStack stack) { return false; }
                });
            }
        }
        addSlot(new Slot(container, TurretBaseBlockEntity.GUN_SLOT, 84, 17) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof GenericGunItem;
            }
        });
        addSlot(new Slot(container, TurretBaseBlockEntity.ARMOR_SLOT, 84, 41) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.is(TGItems.TURRET_ARMOR_IRON.get());
            }
        });
        addStandardInventorySlots(playerInventory, 8, 84);
        addDataSlots(data);
    }

    public int setting(int index) { return data.get(index); }

    @Override public boolean stillValid(Player player) {
        return container.stillValid(player) && (base == null || base.canConfigure(player));
    }

    @Override public boolean clickMenuButton(Player player, int buttonId) {
        if (base == null || !base.canConfigure(player)) return false;
        if (buttonId == 0) base.togglePvp();
        else if (buttonId == 1) base.toggleAnimals();
        else return false;
        broadcastChanges();
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved;
        if (index < BASE_SLOTS) {
            moved = moveItemStackTo(stack, BASE_SLOTS, slots.size(), true);
        } else if (stack.getItem() instanceof GenericGunItem) {
            moved = moveItemStackTo(stack, 18, 19, false);
        } else if (stack.is(TGItems.TURRET_ARMOR_IRON.get())) {
            moved = moveItemStackTo(stack, 19, 20, false);
        } else {
            moved = moveItemStackTo(stack, 0, 18, false);
        }
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
