package mytown.event;

import ic2.api.event.LaserEvent;

import java.util.logging.Level;

import mytown.Formatter;
import mytown.MyTown;
import mytown.MyTownDatasource;
import mytown.Term;
import mytown.Utils;
import mytown.cmd.CmdChat;
import mytown.entities.ItemIdRange;
import mytown.entities.Resident;
import mytown.entities.Town;
import mytown.entities.TownBlock;
import mytown.entities.TownSettingCollection.Permissions;
import mytown.event.tick.WorldBorder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRail;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderEye;
import net.minecraft.item.ItemExpBottle;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemMinecart;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.event.EventPriority;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.minecart.MinecartCollisionEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import cpw.mods.fml.common.IPlayerTracker;

public class PlayerEvents implements IPlayerTracker {
	public static boolean disableAutoChatChannelUsage;
	public int explosionRadius = 6; // IC2 Mining Laser explosion radius

	@ForgeSubscribe(priority=EventPriority.HIGHEST)
	public void laserHitEntity(LaserEvent.LaserHitsEntityEvent event) {
		if (event.isCanceled())
			return;
		if (event.owner == null || !(event.owner instanceof EntityPlayer))
			return;
		Resident res = MyTownDatasource.instance.getOrMakeResident((EntityPlayer) event.owner);
		if (res == null)
			return;
		if (!res.canAttack(event.hitentity)) {
			MyTown.instance.bypassLog.severe("[IC2]Player %s tried to bypass at dim %d, %d,%d,%d using Mining Laser - Target in MyTown protected area", event.owner, event.world.provider.dimensionId, (int) event.lasershot.posX, (int) event.lasershot.posY, (int) event.lasershot.posZ);
			MyTown.sendChatToPlayer((EntityPlayer) event.owner, "§4You cannot use that here - Target in MyTown protected area");
			event.setCanceled(true);
		}
	}

	@ForgeSubscribe(priority=EventPriority.HIGHEST)
	public void laserHitBlock(LaserEvent.LaserHitsBlockEvent event) {
		if (event.isCanceled())
			return;
		if (event.owner == null || !(event.owner instanceof EntityPlayer))
			return;
		Resident res = MyTownDatasource.instance.getOrMakeResident((EntityPlayer) event.owner);
		if (res == null)
			return;
		if (!res.canInteract(event.world.provider.dimensionId, event.x, event.y, event.z, Permissions.Build)) {
			MyTown.instance.bypassLog.severe("[IC2]Player %s tried to bypass at dim %d, %d,%d,%d using Mining Laser - Target in MyTown protected area", event.owner, event.world.provider.dimensionId, (int) event.lasershot.posX, (int) event.lasershot.posY, (int) event.lasershot.posZ);
			MyTown.sendChatToPlayer((EntityPlayer) event.owner, "§4You cannot use that here - Target in MyTown protected area");
			event.setCanceled(true);
		}
	}

	@ForgeSubscribe(priority=EventPriority.HIGHEST)
	public void laserExplodes(LaserEvent.LaserExplodesEvent event) {
		if (event.isCanceled())
			return;
		if (event.owner == null || !(event.owner instanceof EntityPlayer))
			return;
		Resident res = MyTownDatasource.instance.getOrMakeResident((EntityPlayer) event.owner);
		if (res == null)
			return;

		int x = (int) event.lasershot.posX, y = (int) event.lasershot.posY, z = (int) event.lasershot.posZ;

		// Do a 4-corner check. Hopefully wont need much else to protect against
		// Mining Laser explosions
		if (!res.canInteract(event.world.provider.dimensionId, x, y, z, Permissions.Build) || !res.canInteract(event.world.provider.dimensionId, x - explosionRadius, y, z - explosionRadius, Permissions.Build)
				|| !res.canInteract(event.world.provider.dimensionId, x - explosionRadius, y, z + explosionRadius, Permissions.Build) || !res.canInteract(event.world.provider.dimensionId, x + explosionRadius, y, z - explosionRadius, Permissions.Build)
				|| !res.canInteract(event.world.provider.dimensionId, x + explosionRadius, y, z + explosionRadius, Permissions.Build)) {
			event.setCanceled(true);
			MyTown.instance.bypassLog.severe("[IC2]Player %s tried to bypass at dim %d, %d,%d,%d using Mining Laser - Explosion would hit a protected town", event.owner, event.world.provider.dimensionId, (int) event.lasershot.posX, (int) event.lasershot.posY, (int) event.lasershot.posZ);
			MyTown.sendChatToPlayer((EntityPlayer) event.owner, "§4You cannot use that here - Explosion would hit a protected town");
			return;
		}
	}

