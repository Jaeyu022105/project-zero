package com.projectzero.registry;

import com.projectzero.ProjectZero;
import com.projectzero.domain.JobCardEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ProjectZero.MOD_ID);

    /** A floating job card in the Library domain. Never saved, never summoned by hand. */
    public static final RegistryObject<EntityType<JobCardEntity>> JOB_CARD = ENTITIES.register("job_card",
            () -> EntityType.Builder.<JobCardEntity>of(JobCardEntity::new, MobCategory.MISC)
                    .sized(0.8F, 1.2F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSummon()
                    .build("job_card"));
}
