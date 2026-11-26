package com.projectzero.domain;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * One job card floating in the Library domain. All the logic lives in {@link LibraryDomain};
 * the card only carries what the client needs to draw it (synced data) and forwards clicks.
 *
 * The job is decided on the server when the card is made, but the client only learns it
 * when the card is revealed, so there is no peeking.
 */
public class JobCardEntity extends Entity {
    /** Empty until revealed. */
    private static final EntityDataAccessor<String> JOB_ID =
            SynchedEntityData.defineId(JobCardEntity.class, EntityDataSerializers.STRING);
    /** -1 until revealed, else {@code JobRarity.ordinal()}. */
    private static final EntityDataAccessor<Integer> RARITY =
            SynchedEntityData.defineId(JobCardEntity.class, EntityDataSerializers.INT);
    /** Card is burning away (not chosen). */
    private static final EntityDataAccessor<Boolean> FADING =
            SynchedEntityData.defineId(JobCardEntity.class, EntityDataSerializers.BOOLEAN);

    /** World y of a mirror surface under the card (the Reawakening sea), or {@link #NO_MIRROR}. */
    private static final EntityDataAccessor<Float> MIRROR_Y =
            SynchedEntityData.defineId(JobCardEntity.class, EntityDataSerializers.FLOAT);
    public static final float NO_MIRROR = -10_000F;

    /** Client-side: tick the reveal started, for the flip animation. */
    public int revealStartTick = -1;
    /** Client-side: tick fading started. */
    public int fadeStartTick = -1;

    public JobCardEntity(EntityType<? extends JobCardEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(JOB_ID, "");
        builder.define(RARITY, -1);
        builder.define(FADING, false);
        builder.define(MIRROR_Y, NO_MIRROR);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (level().isClientSide()) {
            if (key.equals(RARITY) && isRevealed() && revealStartTick < 0) revealStartTick = tickCount;
            if (key.equals(FADING) && isFading() && fadeStartTick < 0) fadeStartTick = tickCount;
        }
    }

    public void reveal(String jobId, int rarityOrdinal) {
        entityData.set(JOB_ID, jobId);
        entityData.set(RARITY, rarityOrdinal);
    }

    public void startFading() {
        entityData.set(FADING, true);
    }

    /** Draw a reflection of this card in a mirror surface at world height {@code y}. */
    public void setMirrorY(float y) {
        entityData.set(MIRROR_Y, y);
    }

    public boolean hasMirror() { return entityData.get(MIRROR_Y) > NO_MIRROR + 1; }
    public float mirrorY() { return entityData.get(MIRROR_Y); }

    public boolean isRevealed() { return entityData.get(RARITY) >= 0; }
    public boolean isFading() { return entityData.get(FADING); }
    public String jobId() { return entityData.get(JOB_ID); }
    public int rarityOrdinal() { return entityData.get(RARITY); }

    @Override
    public void tick() {
        super.tick();
        // Reawakening cards are moved by the vision's own tick; everything else belongs to the library.
        if (!level().isClientSide() && !ReawakeningVision.owns(this)) {
            LibraryDomain.tickCard(this);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            LibraryDomain.pick(serverPlayer, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    @Override
    public boolean isPickable() {
        return !isFading() && !hasMirror();
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    /** Cards belong to one ceremony; they are never written to the world save. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}
}
