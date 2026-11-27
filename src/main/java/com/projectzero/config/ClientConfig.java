package com.projectzero.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-only options (config/projectzero-client.toml). Accessibility toggles for hit feel. */
public final class ClientConfig {
    private ClientConfig() {}

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SCREEN_SHAKE;
    public static final ForgeConfigSpec.BooleanValue HIT_FLASH;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("feel");
        SCREEN_SHAKE = b.comment("Small camera shake and FOV punch-in on heavy skill hits.")
                .define("screenShake", true);
        HIT_FLASH = b.comment("White flash and shake on entities hit by skills.")
                .define("hitFlash", true);
        b.pop();
        SPEC = b.build();
    }
}
