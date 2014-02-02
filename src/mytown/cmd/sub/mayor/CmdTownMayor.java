package mytown.cmd.sub.mayor;

import java.util.List;
import java.util.logging.Level;

import mytown.CommandException;
import mytown.Formatter;
import mytown.MyTown;
import mytown.MyTownDatasource;
import mytown.NoAccessException;
import mytown.Term;
import mytown.cmd.api.MyTownSubCommandAdapter;
import mytown.entities.Resident;
import mytown.entities.Resident.Rank;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;

import com.sperion.forgeperms.ForgePerms;

public class CmdTownMayor extends MyTownSubCommandAdapter {

	@Override
	public String getName() {
		return "mayor";
	}

	@Override
	public String getPermNode() {
		return "mytown.cmd.mayor";
	}

	@Override
	public void process(ICommandSender sender, String[] args) throws CommandException, NoAccessException {
		Resident res = MyTownDatasource.instance.getOrMakeResident((EntityPlayer) sender);
		if (args.length != 2) {
			MyTown.sendChatToPlayer(sender, Formatter.formatCommand(Term.TownCmdMayor.toString(), Term.TownCmdMayorArgs.toString(), Term.TownCmdMayorDesc.toString(), null));
		} else {
			String name = args[1];

			Resident r = MyTownDatasource.instance.getResident(name);
			if (r == null) {
				throw new CommandException(Term.TownErrPlayerNotFound);
			}
			if (r == res) {
				throw new CommandException(Term.TownErrCannotDoWithYourself);
			}
			if (r.town() != res.town()) {
				throw new CommandException(Term.TownErrPlayerNotInYourTown);
			}

			if (!ForgePerms.getPermissionManager().canAccess(r.onlinePlayer.username, r.onlinePlayer.worldObj.provider.getDimensionName(), "mytown.cmd.new")) {
				throw new CommandException(Term.TownErrPlayerDoesntHaveAccessToTownManagement);
			}

			res.town().setResidentRank(r, Rank.Mayor);
			res.town().setResidentRank(res, Rank.Assistant);

			res.town().sendNotification(Level.INFO, Term.TownPlayerPromotedToMayor.toString(r.name()));
		}
	}

	@Override
	public List<String> tabComplete(ICommandSender sender, String[] args) {
		// TODO Auto-generated method stub
		return null;
	}

}