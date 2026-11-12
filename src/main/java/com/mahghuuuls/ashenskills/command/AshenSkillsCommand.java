package com.mahghuuuls.ashenskills.command;

import com.mahghuuuls.ashenskills.AshenSkillsMod;
import com.mahghuuuls.ashenskills.runtime.RefusalReason;
import com.mahghuuuls.ashenskills.runtime.SkillRuntime;
import com.mahghuuuls.ashenskills.runtime.SkillRuntimeHooks;
import com.mahghuuuls.ashenskills.runtime.SkillSession;
import com.mahghuuuls.ashenskills.weapon.AttachedSkill;
import com.mahghuuuls.ashenskills.weapon.WeaponTypes;
import java.util.Collections;
import java.util.List;
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
 * skill state, starts a player's skill for testing, and clears a stuck phase or a cooldown.
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
        } else {
            throw new WrongUsageException(USAGE);
        }
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
            return getListOfStringsMatchingLastWord(args, "inspect", "start", "clear");
        }
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        if (args.length == 3 && "clear".equals(args[0])) {
            return getListOfStringsMatchingLastWord(args, "phase", "cooldown");
        }
        return Collections.emptyList();
    }
}
