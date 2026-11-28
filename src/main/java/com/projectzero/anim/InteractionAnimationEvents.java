package com.projectzero.anim;

import com.projectzero.ProjectZero;
import com.projectzero.client.ClientSkillVisuals;
import com.projectzero.network.ModNetwork;
import com.projectzero.network.SkillVisualPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Broadcasts a light open-hand touch for bare-handed block and entity interactions. */
@Mod.EventBusSubscriber(modid = ProjectZero.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class InteractionAnimationEvents {
    private InteractionAnimationEvents() {}

    @SubscribeEvent
    public static void onBlockUse(PlayerInteractEvent.RightClickBlock event) {
        play(event);
    }

    @SubscribeEvent
    public static void onEntityUse(PlayerInteractEvent.EntityInteract event) {
        play(event);
    }

    private static void play(PlayerInteractEvent event) {
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND
                || !event.getItemStack().isEmpty()
                || !(event.getEntity() instanceof ServerPlayer player)
                || player.level().isClientSide()) return;
        ModNetwork.sendToTrackingAndSelf(player, new SkillVisualPacket(player.getId(),
                ClientSkillVisuals.Kind.ANIM.ordinal(), PlayerAnim.INTERACT.ordinal(), PlayerAnim.INTERACT.ticks));
    }
}
