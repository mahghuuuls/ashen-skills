package com.mahghuuuls.ashenskills.runtime;

import com.mahghuuuls.ashenskills.api.HitShape;
import com.mahghuuuls.ashenskills.api.SkillContext;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.combat.SkillDamage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

/** What a skill's behavior may see and do during one call. */
final class RuntimeSkillContext implements SkillContext {

    private final EntityPlayerMP player;
    private final SkillSession session;

    RuntimeSkillContext(EntityPlayerMP player, SkillSession session) {
        this.player = player;
        this.session = session;
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

    @Override
    public int hit(HitShape shape) {
        return SkillDamage.strike(player, weapon(), session.skill(), shape, session.facingYaw());
    }
}