	@ForgeSubscribe(priority=EventPriority.HIGHEST)
	public void interact(PlayerInteractEvent ev) {
		if (ev.isCanceled())
			return;

		Resident r = source().getOrMakeResident(ev.entityPlayer);
		if (ev.action == Action.RIGHT_CLICK_AIR || ev.action == Action.RIGHT_CLICK_BLOCK) {
			if (r.pay.tryPayByHand()) {
				ev.setCanceled(true);
				r.onlinePlayer.stopUsingItem();
				return;
			}
		} else {
			r.pay.cancelPayment();
		}

		if (!ProtectionEvents.instance.itemUsed(r)) {
			ev.setCanceled(true);
			r.onlinePlayer.stopUsingItem();
			return;
		}
		Permissions perm = Permissions.Build;
		int x = ev.x, y = ev.y, z = ev.z;
		Action action = ev.action;

		if (action == Action.RIGHT_CLICK_AIR) // entity or air click
		{
			if (ev.entityPlayer.getHeldItem() != null && ev.entityPlayer.getHeldItem().getItem() != null) {
				Item item = ev.entityPlayer.getHeldItem().getItem();
				MovingObjectPosition pos = Utils.getMovingObjectPositionFromPlayer(r.onlinePlayer.worldObj, r.onlinePlayer, false);
				if (pos == null) {
					if (item instanceof ItemBow || item instanceof ItemEgg || item instanceof ItemPotion || item instanceof ItemFishingRod || item instanceof ItemExpBottle || item instanceof ItemEnderEye || item.getClass().getSimpleName().equalsIgnoreCase("ItemNanoBow")) {
						perm = Permissions.Build;
					} else {
						return;
					}

					x = (int) ev.entityPlayer.posX;
					y = (int) ev.entityPlayer.posY;
					z = (int) ev.entityPlayer.posZ;
				} else {
					action = Action.RIGHT_CLICK_BLOCK;
					if (pos.typeOfHit == EnumMovingObjectType.ENTITY) {
						x = (int) pos.entityHit.posX;
						y = (int) pos.entityHit.posY;
						z = (int) pos.entityHit.posZ;
					} else {
						x = pos.blockX;
						y = pos.blockY;
						z = pos.blockZ;
					}
				}
			} else {
				return;
			}
		}

		TownBlock targetBlock = MyTownDatasource.instance.getPermBlockAtCoord(ev.entityPlayer.dimension, x, y, z);

		if (action == Action.RIGHT_CLICK_BLOCK && ev.entityPlayer.getHeldItem() != null && ev.entityPlayer.getHeldItem().getItem() != null && (ev.entityPlayer.getHeldItem().getItem() instanceof ItemMinecart || ItemIdRange.contains(MyTown.instance.carts, ev.entityPlayer.getHeldItem()))) {
			int en = ev.entityPlayer.worldObj.getBlockId(x, y, z);
			if (Block.blocksList[en] instanceof BlockRail) {
				if (targetBlock != null && targetBlock.town() != null && targetBlock.settings.allowCartInteraction || (targetBlock == null || targetBlock.town() == null) && MyTown.instance.getWorldWildSettings(ev.entityPlayer.dimension).allowCartInteraction) {
					return;
				}
			}
		} else if (action == Action.RIGHT_CLICK_BLOCK) {
			if (!r.onlinePlayer.isSneaking()) {
				TileEntity te = r.onlinePlayer.worldObj.getBlockTileEntity(x, y, z);
				if (te != null && te instanceof IInventory){
					try{
						if (((IInventory) te).isUseableByPlayer(r.onlinePlayer)) {
							perm = Permissions.Access;
						}
					} catch(AbstractMethodError e){
						MyTown.instance.coreLog.warning("A tile entity (%s) didn't extend isUseableByPlayer correctly! Shame on the mod maker!", te.getClass().getName());
					}
				}
			}

			ItemStack itemstack = ev.entityPlayer.getHeldItem();
			Item item = itemstack == null ? null : itemstack.getItem();

			if (ev.entityPlayer.getHeldItem() == null) {
				perm = Permissions.Access;
			}

			// placing a block
			if (ev.face != -1 && perm == Permissions.Build && item != null && item instanceof ItemBlock) {
				if (ev.face == 0) {
					y--;
				} else if (ev.face == 1) {
					y++;
				} else if (ev.face == 2) {
					z--;
				} else if (ev.face == 3) {
					z++;
				} else if (ev.face == 4) {
					x--;
				} else if (ev.face == 5) {
					x++;
				}

				targetBlock = MyTownDatasource.instance.getPermBlockAtCoord(ev.entityPlayer.dimension, x, y, z);
			}
		}

		if (!r.canInteract(targetBlock, perm)) {
			// see if its a allowed block
			if (perm == Permissions.Build && action == Action.LEFT_CLICK_BLOCK) {
				World w = ev.entityPlayer.worldObj;
				if (ItemIdRange.contains(MyTown.instance.leftClickAccessBlocks, w.getBlockId(x, y, z), w.getBlockMetadata(x, y, z))) {
					perm = Permissions.Access;
					if (r.canInteract(targetBlock, perm)) {
						return;
					}
				}
			}

			r.onlinePlayer.stopUsingItem();
			ev.setCanceled(true);
			if (perm == Permissions.Access) {
				MyTown.sendChatToPlayer(ev.entityPlayer, Term.ErrPermCannotAccessHere.toString());
			} else {
				MyTown.sendChatToPlayer(ev.entityPlayer, Term.ErrPermCannotAccessHere.toString());
			}
		}
	}

