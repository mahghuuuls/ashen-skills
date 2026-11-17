package com.mahghuuuls.ashenskills.command;

import com.mahghuuuls.ashenskills.ashenforge.AshenForgeTile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.common.ForgeHooks;
import com.mahghuuuls.ashenskills.AshenSkillsMod;
import com.mahghuuuls.ashenskills.api.SkillPhase;
import com.mahghuuuls.ashenskills.network.AshenNetwork;
import com.mahghuuuls.ashenskills.network.PoseControlMessage;
import com.mahghuuuls.ashenskills.runtime.RefusalReason;
import com.mahghuuuls.ashenskills.runtime.SkillRuntime;
import com.mahghuuuls.ashenskills.runtime.SkillRuntimeHooks;
import com.mahghuuuls.ashenskills.runtime.SkillSession;
import com.mahghuuuls.ashenskills.weapon.AttachedSkill;
import com.mahghuuuls.ashenskills.weapon.SkillTag;
import com.mahghuuuls.ashenskills.weapon.WeaponTypes;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

/**
 * Operator command {@code /ashenskills}: shows what the mod knows about a player's weapon and
 * skill state, starts a player's skill for testing ({@code start <player>} acts as the skill key
 * going down, {@code start <player> release} as the key coming up, {@code start <player> lost} as
 * a screen taking the key away, {@code start <player> attack} as the attack button in a stance), clears a stuck phase or a cooldown, and asks a player's client
 * to use its animation check controls.
 */
public final class AshenSkillsCommand extends CommandBase {

    private static final String USAGE = "commands.ashenskills.usage";
    private static final String PREFIX = "[Ashen Skills] ";

