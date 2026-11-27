package com.projectzero.ui;

import com.projectzero.ProjectZero;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge event subscriber registering {@link NoticeCommands}. */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NoticeEvents {
    private NoticeEvents() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        NoticeCommands.register(event.getDispatcher());
    }
}