	@ForgeSubscribe(priority=EventPriority.HIGHEST)
	public void pickup(EntityItemPickupEvent ev) {
		if (ev.isCanceled())
			return;

		Resident r = source().getOrMakeResident(ev.entityPlayer);

		if (!r.canInteract(ev.item)) {
			long time = System.currentTimeMillis();
			if (time > r.pickupWarningCooldown) {
				MyTown.sendChatToPlayer(ev.entityPlayer, Term.ErrPermCannotPickup.toString());
				r.pickupWarningCooldown = time + Resident.pickupSpamCooldown;
			}
			ev.setCanceled(true);
		}
	}

	@ForgeSubscribe(priority=EventPriority.HIGHEST)
	public void entityAttack(AttackEntityEvent ev) {
		if (ev.isCanceled())
			return;

		Resident attacker = source().getOrMakeResident(ev.entityPlayer);

		if (!attacker.canAttack(ev.target)) {
			MyTown.sendChatToPlayer(ev.entityPlayer, Term.ErrPermCannotAttack.toString());
			ev.setCanceled(true);
		}
	}

	@ForgeSubscribe(priority=EventPriority.HIGHEST)
	public void onLivingAttackEvent(LivingAttackEvent ev) {
		if (ev.isCanceled() || ev.entity != null)
			return;

		if (ev.entityLiving instanceof EntityPlayer) {
			Resident t = source().getOrMakeResident((EntityPlayer) ev.entityLiving);

			if (ev.source.getEntity() != null && !t.canBeAttackedBy(ev.source.getEntity()) || ev.source.getSourceOfDamage() != null && !t.canBeAttackedBy(ev.source.getSourceOfDamage())) {
				ev.setCanceled(true);
			}
		}

		Entity target = ev.entity;
		Resident attacker = null;

		if (ev.source.getEntity() != null && ev.source.getEntity() instanceof EntityPlayer) {
			attacker = source().getOrMakeResident((EntityPlayer) ev.source.getEntity());
		}

		if (ev.source.getSourceOfDamage() != null && ev.source.getSourceOfDamage() instanceof EntityPlayer) {
			attacker = source().getOrMakeResident((EntityPlayer) ev.source.getSourceOfDamage());
		}

		if (!attacker.isOnline()) {
			ev.setCanceled(true);
			return;
		}

		if (!attacker.canAttack(target)) {
			MyTown.sendChatToPlayer(attacker.onlinePlayer, Term.ErrPermCannotAttack.toString());
			ev.setCanceled(true);
			return;
		}
	}

	@ForgeSubscribe(priority=EventPriority.HIGHEST)
	public void entityInteract(EntityInteractEvent ev) {
		if (ev.isCanceled())
			return;

		Resident r = source().getOrMakeResident(ev.entityPlayer);

		if (!r.canInteract(ev.target)) {
			MyTown.sendChatToPlayer(ev.entityPlayer, Term.ErrPermCannotInteract.toString());
			ev.setCanceled(true);
		}
	}

