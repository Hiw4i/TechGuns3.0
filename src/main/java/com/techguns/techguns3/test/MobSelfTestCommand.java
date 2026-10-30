package com.techguns.techguns3.test;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.techguns.techguns3.TechGuns3;
import com.techguns.techguns3.item.GenericGunItem;
import com.techguns.techguns3.mob.MobProfile;
import com.techguns.techguns3.mob.MobRegistry;
import com.techguns.techguns3.mob.TGArmedMob;
import com.techguns.techguns3.registry.TGEntities;
import com.techguns.techguns3.registry.TGItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Headless armed-NPC diagnostics: {@code /tgtest mobs} (perm 2 / console), or
 * env {@code TECHGUNS3_SELFTEST=mobs} right after boot.
 *
 * <p>Drives the real server AI on a flat stone stage: profile application and
 * spawn equipment, aimed fire at a live target (lead, burst, reload), TNT
 * wall-breach, wall mining, weapon scavenging, gap bridging and gunfire
 * hearing — each phase logs PASS/FAIL and the run halts the server.</p>
 */
@EventBusSubscriber(modid = TechGuns3.MODID)
public final class MobSelfTestCommand {
    private MobSelfTestCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tgtest")
                .requires(src -> src.permissions().hasPermission(
                        net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("mobs").executes(ctx -> runMobs(ctx.getSource()))));
    }

    private static int runMobs(CommandSourceStack src) {
        runHeadless(src.getServer());
        src.sendSuccess(() -> net.minecraft.network.chat.Component.literal(
                "[TGTEST] mobs started, see log"), true);
        return Command.SINGLE_SUCCESS;
    }

    public static void runHeadless(MinecraftServer server) {
        NeoForgeBus.run(server);
    }

    private static final class NeoForgeBus {
        private final MinecraftServer server;
        private ServerLevel level;
        private final List<Long> forcedChunks = new ArrayList<>();
        private final List<Entity> cleanup = new ArrayList<>();
        private int tick;
        private boolean done;
        private boolean allOk = true;
        private int failedCount;

        // Stage geometry (computed in setup).
        private int x0;
        private int z0;
        private int floorY;

        // Actors.
        private TGArmedMob soldier;
        private TGArmedMob commando;
        private TGArmedMob miner;
        private TGArmedMob policeman;
        private TGArmedMob farmerMiner;
        private IronGolem golemShoot;
        private IronGolem golemWall;
        private IronGolem golemGap;
        private net.neoforged.neoforge.common.util.FakePlayer noisyPlayer;
        private BlockPos wallCenter;
        private BlockPos trenchMin;
        private BlockPos trenchMax;

        static void run(MinecraftServer server) {
            NeoForgeBus test = new NeoForgeBus(server);
            NeoForge.EVENT_BUS.register(test);
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
                TechGuns3.LOGGER.error("[TGTEST-MOBS] exception at tick {}: {}", tick, e.toString(), e);
                fail("exception " + e);
                finish();
            }
        }

        private void pass(String what) {
            TechGuns3.LOGGER.info("[TGTEST-MOBS] PASS {} (tick {})", what, tick);
        }

        private void fail(String what) {
            failedCount++;
            allOk = false;
            TechGuns3.LOGGER.info("[TGTEST-MOBS] FAIL {} (tick {})", what, tick);
        }

        private void check(boolean ok, String what) {
            if (ok) pass(what); else fail(what);
        }

        // --- stage -----------------------------------------------------------

        private void buildPlatform() {
            BlockPos spawn = level.getLevelData().getRespawnData().pos();
            x0 = spawn.getX() - 8;
            z0 = spawn.getZ() - 8;
            int top = 0;
            for (int dx = 0; dx < 40; dx++) {
                for (int dz = 0; dz < 40; dz++) {
                    top = Math.max(top, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x0 + dx, z0 + dz));
                }
            }
            floorY = Math.min(top + 2, level.getMaxY() - 8);
            for (int dx = 0; dx < 40; dx++) {
                for (int dz = 0; dz < 40; dz++) {
                    level.setBlock(new BlockPos(x0 + dx, floorY, z0 + dz), Blocks.STONE.defaultBlockState(), 3);
                    for (int dy = 1; dy <= 8; dy++) {
                        level.setBlock(new BlockPos(x0 + dx, floorY + dy, z0 + dz), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
            for (Entity e : level.getEntitiesOfClass(Entity.class,
                    new AABB(x0 - 10, floorY - 10, z0 - 10, x0 + 50, floorY + 20, z0 + 50),
                    e -> !(e instanceof net.minecraft.world.entity.player.Player))) {
                e.discard();
            }
            for (int dx = 0; dx < 40; dx += 8) {
                for (int dz = 0; dz < 40; dz += 8) {
                    int cx = (x0 + dx) >> 4;
                    int cz = (z0 + dz) >> 4;
                    level.setChunkForced(cx, cz, true);
                    forcedChunks.add(net.minecraft.world.level.ChunkPos.pack(cx, cz));
                }
            }
        }

        private void clearStageAboveFloor() {
            // Deterministic sweep: every actor this test spawned explicitly,
            // then a padded area scan — TNT knockback flings mobs far past the
            // stage, and a leftover armed NPC would race later phases for loot.
            for (Entity e : cleanup) {
                e.discard();
            }
            for (Entity e : level.getEntitiesOfClass(Entity.class,
                    new AABB(x0 - 60, floorY - 40, z0 - 60, x0 + 100, floorY + 60, z0 + 100))) {
                if (e instanceof net.minecraft.world.entity.player.Player) continue;
                e.discard();
            }
            for (int dx = 0; dx < 40; dx++) {
                for (int dz = 0; dz < 40; dz++) {
                    for (int dy = 1; dy <= 8; dy++) {
                        level.setBlock(new BlockPos(x0 + dx, floorY + dy, z0 + dz), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }

        private TGArmedMob spawnMob(String id, int x, int z) {
            var type = (net.minecraft.world.entity.EntityType<TGArmedMob>) (Object) net.minecraft.core.registries.BuiltInRegistries
                    .ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath(
                            TechGuns3.MODID, id));
            TGArmedMob mob = type.spawn(level, new BlockPos(x, floorY + 1, z), EntitySpawnReason.COMMAND);
            if (mob == null) throw new IllegalStateException(id + " did not spawn");
            mob.setPersistenceRequired();
            mob.setPos(x + 0.5, floorY + 1, z + 0.5);
            cleanup.add(mob);
            return mob;
        }

        private IronGolem spawnGolem(int x, int z) {
            var type = (net.minecraft.world.entity.EntityType<IronGolem>) (Object) net.minecraft.core.registries.BuiltInRegistries
                    .ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath(
                            "minecraft", "iron_golem"));
            IronGolem golem = type.spawn(level, new BlockPos(x, floorY + 1, z), EntitySpawnReason.COMMAND);
            if (golem == null) throw new IllegalStateException("iron_golem did not spawn");
            golem.setNoAi(true);
            golem.setPos(x + 0.5, floorY + 1, z + 0.5);
            golem.setPersistenceRequired();
            cleanup.add(golem);
            return golem;
        }

        // --- phases ------------------------------------------------------------

        private void phase() {
            switch (tick) {
                case 1 -> setup();
                case 20 -> phaseProfile();
                case 21 -> phaseFireSetup();
                case 200 -> phaseFireSample();
                case 201 -> phaseWallSetup();
                case 500 -> phaseWallSample();
                case 501 -> phaseMineSetup();
                case 900 -> phaseMineSample();
                case 901 -> phasePickupSetup();
                case 1200 -> phasePickupSample();
                case 1201 -> phaseBridgeSetup();
                case 1600 -> phaseBridgeSample();
                case 1601 -> phaseHearingSetup();
                case 1620 -> phaseHearingShot();
                case 1760 -> phaseHearingSample();
                case 1761 -> finish();
                default -> { }
            }
        }

        private void setup() {
            level = server.overworld();
            server.setDifficulty(Difficulty.NORMAL, true);
            // Keep the tick loop alive: the default 60s empty-server pause would
            // freeze the later phases of this ~90s run.
            if (server instanceof net.minecraft.server.dedicated.DedicatedServer dedicated) {
                dedicated.setPauseWhenEmptySeconds(0);
            }
            buildPlatform();
            TechGuns3.LOGGER.info("[TGTEST-MOBS] stage ready, floor={} profiles={}",
                    floorY, MobRegistry.snapshot().size());
        }

        /** Phase 1: profile data applied to a fresh spawn (stats + weapon). */
        private void phaseProfile() {
            check(MobRegistry.isLoaded(), "mob registry loaded (" + MobRegistry.snapshot().size() + " profiles)");
            soldier = spawnMob("army_soldier", x0 + 4, z0 + 4);
            // The profile picks a random service weapon per spawn; pin the M4
            // so the ammo/damage asserts are deterministic run to run.
            soldier.equipGun(TGItems.M4.get());
            MobProfile profile = soldier.profile();
            check(profile != null, "army_soldier profile resolved");
            if (profile != null) {
                check(soldier.profileApplied(), "profile attributes applied");
                check(soldier.getMaxHealth() == profile.stats().maxHealth(),
                        "max health " + soldier.getMaxHealth() + " == profile " + profile.stats().maxHealth());
            }
            check(soldier.getMainHandItem().getItem() instanceof GenericGunItem,
                    "spawn weapon: " + soldier.getMainHandItem());
            check(soldier.loadedRounds() > 0, "spawn magazine loaded: " + soldier.loadedRounds());
        }

        /** Phase 2: real aimed fire at a live target through the goal AI. */
        private void phaseFireSetup() {
            golemShoot = spawnGolem(x0 + 4, z0 + 12);
            soldier.setTarget(golemShoot);
            TechGuns3.LOGGER.info("[TGTEST-MOBS] fire: soldier {} -> golem {} (dist {})",
                    soldier.blockPosition(), golemShoot.blockPosition(),
                    (int) soldier.distanceTo(golemShoot));
        }

        private void phaseFireSample() {
            float hp = golemShoot.isAlive() ? golemShoot.getHealth() : 0.0f;
            boolean hit = hp < golemShoot.getMaxHealth() || !golemShoot.isAlive();
            boolean spent = soldier.loadedRounds() < ((GenericGunItem)
                    soldier.getMainHandItem().getItem()).def().magazine()
                    || soldier.memory().burstShots > 0 || soldier.memory().reloadTicks > 0
                    || soldier.spareMagazines() < 3;
            check(spent, "soldier fired (ammo spent)");
            check(hit, "target damaged by AI fire (hp " + hp + ")");
            soldier.setTarget(null);
            golemShoot.discard();
        }

        /** Phase 3: commando breaches a wall with TNT. */
        private void phaseWallSetup() {
            clearStageAboveFloor();
            commando = spawnMob("commando", x0 + 6, z0 + 4);
            golemWall = spawnGolem(x0 + 6, z0 + 16);
            // Solid cobble wall across the whole stage: the only way to the
            // target is breaching, not walking around the ends.
            wallCenter = new BlockPos(x0 + 6, floorY + 1, z0 + 10);
            for (int dx = -8; dx <= 48; dx++) {
                for (int dy = 0; dy < 3; dy++) {
                    level.setBlock(wallCenter.offset(dx, dy, 0), Blocks.COBBLESTONE.defaultBlockState(), 3);
                }
            }
            commando.setTarget(golemWall);
            TechGuns3.LOGGER.info("[TGTEST-MOBS] wall breach: commando {} vs golem {} wall {}",
                    commando.blockPosition(), golemWall.blockPosition(), wallCenter);
        }

        private void phaseWallSample() {
            boolean tntSeen = !level.getEntitiesOfClass(PrimedTnt.class,
                    new AABB(x0, floorY, z0, x0 + 40, floorY + 8, z0 + 40)).isEmpty();
            boolean hole = level.getBlockState(wallCenter).isAir();
            check(commando.dynamiteLeft() < 2 || tntSeen || hole,
                    "dynamite used (left=" + commando.dynamiteLeft() + " tntSeen=" + tntSeen + " hole=" + hole + ")");
            commando.setTarget(null);
        }

        /** Phase 4: zombie miner digs through the wall. */
        private void phaseMineSetup() {
            clearStageAboveFloor();
            // Rebuild the wall (phase 3's TNT may have opened it).
            for (int dx = -8; dx <= 48; dx++) {
                for (int dy = 0; dy < 3; dy++) {
                    level.setBlock(wallCenter.offset(dx, dy, 0), Blocks.COBBLESTONE.defaultBlockState(), 3);
                }
            }
            miner = spawnMob("zombie_miner", x0 + 6, z0 + 4);
            golemWall = spawnGolem(x0 + 6, z0 + 16);
            miner.setTarget(golemWall);
            TechGuns3.LOGGER.info("[TGTEST-MOBS] mine: miner {} vs golem {} behind wall",
                    miner.blockPosition(), golemWall.blockPosition());
        }

        private void phaseMineSample() {
            // The digger aims at its eye-to-eye line, which is not necessarily
            // the wall's center column: accept a hole anywhere near the center.
            boolean hole = false;
            for (int dx = -2; dx <= 2 && !hole; dx++) {
                for (int dy = -1; dy <= 3 && !hole; dy++) {
                    hole = level.getBlockState(wallCenter.offset(dx, dy, 0)).isAir();
                }
            }
            check(hole, "wall mined through (center=" + level.getBlockState(wallCenter) + ")");
            check(miner.buildMaterials() > 32,
                    "mined block stocked as materials: " + miner.buildMaterials() + " (spawn stock 32)");
            miner.setTarget(null);
            golemWall.discard();
        }

        /** Phase 5: an unarmed policeman scavenges a dropped minigun. */
        private void phasePickupSetup() {
            clearStageAboveFloor();
            policeman = spawnMob("zombie_policeman", x0 + 6, z0 + 4);
            policeman.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            ItemEntity dropped = new ItemEntity(level, x0 + 6.5, floorY + 1, z0 + 8.5,
                    new ItemStack(TGItems.MINIGUN.get()));
            level.addFreshEntity(dropped);
            cleanup.add(dropped);
            TechGuns3.LOGGER.info("[TGTEST-MOBS] pickup: unarmed policeman at {} minigun at {}",
                    policeman.blockPosition(), dropped.blockPosition());
        }

        private void phasePickupSample() {
            check(policeman.getMainHandItem().getItem() == TGItems.MINIGUN.get(),
                    "picked up minigun: " + policeman.getMainHandItem());
        }

        /** Phase 6: zombie miner bridges a 4-wide trench toward the target. */
        private void phaseBridgeSetup() {
            clearStageAboveFloor();
            farmerMiner = spawnMob("zombie_miner", x0 + 6, z0 + 2);
            golemGap = spawnGolem(x0 + 6, z0 + 20);
            // Trench: 4 wide, 3 deep, spanning the whole stage so the only
            // way across is bridging (no walking around the ends).
            trenchMin = new BlockPos(x0 - 1, floorY - 3, z0 + 8);
            trenchMax = new BlockPos(x0 + 41, floorY, z0 + 12);
            for (int x = trenchMin.getX(); x <= trenchMax.getX(); x++) {
                for (int z = z0 + 8; z <= z0 + 11; z++) {
                    for (int dy = 0; dy <= 3; dy++) {
                        level.setBlock(new BlockPos(x, floorY - dy, z), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
            farmerMiner.setTarget(golemGap);
            TechGuns3.LOGGER.info("[TGTEST-MOBS] bridge: miner {} target {} materials {}",
                    farmerMiner.blockPosition(), golemGap.blockPosition(), farmerMiner.buildMaterials());
        }

        private void phaseBridgeSample() {
            int placed = 0;
            for (int x = x0; x <= x0 + 39; x++) {
                for (int z = z0 + 8; z <= z0 + 11; z++) {
                    if (!level.getBlockState(new BlockPos(x, floorY, z)).isAir()) placed++;
                }
            }
            check(placed > 0 || farmerMiner.blockPosition().getZ() > z0 + 7,
                    "bridged trench (placed=" + placed + " minerZ=" + farmerMiner.blockPosition().getZ() + ")");
            farmerMiner.setTarget(null);
            golemGap.discard();
        }

        /** Phase 7: gunfire through a wall makes the mob hunt the shooter. */
        private void phaseHearingSetup() {
            clearStageAboveFloor();
            soldier = spawnMob("army_soldier", x0 + 6, z0 + 4);
            // Wall between soldier and the shooter.
            for (int dx = -4; dx <= 4; dx++) {
                for (int dy = 0; dy < 3; dy++) {
                    level.setBlock(new BlockPos(x0 + 6 + dx, floorY + 1 + dy, z0 + 10),
                            Blocks.COBBLESTONE.defaultBlockState(), 3);
                }
            }
            noisyPlayer = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "TGNoise"));
            // Fake players spawn invulnerable; an invulnerable shooter is not
            // a valid attack target, which would defeat the hearing assert.
            noisyPlayer.setPermanentlyInvulnerable(false);
            noisyPlayer.setPos(x0 + 6.5, floorY + 1, z0 + 16.5);
            cleanup.add(noisyPlayer);
            TechGuns3.LOGGER.info("[TGTEST-MOBS] hearing: soldier {} shooter behind wall at {}",
                    soldier.blockPosition(), noisyPlayer.blockPosition());
        }

        private void phaseHearingShot() {
            ItemStack gun = new ItemStack(TGItems.PISTOL.get());
            noisyPlayer.setItemSlot(EquipmentSlot.MAINHAND, gun);
            com.techguns.techguns3.combat.BallisticCombat.fire(level, noisyPlayer,
                    TGItems.PISTOL.get().stats(),
                    com.techguns.techguns3.combat.GunServerLogic.muzzlePos(noisyPlayer),
                    0.0f, 0.0f, noisyPlayer.getSoundSource());
            TechGuns3.LOGGER.info("[TGTEST-MOBS] shooter fired at {}", noisyPlayer.blockPosition());
        }

        private void phaseHearingSample() {
            check(soldier.getTarget() == noisyPlayer,
                    "heard gunfire through wall -> target=" + soldier.getTarget());
        }

        private void finish() {
            done = true;
            NeoForge.EVENT_BUS.unregister(this);
            TechGuns3.LOGGER.info("[TGTEST-MOBS] done ok={} failures={} — halting server", allOk, failedCount);
            for (Entity e : cleanup) {
                e.discard();
            }
            // Sweep any leftovers (primed TNT included) before unpinning chunks.
            for (Entity e : level.getEntitiesOfClass(Entity.class,
                    new AABB(x0 - 10, floorY - 10, z0 - 10, x0 + 50, floorY + 20, z0 + 50),
                    e -> !(e instanceof net.minecraft.world.entity.player.Player))) {
                e.discard();
            }
            for (long packed : forcedChunks) {
                level.setChunkForced(net.minecraft.world.level.ChunkPos.getX(packed),
                        net.minecraft.world.level.ChunkPos.getZ(packed), false);
            }
            server.halt(false);
        }
    }
}
