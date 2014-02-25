package mytown.cmd.sub.nonresident;

import mytown.*;
import mytown.cmd.api.MyTownSubCommandAdapter;
import mytown.entities.PayHandler;
import mytown.entities.Resident;
import mytown.entities.Town;
import mytown.entities.TownBlock;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public class CmdTownNew extends MyTownSubCommandAdapter {
	@Override
	public String getName() {
		return "new";
	}

	@Override
	public String getPermNode() {
		return "mytown.cmd.new.dim";
	}

	@Override
	public void canUse(ICommandSender sender) throws CommandException {
		super.canUse(sender);
		Resident res = MyTownDatasource.instance.getOrMakeResident((EntityPlayer) sender);
		if (res.town() != null)
			throw new CommandException("Already in a town");
	}

	@Override
	public void process(ICommandSender sender, String[] args) throws CommandException {
		Resident res = MyTownDatasource.instance.getOrMakeResident((EntityPlayer) sender);
		Assert.Perm(sender, "mytown.cmd.new.dim" + res.onlinePlayer.dimension);

		if (args.length < 1 || args.length > 1) {
			MyTown.sendChatToPlayer(sender, Formatter.formatCommand(Term.TownCmdNew.toString(), Term.TownCmdNewArgs.toString(), Term.TownCmdNewDesc.toString(), null));
		} else {
			TownBlock home = MyTownDatasource.instance.getOrMakeBlock(res.onlinePlayer.dimension, res.onlinePlayer.chunkCoordX, res.onlinePlayer.chunkCoordZ);
			
			res.pay.requestPayment("townnew", Cost.TownNew.item, new PayHandler.IDone() {
				@Override
				public void run(Resident res, Object[] ar2) {
					String[] args = (String[]) ar2[0];

					Town t = null;
					try { // should never crash because we're doing the same
							// checks before
						t = new Town(args[0], res, (TownBlock) ar2[1]);
					} catch (CommandException e) {
						Log.severe("Town creation failed after taking payment", e);
					}

					// emulate that the player just entered it
					res.checkLocation();

					String msg = Term.TownBroadcastCreated.toString(res.name(), t.name());
					for (Object obj : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
						MyTown.sendChatToPlayer((EntityPlayer) obj, msg);
					}

					t.sendTownInfo(res.onlinePlayer);
				}
			}, args, home);
		}
	}

	@Override
	public List<String> tabComplete(ICommandSender sender, String[] args) {
		return null;
	}
	
	@Override
	public String getDesc(ICommandSender sender) {
		return Term.TownCmdNewDesc.toString();
	}

	@Override
	public String getArgs(ICommandSender sender) {
		return Term.TownCmdNewArgs.toString();
	}
}