package com.ghosts.abyss.init;

import com.ghosts.abyss.Abyss;
import com.ghosts.abyss.entity.HakariScenarioActor;
import com.ghosts.abyss.entity.PiercingBloodEntity;
import com.ghosts.abyss.entity.RockEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class JujutsuSlopEntities {

   public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
           DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "abyss");

   public static final RegistryObject<EntityType<RockEntity>> ROCK =
           ENTITY_TYPES.register("rock",
                   () -> EntityType.Builder.<RockEntity>of(RockEntity::new, MobCategory.MISC)
                           .sized(0.4F, 0.4F)
                           .clientTrackingRange(8)
                           .updateInterval(1)
                           .build("rock"));
   public static final RegistryObject<EntityType<PiercingBloodEntity>>
           PIERCING_BLOOD = ENTITY_TYPES.register(
           "piercing_blood",
           () -> EntityType.Builder
                   .<PiercingBloodEntity>of(PiercingBloodEntity::new, MobCategory.MISC)
                   .sized(0.1F, 0.1F)
                   .noSummon()
                   .clientTrackingRange(8)
                   .updateInterval(1)
                   .build("piercing_blood")
   );

   public static final RegistryObject<EntityType<HakariScenarioActor>> HAKARI_SCENARIO_ACTOR =
           ENTITY_TYPES.register("hakari_scenario_actor",
                   () -> EntityType.Builder.<HakariScenarioActor>of(HakariScenarioActor::new, MobCategory.MISC)
                           .sized(0.6F, 1.8F)
                           .noSummon()
                           .noSave()
                           .fireImmune()
                           .clientTrackingRange(10)
                           .updateInterval(1)
                           .build("hakari_scenario_actor"));
}
