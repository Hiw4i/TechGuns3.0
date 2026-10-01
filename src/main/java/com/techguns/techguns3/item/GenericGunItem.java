package com.techguns.techguns3.item;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.registry.TGDataComponents;
import com.techguns.techguns3.registry.TGItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 26.3 firearm base.
 *
 * <p>Controls (like the 1.12 original): <b>LMB = fire</b> (handled by the client
 * input handler + server-authoritative {@code GunServerLogic}, not by vanilla
 * attack), <b>RMB hold = aim/zoom</b> (vanilla {@code use} with the {@code BOW}
 * pose), <b>R = reload</b> (keybind + auto-reload on empty).</p>
 *
 * <p>Loaded round count lives in the {@code AMMO} data component (was NBT {@code "ammo"}).
 * A stack without the component counts as a full magazine. Fire rate, ammo consumption
 * and projectiles are owned by the server; this class only describes the gun.</p>
 */
public class GenericGunItem extends Item {
    private final GunStats stats;
    private final Supplier<Item> ammoItem;
    private final String ammoId;
    private final GunExtras extras;

    public GenericGunItem(Properties properties, GunStats stats, Supplier<Item> ammoItem, String ammoId) {
        this(properties, stats, ammoItem, ammoId, GunExtras.DEFAULT);
    }

    public GenericGunItem(Properties properties, GunStats stats, Supplier<Item> ammoItem, String ammoId,
                          GunExtras extras) {
        super(properties.stacksTo(1));
        this.stats = stats;
        this.ammoItem = ammoItem;
        this.ammoId = ammoId;
        this.extras = extras == null ? GunExtras.DEFAULT : extras;
    }

    public GunStats stats() { return stats; }

    public GunExtras extras() { return extras; }

    public Item ammoItem() { return ammoItem.get(); }

    public boolean isTwoHanded() {
        return this != TGItems.PISTOL.get() && this != TGItems.REVOLVER.get()
                && this != TGItems.LASERPISTOL.get();
    }

    public Item emptyMagazineItem() {
        if (ammoItem.get() == TGItems.PISTOL_MAGAZINE.get()) return TGItems.PISTOL_MAGAZINE_EMPTY.get();
        if (ammoItem.get() == TGItems.ASSAULT_RIFLE_MAGAZINE.get()) return TGItems.ASSAULT_RIFLE_MAGAZINE_EMPTY.get();
        if (ammoItem.get() == TGItems.SMG_MAGAZINE.get()) return TGItems.SMG_MAGAZINE_EMPTY.get();
        if (ammoItem.get() == TGItems.MINIGUN_DRUM.get()) return TGItems.MINIGUN_DRUM_EMPTY.get();
        if (ammoItem.get() == TGItems.ENERGY_CELL.get()) return TGItems.ENERGY_CELL_EMPTY.get();
        if (ammoItem.get() == TGItems.BIO_TANK.get()) return TGItems.BIO_TANK_EMPTY.get();
        if (ammoItem.get() == TGItems.FUEL_TANK.get()) return TGItems.FUEL_TANK_EMPTY.get();
        if (ammoItem.get() == TGItems.NUCLEAR_POWERCELL.get()) return TGItems.NUCLEAR_POWERCELL_EMPTY.get();
        if (ammoItem.get() == TGItems.ADVANCED_MAGAZINE.get()) return TGItems.ADVANCED_MAGAZINE_EMPTY.get();
        if (ammoItem.get() == TGItems.AS50_MAGAZINE.get()) return TGItems.AS50_MAGAZINE_EMPTY.get();
        if (ammoItem.get() == TGItems.GAUSS_MAGAZINE.get()) return TGItems.GAUSS_MAGAZINE_EMPTY.get();
        if (ammoItem.get() == TGItems.REDSTONE_BATTERY.get()) return TGItems.REDSTONE_BATTERY_EMPTY.get();
        return null;
    }

    /**
     * All accepted ammo items in priority order (default first, then variants).
     * Resolved at runtime by id so TGItems static-init order never matters.
     */
    public java.util.List<Item> acceptedAmmoItems() {
        java.util.List<Item> out = new java.util.ArrayList<>();
        out.add(ammoItem());
        if (extras().hasExtraAmmo()) {
            for (String id : extras().extraAmmoIds()) {
                try {
                    var loc = net.minecraft.resources.Identifier.fromNamespaceAndPath(
                            com.techguns.techguns3.TechGuns3.MODID, id);
                    var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(loc);
                    if (item != null && item != net.minecraft.world.item.Items.AIR && !out.contains(item)) {
                        out.add(item);
                    }
                } catch (Exception ignored) {}
            }
        }
        return out;
    }

    public float effectiveDamage() {
        return (float) (stats.baseDamage() * TGConfig.gunDamageMultiplier());
    }

    public int loadedRounds(ItemStack gun) {
        return gun.getOrDefault(TGDataComponents.AMMO.get(), stats.magazineSize());
    }

    public boolean isReloading(Player player, ItemStack gun) {
        return player.getCooldowns().isOnCooldown(gun);
    }

    // --- RMB hold = aim/zoom (vanilla use system, BOW pose gives aiming arms for free) ---

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!stats.canZoom()) {
            return InteractionResult.PASS;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        // Guns shoulder like a crossbow, never draw like a bow: both hands
        // come forward around the weapon in first and third person.
        return ItemUseAnimation.CROSSBOW;
    }

    // --- tooltip + ammo bar ---

    // Still the override path in vanilla (DiscFragment/SmithingTemplate do the
    // same); the deprecation targets external callers, not overrides.
    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip." + TechGuns3.MODID + ".gun.damage",
                String.format("%.1f", effectiveDamage())).withStyle(ChatFormatting.RED));
        tooltip.accept(Component.translatable("tooltip." + TechGuns3.MODID + ".gun.magazine",
                stats.magazineSize(), ammoId).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip." + TechGuns3.MODID + ".gun.loaded",
                loadedRounds(stack), stats.magazineSize()).withStyle(ChatFormatting.YELLOW));
        tooltip.accept(Component.translatable("tooltip." + TechGuns3.MODID + ".gun.range",
                stats.projectileKind() == GunStats.ProjectileKind.ELECTRIC
                        ? stats.maxRange() : Math.round(stats.maxRange() * stats.bulletSpeed()))
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("tooltip." + TechGuns3.MODID + ".gun.controls")
                .withStyle(ChatFormatting.DARK_GRAY));
        // No super call: the base impl is an empty deprecated no-op.
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return loadedRounds(stack) < stats.magazineSize();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0f * loadedRounds(stack) / (float) stats.magazineSize());
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float fraction = loadedRounds(stack) / (float) stats.magazineSize();
        return fraction > 0.5f ? 0x55FF55 : fraction > 0.2f ? 0xFFAA00 : 0xFF5555;
    }
}
