package mytown.cmd.sub.nation;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import mytown.CommandException;
import mytown.Formatter;
import mytown.MyTown;
import mytown.MyTownDatasource;
import mytown.NoAccessException;
import mytown.Term;
import mytown.cmd.api.MyTownSubCommandAdapter;
import mytown.entities.Nation;
import mytown.entities.Resident;
import mytown.entities.Resident.Rank;
import mytown.entities.Town;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;

public class CmdNationTransfer extends MyTownSubCommandAdapter {

	@Override
	public String getName() {
		return "transfer";
	}

	@Override
	public String getPermNode() {
		return "mytown.cmd.nationtransfer";
	}

	@Override
	public void process(ICommandSender sender, String[] args) throws CommandException, NoAccessException {
		Resident res = MyTownDatasource.instance.getOrMakeResident((EntityPlayer) sender);
		if (res.town() == null || res.rank() != Rank.Mayor) {
			return;
		}

		Town town = res.town();
		Nation nation = town.nation();

		if (args.length == 1) {
			Town t = MyTownDatasource.instance.getTown(args[0]);
			if (t == null) {
				throw new CommandException(Term.TownErrNotFound, args[0]);
			}

			if (t.nation() != nation) {
				throw new CommandException(Term.TownErrNationNotPartOfNation);
			}

			if (t == town) {
				throw new CommandException(Term.TownErrNationCannotTransferSelf);
			}

			nation.setCapital(t);
			t.sendNotification(Level.INFO, Term.NationNowCapital.toString(nation.name()));

			MyTown.sendChatToPlayer(sender, Term.NationCapitalTransfered.toString(t.name()));
		} else {
			MyTown.sendChatToPlayer(sender, Formatter.formatCommand(Term.TownCmdNation.toString() + " " + Term.TownCmdNationInvite.toString(), Term.TownCmdNationInviteArgs.toString(), Term.TownCmdNationInviteDesc.toString(), null));
		}
	}

	@Override
	public List<String> tabComplete(ICommandSender sender, String[] args) {
		Resident res = MyTownDatasource.instance.getOrMakeResident((EntityPlayer) sender);
		if (res == null || res.town() == null || res.town().nation() == null)
			return null;
		Nation n = res.town().nation();
		List<String> nationTowns = new ArrayList<String>();
		nationTowns.addAll(n.towns().keySet());
		return nationTowns;
	}

}