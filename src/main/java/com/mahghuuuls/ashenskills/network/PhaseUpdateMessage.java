package com.mahghuuuls.ashenskills.network;

import com.mahghuuuls.ashenskills.AshenSkillsMod;
import com.mahghuuuls.ashenskills.api.SkillPhase;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to clients: the skill phase a player is in now. Sent at every phase change, when a
 * client starts to see the player, and again every {@link #OPEN_ENDED_REPORT_TICKS} ticks while a
 * phase without a length lasts; receivers replace what they knew with it.
 */
public final class PhaseUpdateMessage implements IMessage {

    /** Phase code for "in no phase". */
    public static final byte NO_PHASE = 0;
    /** The length a report carries for a phase without one (a preparation): it lasts until the next report says otherwise. */
    public static final int NO_FIXED_LENGTH = 0;
    /**
     * How often the server sends the report of a phase without a length again while it lasts, in
     * server ticks. A receiver trusts such a report for a few of these periods and then drops it,
     * because no "ended" report reaches a client that stopped seeing the player.
     */
    public static final int OPEN_ENDED_REPORT_TICKS = 40;

    /** The code that stands for a phase in the mod's messages. */
    public static byte codeOf(SkillPhase phase) {
        return (byte) (phase.ordinal() + 1);
    }

    /** @return the phase a code stands for, or null for {@link #NO_PHASE} and unknown codes */
    public static SkillPhase phaseOf(byte code) {
        SkillPhase[] phases = SkillPhase.values();
        return code >= 1 && code <= phases.length ? phases[code - 1] : null;
    }

    private int entityId;
    private String skillId = "";
    private byte phaseCode;
    private int elapsedTicks;
    private int lengthTicks;
    private boolean attackLocked;
    private boolean jumpLocked;
    private boolean sneakLocked;

    public PhaseUpdateMessage() {
    }

    /**
     * @param attackLocked whether a normal attack by that player has no effect in this phase
     * @param jumpLocked whether that player may not jump in this phase
     * @param sneakLocked whether that player may not sneak in this phase
     */
    public PhaseUpdateMessage(
            int entityId, String skillId, byte phaseCode, int elapsedTicks, int lengthTicks,
            boolean attackLocked, boolean jumpLocked, boolean sneakLocked) {
        this.entityId = entityId;
        this.skillId = skillId;
        this.phaseCode = phaseCode;
        this.elapsedTicks = elapsedTicks;
        this.lengthTicks = lengthTicks;
        this.attackLocked = attackLocked;
        this.jumpLocked = jumpLocked;
        this.sneakLocked = sneakLocked;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        skillId = ByteBufUtils.readUTF8String(buf);
        phaseCode = buf.readByte();
        elapsedTicks = buf.readInt();
        lengthTicks = buf.readInt();
        attackLocked = buf.readBoolean();
        jumpLocked = buf.readBoolean();
        sneakLocked = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        ByteBufUtils.writeUTF8String(buf, skillId);
        buf.writeByte(phaseCode);
        buf.writeInt(elapsedTicks);
        buf.writeInt(lengthTicks);
        buf.writeBoolean(attackLocked);
        buf.writeBoolean(jumpLocked);
        buf.writeBoolean(sneakLocked);
    }

    public static final class Handler implements IMessageHandler<PhaseUpdateMessage, IMessage> {

        @Override
        public IMessage onMessage(PhaseUpdateMessage message, MessageContext ctx) {
            AshenSkillsMod.proxy.onPhaseUpdate(
                    message.entityId, message.skillId, message.phaseCode, message.elapsedTicks, message.lengthTicks,
                    message.attackLocked, message.jumpLocked, message.sneakLocked);
            return null;
        }
    }
}
