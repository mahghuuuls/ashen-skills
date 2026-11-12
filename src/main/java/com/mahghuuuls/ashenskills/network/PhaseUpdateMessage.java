package com.mahghuuuls.ashenskills.network;

import com.mahghuuuls.ashenskills.AshenSkillsMod;
import com.mahghuuuls.ashenskills.api.SkillPhase;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to clients: the skill phase a player is in now. Sent at every phase change and when a
 * client starts to see the player; receivers replace what they knew with it.
 */
public final class PhaseUpdateMessage implements IMessage {

    /** Phase code for "in no phase". */
    public static final byte NO_PHASE = 0;

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

    public PhaseUpdateMessage() {
    }

    /**
     * @param attackLocked whether a normal attack by that player has no effect in this phase
     * @param jumpLocked whether that player may not jump in this phase
     */
    public PhaseUpdateMessage(
            int entityId, String skillId, byte phaseCode, int elapsedTicks, int lengthTicks,
            boolean attackLocked, boolean jumpLocked) {
        this.entityId = entityId;
        this.skillId = skillId;
        this.phaseCode = phaseCode;
        this.elapsedTicks = elapsedTicks;
        this.lengthTicks = lengthTicks;
        this.attackLocked = attackLocked;
        this.jumpLocked = jumpLocked;
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
    }

    public static final class Handler implements IMessageHandler<PhaseUpdateMessage, IMessage> {

        @Override
        public IMessage onMessage(PhaseUpdateMessage message, MessageContext ctx) {
            AshenSkillsMod.proxy.onPhaseUpdate(
                    message.entityId, message.skillId, message.phaseCode, message.elapsedTicks, message.lengthTicks,
                    message.attackLocked, message.jumpLocked);
            return null;
        }
    }
}
