package mytown.cmd.sub.admin;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import mytown.Formatter;
import mytown.MyTown;
import mytown.MyTownDatasource;
import mytown.NoAccessException;
import mytown.Term;
import mytown.cmd.api.MyTownSubCommandAdapter;
import mytown.entities.Resident;
import mytown.entities.Town;

public class CmdTownRem extends MyTownSubCommandAdapter {

	@Override
	public String getName() {
		return "rem";
	}

	@Override
	public String getPermNode() {
		return "mytown.adm.cmd.rem";
	}

	@Override
	public void process(ICommandSender sender, String[] args) throws CommandException, NoAccessException {
		if (args.length < 2) {
			MyTown.sendChatToPlayer(sender, Formatter.formatAdminCommand(Term.TownadmCmdRem.toString(), Term.TownadmCmdRemArgs.toString(), Term.TownadmCmdRemDesc.toString(), null));
		} else {
			Town t = MyTownDatasource.instance.getTown(args[0]);
			if (t == null) {
				throw new CommandException(Term.TownErrNotFound.toString(), args[0]);
			}

			for (int i = 1; i < args.length; i++) {
				Resident r = MyTownDatasource.instance.getOrMakeResident(args[i]);
				if (r.town() != null && r.town() == t) {
					t.removeResident(r); // unloads the resident
				}
			}
			MyTown.sendChatToPlayer(sender, Term.TownadmResidentsSet.toString());
		}
	}

	@Override
	public List<String> tabComplete(ICommandSender sender, String[] args) {
		return null;
	}

}
