package com.techguns.techguns3.item;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.registry.TGDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
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
 * 26.3 firearm base — data-driven.
 *
 * <p>Controls (like the 1.12 original): <b>LMB = fire</b> (handled by the client
 * input handler + server-authoritative {@code GunServerLogic}, not by vanilla
 * attack), <b>RMB hold = aim/zoom</b> (vanilla {@code use} with the {@code BOW}
 * pose), <b>R = reload</b> (keybind + auto-reload on empty).</p>
 *
 * <p>All tunables live in {@code data/<namespace>/guns/<gunId>.json} and are
 * resolved per call via {@link GunDefProvider} so {@code /reload} applies to
 * the next shot. The constructor only carries the gun id, the default ammo
 * supplier (for empty-mag mapping) and emergency sound fallbacks used when a
 * JSON entry is missing its sounds.</p>
 *
 * <p>Loaded round count lives in the {@code AMMO} data component (was NBT {@code "ammo"}).
 * A stack without the component counts as a full magazine. Fire rate, ammo consumption
 * and projectiles are owned by the server; this class only describes the gun.</p>
 */
public class GenericGunItem extends Item {
    /** Vanilla bow-style "hold indefinitely" use duration (aim pose). */
    private static final int USE_DURATION_INFINITE = 72000;
    /** Ammo bar geometry and thresholds. */
    private static final float BAR_WIDTH = 13.0f;
    private static final float BAR_FULL_FRACTION = 0.5f;
    private static final float BAR_LOW_FRACTION = 0.2f;
    private static final int BAR_COLOR_FULL = 0x55FF55;
    private static final int BAR_COLOR_MID = 0xFFAA00;
    private static final int BAR_COLOR_LOW = 0xFF5555;

    private final Identifier gunId;
    private final Supplier<Item> ammoItem;
    private final String ammoId;
    private final Supplier<SoundEvent> fallbackFireSound;
    private final Supplier<SoundEvent> fallbackReloadSound;

    public GenericGunItem(Properties properties, Identifier gunId,
                          Supplier<Item> ammoItem, String ammoId,
                          Supplier<SoundEvent> fallbackFireSound,
                          Supplier<SoundEvent> fallbackReloadSound) {
        super(properties.stacksTo(1));
        this.gunId = gunId;
        this.ammoItem = ammoItem;
        this.ammoId = ammoId;
        this.fallbackFireSound = fallbackFireSound;
        this.fallbackReloadSound = fallbackReloadSound;
    }

    public Identifier gunId() { return gunId; }

    public GunDefinition def() { return GunDefProvider.get(gunId); }

    public GunStats stats() { return def().toStats(fallbackFireSound, fallbackReloadSound); }

    public GunExtras extras() { return def().toExtras(); }

    public Item ammoItem() { return ammoItem.get(); }

    public boolean isTwoHanded() {
        return def().twoHanded();
    }

    public Item emptyMagazineItem() {
        var emptyId = def().emptyMagazine();
        if (emptyId.isPresent()) {
            Item resolved = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(emptyId.get());
            if (resolved != null && resolved != net.minecraft.world.item.Items.AIR) return resolved;
        }
        // Legacy family mapping (kept as fallback when JSON omits empty_magazine).
        return com.techguns.techguns3.registry.TGItems.emptyMagazineOf(ammoItem.get());
    }

    /**
     * All accepted ammo items in priority order (default first, then variants).
     * Resolved at runtime by id so TGItems static-init order never matters.
     */
    public java.util.List<Item> acceptedAmmoItems() {
        java.util.List<Item> out = new java.util.ArrayList<>();
        out.add(ammoItem());
        GunExtras ex = extras();
        if (ex.hasExtraAmmo()) {
            for (Identifier id : ex.extraAmmoIds()) {
                Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id);
                if (item == net.minecraft.world.item.Items.AIR) {
                    TechGuns3.LOGGER.debug("[TechGuns3] skipping unknown extra ammo {} for {}", id, gunId);
                } else if (!out.contains(item)) {
                    out.add(item);
                }
            }
        }
        return out;
    }

    public float effectiveDamage() {
        return (float) (def().damage() * TGConfig.gunDamageMultiplier());
    }

    public int loadedRounds(ItemStack gun) {
        return gun.getOrDefault(TGDataComponents.AMMO.get(), def().magazine());
    }

    // --- RMB hold = aim/zoom (vanilla use system, BOW pose gives aiming arms for free) ---

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!def().canZoom()) {
            return InteractionResult.PASS;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return USE_DURATION_INFINITE;
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
        // Single definition lookup: stats()/effectiveDamage()/loadedRounds() each
        // resolve it again, so derive everything from this one copy.
        GunDefinition d = def();
        float dmg = (float) (d.damage() * TGConfig.gunDamageMultiplier());
        int mag = d.magazine();
        int loaded = stack.getOrDefault(TGDataComponents.AMMO.get(), mag);
        boolean electric = "ELECTRIC".equals(d.projectile());
        tooltip.accept(Component.translatable("tooltip." + TechGuns3.MODID + ".gun.damage",
                String.format("%.1f", dmg)).withStyle(ChatFormatting.RED));
        tooltip.accept(Component.translatable("tooltip." + TechGuns3.MODID + ".gun.magazine",
                mag, ammoId).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip." + TechGuns3.MODID + ".gun.loaded",
                loaded, mag).withStyle(ChatFormatting.YELLOW));
        tooltip.accept(Component.translatable("tooltip." + TechGuns3.MODID + ".gun.range",
                electric ? d.rangeTtl() : Math.round(d.rangeTtl() * d.speed()))
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("tooltip." + TechGuns3.MODID + ".gun.controls")
                .withStyle(ChatFormatting.DARK_GRAY));
        // No super call: the base impl is an empty deprecated no-op.
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        int mag = def().magazine();
        return stack.getOrDefault(TGDataComponents.AMMO.get(), mag) < mag;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int mag = def().magazine();
        return Math.round(BAR_WIDTH * stack.getOrDefault(TGDataComponents.AMMO.get(), mag) / (float) mag);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int mag = def().magazine();
        float fraction = stack.getOrDefault(TGDataComponents.AMMO.get(), mag) / (float) mag;
        return fraction > BAR_FULL_FRACTION ? BAR_COLOR_FULL
                : fraction > BAR_LOW_FRACTION ? BAR_COLOR_MID : BAR_COLOR_LOW;
    }
}
