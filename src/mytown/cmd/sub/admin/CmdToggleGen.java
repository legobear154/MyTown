package mytown.cmd.sub.admin;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import mytown.MyTown;
import mytown.NoAccessException;
import mytown.cmd.api.MyTownSubCommandAdapter;
import mytown.event.tick.WorldBorder;

public class CmdToggleGen extends MyTownSubCommandAdapter {

	@Override
	public String getName() {
		return "togglegen";
	}

	@Override
	public String getPermNode() {
		return "mytown.adm.cmd.togglegen";
	}

	@Override
	public void process(ICommandSender sender, String[] args) throws CommandException, NoAccessException {
		WorldBorder.instance.genenabled = !WorldBorder.instance.genenabled;
		MyTown.instance.config.get("worldborder", "chunk-generator-enabled", WorldBorder.instance.genenabled, "Generate blocks?").set(WorldBorder.instance.genenabled);
		MyTown.instance.config.save();
		MyTown.sendChatToPlayer(sender, String.format("§aWorld gen is now %s", WorldBorder.instance.genenabled ? "§2ENABLED" : "§4DISABLED"));
	}

	@Override
	public List<String> tabComplete(ICommandSender sender, String[] args) {
		return null;
	}

}