	@ForgeSubscribe(priority=EventPriority.HIGHEST)
	public void minecartCollision(MinecartCollisionEvent ev) {
		if (!(ev.collider instanceof EntityPlayer))
			return;

		Resident r = source().getOrMakeResident((EntityPlayer) ev.collider);

		TownBlock t = source().getBlock(r.onlinePlayer.dimension, ev.minecart.chunkCoordX, ev.minecart.chunkCoordZ);

		if (t == null || t.town() == null || t.town() == r.town() || t.settings.allowCartInteraction) {
			return;
		}

		long time = System.currentTimeMillis();
		if (t.town().minecraftNotificationTime < time) {
			t.town().minecraftNotificationTime = time + Town.dontSendCartNotification;
			t.town().sendNotification(Level.WARNING, Term.MinecartMessedWith.toString());
		}
	}

	@ForgeSubscribe(priority=EventPriority.HIGHEST)
	public void entityEnterChunk(EntityEvent.EnteringChunk event) {
		if (event.isCanceled())
			return;
		if (event.entity == null || !(event.entity instanceof EntityPlayer))
			return;
		Resident res = MyTownDatasource.instance.getOrMakeResident((EntityPlayer) event.entity);
		if (res == null)
			return;
		res.checkLocation();
		if (res.mapMode) {
			res.sendLocationMap(res.onlinePlayer.dimension, res.onlinePlayer.chunkCoordX, res.onlinePlayer.chunkCoordZ);
		}
	}

	private MyTownDatasource source() {
		return MyTownDatasource.instance;
	}

	@Override
	public void onPlayerLogin(EntityPlayer player) {
		// load the resident
		Resident res = source().getOrMakeResident(player);

		if (!WorldBorder.instance.isWithinArea(player)) {
			MyTown.instance.coreLog.warning(String.format("Player %s logged in over the world edge %s (%s, %s, %s). Sending to spawn.", res.nick(), player.dimension, player.posX, player.posY, player.posZ));
			res.respawnPlayer();
		}

		TownBlock t = source().getBlock(res.onlinePlayer.dimension, player.chunkCoordX, player.chunkCoordZ);

		res.location = t != null && t.town() != null ? t.town() : null;
		res.location2 = t != null && t.town() != null ? t.owner() : null;

		if (!res.canInteract(t, (int) player.posY, Permissions.Enter)) {
			MyTown.instance.coreLog.warning(String.format("Player %s logged in at a enemy town %s (%s, %s, %s, %s) with bouncing on. Sending to spawn.", res.nick(), res.location.name(), player.dimension, player.posX, player.posY, player.posZ));
			res.respawnPlayer();
		}

		if (res.town() != null) {
			res.town().notifyPlayerLoggedOn(res);
		}

		res.loggedIn();
	}

	@Override
	public void onPlayerLogout(EntityPlayer player) {
		Resident res = source().getOrMakeResident(player);

		if (res.town() != null) {
			res.town().notifyPlayerLoggedOff(res);
		}

		res.loggedOff();
	}

	@Override
	public void onPlayerChangedDimension(EntityPlayer player) {
		if (player == null)
			return;
		Resident res = MyTownDatasource.instance.getOrMakeResident(player);
		res.prevDimension2 = res.prevDimension;
		res.prevDimension = player.dimension;
	}

	@Override
	public void onPlayerRespawn(EntityPlayer player) {
	}

	@ForgeSubscribe(priority=EventPriority.HIGHEST)
	public void serverChat(ServerChatEvent ev) {
		if (ev.isCanceled() || ev.message == null || ev.message.trim().length() < 1) {
			return;
		}
		if (!Formatter.formatChat) {
			MyTown.instance.chatLog.info("%s: %s", ev.player.getDisplayName(), ev.message);
			return;
		}
		if (!disableAutoChatChannelUsage) {
			ev.setCanceled(true);
			Resident res = source().getOrMakeResident(ev.player);
			CmdChat.sendToChannelFromDirectTalk(res, ev.message, res.activeChannel, false);
		}
	}

	@ForgeSubscribe(priority=EventPriority.HIGHEST)
	public void livingUpdate(LivingUpdateEvent ev) {
		if (ev.isCanceled() || !(ev.entityLiving instanceof EntityPlayer)) {
			return;
		}

		// so we don't re-link to player to be online
		// as this is called after the player logs off
		Resident res = source().getOrMakeResident((EntityPlayer) ev.entityLiving);

		if (res != null && res.isOnline()) {
			res.update();
		}
	}
}
