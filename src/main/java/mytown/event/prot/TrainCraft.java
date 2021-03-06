package mytown.event.prot;

import mytown.ChatChannel;
import mytown.Formatter;
import mytown.MyTown;
import mytown.MyTownDatasource;
import mytown.cmd.CmdChat;
import mytown.entities.TownBlock;
import mytown.event.ProtBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecart;

public class TrainCraft extends ProtBase {
	public static TrainCraft instance = new TrainCraft();

	Class<?> clEntityTracksBuilder;

	@Override
	public void load() throws Exception {
		clEntityTracksBuilder = Class.forName("src.train.common.entity.rollingStock.EntityTracksBuilder");
	}

	@Override
	public boolean loaded() {
		return clEntityTracksBuilder != null;
	}

	@Override
	public boolean isEntityInstance(Entity e) {
		return clEntityTracksBuilder.isInstance(e);
	}

	@Override
	public String update(Entity e) throws Exception {
		if ((int) e.posX == (int) e.prevPosX && (int) e.posY == (int) e.prevPosY && (int) e.posZ == (int) e.prevPosZ) {
			return null;
		}

		int radius = 3 + 1;
		int y = (int) e.posY + 2;

		if (!canRoam(e.dimension, e.posX - radius, y - radius, y + radius, e.posZ - radius) || !canRoam(e.dimension, e.posX - radius, y - radius, y + radius, e.posZ + radius) || !canRoam(e.dimension, e.posX + radius, y - radius, y + radius, e.posZ - radius)
				|| !canRoam(e.dimension, e.posX + radius, y - radius, y + radius, e.posZ + radius)) {
			blockAction((EntityMinecart) e);
			return null;
		}

		return null;
	}

	private boolean canRoam(int dim, double x, double yFrom, double yTo, double z) {
		TownBlock b = MyTownDatasource.instance.getPermBlockAtCoord(dim, (int) x, (int) yFrom, (int) yTo, (int) z);

		if (b == null || b.town() == null) {
			return MyTown.instance.getWorldWildSettings(dim).allowStevecartsMiners && MyTown.instance.getWorldWildSettings(dim).allowStevecartsRailers;
		}

		return b.settings.allowStevecartsMiners && b.settings.allowStevecartsRailers;
	}

	private void blockAction(EntityMinecart e) throws IllegalArgumentException, IllegalAccessException {
		dropMinecart(e);

		MyTown.instance.bypassLog.severe(String.format("§4Stopped a train found in %s @ dim %s, %s,%s,%s", e.dimension, (int) e.posX, (int) e.posY, (int) e.posZ));

		String msg = String.format("A train broke @ %s,%s,%s because it wasn't allowed there", (int) e.posX, (int) e.posY, (int) e.posZ);
		String formatted = Formatter.formatChatSystem(msg, ChatChannel.Local);
		CmdChat.sendChatToAround(e.dimension, e.posX, e.posY, e.posZ, formatted, null);
	}

	@Override
	public String getMod() {
		return "TrainCraft";
	}

	@Override
	public String getComment() {
		return "Town permission: allowStevecartsMiners & allowStevecartsRailers (yes, same)";
	}
}
