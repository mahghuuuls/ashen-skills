package com.mahghuuuls.ashenskills.runtime;

import com.mahghuuuls.ashenskills.Tags;
import java.util.function.LongSupplier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

/**
 * A player's exclusive cooldown: one lockout for all skills. Kept with the player's own saved data
 * as a wall-clock end time, so it keeps counting through death, logout and server restarts.
 */
public final class CooldownStore {

    private static final String EXPIRY_KEY = "cooldownExpiry";
    private static final String LENGTH_KEY = "cooldownLength";

    private final LongSupplier clockMillis;

    public CooldownStore(LongSupplier clockMillis) {
        this.clockMillis = clockMillis;
    }

    /** Milliseconds until the player may use a skill again; 0 when no cooldown runs. */
    public long remainingMillis(EntityPlayer player) {
        NBTTagCompound own = ownData(player, false);
        if (own == null) {
            return 0;
        }
        return remaining(own.getLong(EXPIRY_KEY), own.getLong(LENGTH_KEY), clockMillis.getAsLong());
    }

    /** Length in milliseconds of the cooldown that was last started; 0 when there was none. */
    public long lengthMillis(EntityPlayer player) {
        NBTTagCompound own = ownData(player, false);
        return own == null ? 0 : own.getLong(LENGTH_KEY);
    }

    public void start(EntityPlayer player, long lengthMillis) {
        NBTTagCompound own = ownData(player, true);
        own.setLong(EXPIRY_KEY, clockMillis.getAsLong() + lengthMillis);
        own.setLong(LENGTH_KEY, lengthMillis);
    }

    public void clear(EntityPlayer player) {
        NBTTagCompound own = ownData(player, false);
        if (own != null) {
            own.removeTag(EXPIRY_KEY);
            own.removeTag(LENGTH_KEY);
        }
    }

    /**
     * Time left, never more than the cooldown's own length. If the clock was set back, the stored
     * end time lies too far ahead; without this limit the lockout would last until the clock
     * catches up.
     */
    static long remaining(long expiryMillis, long lengthMillis, long nowMillis) {
        long left = expiryMillis - nowMillis;
        if (left <= 0) {
            return 0;
        }
        return Math.min(left, Math.max(0, lengthMillis));
    }

    /** The mod's compound inside the part of the player's data that survives death. */
    private static NBTTagCompound ownData(EntityPlayer player, boolean create) {
        NBTTagCompound entityData = player.getEntityData();
        if (!entityData.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            if (!create) {
                return null;
            }
            entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        NBTTagCompound persisted = entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        if (!persisted.hasKey(Tags.MOD_ID)) {
            if (!create) {
                return null;
            }
            persisted.setTag(Tags.MOD_ID, new NBTTagCompound());
        }
        return persisted.getCompoundTag(Tags.MOD_ID);
    }
}
