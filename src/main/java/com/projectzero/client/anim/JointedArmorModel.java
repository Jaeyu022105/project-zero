package com.projectzero.client.anim;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;

/** Armor with elbows and knees; its joints are copied from {@link AnimatedPlayerModel} each frame. */
public class JointedArmorModel extends HumanoidModel<AbstractClientPlayer> {
    public JointedArmorModel(ModelPart root) {
        super(root);
    }
}
