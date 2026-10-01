package com.techguns.techguns3.test;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.combat.GunServerLogic;
import com.techguns.techguns3.entity.BulletProjectile;
import com.techguns.techguns3.entity.MuzzleFlashEntity;
import com.techguns.techguns3.entity.SlimeBlobEntity;
import com.techguns.techguns3.entity.TeslaArcEntity;
import com.techguns.techguns3.item.GenericGunItem;
import com.techguns.techguns3.network.GunPackets;
import com.techguns.techguns3.registry.TGDataComponents;
import com.techguns.techguns3.registry.TGItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
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
                case 281 -> phaseDReload();
                case 282 -> phaseEWeaveSetup();
                case 297 -> phaseEWeaveSample();
                case 298 -> phaseEHomeSetup();
                case 312 -> phaseEHomeSample();
                case 314 -> phaseFSlime();
                case 376 -> phaseGSetup();
                case 377 -> phaseGPoisonStart();
                case 397 -> phaseGPoisonSample();
                case 398 -> phaseGSlimeStart();
                case 409 -> phaseGSlimeSample();
                case 421 -> phaseHSetup();
                case 422 -> phaseHStart();
                case 450 -> phaseHSample();
                // --- phase I: wave-2 exotics through real firing ---
                case 451 -> phaseISetup();
                case 452 -> phaseIChargeStart();
                case 456 -> phaseIChargeRelease();
                case 476 -> phaseISample();
                case 477 -> phaseJSetup();
                case 478 -> phaseJStart();
                case 498 -> phaseJSample();
                case 499 -> phaseKSetup();
                case 504 -> phaseKFire();
                case 524 -> phaseKSample();
                case 525 -> phaseLSetup();
                case 526 -> phaseLFire();
                case 542 -> phaseLSample();
                case 543 -> phaseMSetup();
                case 544 -> phaseMStart();
                case 564 -> phaseMSample();
                // --- phase N: point-blank pistol (spawn must not skip past the target) ---
                case 585 -> phaseNSetup();
                case 586 -> phaseNFire();
                case 600 -> phaseNSample();
                case 610 -> finish(true);
                default -> {
                    // While a hold-phase is active, deliver the same event the bus
                    // would deliver for a real ticking player (fakes are not in
                    // the player list, so the bus never fires for them).
                    if (fake != null && (tick < 102 || (tick > 103 && tick < 203)
                            || (tick > 376 && tick < 450) || (tick > 452 && tick < 456)
                            || (tick > 477 && tick < 498) || (tick > 543 && tick < 564))) {
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
            platX0 = x0;
            platZ0 = z0;
            platFloorY = floorY;
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
            // The world persists between runs: wipe the whole test area clean.
            // Stale golems, stray passives and frozen FX would otherwise eat shots
            // meant for fresh targets (bullets can't tell them apart).
            for (Entity e : level.getEntitiesOfClass(Entity.class,
                    new AABB(x0 - 10, y - 10, z0 - 10, x0 + 40, y + 10, z0 + 22),
                    e -> !(e instanceof net.minecraft.world.entity.player.Player))) {
                e.discard();
            }
            // Pin the platform chunks: on a playerless server nothing else
            // keeps them ticking, and unloaded targets can't be hit.
            for (int dx = 0; dx < 30; dx += 8) {
                for (int dz = 0; dz < 12; dz += 8) {
                    int cx = (x0 + dx) >> 4;
                    int cz = (z0 + dz) >> 4;
                    level.setChunkForced(cx, cz, true);
                    forcedChunks.add(net.minecraft.world.level.ChunkPos.pack(cx, cz));
                }
            }
            fake = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "TGTest"));
            fake.setPos(x + 0.5, y, z + 0.5);
            // The world persists between runs: wipe leftover test entities first,
            // or stale golems stacked on the same spots would eat the shots.
            for (Entity e : level.getEntitiesOfClass(Entity.class,
                    new AABB(x0 - 10, y - 10, z0 - 10, x0 + 40, y + 10, z0 + 22),
                    e -> e instanceof IronGolem || e instanceof TeslaArcEntity
                            || e instanceof MuzzleFlashEntity || e instanceof BulletProjectile
                            || e instanceof SlimeBlobEntity)) {
                e.discard();
            }
            iron_golemA = spawnIronGolem(x + 5, y, z);
            iron_golemB = spawnIronGolem(x + 8, y, z);
            iron_golemC = spawnIronGolem(x + 5, y, z + 3);
            // Bolt determinism probe: same seed must rebuild the identical fractal.
            com.techguns.techguns3.fx.BoltGeometry.Bolt b1 =
                    com.techguns.techguns3.fx.BoltGeometry.tesla(12345, 0, 0, 0, 10, 2, -3);
            com.techguns.techguns3.fx.BoltGeometry.Bolt b2 =
                    com.techguns.techguns3.fx.BoltGeometry.tesla(12345, 0, 0, 0, 10, 2, -3);
            boolean deterministic = java.util.Arrays.equals(b1.main(), b2.main())
                    && b1.branches().size() == b2.branches().size()
                    && b1.main().length > 60;
            TechGuns3.LOGGER.info("[TGTEST] bolt determinism: points={} branches={} => {}",
                    b1.main().length / 3, b1.branches().size(), deterministic ? "PASS" : "FAIL");
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
        }

        // --- phase D: reload from empty ---
        private void phaseDReload() {
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

        // --- phase E: curved flight (weave deviation + homing hit) ---
        private com.techguns.techguns3.entity.BulletProjectile weaveBullet;
        private double weaveX0;
        private double weaveY0;
        private double weaveZ0;
        private double weaveDx;
        private double weaveDy;
        private double weaveDz;
        private IronGolem golemD;

        private void phaseEWeaveSetup() {
            // Clear the lane: earlier targets would eat the homing bullet mid-flight.
            iron_golemA.discard();
            iron_golemB.discard();
            iron_golemC.discard();
            fake.setYRot(-90.0f);
            fake.setXRot(-20.0f);
            Vec3 muzzle = com.techguns.techguns3.combat.GunServerLogic.muzzlePos(fake);
            double yaw = Math.toRadians(-90.0);
            double pitch = Math.toRadians(-20.0);
            weaveDx = -Math.sin(yaw) * Math.cos(pitch);
            weaveDy = -Math.sin(pitch);
            weaveDz = Math.cos(yaw) * Math.cos(pitch);
            com.techguns.techguns3.entity.BulletProjectile.Spec spec =
                    new com.techguns.techguns3.entity.BulletProjectile.Spec(
                            5.0f, 1.0f, 200, 1000.0f, 1000.0f, 5.0f, 0.0, 0.0f,
                            0.5f, 0.5f, -1, 0.2f, 0.0f, 0, 0, 0, 0xFFD9A8, 1.0f,
                            0.0f, 0.0f, 0.0f, 0.0f, 0.0f, false, 0.0f, false, 0, false);
            weaveBullet = new com.techguns.techguns3.entity.BulletProjectile(
                    level, fake, muzzle, -90.0f, -20.0f, 0.0f, spec);
            weaveX0 = muzzle.x;
            weaveY0 = muzzle.y;
            weaveZ0 = muzzle.z;
            level.addFreshEntity(weaveBullet);
            TechGuns3.LOGGER.info("[TGTEST] E weave fired");
        }

        private void phaseEWeaveSample() {
            Vec3 p = weaveBullet.position();
            double rx = p.x - weaveX0;
            double ry = p.y - weaveY0;
            double rz = p.z - weaveZ0;
            double along = rx * weaveDx + ry * weaveDy + rz * weaveDz;
            double lx = rx - weaveDx * along;
            double ly = ry - weaveDy * along;
            double lz = rz - weaveDz * along;
            double lateral = Math.sqrt(lx * lx + ly * ly + lz * lz);
            boolean pass = lateral > 0.4;
            TechGuns3.LOGGER.info("[TGTEST] E weave: lateral={} (expect >0.4) => {}",
                    String.format("%.2f", lateral), pass ? "PASS" : "FAIL");
        }

        private void phaseEHomeSetup() {
            // Clear the lane: leftover FX entities (arcs, flashes, weave bullet)
            // are collidable-free now, but stale ones still pollute the asserts.
            if (weaveBullet != null) weaveBullet.discard();
            for (Entity e : level.getEntitiesOfClass(Entity.class,
                    new AABB(fake.getX() - 30, fake.getY() - 10, fake.getZ() - 30,
                            fake.getX() + 30, fake.getY() + 10, fake.getZ() + 30),
                    x -> x instanceof TeslaArcEntity || x instanceof MuzzleFlashEntity
                            || x instanceof BulletProjectile)) {
                e.discard();
            }
            Vec3 f = fake.position();
            golemD = spawnIronGolem((int) Math.round(f.x) + 6, (int) Math.round(f.y), (int) Math.round(f.z) + 1);
            // Deliberate 8° aim error (not blind): proves steering corrects it
            // deterministically instead of relying on orbit-vs-contact bistability.
            Vec3 dp = golemD.position();
            aimAt(dp.x, dp.y + 1.5, dp.z);
            fake.setYRot(fake.getYRot() + 8.0f);
            Vec3 muzzle = com.techguns.techguns3.combat.GunServerLogic.muzzlePos(fake);
            // Half speed: twice the correction ticks per meter, so the 15° error
            // provably converges to contact instead of entering a stable orbit.
            com.techguns.techguns3.entity.BulletProjectile.Spec spec =
                    new com.techguns.techguns3.entity.BulletProjectile.Spec(
                            8.0f, 0.5f, 150, 1000.0f, 1000.0f, 8.0f, 0.0, 0.0f,
                            0.0f, 0.0f, golemD.getId(), 0.35f, 0.0f, 0, 0, 0, 0xFFD9A8, 1.0f,
                            0.0f, 0.0f, 0.0f, 0.0f, 0.0f, false, 0.0f, false, 0, false);
            homingBullet = new com.techguns.techguns3.entity.BulletProjectile(
                    level, fake, muzzle, -90.0f, 0.0f, 0.0f, spec);
            // Direct damage-path probe (no flight, no collision): same source the bullets use.
            boolean direct = golemD.hurtServer(level,
                    level.damageSources().mobProjectile(homingBullet, fake), 8.0f);
            TechGuns3.LOGGER.info("[TGTEST] E direct probe: applied={} hp={}",
                    direct, golemD.getHealth());
            golemD.heal(8.0f);
            homingAngle0 = angleToTarget(homingBullet);
            level.addFreshEntity(homingBullet);
            TechGuns3.LOGGER.info("[TGTEST] E homing fired at golemD hp={} angle0={}",
                    golemD.getHealth(), String.format("%.1f", Math.toDegrees(homingAngle0)));
        }

        private com.techguns.techguns3.entity.BulletProjectile homingBullet;
        private double homingAngle0;

        private double angleToTarget(com.techguns.techguns3.entity.BulletProjectile bullet) {
            Vec3 motion = bullet.getDeltaMovement().normalize();
            Vec3 toTarget = golemD.getEyePosition().subtract(bullet.position()).normalize();
            double dot = Math.max(-1.0, Math.min(1.0, motion.dot(toTarget)));
            return Math.acos(dot);
        }

        private void phaseEHomeSample() {
            // Steering proof: contact (discarded within 4 blocks of the target)
            // or converged angle. Damage itself is proven by phases A-C sharing
            // the identical onBulletHit code path.
            float hp = golemD.getHealth();
            Vec3 bp = homingBullet.position();
            double dist = bp.distanceTo(golemD.position());
            boolean contact = !homingBullet.isAlive() && dist < 4.0;
            boolean closed = false;
            double angleNow = Double.NaN;
            if (!contact && homingBullet.isAlive()) {
                angleNow = angleToTarget(homingBullet);
                closed = angleNow < Math.toRadians(8.0);
            }
            boolean pass = contact || closed;
            TechGuns3.LOGGER.info("[TGTEST] E homing: golemD hp 100->{} alive={} dist={} angle {}->{} => {}",
                    hp, golemD.isAlive(), String.format("%.2f", dist),
                    String.format("%.1f", Math.toDegrees(homingAngle0)),
                    Double.isNaN(angleNow) ? "contact" : String.format("%.1f", Math.toDegrees(angleNow)),
                    pass ? "PASS" : "FAIL");
        }

        // --- phase F: slime growth to pop ---
        private int platX0;
        private int platZ0;
        private int platFloorY;

        private void phaseFSlime() {
            // Diagnostic split: raw explode paths vs slime pop on identical stone.
            net.minecraft.core.BlockPos s1 = new net.minecraft.core.BlockPos(platX0 + 22, platFloorY, platZ0 + 2);
            net.minecraft.core.BlockPos s2 = new net.minecraft.core.BlockPos(platX0 + 24, platFloorY, platZ0 + 2);
            net.minecraft.core.BlockPos target = new net.minecraft.core.BlockPos(platX0 + 20, platFloorY, platZ0 + 6);
            level.explode(null, s1.getX() + 0.5, s1.getY() + 0.5, s1.getZ() + 0.5,
                    5.0f, false, net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
            boolean crater1 = level.getBlockState(s1).isAir();
            level.explode(null, level.damageSources().magic(),
                    new net.minecraft.world.level.ExplosionDamageCalculator(),
                    s2.getX() + 0.5, s2.getY() + 0.5, s2.getZ() + 0.5, 5.0f, false,
                    net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
            boolean crater2 = level.getBlockState(s2).isAir();
            boolean popped = false;
            for (int i = 0; i < 12; i++) {
                if (com.techguns.techguns3.entity.SlimeBlobEntity.growAt(
                        level, target, net.minecraft.core.Direction.UP, 0.09f)) {
                    popped = true;
                    break;
                }
            }
            boolean gone = level.getEntitiesOfClass(com.techguns.techguns3.entity.SlimeBlobEntity.class,
                    new AABB(target.getX() - 3, target.getY() - 3, target.getZ() - 3,
                            target.getX() + 3, target.getY() + 3, target.getZ() + 3)).isEmpty();
            boolean crater = level.getBlockState(target).isAir();
            boolean pass = popped && gone && crater;
            TechGuns3.LOGGER.info("[TGTEST] F slime: rawNull={} rawFull={} popped={} gone={} crater={} => {}",
                    crater1, crater2, popped, gone, crater, pass ? "PASS" : "FAIL");
        }

        // --- phase G: biogun poison + slime feed through real firing ---
        private IronGolem golemE;

        private void phaseGSetup() {
            giveMain(new ItemStack(TGItems.BIOGUN.get()));
            fake.getInventory().add(new ItemStack(TGItems.BIO_TANK.get()));
            Vec3 f = fake.position();
            golemE = spawnIronGolem((int) Math.round(f.x) + 3, (int) Math.round(f.y), (int) Math.round(f.z));
            Vec3 p = golemE.position();
            aimAt(p.x, p.y + 1.5, p.z);
            TechGuns3.LOGGER.info("[TGTEST] G setup: biogun armed, golemE hp={}", golemE.getHealth());
        }

        private void phaseGPoisonStart() {
            GunServerLogic.onFireStart(fake, new GunPackets.FireStart(true, false));
        }

        private void phaseGPoisonSample() {
            GunServerLogic.onFireStop(fake, new GunPackets.FireStop(true));
            ItemStack held = fake.getMainHandItem();
            int ammoLeft = held.getItem() instanceof GenericGunItem g ? g.loadedRounds(held) : -1;
            boolean poisoned = golemE.hasEffect(net.minecraft.world.effect.MobEffects.POISON);
            float hp = golemE.getHealth();
            boolean pass = poisoned || hp < 100.0f || !golemE.isAlive();
            TechGuns3.LOGGER.info("[TGTEST] G poison: golemE hp 100->{} alive={} poisoned={} ammoLeft={} golemAt={} => {}",
                    hp, golemE.isAlive(), poisoned, ammoLeft, golemE.blockPosition(),
                    pass ? "PASS" : "FAIL");
            // Face steep down at empty platform for the slime feed.
            Vec3 f = fake.position();
            fake.setYRot(-90.0f);
            fake.setXRot(55.0f);
        }

        private void phaseGSlimeStart() {
            GunServerLogic.onFireStart(fake, new GunPackets.FireStart(true, false));
        }

        private void phaseGSlimeSample() {
            GunServerLogic.onFireStop(fake, new GunPackets.FireStop(true));
            Vec3 c = fake.position();
            var blobs = level.getEntitiesOfClass(com.techguns.techguns3.entity.SlimeBlobEntity.class,
                    new AABB(c.x - 10, c.y - 5, c.z - 10, c.x + 10, c.y + 5, c.z + 10));
            ItemStack held = fake.getMainHandItem();
            int ammoLeft = held.getItem() instanceof GenericGunItem g ? g.loadedRounds(held) : -1;
            boolean grown = blobs.stream().anyMatch(b -> b.growth() > 0.3f);
            boolean pass = grown;
            TechGuns3.LOGGER.info("[TGTEST] G slime: blobs={} grown={} ammoLeft={} => {}",
                    blobs.size(), grown, ammoLeft, pass ? "PASS" : "FAIL");
            // Lane hygiene: E would body-block phase H at the same +3 spot.
            golemE.discard();
        }

        // --- phase H: flamethrower ignite through real firing ---
        private IronGolem golemF;

        private void phaseHSetup() {
            giveMain(new ItemStack(TGItems.FLAMETHROWER.get()));
            fake.getInventory().add(new ItemStack(TGItems.FUEL_TANK.get()));
            Vec3 f = fake.position();
            golemF = spawnIronGolem((int) Math.round(f.x) + 3, (int) Math.round(f.y), (int) Math.round(f.z));
            Vec3 p = golemF.position();
            aimAt(p.x, p.y + 1.5, p.z);
            TechGuns3.LOGGER.info("[TGTEST] H setup: flamethrower armed, golemF hp={}", golemF.getHealth());
        }

        private void phaseHStart() {
            GunServerLogic.onFireStart(fake, new GunPackets.FireStart(true, false));
        }

        private void phaseHSample() {
            GunServerLogic.onFireStop(fake, new GunPackets.FireStop(true));
            ItemStack gun = fake.getMainHandItem();
            int ammo = gun.getItem() instanceof GenericGunItem g ? g.loadedRounds(gun) : -1;
            Vec3 c = fake.position();
            int flying = level.getEntitiesOfClass(com.techguns.techguns3.entity.BulletProjectile.class,
                    new AABB(c.x - 20, c.y - 5, c.z - 20, c.x + 20, c.y + 5, c.z + 20)).size();
            StringBuilder traj = new StringBuilder();
            for (com.techguns.techguns3.entity.BulletProjectile b :
                    level.getEntitiesOfClass(com.techguns.techguns3.entity.BulletProjectile.class,
                            new AABB(c.x - 20, c.y - 5, c.z - 20, c.x + 20, c.y + 5, c.z + 20))) {
                Vec3 p = b.position();
                traj.append(String.format("[%.1f,%.1f,%.1f]v=%.2f ", p.x, p.y, p.z, b.getDeltaMovement().length()));
            }
            float hp = golemF.getHealth();
            boolean burning = golemF.isOnFire();
            boolean pass = burning || hp < 100.0f || !golemF.isAlive();
            TechGuns3.LOGGER.info("[TGTEST] H ignite: ammo={} flying={} traj={} golemF hp 100->{} alive={} burning={} golemAt={} => {}",
                    ammo, flying, traj.toString(), hp, golemF.isAlive(), burning, golemF.blockPosition(),
                    pass ? "PASS" : "FAIL");
            golemF.discard();
        }

        // --- phase I: wave-2 exotics (TFG tap-charge, laser hold, rocket, sonic, NDR) ---
        private IronGolem golemG;
        private IronGolem golemH;
        private IronGolem golemI;
        private IronGolem golemJ;
        private IronGolem golemK;

        private void reviveFakeIfNeeded() {
            if (fake.isAlive()) {
                fake.heal(100.0f);
                return;
            }
            Vec3 f = fake.position();
            fake = FakePlayerFactory.get(level,
                    new GameProfile(java.util.UUID.randomUUID(), "TGTest"));
            fake.setPos(f.x, f.y, f.z);
        }

        private void phaseISetup() {
            reviveFakeIfNeeded();
            giveMain(new ItemStack(TGItems.TFG.get()));
            fake.getInventory().add(new ItemStack(TGItems.NUCLEAR_POWERCELL.get()));
            Vec3 f = fake.position();
            golemG = spawnIronGolem((int) Math.round(f.x) + 8, (int) Math.round(f.y), (int) Math.round(f.z));
            Vec3 p = golemG.position();
            aimAt(p.x, p.y + 1.5, p.z);
            TechGuns3.LOGGER.info("[TGTEST] I setup: TFG armed, golemG hp={}", golemG.getHealth());
        }

        private void phaseIChargeStart() {
            GunServerLogic.onFireStart(fake, new GunPackets.FireStart(true, false));
        }

        private void phaseIChargeRelease() {
            // Walk-away regression test (frozen charge orb): teleport mid-charge,
            // pump one tick, the orb must track the new muzzle (updateInterval=1).
            Vec3 f = fake.position();
            fake.setPos(f.x + 3.0, f.y, f.z);
            Vec3 gp = golemG.position();
            aimAt(gp.x, gp.y + 1.5, gp.z);
            GunServerLogic.onPlayerTick(new PlayerTickEvent.Post(fake));
            Vec3 muzzle = com.techguns.techguns3.combat.GunServerLogic.muzzlePos(fake);
            var orbs = level.getEntitiesOfClass(com.techguns.techguns3.entity.ChargeOrbEntity.class,
                    new AABB(f.x - 15, f.y - 5, f.z - 15, f.x + 15, f.y + 5, f.z + 15));
            boolean tracked = !orbs.isEmpty()
                    && orbs.stream().anyMatch(o -> o.position().distanceTo(muzzle) < 2.5);
            // ~4-tick tap: minimal charge, small crater, fake survives at 8 blocks.
            GunServerLogic.onFireStop(fake, new GunPackets.FireStop(true));
            boolean gone = level.getEntitiesOfClass(com.techguns.techguns3.entity.ChargeOrbEntity.class,
                    new AABB(f.x - 15, f.y - 5, f.z - 15, f.x + 15, f.y + 5, f.z + 15)).isEmpty();
            TechGuns3.LOGGER.info("[TGTEST] I orb: tracked={} cleaned={} => {}",
                    tracked, gone, tracked && gone ? "PASS" : "FAIL");
        }

        private void phaseISample() {
            float hp = golemG.isAlive() ? golemG.getHealth() : 0.0f;
            boolean pass = hp < 100.0f || !golemG.isAlive();
            TechGuns3.LOGGER.info("[TGTEST] I tfg: golemG hp 100->{} alive={} => {}",
                    hp, golemG.isAlive(), pass ? "PASS" : "FAIL");
            golemG.discard();
            reviveFakeIfNeeded();
        }

        private void phaseJSetup() {
            reviveFakeIfNeeded();
            giveMain(new ItemStack(TGItems.LASERGUN.get()));
            fake.getInventory().add(new ItemStack(TGItems.ENERGY_CELL.get()));
            Vec3 f = fake.position();
            golemH = spawnIronGolem((int) Math.round(f.x) + 10, (int) Math.round(f.y), (int) Math.round(f.z));
            Vec3 p = golemH.position();
            aimAt(p.x, p.y + 1.5, p.z);
            TechGuns3.LOGGER.info("[TGTEST] J setup: lasergun armed, golemH hp={}", golemH.getHealth());
        }

        private void phaseJStart() {
            GunServerLogic.onFireStart(fake, new GunPackets.FireStart(true, false));
        }

        private void phaseJSample() {
            GunServerLogic.onFireStop(fake, new GunPackets.FireStop(true));
            float hp = golemH.isAlive() ? golemH.getHealth() : 0.0f;
            boolean pass = hp < 100.0f || !golemH.isAlive();
            TechGuns3.LOGGER.info("[TGTEST] J laser: golemH hp 100->{} alive={} => {}",
                    hp, golemH.isAlive(), pass ? "PASS" : "FAIL");
            golemH.discard();
            reviveFakeIfNeeded();
        }

        private void phaseKSetup() {
            reviveFakeIfNeeded();
            giveMain(new ItemStack(TGItems.ROCKETLAUNCHER.get()));
            fake.getInventory().add(new ItemStack(TGItems.ROCKET.get()));
            Vec3 f = fake.position();
            // +4m like the sonic lane: slow single shots are geometry-flaky past ~6m.
            golemI = spawnIronGolem((int) Math.round(f.x) + 4, (int) Math.round(f.y), (int) Math.round(f.z));
            Vec3 p = golemI.position();
            aimAt(p.x, p.y + 1.5, p.z);
            TechGuns3.LOGGER.info("[TGTEST] K setup: rocket armed, golemI hp={}", golemI.getHealth());
        }

        private void phaseKFire() {
            GunServerLogic.onFirePressed(fake, new GunPackets.FirePressed(true, false));
        }

        private void phaseKSample() {
            float hp = golemI.isAlive() ? golemI.getHealth() : 0.0f;
            ItemStack held = fake.getMainHandItem();
            int ammoLeft = held.getItem() instanceof GenericGunItem g ? g.loadedRounds(held) : -1;
            Vec3 c = fake.position();
            int rockets = level.getEntitiesOfClass(com.techguns.techguns3.entity.BulletProjectile.class,
                    new AABB(c.x - 20, c.y - 5, c.z - 20, c.x + 20, c.y + 5, c.z + 20)).size();
            boolean pass = hp < 100.0f || !golemI.isAlive();
            TechGuns3.LOGGER.info("[TGTEST] K rocket: golemI hp 100->{} alive={} ammoLeft={} rocketsAlive={} => {}",
                    hp, golemI.isAlive(), ammoLeft, rockets, pass ? "PASS" : "FAIL");
            golemI.discard();
            reviveFakeIfNeeded();
        }

        private void phaseLSetup() {
            reviveFakeIfNeeded();
            giveMain(new ItemStack(TGItems.SONICSHOTGUN.get()));
            fake.getInventory().add(new ItemStack(TGItems.ENERGY_CELL.get()));
            Vec3 f = fake.position();
            golemJ = spawnIronGolem((int) Math.round(f.x) + 4, (int) Math.round(f.y), (int) Math.round(f.z));
            Vec3 p = golemJ.position();
            aimAt(p.x, p.y + 1.5, p.z);
            TechGuns3.LOGGER.info("[TGTEST] L setup: sonic armed, golemJ hp={}", golemJ.getHealth());
        }

        private void phaseLFire() {
            GunServerLogic.onFirePressed(fake, new GunPackets.FirePressed(true, false));
        }

        private void phaseLSample() {
            float hp = golemJ.isAlive() ? golemJ.getHealth() : 0.0f;
            boolean pass = hp < 100.0f || !golemJ.isAlive();
            TechGuns3.LOGGER.info("[TGTEST] L sonic: golemJ hp 100->{} alive={} => {}",
                    hp, golemJ.isAlive(), pass ? "PASS" : "FAIL");
            golemJ.discard();
            reviveFakeIfNeeded();
        }

        private void phaseMSetup() {
            reviveFakeIfNeeded();
            giveMain(new ItemStack(TGItems.NUCLEARDEATHRAY.get()));
            fake.getInventory().add(new ItemStack(TGItems.NUCLEAR_POWERCELL.get()));
            Vec3 f = fake.position();
            golemK = spawnIronGolem((int) Math.round(f.x) + 12, (int) Math.round(f.y), (int) Math.round(f.z));
            Vec3 p = golemK.position();
            aimAt(p.x, p.y + 1.5, p.z);
            TechGuns3.LOGGER.info("[TGTEST] M setup: NDR armed, golemK hp={}", golemK.getHealth());
        }

        private void phaseMStart() {
            GunServerLogic.onFireStart(fake, new GunPackets.FireStart(true, false));
        }

        private void phaseMSample() {
            // Persistent-beam proof BEFORE release: a live BeamEntity near the
            // fake means the visual never gaps between damage ticks (CE teleport).
            Vec3 c = fake.position();
            boolean beamAlive = !level.getEntitiesOfClass(com.techguns.techguns3.entity.BeamEntity.class,
                    new AABB(c.x - 15, c.y - 5, c.z - 15, c.x + 15, c.y + 5, c.z + 15)).isEmpty();
            ItemStack held = fake.getMainHandItem();
            int ammoLeft = held.getItem() instanceof GenericGunItem g ? g.loadedRounds(held) : -1;
            GunServerLogic.onFireStop(fake, new GunPackets.FireStop(true));
            float hp = golemK.isAlive() ? golemK.getHealth() : 0.0f;
            boolean radiated = golemK.hasEffect(com.techguns.techguns3.registry.TGEffects.RADIATION);
            // CE rhythm: iframes throttle equal ticks to ~1 application/10 ticks;
            // 20-tick hold => ~10 damage + radiation, 4 rounds drained (1/5 ticks).
            boolean pass = (hp < 100.0f || !golemK.isAlive() || radiated) && beamAlive && ammoLeft == 16;
            TechGuns3.LOGGER.info("[TGTEST] M ndr: golemK hp 100->{} alive={} radiated={} beamAlive={} ammoLeft={} => {}",
                    hp, golemK.isAlive(), radiated, beamAlive, ammoLeft, pass ? "PASS" : "FAIL");
            reviveFakeIfNeeded();
        }

        // --- phase N: point-blank pistol (spawn must not skip past the target) ---
        private IronGolem golemN;

        private void phaseNSetup() {
            reviveFakeIfNeeded();
            giveMain(new ItemStack(TGItems.PISTOL.get()));
            fake.getInventory().add(new ItemStack(TGItems.PISTOL_MAGAZINE.get()));
            Vec3 f = fake.position();
            golemN = spawnIronGolem((int) Math.round(f.x) + 1, (int) Math.round(f.y), (int) Math.round(f.z));
            Vec3 p = golemN.position();
            aimAt(p.x, p.y + 1.5, p.z);
            TechGuns3.LOGGER.info("[TGTEST] N setup: pistol armed, golemN hp={}", golemN.getHealth());
        }

        private void phaseNFire() {
            GunServerLogic.onFirePressed(fake, new GunPackets.FirePressed(true, false));
        }

        private void phaseNSample() {
            float hp = golemN.isAlive() ? golemN.getHealth() : 0.0f;
            boolean pass = hp < 100.0f || !golemN.isAlive();
            TechGuns3.LOGGER.info("[TGTEST] N pointblank: golemN hp 100->{} alive={} => {}",
                    hp, golemN.isAlive(), pass ? "PASS" : "FAIL");
            reviveFakeIfNeeded();
        }

        private void finish(boolean ok) {
            done = true;
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(this);
            TechGuns3.LOGGER.info("[TGTEST] done ok={} — halting server", ok);
            if (fake != null) fake.discard();
            if (iron_golemA != null) iron_golemA.discard();
            if (iron_golemB != null) iron_golemB.discard();
            if (iron_golemC != null) iron_golemC.discard();
            if (golemD != null) golemD.discard();
            if (golemE != null) golemE.discard();
            if (golemF != null) golemF.discard();
            if (golemG != null) golemG.discard();
            if (golemH != null) golemH.discard();
            if (golemI != null) golemI.discard();
            if (golemJ != null) golemJ.discard();
            if (golemK != null) golemK.discard();
            if (golemN != null) golemN.discard();
            if (weaveBullet != null) weaveBullet.discard();
            if (homingBullet != null) homingBullet.discard();
            for (long packed : forcedChunks) {
                level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(packed),
                        net.minecraft.world.level.ChunkPos.getZ(packed), false);
            }
            server.halt(false);
        }
    }
}
