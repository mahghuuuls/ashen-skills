package com.mahghuuuls.ashenskills.network;

import com.mahghuuuls.ashenskills.AshenSkillsMod;
import com.mahghuuuls.ashenskills.api.SkillPhase;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to one client: an operator asks that client to use one of its animation check controls.
 * The client obeys only when its own setting for these controls is on. The request comes through
 * the server because the test tools that drive the game can run server commands only.
 */
public final class PoseControlMessage implements IMessage {

    /** Hold a clip at a time. */
    public static final byte PIN = 1;
    /** End a hold. */
    public static final byte UNPIN = 2;
    /** Write the current pose to the client's log. */
    public static final byte DUMP = 3;
    /** Change the camera view. */
    public static final byte VIEW = 4;

    private byte action;
    private String skillId = "";
    private byte phaseCode;
    private double seconds;
    private byte cameraView;

    public PoseControlMessage() {
    }

    private PoseControlMessage(byte action, String skillId, byte phaseCode, double seconds, int cameraView) {
        this.action = action;
        this.skillId = skillId;
        this.phaseCode = phaseCode;
        this.seconds = seconds;
        this.cameraView = (byte) cameraView;
    }

    /** @param seconds time in the clip to hold */
    public static PoseControlMessage pin(String skillId, SkillPhase phase, double seconds) {
        return new PoseControlMessage(PIN, skillId, PhaseUpdateMessage.codeOf(phase), seconds, 0);
    }

    public static PoseControlMessage unpin() {
        return new PoseControlMessage(UNPIN, "", PhaseUpdateMessage.NO_PHASE, 0, 0);
    }

    public static PoseControlMessage dump() {
        return new PoseControlMessage(DUMP, "", PhaseUpdateMessage.NO_PHASE, 0, 0);
    }

    /** @param cameraView 0 first person, 1 from behind, 2 from the front */
    public static PoseControlMessage view(int cameraView) {
        return new PoseControlMessage(VIEW, "", PhaseUpdateMessage.NO_PHASE, 0, cameraView);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readByte();
        skillId = ByteBufUtils.readUTF8String(buf);
        phaseCode = buf.readByte();
        seconds = buf.readDouble();
        cameraView = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
        ByteBufUtils.writeUTF8String(buf, skillId);
        buf.writeByte(phaseCode);
        buf.writeDouble(seconds);
        buf.writeByte(cameraView);
    }

    private static SkillPhase phaseOf(byte code) {
        return PhaseUpdateMessage.phaseOf(code);
    }

    public static final class Handler implements IMessageHandler<PoseControlMessage, IMessage> {

        @Override
        public IMessage onMessage(PoseControlMessage message, MessageContext ctx) {
            AshenSkillsMod.proxy.onPoseControl(
                    message.action, message.skillId, phaseOf(message.phaseCode), message.seconds, message.cameraView);
            return null;
        }
    }
}
