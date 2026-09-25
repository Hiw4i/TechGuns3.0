package com.techguns.techguns3.test;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.combat.GunServerLogic;
import com.techguns.techguns3.entity.BulletProjectile;
import com.techguns.techguns3.item.GenericGunItem;
import com.techguns.techguns3.network.GunPackets;
import com.techguns.techguns3.registry.TGDataComponents;
import com.techguns.techguns3.registry.TGItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.UUID;

/**
 * Headless gun diagnostics: {@code /tgtest autofire} (perm 2 / console).
 *
 * <p>Spawns a {@code FakePlayer}, script-drives the exact server paths a real
 * client uses (hold-fire via synthetic player ticks, semi presses, reload
 * request) against live iron_golems, logs PASS/FAIL per phase and halts the server.
 * Lets us prove the auto-fire loop without a display.</p>
 */
@EventBusSubscriber(modid = TechGuns3.MODID)
public final class GunSelfTestCommand {
    private GunSelfTestCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tgtest")
                .requires(src -> src.permissions().hasPermission(
                        net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("autofire").executes(ctx -> runAutoFire(ctx.getSource()))));
    }

    private static int runAutoFire(CommandSourceStack src) {
        runHeadless(src.getServer());
        src.sendSuccess(() -> net.minecraft.network.chat.Component.literal("[TGTEST] started, see log"), true);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Headless entry point: set env {@code TECHGUNS3_SELFTEST=autofire} and the
     * test starts itself shortly after server boot, then halts the server.
     */
    public static void runHeadless(MinecraftServer server) {
        NeoForgeBus.run(server);
    }

    /** Tick-driven phases; registered on the game bus for the test duration only. */
    private static final class NeoForgeBus {
        private ServerLevel level;
        private final MinecraftServer server;
        private final java.util.List<Long> forcedChunks = new java.util.ArrayList<>();
        private int tick;
        private boolean done;

        private net.neoforged.neoforge.common.util.FakePlayer fake;
        private IronGolem iron_golemA;
        private IronGolem iron_golemB;
        private IronGolem iron_golemC;
        private float iron_golemAHp;
        private float iron_golemBHp;
        private float iron_golemCHp;
        private int ammoA0;
        private int ammoB0;

        static void run(MinecraftServer server) {
            NeoForgeBus test = new NeoForgeBus(server);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.register(test);
        }

        private NeoForgeBus(MinecraftServer server) {
            this.server = server;
        }

        @SubscribeEvent
        public void onServerTick(ServerTickEvent.Post event) {
            if (done || event.getServer() != server) return;
            tick++;
            try {
                phase();
            } catch (Exception e) {
                TechGuns3.LOGGER.error("[TGTEST] exception at tick {}: {}", tick, e.toString());
                finish(false);
            }
        }

        private void phase() {
            switch (tick) {
                case 1 -> setup();
                case 2 -> phaseAStart();
                case 60 -> midBurstSample();
                case 102 -> phaseAEnd();
                case 103 -> phaseBStart();
                case 160 -> midArcSample();
                case 203 -> phaseBEnd();
                case 220 -> phaseCStart();
                case 250 -> phaseCMid();
                case 280 -> phaseCEnd();
                case 281 -> finish(true);
                default -> {
                    // While a hold-phase is active, deliver the same event the bus
                    // would deliver for a real ticking player (fakes are not in
                    // the player list, so the bus never fires for them).
                    if (fake != null && (tick < 102 || (tick > 103 && tick < 203))) {
                        GunServerLogic.onPlayerTick(new PlayerTickEvent.Post(fake));
                    }
                }
            }
        }

        private void setup() {
            level = server.overworld();
            server.setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true);
            BlockPos spawn = level.getLevelData().getRespawnData().pos();
            // Flat stone platform above local terrain: deterministic footing, no falls,
            // no suffocation, no environmental damage to confuse the asserts.
            int x0 = spawn.getX() - 10;
            int z0 = spawn.getZ() - 10;
            int top = 0;
            for (int dx = 0; dx < 30; dx++) {
                for (int dz = 0; dz < 12; dz++) {
                    top = Math.max(top, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x0 + dx, z0 + dz));
                }
            }
            int floorY = Math.min(top + 2, level.getMaxY() - 4);
            net.minecraft.world.level.block.state.BlockState stone =
                    net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
            net.minecraft.world.level.block.state.BlockState air =
                    net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            for (int dx = 0; dx < 30; dx++) {
                for (int dz = 0; dz < 12; dz++) {
                    level.setBlock(new BlockPos(x0 + dx, floorY, z0 + dz), stone, 3);
                    // Clear trees/leaves above: suffocation would fake damage readings.
                    for (int dy = 1; dy <= 6; dy++) {
                        level.setBlock(new BlockPos(x0 + dx, floorY + dy, z0 + dz), air, 3);
                    }
                }
            }
            int x = x0 + 2;
            int z = z0 + 6;
            int y = floorY + 1;
            // Pin the platform chunks: on a playerless server nothing else
            // keeps them ticking, and unloaded targets can't be hit.
            for (int dx = 0; dx < 30; dx += 16) {
                for (int dz = 0; dz < 12; dz += 16) {
                    int cx = (x0 + dx) >> 4;
                    int cz = (z0 + dz) >> 4;
                    level.setChunkForced(cx, cz, true);
                    forcedChunks.add(net.minecraft.world.level.ChunkPos.pack(cx, cz));
                }
            }
            fake = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "TGTest"));
            fake.setPos(x + 0.5, y, z + 0.5);
            iron_golemA = spawnIronGolem(x + 5, y, z);
            iron_golemB = spawnIronGolem(x + 8, y, z);
            iron_golemC = spawnIronGolem(x + 5, y, z + 3);
            // Calibration probe: flat 5 magic damage, no gun involved. Validates that
            // hp readings observe damage at all in this harness.
            iron_golemA.hurtServer(level, level.damageSources().magic(), 5.0f);
            TechGuns3.LOGGER.info("[TGTEST] setup: fake at {} iron_golems A({}) B({}) C({}) probeA hp={}",
                    fake.blockPosition(), iron_golemA.blockPosition(), iron_golemB.blockPosition(),
                    iron_golemC.blockPosition(), iron_golemA.getHealth());
        }

        private IronGolem spawnIronGolem(int x, int y, int z) {
            IronGolem iron_golem = iron_golemType().spawn(level, new BlockPos(x, y, z), EntitySpawnReason.COMMAND);
            if (iron_golem == null) throw new IllegalStateException("iron_golem did not spawn");
            iron_golem.setNoAi(true);
            iron_golem.setPos(x + 0.5, y, z + 0.5);
            return iron_golem;
        }

        @SuppressWarnings("unchecked")
        private static net.minecraft.world.entity.EntityType<IronGolem> iron_golemType() {
            return (net.minecraft.world.entity.EntityType<IronGolem>) net.minecraft.core.registries.BuiltInRegistries
                    .ENTITY_TYPE.getValue(net.minecraft.resources.Identifier
                            .fromNamespaceAndPath("minecraft", "iron_golem"));
        }

        /** Face a target point from the fake's muzzle height (feet + 1.52). */
        private void aimAt(double tx, double ty, double tz) {
            Vec3 from = fake.position();
            double mx = from.x;
            double my = from.y + 1.52;
            double mz = from.z;
            double dx = tx - mx;
            double dy = ty - my;
            double dz = tz - mz;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            fake.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
            fake.setXRot((float) Math.toDegrees(-Math.atan2(dy, horizontal)));
        }

        private void aimGolemA() {
            Vec3 p = iron_golemA.position();
            aimAt(p.x, p.y + 1.5, p.z);
        }

        private void aimGolemB() {
            Vec3 p = iron_golemB.position();
            aimAt(p.x, p.y + 1.5, p.z);
        }

        private void aimGolemC() {
            Vec3 p = iron_golemC.position();
            aimAt(p.x, p.y + 1.5, p.z);
        }

        private void giveMain(ItemStack stack) {
            fake.setItemSlot(EquipmentSlot.MAINHAND, stack);
        }

        private int ammoOf(ItemStack stack) {
            return ((GenericGunItem) stack.getItem()).loadedRounds(stack);
        }

        private int bulletsAlive() {
            Vec3 c = fake.position();
            return level.getEntitiesOfClass(BulletProjectile.class,
                    new AABB(c.x - 40, c.y - 10, c.z - 40, c.x + 40, c.y + 10, c.z + 40)).size();
        }

        // --- phase A: minigun hold-fire for 100 ticks ---

        private void phaseAStart() {
            ItemStack gun = new ItemStack(TGItems.MINIGUN.get());
            giveMain(gun);
            aimGolemA();
            fake.getInventory().add(new ItemStack(TGItems.MINIGUN_DRUM.get()));
            ammoA0 = ammoOf(gun);
            iron_golemAHp = iron_golemA.getHealth();
            GunServerLogic.onFireStart(fake, new GunPackets.FireStart(true, false));
            TechGuns3.LOGGER.info("[TGTEST] A start: minigun ammo={} golemA hp={} pos={}",
                    ammoA0, iron_golemAHp, iron_golemA.blockPosition());
        }

        private void midBurstSample() {
            ItemStack gun = fake.getMainHandItem();
            TechGuns3.LOGGER.info("[TGTEST] mid-burst: minigun ammo={} bulletsAlive={} golemA hp={} alive={} removed={}",
                    ammoOf(gun), bulletsAlive(), iron_golemA.getHealth(), iron_golemA.isAlive(),
                    iron_golemA.isRemoved() ? String.valueOf(iron_golemA.getRemovalReason()) : "no");
        }

        private void phaseAEnd() {
            ItemStack gun = fake.getMainHandItem();
            int ammo = ammoOf(gun);
            int bullets = bulletsAlive();
            float hp = iron_golemA.getHealth();
            boolean alive = iron_golemA.isAlive();
            GunServerLogic.onFireStop(fake, new GunPackets.FireStop(true));
            boolean fired = ammo <= ammoA0 - 90;
            boolean pass = fired && (bullets > 0 || hp < iron_golemAHp || !alive);
            TechGuns3.LOGGER.info("[TGTEST] A end: ammo {}->{} bulletsAlive={} golemA hp {}->{} alive={} => {}",
                    ammoA0, ammo, bullets, iron_golemAHp, hp, alive, pass ? "PASS" : "FAIL");
        }

        // --- phase B: teslagun hold-fire for 100 ticks ---

        private void phaseBStart() {
            ItemStack gun = new ItemStack(TGItems.TESLAGUN.get());
            giveMain(gun);
            aimGolemB();
            fake.getInventory().add(new ItemStack(TGItems.ENERGY_CELL.get()));
            ammoB0 = ammoOf(gun);
            iron_golemBHp = iron_golemB.getHealth();
            GunServerLogic.onFireStart(fake, new GunPackets.FireStart(true, false));
            TechGuns3.LOGGER.info("[TGTEST] B start: teslagun ammo={} golemB hp={} pos={}",
                    ammoB0, iron_golemBHp, iron_golemB.blockPosition());
        }

        private void midArcSample() {
            TechGuns3.LOGGER.info("[TGTEST] mid-arc: teslagun ammo={} golemB hp={} alive={} arcsAlive={}",
                    ammoOf(fake.getMainHandItem()), iron_golemB.getHealth(), iron_golemB.isAlive(), arcsAlive());
        }

        private int arcsAlive() {
            Vec3 c = fake.position();
            return level.getEntitiesOfClass(com.techguns.techguns3.entity.TeslaArcEntity.class,
                    new AABB(c.x - 40, c.y - 10, c.z - 40, c.x + 40, c.y + 10, c.z + 40)).size();
        }

        private void phaseBEnd() {
            ItemStack gun = fake.getMainHandItem();
            int ammo = ammoOf(gun);
            float hp = iron_golemB.getHealth();
            boolean alive = iron_golemB.isAlive();
            GunServerLogic.onFireStop(fake, new GunPackets.FireStop(true));
            boolean fired = ammo <= ammoB0 - 10;
            boolean pass = fired && (hp < iron_golemBHp || !alive);
            TechGuns3.LOGGER.info("[TGTEST] B end: ammo {}->{} golemB hp {}->{} alive={} => {}",
                    ammoB0, ammo, iron_golemBHp, hp, alive, pass ? "PASS" : "FAIL");
        }

        // --- phase C: shotgun semi control (2 presses, 30 ticks apart) ---

        private void phaseCStart() {
            ItemStack gun = new ItemStack(TGItems.COMBAT_SHOTGUN.get());
            giveMain(gun);
            aimGolemC();
            iron_golemCHp = iron_golemC.getHealth();
            GunServerLogic.onFirePressed(fake, new GunPackets.FirePressed(true, false));
            TechGuns3.LOGGER.info("[TGTEST] C press1: shotgun ammo={} golemC hp={}", ammoOf(gun), iron_golemCHp);
        }

        private void phaseCMid() {
            GunServerLogic.onFirePressed(fake, new GunPackets.FirePressed(true, false));
            TechGuns3.LOGGER.info("[TGTEST] C press2: shotgun ammo={}", ammoOf(fake.getMainHandItem()));
        }

        private void phaseCEnd() {
            ItemStack gun = fake.getMainHandItem();
            int ammo = ammoOf(gun);
            float hp = iron_golemC.getHealth();
            boolean alive = iron_golemC.isAlive();
            boolean pass = ammo == 6 && (hp < iron_golemCHp || !alive);
            TechGuns3.LOGGER.info("[TGTEST] C end: ammo {} (expect 6) golemC hp {}->{} alive={} => {}",
                    ammo, iron_golemCHp, hp, alive, pass ? "PASS" : "FAIL");

            // --- phase D: reload from empty ---
            gun.set(TGDataComponents.AMMO.get(), 0);
            fake.getInventory().add(new ItemStack(TGItems.MINIGUN_DRUM.get(), 2));
            giveMain(new ItemStack(TGItems.MINIGUN.get()));
            ItemStack mg = fake.getMainHandItem();
            mg.set(TGDataComponents.AMMO.get(), 0);
            boolean started = GunServerLogic.startReload(fake, mg, (GenericGunItem) mg.getItem());
            int after = ammoOf(mg);
            boolean passD = started && after == 200;
            TechGuns3.LOGGER.info("[TGTEST] D reload: started={} ammo={} (expect 200) => {}",
                    started, after, passD ? "PASS" : "FAIL");
        }

        private void finish(boolean ok) {
            done = true;
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(this);
            TechGuns3.LOGGER.info("[TGTEST] done ok={} — halting server", ok);
            if (fake != null) fake.discard();
            if (iron_golemA != null) iron_golemA.discard();
            if (iron_golemB != null) iron_golemB.discard();
            if (iron_golemC != null) iron_golemC.discard();
            for (long packed : forcedChunks) {
                level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(packed),
                        net.minecraft.world.level.ChunkPos.getZ(packed), false);
            }
            server.halt(false);
        }
    }
}
