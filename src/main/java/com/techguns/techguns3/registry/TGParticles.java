package com.techguns.techguns3.registry;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.techguns.techguns3.TechGuns3;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Custom particles (Lodestone-style builder, own code, zero new dependencies).
 *
 * <p>Three quad types sharing one curve behavior: {@code glow} (fullbright
 * spark dot), {@code puff} (soft shaded smoke) and {@code flame} (fullbright
 * fire blob with a teardrop sprite). All carry start/end color, start/end
 * size, life, gravity and drag, so effects stay data per spawn call instead
 * of hardcoded classes.</p>
 *
 * <p>{@code blood} is the gore counterpart of {@code glow}: same dot sprite,
 * but shaded (not fullbright) so arterial spray reads as liquid, darkening
 * as it flies, instead of glowing sparks. Flesh chunks reuse {@code puff}.</p>
 */
public final class TGParticles {
    private TGParticles() {}

    public static final DeferredRegister<ParticleType<?>> TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, TechGuns3.MODID);

    public static final DeferredHolder<ParticleType<?>, ParticleType<GlowOptions>> GLOW =
            TYPES.register("glow", GlowType::new);
    public static final DeferredHolder<ParticleType<?>, ParticleType<PuffOptions>> PUFF =
            TYPES.register("puff", PuffType::new);
    public static final DeferredHolder<ParticleType<?>, ParticleType<FlameOptions>> FLAME =
            TYPES.register("flame", FlameType::new);
    public static final DeferredHolder<ParticleType<?>, ParticleType<BloodOptions>> BLOOD =
            TYPES.register("blood", BloodType::new);

    public record GlowOptions(int color, int endColor, float size0, float size1,
                              int life, float gravity, float drag) implements ParticleOptions {
        public static final MapCodec<GlowOptions> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                ExtraCodecs.RGB_COLOR_CODEC.fieldOf("color").forGetter(GlowOptions::color),
                ExtraCodecs.RGB_COLOR_CODEC.fieldOf("end_color").forGetter(GlowOptions::endColor),
                com.mojang.serialization.Codec.FLOAT.fieldOf("size0").forGetter(GlowOptions::size0),
                com.mojang.serialization.Codec.FLOAT.fieldOf("size1").forGetter(GlowOptions::size1),
                ExtraCodecs.POSITIVE_INT.fieldOf("life").forGetter(GlowOptions::life),
                com.mojang.serialization.Codec.FLOAT.fieldOf("gravity").forGetter(GlowOptions::gravity),
                com.mojang.serialization.Codec.FLOAT.fieldOf("drag").forGetter(GlowOptions::drag)
        ).apply(inst, GlowOptions::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, GlowOptions> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.INT, GlowOptions::color,
                        ByteBufCodecs.INT, GlowOptions::endColor,
                        ByteBufCodecs.FLOAT, GlowOptions::size0,
                        ByteBufCodecs.FLOAT, GlowOptions::size1,
                        ByteBufCodecs.INT, GlowOptions::life,
                        ByteBufCodecs.FLOAT, GlowOptions::gravity,
                        ByteBufCodecs.FLOAT, GlowOptions::drag,
                        GlowOptions::new);

        @Override
        public ParticleType<?> getType() {
            return GLOW.get();
        }
    }

    public record PuffOptions(int color, int endColor, float size0, float size1,
                              int life, float gravity, float drag) implements ParticleOptions {
        public static final MapCodec<PuffOptions> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                ExtraCodecs.RGB_COLOR_CODEC.fieldOf("color").forGetter(PuffOptions::color),
                ExtraCodecs.RGB_COLOR_CODEC.fieldOf("end_color").forGetter(PuffOptions::endColor),
                com.mojang.serialization.Codec.FLOAT.fieldOf("size0").forGetter(PuffOptions::size0),
                com.mojang.serialization.Codec.FLOAT.fieldOf("size1").forGetter(PuffOptions::size1),
                ExtraCodecs.POSITIVE_INT.fieldOf("life").forGetter(PuffOptions::life),
                com.mojang.serialization.Codec.FLOAT.fieldOf("gravity").forGetter(PuffOptions::gravity),
                com.mojang.serialization.Codec.FLOAT.fieldOf("drag").forGetter(PuffOptions::drag)
        ).apply(inst, PuffOptions::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, PuffOptions> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.INT, PuffOptions::color,
                        ByteBufCodecs.INT, PuffOptions::endColor,
                        ByteBufCodecs.FLOAT, PuffOptions::size0,
                        ByteBufCodecs.FLOAT, PuffOptions::size1,
                        ByteBufCodecs.INT, PuffOptions::life,
                        ByteBufCodecs.FLOAT, PuffOptions::gravity,
                        ByteBufCodecs.FLOAT, PuffOptions::drag,
                        PuffOptions::new);

        @Override
        public ParticleType<?> getType() {
            return PUFF.get();
        }
    }

    public record FlameOptions(int color, int endColor, float size0, float size1,
                               int life, float gravity, float drag) implements ParticleOptions {
        public static final MapCodec<FlameOptions> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                ExtraCodecs.RGB_COLOR_CODEC.fieldOf("color").forGetter(FlameOptions::color),
                ExtraCodecs.RGB_COLOR_CODEC.fieldOf("end_color").forGetter(FlameOptions::endColor),
                com.mojang.serialization.Codec.FLOAT.fieldOf("size0").forGetter(FlameOptions::size0),
                com.mojang.serialization.Codec.FLOAT.fieldOf("size1").forGetter(FlameOptions::size1),
                ExtraCodecs.POSITIVE_INT.fieldOf("life").forGetter(FlameOptions::life),
                com.mojang.serialization.Codec.FLOAT.fieldOf("gravity").forGetter(FlameOptions::gravity),
                com.mojang.serialization.Codec.FLOAT.fieldOf("drag").forGetter(FlameOptions::drag)
        ).apply(inst, FlameOptions::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, FlameOptions> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.INT, FlameOptions::color,
                        ByteBufCodecs.INT, FlameOptions::endColor,
                        ByteBufCodecs.FLOAT, FlameOptions::size0,
                        ByteBufCodecs.FLOAT, FlameOptions::size1,
                        ByteBufCodecs.INT, FlameOptions::life,
                        ByteBufCodecs.FLOAT, FlameOptions::gravity,
                        ByteBufCodecs.FLOAT, FlameOptions::drag,
                        FlameOptions::new);

        @Override
        public ParticleType<?> getType() {
            return FLAME.get();
        }
    }

    private static final class GlowType extends ParticleType<GlowOptions> {
        GlowType() {
            super(false);
        }

        @Override
        public MapCodec<GlowOptions> codec() {
            return GlowOptions.CODEC;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, GlowOptions> streamCodec() {
            return GlowOptions.STREAM_CODEC;
        }
    }

    private static final class FlameType extends ParticleType<FlameOptions> {
        FlameType() {
            super(false);
        }

        @Override
        public MapCodec<FlameOptions> codec() {
            return FlameOptions.CODEC;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, FlameOptions> streamCodec() {
            return FlameOptions.STREAM_CODEC;
        }
    }

    private static final class PuffType extends ParticleType<PuffOptions> {
        PuffType() {
            super(false);
        }

        @Override
        public MapCodec<PuffOptions> codec() {
            return PuffOptions.CODEC;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, PuffOptions> streamCodec() {
            return PuffOptions.STREAM_CODEC;
        }
    }

    public record BloodOptions(int color, int endColor, float size0, float size1,
                               int life, float gravity, float drag) implements ParticleOptions {
        public static final MapCodec<BloodOptions> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                ExtraCodecs.RGB_COLOR_CODEC.fieldOf("color").forGetter(BloodOptions::color),
                ExtraCodecs.RGB_COLOR_CODEC.fieldOf("end_color").forGetter(BloodOptions::endColor),
                com.mojang.serialization.Codec.FLOAT.fieldOf("size0").forGetter(BloodOptions::size0),
                com.mojang.serialization.Codec.FLOAT.fieldOf("size1").forGetter(BloodOptions::size1),
                ExtraCodecs.POSITIVE_INT.fieldOf("life").forGetter(BloodOptions::life),
                com.mojang.serialization.Codec.FLOAT.fieldOf("gravity").forGetter(BloodOptions::gravity),
                com.mojang.serialization.Codec.FLOAT.fieldOf("drag").forGetter(BloodOptions::drag)
        ).apply(inst, BloodOptions::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, BloodOptions> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.INT, BloodOptions::color,
                        ByteBufCodecs.INT, BloodOptions::endColor,
                        ByteBufCodecs.FLOAT, BloodOptions::size0,
                        ByteBufCodecs.FLOAT, BloodOptions::size1,
                        ByteBufCodecs.INT, BloodOptions::life,
                        ByteBufCodecs.FLOAT, BloodOptions::gravity,
                        ByteBufCodecs.FLOAT, BloodOptions::drag,
                        BloodOptions::new);

        @Override
        public ParticleType<?> getType() {
            return BLOOD.get();
        }
    }

    private static final class BloodType extends ParticleType<BloodOptions> {
        BloodType() {
            super(false);
        }

        @Override
        public MapCodec<BloodOptions> codec() {
            return BloodOptions.CODEC;
        }

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, BloodOptions> streamCodec() {
            return BloodOptions.STREAM_CODEC;
        }
    }
}
