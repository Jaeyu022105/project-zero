package com.projectzero.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** Client-only screen entry points used after server waypoint validation. */
public final class ClientWaypointScreenOpener {
    private ClientWaypointScreenOpener() {}
    public static void list() {
        if (Minecraft.getInstance().player != null) Minecraft.getInstance().setScreen(new WaypointScreen());
    }
    public static void rename(ResourceLocation dimension, long pos, String name) {
        if (Minecraft.getInstance().player != null) Minecraft.getInstance().setScreen(new WaypointRenameScreen(dimension, pos, name));
    }
}
