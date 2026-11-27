package com.projectzero.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-only options (config/projectzero-client.toml). Accessibility toggles for hit feel, and ambience. */
public final class ClientConfig {
    private ClientConfig() {}

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue SCREEN_SHAKE;
    public static final ForgeConfigSpec.BooleanValue HIT_FLASH;
    public static final ForgeConfigSpec.BooleanValue GORE;
    public static final ForgeConfigSpec.BooleanValue FALLING_LEAVES;
    public static final ForgeConfigSpec.BooleanValue NOTICE_CHAT_LINES;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("ui");
        NOTICE_CHAT_LINES = b.comment("Also print [System] chat lines for events shown as bottom-right System notices.")
                .define("noticeChatLines", false);
        b.pop();
        b.push("feel");
        SCREEN_SHAKE = b.comment("Small camera shake and FOV punch-in on heavy skill hits.")
                .define("screenShake", true);
        HIT_FLASH = b.comment("White flash and shake on entities hit by skills.")
                .define("hitFlash", true);
        GORE = b.comment("Red blood and rot when creatures are hit. Off keeps bone dust, ichor and sparks.")
                .define("gore", true);
        b.pop();
        b.push("ambience");
        FALLING_LEAVES = b.comment("Leaves drift down from trees (and some get pulled into open gates).")
                .define("fallingLeaves", true);
        b.pop();
        SPEC = b.build();
    }
}
