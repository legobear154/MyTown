package mytown.cmd;

import java.util.List;
import java.util.logging.Level;

import mytown.Formatter;
import mytown.MyTown;
import mytown.MyTownDatasource;
import mytown.Term;
import mytown.cmd.api.MyTownCommandBase;
import mytown.entities.Resident;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;

public class CmdDelHome extends MyTownCommandBase {
	@Override
	public String getCommandName() {
		return "delhome";
	}

	@Override
	public void processCommand(ICommandSender cs, String[] args) {
		canCommandSenderUseCommand(cs);
		EntityPlayerMP pl = (EntityPlayerMP) cs;
		Resident res = MyTownDatasource.instance.getOrMakeResident(pl);

		try {
			if (!res.home.hasHomes()) {
				throw new CommandException(Term.HomeCmdNoHomes.toString());
			}

			res.home.delete(args.length == 0 ? null : args[0]);
			MyTown.sendChatToPlayer(cs, args.length == 0 ? Term.HomeCmdHomeDeleted.toString() : Term.HomeCmdHome2Deleted.toString(args[0]));
		} catch (Throwable ex) {
			MyTown.instance.coreLog.log(Level.WARNING, String.format("Command execution error by %s", cs), ex);
			MyTown.sendChatToPlayer(cs, Formatter.commandError(Level.SEVERE, ex.toString()));
		}
	}

	@Override
	public String getCommandUsage(ICommandSender icommandsender) {
		return "/" + getCommandName();
	}

	@Override
	public List<String> dumpCommands() {
		return null;
	}

	@Override
	public String getPermNode() {
		return "mytown.ecmd.delhome";
	}
}