    @Override
    public String getName() {
        return "ashenskills";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return USAGE;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1) {
            throw new WrongUsageException(USAGE);
        }
        SkillRuntime runtime = SkillRuntimeHooks.runtime();
        if ("inspect".equals(args[0]) && args.length <= 2) {
            EntityPlayerMP player = args.length == 2 ? getPlayer(server, sender, args[1]) : getCommandSenderAsPlayer(sender);
            reply(sender, describe(player, runtime));
        } else if ("start".equals(args[0]) && args.length == 2) {
            EntityPlayerMP player = getPlayer(server, sender, args[1]);
            RefusalReason refusal = runtime.requestStart(player);
            String result = "start player=" + player.getName()
                    + (refusal == null ? " result=STARTED" : " result=REFUSED reason=" + refusal);
            reply(sender, result);
            audit(sender, result);
        } else if ("start".equals(args[0]) && args.length == 3 && "release".equals(args[2])) {
            EntityPlayerMP player = getPlayer(server, sender, args[1]);
            String result = "start player=" + player.getName() + " keyReleased endedPreparation="
                    + runtime.onKeyReleased(player);
            reply(sender, result);
            audit(sender, result);
        } else if ("start".equals(args[0]) && args.length == 3 && "lost".equals(args[2])) {
            EntityPlayerMP player = getPlayer(server, sender, args[1]);
            String result = "start player=" + player.getName() + " keyLost endedPreparation=" + runtime.onKeyLost(player);
            reply(sender, result);
            audit(sender, result);
        } else if ("start".equals(args[0]) && args.length == 3 && "attack".equals(args[2])) {
            EntityPlayerMP player = getPlayer(server, sender, args[1]);
            RefusalReason refusal = runtime.onAttackPressed(player);
            String result = "start player=" + player.getName() + " attackPressed"
                    + (refusal == null ? " result=EXECUTED"
                            : refusal == RefusalReason.NOT_READY_YET ? " result=KEPT_FOR_WIND_UP"
                            : " result=REFUSED reason=" + refusal);
            reply(sender, result);
            audit(sender, result);
        } else if ("clear".equals(args[0]) && args.length == 3 && "phase".equals(args[2])) {
            EntityPlayerMP player = getPlayer(server, sender, args[1]);
            String result = "clear phase player=" + player.getName()
                    + " wasInPhase=" + runtime.endNow(player, "operatorClear");
            reply(sender, result);
            audit(sender, result);
        } else if ("clear".equals(args[0]) && args.length == 3 && "cooldown".equals(args[2])) {
            EntityPlayerMP player = getPlayer(server, sender, args[1]);
            long had = runtime.cooldowns().remainingMillis(player);
            runtime.clearCooldown(player);
            String result = "clear cooldown player=" + player.getName() + " hadRemainingMs=" + had;
            reply(sender, result);
            audit(sender, result);
        } else if ("forge".equals(args[0]) && (args.length == 1 || args.length == 4)) {
            reply(sender, describeForge(sender, args));
        } else if ("pose".equals(args[0]) && args.length >= 3) {
            EntityPlayerMP player = getPlayer(server, sender, args[1]);
            AshenNetwork.channel().sendTo(poseControl(args), player);
            String result = "pose player=" + player.getName() + " request=" + buildString(args, 2)
                    + " (acts only when that client's animationCheckControls setting is on)";
            reply(sender, result);
            audit(sender, result);
        } else {
            throw new WrongUsageException(USAGE);
        }
    }

    /** {@code pose <player> pin <skill> <phase> <seconds> | unpin | dump | view <0|1|2>} */
    private static PoseControlMessage poseControl(String[] args) throws CommandException {
        String control = args[2];
        if ("pin".equals(control) && args.length == 6) {
            SkillPhase phase = phaseNamed(args[4]);
            if (phase == null) {
                throw new WrongUsageException(USAGE);
            }
            return PoseControlMessage.pin(
                    new ResourceLocation(args[3]).toString(), phase, parseDouble(args[5], 0.0, 600.0));
        }
        if ("unpin".equals(control) && args.length == 3) {
            return PoseControlMessage.unpin();
        }
        if ("dump".equals(control) && args.length == 3) {
            return PoseControlMessage.dump();
        }
        if ("view".equals(control) && args.length == 4) {
            return PoseControlMessage.view(parseInt(args[3], 0, 2));
        }
        throw new WrongUsageException(USAGE);
    }

    /** {@code forge}: the Forge the operator looks at; {@code forge <x> <y> <z>}: the Forge at that place. Changes nothing. */
    private static String describeForge(ICommandSender sender, String[] args) throws CommandException {
        BlockPos pos;
        if (args.length == 4) {
            pos = parseBlockPos(sender, args, 1, false);
        } else {
            RayTraceResult hit = ForgeHooks.rayTraceEyes(getCommandSenderAsPlayer(sender), 6.0);
            if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
                return "forge result=NO_BLOCK_IN_VIEW";
            }
            pos = hit.getBlockPos();
        }
        String place = pos.getX() + "," + pos.getY() + "," + pos.getZ();
        if (!sender.getEntityWorld().isBlockLoaded(pos)) {
            return "forge pos=" + place + " result=NOT_LOADED";
        }
        TileEntity tile = sender.getEntityWorld().getTileEntity(pos);
        if (!(tile instanceof AshenForgeTile)) {
            return "forge pos=" + place + " result=NOT_A_FORGE";
        }
        AshenForgeTile forge = (AshenForgeTile) tile;
        int used = 0;
        int ashes = 0;
        for (int slot = 0; slot < forge.ashes().getSlots(); slot++) {
            ItemStack stack = forge.ashes().getStackInSlot(slot);
            if (!stack.isEmpty()) {
                used++;
                ashes += stack.getCount();
            }
        }
        ItemStack weapon = forge.weapon();
        String weaponSkill = SkillTag.getSkillId(weapon);
        return "forge pos=" + place + " slots=" + forge.ashes().getSlots() + " slotsUsed=" + used + " ashes=" + ashes
                + " distinctSkills=" + new TreeSet<>(withoutNulls(forge.storedSkillIds()))
                + " weapon=" + (weapon.isEmpty() ? "none" : weapon.getItem().getRegistryName())
                + " weaponSkill=" + (weaponSkill == null ? "none" : weaponSkill);
    }

    private static List<String> withoutNulls(List<String> ids) {
        List<String> named = new ArrayList<>(ids);
        named.removeAll(Collections.<String>singleton(null));
        return named;
    }

    private static SkillPhase phaseNamed(String name) {
        for (SkillPhase phase : SkillPhase.values()) {
            if (phase.name().toLowerCase(Locale.ROOT).equals(name)) {
                return phase;
            }
        }
        return null;
    }

    private static void reply(ICommandSender sender, String text) {
        sender.sendMessage(new TextComponentString(PREFIX + text));
    }

    /** Actions that change a player's state are always written to the log with who did them. */
    private static void audit(ICommandSender sender, String action) {
        AshenSkillsMod.LOGGER.info("[OPERATOR] by={} {}", sender.getName(), action);
    }

    private static String describe(EntityPlayerMP player, SkillRuntime runtime) {
        ItemStack stack = player.getHeldItemMainhand();
        ResourceLocation itemName = stack.isEmpty() ? null : stack.getItem().getRegistryName();
        AttachedSkill attached = AttachedSkill.of(stack);
        SkillSession session = runtime.sessionOf(player);
        return "player=" + player.getName()
                + " item=" + (itemName == null ? "none" : itemName)
                + " types=" + WeaponTypes.effectiveTypes(stack)
                + " skill=" + (attached.skillId() == null ? "none" : attached.skillId())
                + " status=" + attached.status()
                + " phase=" + (session == null ? "NONE" : session.phase())
                + " usingItem=" + player.isHandActive()
                + " cooldownMs=" + runtime.cooldowns().remainingMillis(player);
    }

    @Override
    public List<String> getTabCompletions(
            MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "inspect", "start", "clear", "pose");
        }
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        if (args.length == 3 && "start".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args, "release", "lost", "attack");
        }
        if (args.length == 3 && "clear".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args, "phase", "cooldown");
        }
        if (args.length == 3 && "pose".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args, "pin", "unpin", "dump", "view");
        }
        return Collections.emptyList();
    }
}
