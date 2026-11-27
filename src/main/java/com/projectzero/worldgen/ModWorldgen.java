package com.projectzero.worldgen;

import com.projectzero.ProjectZero;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;

/** Hooks our region and surface rules into TerraBlender during common setup. */
public final class ModWorldgen {
    private ModWorldgen() {}

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Regions.register(new ProjectZeroRegion(ProjectZero.id("overworld")));
            SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, ProjectZero.MOD_ID,
                    ModSurfaceRules.rules());
        });
    }
}
