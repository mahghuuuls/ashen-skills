package com.mahghuuuls.ashenskills.runtime;

import com.mahghuuuls.ashenskills.api.HitShape;
import com.mahghuuuls.ashenskills.api.SkillContext;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillNumber;
import com.mahghuuuls.ashenskills.combat.SkillDamage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;

/** What a skill's behavior may see and do during one call. */
final class RuntimeSkillContext implements SkillContext {

    private final EntityPlayerMP player;
    private final SkillSession session;
    private final SkillMessenger messenger;

    RuntimeSkillContext(EntityPlayerMP player, SkillSession session, SkillMessenger messenger) {
        this.player = player;
        this.session = session;
        this.messenger = messenger;
    }

    @Override
    public EntityPlayer user() {
        return player;
    }

    @Override
    public ItemStack weapon() {
        return player.getHeldItemMainhand();
    }

    @Override
    public SkillDefinition skill() {
        return session.skill();
    }

    @Override
    public float facingYaw() {
        return session.facingYaw();
    }

    /**
     * A strike is seen and heard as well as dealt: one swing in front of the user, at half the
     * strike's reach and at the middle of the user's height, and one impact at the middle of each
     * target that took damage.
     */
    @Override
    public int hit(HitShape shape) {
        final int impactParticles = (int) Math.round(session.skill().number(SkillNumber.IMPACT_PARTICLES));
        double yaw = Math.toRadians(session.facingYaw());
        double forward = shape.reach() / 2;
        messenger.swingShown(player, session,
                player.posX - Math.sin(yaw) * forward, player.posY + player.height / 2,
                player.posZ + Math.cos(yaw) * forward);
        return SkillDamage.strike(player, weapon(), session.skill(), shape, session.facingYaw(),
                new SkillDamage.StrikeListener() {
                    @Override
                    public void onDamaged(Entity target) {
                        AxisAlignedBB box = target.getEntityBoundingBox();
                        messenger.impactShown(player, session,
                                (box.minX + box.maxX) / 2, (box.minY + box.maxY) / 2, (box.minZ + box.maxZ) / 2,
                                impactParticles);
                    }
                });
    }
}
