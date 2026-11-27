package com.projectzero.fx;

import com.projectzero.ProjectZero;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Project Zero's own particle graphics (textures/particle/*). Use these through
 * {@link Fx} instead of vanilla particles so skills, gates and visions look like the mod.
 */
public final class ModParticles {
    private ModParticles() {}

    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, ProjectZero.MOD_ID);

    /** Soft round glow that drifts up and shrinks. The workhorse for shapes. */
    public static final RegistryObject<ManaParticleType> GLOW = PARTICLES.register("mana_glow", ManaParticleType::new);
    /** Small four-point star that twinkles: hits, crits, lightning. */
    public static final RegistryObject<ManaParticleType> SPARK = PARTICLES.register("mana_spark", ManaParticleType::new);
    /** A System glyph that fades in and out: sigils, curses, skill casts. */
    public static final RegistryObject<ManaParticleType> RUNE = PARTICLES.register("rune", ManaParticleType::new);
    /** A spinning crystal fleck that falls: impacts, shattering, loot. */
    public static final RegistryObject<ManaParticleType> SHARD = PARTICLES.register("shard", ManaParticleType::new);
    /** A slow mote that spirals inward: gates and rifts. */
    public static final RegistryObject<ManaParticleType> MOTE = PARTICLES.register("rift_mote", ManaParticleType::new);
    /** A mote that flies straight into a point over 22 ticks and shrinks: rifts sucking things in. */
    public static final RegistryObject<ManaParticleType> INFLOW = PARTICLES.register("inflow", ManaParticleType::new);
    /** A thin streak stretched along its motion: slashes and dashes. */
    public static final RegistryObject<ManaParticleType> STREAK = PARTICLES.register("streak", ManaParticleType::new);
}
