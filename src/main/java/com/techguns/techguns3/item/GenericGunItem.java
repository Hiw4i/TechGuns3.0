package com.techguns.techguns3.item;

import com.techguns.techguns3.TGConfig;
import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.entity.BulletProjectile;
import com.techguns.techguns3.registry.TGDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 26.3 firearm base. Ports the 1.12 {@code GenericGun.shootGunPrimary} loop in simplified form:
 *
 * <ul>
 *   <li>Loaded round count lives in the {@code AMMO} data component (was NBT {@code "ammo"}).
 *       A stack without the component counts as a full magazine.</li>
 *   <li>Each trigger pull consumes 1 loaded round and spawns {@code pellets} bullets server-side.</li>
 *   <li>An empty gun reloads from the player's inventory: 1 magazine item refills a
 *       magazine-fed gun, loose rounds refill round-by-round (shotgun shells use the
 *       1.12 {@code ammoCount}-style multi-consume). Creative reloads for free.</li>
 *   <li>Fire rate is enforced with vanilla item cooldowns ({@code minFiretime} /
 *       {@code reloadtime} in ticks); full-auto guns keep firing via the use-hold loop,
 *       semi-auto guns fire once per click.</li>
 * </ul>
 * Not ported yet: zoom, recoil spring, muzzle flash, ammo variants (incendiary),
 * melee-attack mode of tools, NPC AI usage, reload key handling.
 */
public class GenericGunItem extends Item {
    private final GunStats stats;
    private final Supplier<Item> ammoItem;
    private final String ammoId;

    public GenericGunItem(Properties properties, GunStats stats, Supplier<Item> ammoItem, String ammoId) {
        super(properties.stacksTo(1));
        this.stats = stats;
        this.ammoItem = ammoItem;
        this.ammoId = ammoId;
    }

    public GunStats stats() { return stats; }

    public float effectiveDamage() {
        return (float) (stats.baseDamage() * TGConfig.gunDamageMultiplier());
    }

    public int loadedRounds(ItemStack gun) {
        return gun.getOrDefault(TGDataComponents.AMMO.get(), stats.magazineSize());
    }

    // --- hold-to-fire: use() starts using, onUseTick() fires while held ---

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack gun = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(gun)) {
            return InteractionResult.FAIL;
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
        return ItemUseAnimation.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack gun, int remainingTicks) {
        if (!(user instanceof Player player)) {
            return;
        }
        int elapsed = getUseDuration(gun, user) - remainingTicks;
        if (stats.semiAuto() && elapsed > 1) {
            return;
        }
        if (player.getCooldowns().isOnCooldown(gun)) {
            return;
        }
        fire(level, player, gun);
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingTicks) {
        return true;
    }

    private void fire(Level level, Player player, ItemStack gun) {
        int loaded = loadedRounds(gun);
        if (loaded > 0) {
            if (!player.isCreative()) {
                gun.set(TGDataComponents.AMMO.get(), loaded - 1);
            }
            player.getCooldowns().addCooldown(gun, stats.fireCooldownTicks());
            if (!level.isClientSide()) {
                Vec3 spawn = muzzlePos(player);
                float yaw = player.getYRot();
                float pitch = player.getXRot();
                for (int i = 0; i < stats.pellets(); i++) {
                    float spread = i == 0 ? stats.spread() : stats.pelletSpread();
                    BulletProjectile.Spec spec = new BulletProjectile.Spec(
                            effectiveDamage(), stats.bulletSpeed(),
                            ticksToLive(), stats.dropStart(), stats.dropEnd(), stats.dropMin(),
                            stats.gravity(), stats.penetration());
                    level.addFreshEntity(new BulletProjectile(level, player, spawn, yaw, pitch, spread, spec));
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        stats.fireSound().get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                if (stats.extraSound() != null) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            stats.extraSound().get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }
        } else {
            reload(level, player, gun);
        }
    }

    private void reload(Level level, Player player, ItemStack gun) {
        if (player.isCreative()) {
            gun.set(TGDataComponents.AMMO.get(), stats.magazineSize());
            player.getCooldowns().addCooldown(gun, stats.reloadTimeTicks());
            return;
        }
        Item wanted = ammoItem.get();
        int slots = player.getInventory().getContainerSize();
        int found = 0;
        for (int i = 0; i < slots; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(wanted)) {
                found += stack.getCount();
            }
        }
        if (found <= 0) {
            return;
        }
        int take = stats.magazineFed() ? 1 : Math.min(stats.magazineSize(), found);
        int remaining = take;
        for (int i = 0; i < slots; i++) {
            if (remaining <= 0) {
                break;
            }
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(wanted)) {
                int remove = Math.min(remaining, stack.getCount());
                stack.shrink(remove);
                remaining -= remove;
            }
        }
        int consumed = take - remaining;
        if (consumed <= 0) {
            return;
        }
        int loaded = stats.magazineFed() ? stats.magazineSize() : Math.min(consumed, stats.magazineSize());
        gun.set(TGDataComponents.AMMO.get(), loaded);
        player.getCooldowns().addCooldown(gun, stats.reloadTimeTicks());
        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    stats.reloadSound().get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    /** 1.12 scaled TTL to tick count so range in blocks ≈ TTL. */
    private int ticksToLive() {
        return (int) Math.ceil(stats.maxRange() / stats.bulletSpeed());
    }

    /** Eye position shifted to the right hand side, like the 1.12 spawn offset. */
    private static Vec3 muzzlePos(Player player) {
        Vec3 eye = player.getEyePosition();
        double yawRad = Math.toRadians(player.getYRot());
        return eye.add(-Math.cos(yawRad) * 0.16, -0.1, -Math.sin(yawRad) * 0.16);
    }

    // --- tooltip + ammo bar ---

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
                stats.maxRange()).withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, display, tooltip, flag);
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
