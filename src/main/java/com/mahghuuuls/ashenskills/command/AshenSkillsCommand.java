package com.mahghuuuls.ashenskills.command;

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

/** Operator command {@code /ashenskills}: shows what the mod knows about a player's weapon. */
public final class AshenSkillsCommand extends CommandBase {

    private static final String USAGE = "commands.ashenskills.usage";

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
        if (args.length < 1 || args.length > 2 || !"inspect".equals(args[0])) {
            throw new WrongUsageException(USAGE);
        }
        EntityPlayerMP player = args.length == 2 ? getPlayer(server, sender, args[1]) : getCommandSenderAsPlayer(sender);
        String report = "[Ashen Skills] " + describeMainHand(player);
        sender.sendMessage(new TextComponentString(report));
    }

    private static String describeMainHand(EntityPlayerMP player) {
        ItemStack stack = player.getHeldItemMainhand();
        ResourceLocation itemName = stack.isEmpty() ? null : stack.getItem().getRegistryName();
        AttachedSkill attached = AttachedSkill.of(stack);
        return "player=" + player.getName()
                + " item=" + (itemName == null ? "none" : itemName)
                + " types=" + WeaponTypes.effectiveTypes(stack)
                + " skill=" + (attached.skillId() == null ? "none" : attached.skillId())
                + " status=" + attached.status();
    }

    @Override
    public List<String> getTabCompletions(
            MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "inspect");
        }
        if (args.length == 2) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        return Collections.emptyList();
    }
}
