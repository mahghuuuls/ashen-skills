package com.mahghuuuls.ashenskills.combat;

import com.mahghuuuls.ashenskills.api.HitShape;
import com.mahghuuuls.ashenskills.combat.math.NearestFirst;
import com.mahghuuuls.ashenskills.combat.math.NearestFirst.Ranked;
import com.mahghuuuls.ashenskills.combat.math.VerticalReach;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MultiPartEntityPart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Finds what one strike hits: living things inside the shape, never the user or the user's mount,
 * never a player the server's PvP or team rules protect, each creature once, and only those no
 * solid block shields; the nearest of them up to the cap.
 *
 * <p>A creature made of parts (the Ender Dragon) is reached only through its parts: the game
 * refuses damage given to such a creature directly. The nearest part inside the shape is the
 * target, and the creature counts once.
 */
final class TargetSelector {

    /** What a search found, for the diagnostic log as well as for the strike. */
    static final class Selection {

        /** Creatures found in the search box, before any rule. */
        final int candidates;
        /** How many of them were inside the shape and allowed as targets. */
        final int inShape;
        /** The entities to damage, nearest first: a creature, or one part of a many-part creature. */
        final List<Entity> targets;

        Selection(int candidates, int inShape, List<Entity> targets) {
            this.candidates = candidates;
            this.inShape = inShape;
            this.targets = targets;
        }
    }

    /** Where a strike looks for targets. */
    static final class Area {

        final HitShape shape;
        final float facingYaw;
        final double reachBelow;
        final double reachAbove;

        /**
         * @param reachBelow how far below the user's feet a target's box may end and still be reached
         * @param reachAbove how far above the user's feet a target's box may begin and still be reached
         */
        Area(HitShape shape, float facingYaw, double reachBelow, double reachAbove) {
            this.shape = shape;
            this.facingYaw = facingYaw;
            this.reachBelow = reachBelow;
            this.reachAbove = reachAbove;
        }
    }

    private TargetSelector() {
    }

    static Selection select(EntityPlayer user, Area area, int targetCap, boolean solidBlocksObstruct) {
        double reach = area.shape.reach();
        AxisAlignedBB searchBox = new AxisAlignedBB(
                user.posX - reach, user.posY - area.reachBelow, user.posZ - reach,
                user.posX + reach, user.posY + area.reachAbove, user.posZ + reach);

        Map<Entity, Boolean> counted = new IdentityHashMap<>();
        List<Ranked<Entity>> inShape = new ArrayList<>();
        for (Entity found : user.world.getEntitiesWithinAABBExcludingEntity(user, searchBox)) {
            Entity creature = creatureOf(found);
            if (creature == null) {
                continue;
            }
            counted.put(creature, Boolean.TRUE);
            if (isAllowedTarget(user, creature) && isInside(user, area, found.getEntityBoundingBox())) {
                inShape.add(new Ranked<>(found, user.getDistanceSq(found)));
            }
        }

        Map<Entity, Boolean> taken = new IdentityHashMap<>();
        List<Entity> targets = new ArrayList<>();
        int creaturesInShape = 0;
        for (Entity target : NearestFirst.pick(inShape, inShape.size())) {
            if (taken.put(creatureOf(target), Boolean.TRUE) != null) {
                continue;
            }
            creaturesInShape++;
            if (targets.size() < targetCap && (!solidBlocksObstruct || canReach(user, target))) {
                targets.add(target);
            }
        }
        return new Selection(counted.size(), creaturesInShape, targets);
    }

    private static boolean isInside(EntityPlayer user, Area area, AxisAlignedBB box) {
        return VerticalReach.overlaps(user.posY, area.reachBelow, area.reachAbove, box.minY, box.maxY)
                && area.shape.contains(user.posX, user.posZ, area.facingYaw, box.minX, box.minZ, box.maxX, box.maxZ);
    }

    /**
     * The creature an entity belongs to, or null when a strike cannot hurt this entity: it is not
     * alive, or it is the body of a many-part creature, which is reached through its parts.
     */
    private static Entity creatureOf(Entity found) {
        if (found instanceof MultiPartEntityPart) {
            Object parent = ((MultiPartEntityPart) found).parent;
            return parent instanceof Entity ? (Entity) parent : null;
        }
        return found instanceof EntityLivingBase && found.getParts() == null ? found : null;
    }

    private static boolean isAllowedTarget(EntityPlayer user, Entity creature) {
        if (!creature.isEntityAlive() || creature == user.getRidingEntity()) {
            return false;
        }
        if (creature instanceof EntityPlayer) {
            EntityPlayer other = (EntityPlayer) creature;
            if (other.isSpectator() || !user.canAttackPlayer(other)) {
                return false;
            }
            return user.getServer() == null || user.getServer().isPVPEnabled();
        }
        return true;
    }

    /** A target is shielded only when solid blocks cut both the line to its middle and to its eyes. */
    private static boolean canReach(EntityPlayer user, Entity target) {
        Vec3d eyes = new Vec3d(user.posX, user.posY + user.getEyeHeight(), user.posZ);
        AxisAlignedBB box = target.getEntityBoundingBox();
        Vec3d middle = new Vec3d((box.minX + box.maxX) / 2, (box.minY + box.maxY) / 2, (box.minZ + box.maxZ) / 2);
        Vec3d targetEyes = new Vec3d(middle.x, box.minY + target.getEyeHeight(), middle.z);
        return isClear(user.world, eyes, middle) || isClear(user.world, eyes, targetEyes);
    }

    /** Blocks without a collision box (grass, cobweb) do not shield. */
    private static boolean isClear(World world, Vec3d from, Vec3d to) {
        return world.rayTraceBlocks(from, to, false, true, false) == null;
    }
}
