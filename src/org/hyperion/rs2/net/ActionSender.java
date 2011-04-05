package org.hyperion.rs2.net;

import org.hyperion.rs2.Constants;
import org.hyperion.rs2.model.Item;
import org.hyperion.rs2.model.Location;
import org.hyperion.rs2.model.Palette;
import org.hyperion.rs2.model.Palette.PaletteTile;
import org.hyperion.rs2.model.Player;
import org.hyperion.rs2.model.Skills;
import org.hyperion.rs2.model.container.Equipment;
import org.hyperion.rs2.model.container.Inventory;
import org.hyperion.rs2.model.container.impl.EquipmentContainerListener;
import org.hyperion.rs2.model.container.impl.InterfaceContainerListener;
import org.hyperion.rs2.model.container.impl.WeaponContainerListener;
import org.hyperion.rs2.net.Packet.Type;

/**
 * A utility class for sending packets.
 * 
 * @author Graham Edgecombe
 * 
 */
public class ActionSender {

	/**
	 * The player.
	 */
	private final Player player;

	/**
	 * Creates an action sender for the specified player.
	 * 
	 * @param player
	 *            The player to create the action sender for.
	 */
	public ActionSender(Player player) {
		this.player = player;
	}

	/**
	 * Sends all the login packets.
	 * 
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendLogin() {
		player.setActive(true);
		sendDetails();
		sendMessage("Welcome to RuneScape.");

		sendMapRegion();
		sendSidebarInterfaces();

		final InterfaceContainerListener inventoryListener = new InterfaceContainerListener(
				player, Inventory.INTERFACE);
		player.getInventory().addListener(inventoryListener);

		final InterfaceContainerListener equipmentListener = new InterfaceContainerListener(
				player, Equipment.INTERFACE);
		player.getEquipment().addListener(equipmentListener);
		player.getEquipment().addListener(
				new EquipmentContainerListener(player));
		player.getEquipment().addListener(new WeaponContainerListener(player));

		return this;
	}

	/**
	 * Sends a configuration to the client.
	 * 
	 * @param id
	 *            The configuration id.
	 * @param value
	 *            The configuration value.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendConfig(int id, int value) {
		final PacketBuilder bldr = new PacketBuilder(36);
		bldr.putLEShort(id);
		bldr.put((byte) value);
		player.getSession().write(bldr.toPacket());
		return this;
	}

	/**
	 * Toggles a configuration in the client.
	 * 
	 * @param id
	 *            The configuration in the client.
	 * @param state
	 *            The state to write.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendConfigToggle(int id, int state) {
		final PacketBuilder bldr = new PacketBuilder(87);
		bldr.putLEShort(id);
		bldr.putInt1(state);
		player.getSession().write(bldr.toPacket());
		return this;
	}

	/**
	 * Sends the packet to construct a map region.
	 * 
	 * @param palette
	 *            The palette of map regions.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendConstructMapRegion(Palette palette) {
		player.setLastKnownRegion(player.getLocation());
		final PacketBuilder bldr = new PacketBuilder(241, Type.VARIABLE_SHORT);
		bldr.putShortA(player.getLocation().getRegionY() + 6);
		bldr.startBitAccess();
		for (int z = 0; z < 4; z++) {
			for (int x = 0; x < 13; x++) {
				for (int y = 0; y < 13; y++) {
					final PaletteTile tile = palette.getTile(x, y, z);
					bldr.putBits(1, tile != null ? 1 : 0);
					if (tile != null) {
						bldr.putBits(26, tile.getX() << 14 | tile.getY() << 3
								| tile.getZ() << 24 | tile.getRotation() << 1);
					}
				}
			}
		}
		bldr.finishBitAccess();
		bldr.putShort(player.getLocation().getRegionX() + 6);
		player.write(bldr.toPacket());
		return this;
	}

	/**
	 * Sends the initial login packet (e.g. members, player id).
	 * 
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendDetails() {
		player.write(new PacketBuilder(249)
				.putByteA(player.isMembers() ? 1 : 0)
				.putLEShortA(player.getIndex()).toPacket());
		player.write(new PacketBuilder(107).toPacket());
		return this;
	}

	/**
	 * Sends the player's skills.
	 * 
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendSkills() {
		for (int i = 0; i < Skills.SKILL_COUNT; i++) {
			sendSkill(i);
		}
		return this;
	}

	/**
	 * Sends a specific skill.
	 * 
	 * @param skill
	 *            The skill to send.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendSkill(int skill) {
		final PacketBuilder bldr = new PacketBuilder(134);
		bldr.put((byte) skill);
		bldr.putInt1((int) player.getSkills().getExperience(skill));
		bldr.put((byte) player.getSkills().getLevel(skill));
		player.write(bldr.toPacket());
		return this;
	}

	/**
	 * Sends an inventory interface.
	 * 
	 * @param interfaceId
	 *            The interface id.
	 * @param inventoryInterfaceId
	 *            The inventory interface id.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendInterfaceInventory(int interfaceId,
			int inventoryInterfaceId) {
		player.getInterfaceState().interfaceOpened(interfaceId);
		player.write(new PacketBuilder(248).putShortA(interfaceId)
				.putShort(inventoryInterfaceId).toPacket());
		return this;
	}

	/**
	 * Sends all the sidebar interfaces.
	 * 
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendSidebarInterfaces() {
		final int[] icons = Constants.SIDEBAR_INTERFACES[0];
		final int[] interfaces = Constants.SIDEBAR_INTERFACES[1];
		for (int i = 0; i < icons.length; i++) {
			sendSidebarInterface(icons[i], interfaces[i]);
		}
		return this;
	}

	/**
	 * Sends a specific sidebar interface.
	 * 
	 * @param icon
	 *            The sidebar icon.
	 * @param interfaceId
	 *            The interface id.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendSidebarInterface(int icon, int interfaceId) {
		player.write(new PacketBuilder(71).putShort(interfaceId).putByteA(icon)
				.toPacket());
		return this;
	}

	/**
	 * Sends a message.
	 * 
	 * @param message
	 *            The message to send.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendMessage(String message) {
		player.write(new PacketBuilder(253, Type.VARIABLE)
				.putRS2String(message).toPacket());
		return this;
	}

	/**
	 * Sends the chat privacy settings.
	 * 
	 * @param pub
	 *            Public chat setting.
	 * @param pri
	 *            Private chat setting.
	 * @param trade
	 *            Trade setting.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendChatSettings(int pub, int pri, int trade) {
		player.write(new PacketBuilder(50).put((byte) pub).put((byte) pri)
				.put((byte) trade).toPacket());
		return this;
	}

	/**
	 * Sends the map region load command.
	 * 
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendMapRegion() {
		player.setLastKnownRegion(player.getLocation());
		player.write(new PacketBuilder(73)
				.putShortA(player.getLocation().getRegionX() + 6)
				.putShort(player.getLocation().getRegionY() + 6).toPacket());
		return this;
	}

	/**
	 * Sends the logout packet.
	 * 
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendLogout() {
		player.write(new PacketBuilder(109).toPacket()); // TODO IoFuture
		return this;
	}

	/**
	 * Sends a packet to update a group of items.
	 * 
	 * @param interfaceId
	 *            The interface id.
	 * @param items
	 *            The items.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendUpdateItems(int interfaceId, Item[] items) {
		final PacketBuilder bldr = new PacketBuilder(53, Type.VARIABLE_SHORT);
		bldr.putShort(interfaceId);
		bldr.putShort(items.length);
		for (final Item item : items) {
			if (item != null) {
				final int count = item.getCount();
				if (count > 254) {
					bldr.put((byte) 255);
					bldr.putInt2(count);
				} else {
					bldr.put((byte) count);
				}
				bldr.putLEShortA(item.getId() + 1);
			} else {
				bldr.put((byte) 0);
				bldr.putLEShortA(0);
			}
		}
		player.write(bldr.toPacket());
		return this;
	}

	/**
	 * Sends a packet to update a single item.
	 * 
	 * @param interfaceId
	 *            The interface id.
	 * @param slot
	 *            The slot.
	 * @param item
	 *            The item.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendUpdateItem(int interfaceId, int slot, Item item) {
		final PacketBuilder bldr = new PacketBuilder(34, Type.VARIABLE_SHORT);
		bldr.putShort(interfaceId).putSmart(slot);
		if (item != null) {
			bldr.putShort(item.getId() + 1);
			final int count = item.getCount();
			if (count > 254) {
				bldr.put((byte) 255);
				bldr.putInt(count);
			} else {
				bldr.put((byte) count);
			}
		} else {
			bldr.putShort(0);
			bldr.put((byte) 0);
		}
		player.write(bldr.toPacket());
		return this;
	}

	/**
	 * Sends a packet to update multiple (but not all) items.
	 * 
	 * @param interfaceId
	 *            The interface id.
	 * @param slots
	 *            The slots.
	 * @param items
	 *            The item array.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendUpdateItems(int interfaceId, int[] slots,
			Item[] items) {
		final PacketBuilder bldr = new PacketBuilder(34, Type.VARIABLE_SHORT)
				.putShort(interfaceId);
		for (final int slot : slots) {
			final Item item = items[slot];
			bldr.putSmart(slot);
			if (item != null) {
				bldr.putShort(item.getId() + 1);
				final int count = item.getCount();
				if (count > 254) {
					bldr.put((byte) 255);
					bldr.putInt(count);
				} else {
					bldr.put((byte) count);
				}
			} else {
				bldr.putShort(0);
				bldr.put((byte) 0);
			}
		}
		player.write(bldr.toPacket());
		return this;
	}

	/**
	 * Sends the enter amount interface.
	 * 
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendEnterAmountInterface() {
		player.write(new PacketBuilder(27).toPacket());
		return this;
	}

	/**
	 * Sends the player an option.
	 * 
	 * @param slot
	 *            The slot to place the option in the menu.
	 * @param top
	 *            Flag which indicates the item should be placed at the top.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendInteractionOption(String option, int slot,
			boolean top) {
		final PacketBuilder bldr = new PacketBuilder(104, Type.VARIABLE);
		bldr.put((byte) -slot);
		bldr.putByteA(top ? (byte) 0 : (byte) 1);
		bldr.putRS2String(option);
		player.write(bldr.toPacket());
		return this;
	}

	/**
	 * Sends a string.
	 * 
	 * @param id
	 *            The interface id.
	 * @param string
	 *            The string.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendInterfaceString(int id, String string) {
		final PacketBuilder bldr = new PacketBuilder(126, Type.VARIABLE_SHORT);
		bldr.putRS2String(string);
		bldr.putShortA(id);
		player.write(bldr.toPacket());
		return this;
	}

	/**
	 * Sends a model in an interface.
	 * 
	 * @param id
	 *            The interface id.
	 * @param zoom
	 *            The zoom.
	 * @param model
	 *            The model id.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendInterfaceModel(int id, int zoom, int model) {
		final PacketBuilder bldr = new PacketBuilder(246);
		bldr.putLEShort(id).putShort(zoom).putShort(model);
		player.write(bldr.toPacket());
		return this;
	}

	/**
	 * Sends a interface over the chatbox.
	 * 
	 * @param id
	 *            The interface id.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendChatboxInterface(int id) {
		player.write(new PacketBuilder(164).putLEShort(id).toPacket());
		return this;
	}

	/**
	 * Sends the interface color
	 * 
	 * @param id
	 *            The interface id.
	 * @param color
	 *            The color id.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendInterfaceColor(int id, int color) {
		player.write(new PacketBuilder(122).putLEShortA(id).putLEShortA(color)
				.toPacket());
		return this;
	}

	/**
	 * Sends a interface
	 * 
	 * @param id
	 *            The interface id.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendInterface(int id) {
		player.write(new PacketBuilder(97).putShort(id).toPacket());
		return this;
	}

	/**
	 * Sends an interface to display in walkable mode.
	 * 
	 * @param id
	 *            The interface id.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendWalkableInterface(int id) {
		player.write(new PacketBuilder(208).putShort(id).toPacket());
		return this;
	}

	/**
	 * Removes all open interfaces from the players screen.
	 * 
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendClearScreen() {
		player.write(new PacketBuilder(219).toPacket());
		return this;
	}

	/**
	 * Send a system update
	 * 
	 * @param time
	 *            The time in seconds
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendSystemUpdate(int time) {
		player.write(new PacketBuilder(114).putLEShort(time).toPacket());
		return this;
	}

	/**
	 * Sends the minimap state
	 * 
	 * @param state
	 *            The state (0 = normal, 1 = not clickable, 2 = blackout)
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendMinimapState(int state) {
		player.write(new PacketBuilder(99).put((byte) state).toPacket());
		return this;
	}

	/**
	 * Sends a flashing sidebar
	 * 
	 * @param sidebar
	 *            The sidebar id.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendFlashingSidebar(int sidebar) {
		player.write(new PacketBuilder(24).putByteA(sidebar).toPacket());
		return this;
	}

	/**
	 * Sets the scrollbar position of an interface.
	 * 
	 * @param id
	 *            The interface id.
	 * @param position
	 *            The position of the scrollbar.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendScrollPosition(int id, int position) {
		player.write(new PacketBuilder(79).putLEShortA(id).putShortA(position)
				.toPacket());
		return this;
	}

	/**
	 * Resets the game's camera position to the client default.
	 * 
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendCameraReset() {
		player.write(new PacketBuilder(107).toPacket());
		return this;
	}

	/**
	 * Makes the camera shake for the player
	 * 
	 * @param intensity
	 *            The intensity of the shake
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendCameraShake(int intensity) {
		player.write(new PacketBuilder(35).put((byte) 0).put((byte) intensity)
				.put((byte) intensity).put((byte) intensity).toPacket());
		return this;
	}

	/**
	 * Sends the characters weight
	 * 
	 * @param weight
	 *            The amount of weight
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendWeight(int weight) {
		player.write(new PacketBuilder(240).putShort(weight).toPacket());
		return this;
	}

	/**
	 * Sends the characters energy level
	 * 
	 * @param energy
	 *            The current energy level
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendRunEnergy(int energy) {
		player.write(new PacketBuilder(110).put((byte) (energy & 0xff))
				.toPacket());
		return this;
	}

	/**
	 * Sends the welcome screen
	 * 
	 * @param recovery
	 *            Days since last recovery change (200 for not yet set, 201 for
	 *            members server).
	 * @param messages
	 *            Number of unread messages.
	 * @param warning
	 *            Member warning (1 for member, 0 for non-member).
	 * @param ip
	 *            Last logged IP.
	 * @param last
	 *            Last logged successful login.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendWelcomeScreen(int recovery, int messages,
			int warning, int ip, int last) {
		final PacketBuilder bldr = new PacketBuilder(176);
		bldr.put((byte) recovery).putShortA(messages).put((byte) warning)
				.putInt2(ip).putShort(last);
		player.write(bldr.toPacket());
		return this;
	}

	/**
	 * Sends a private message to a player
	 * 
	 * @param name
	 *            The name of player
	 * @param messageIndex
	 *            The message index
	 * @param rights
	 *            The player's right
	 * @param message
	 *            The message
	 * @param messageSize
	 *            The message size
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendPrivateMessage(long name, int messageIndex,
			int rights, byte[] message, int messageSize) {
		player.write(new PacketBuilder(196, Type.VARIABLE).putLong(name)
				.putInt(messageIndex).put((byte) rights)
				.put(message, 0, messageSize).toPacket());
		return this;
	}

	/**
	 * Friend server friends list load status. Loading = 0 Connecting = 1 OK = 2
	 * 
	 * @param status
	 *            Value to set.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendFriendServer(int status) {
		player.write(new PacketBuilder(221).put((byte) status).toPacket());
		return this;
	}

	/**
	 * Sends a friend to the friend list.
	 * 
	 * @param name
	 *            The name (encoded as a long).
	 * @param world
	 *            The world id.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendFriend(long name, int world) {
		player.write(new PacketBuilder(50).putLong(name).put((byte) world)
				.toPacket());
		return this;
	}

	/**
	 * Sends the hashed version of all ignored player's names.
	 * 
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendInitializeIgnoreList() {
		player.write(new PacketBuilder(214, Type.VARIABLE_SHORT).toPacket());
		return this;
	}

	/**
	 * Sends the player membership flag and player list index.
	 * 
	 * @param membership
	 *            Membership flag (1 = member, 0 = free).
	 * @param index
	 *            Player list index.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendInitializePlayer(int membership, int index) {
		player.write(new PacketBuilder(50).putByteA(membership)
				.putLEShortA(index).toPacket());
		return this;
	}

	/**
	 * Resets all animations in the immediate area.
	 * 
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendAnimationReset() {
		player.write(new PacketBuilder(1).toPacket());
		return this;
	}

	/**
	 * Sends coordinates of a position.
	 * 
	 * @param location
	 *            The location to find the coordinates.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendCoords(Location location) {
		final PacketBuilder bldr = new PacketBuilder(97);
		final int regionX = player.getLastKnownRegion().getRegionX(), regionY = player
				.getLastKnownRegion().getRegionY();
		bldr.putByteS((byte) (location.getY() - (regionY - 6) * 8));
		bldr.putByteC((byte) (location.getX() - (regionX - 6) * 8));
		player.getSession().write(bldr.toPacket());
		return this;
	}

	/**
	 * Removes a object from a position.
	 * 
	 * @param objectType
	 *            The object type.
	 * @param objectFace
	 *            The facing of the object.
	 * @param position
	 *            The position of the object.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendRemoveObject(int objectType, int objectFace,
			Location position) {
		sendCoords(position);
		final PacketBuilder bldr2 = new PacketBuilder(64);
		bldr2.putByteC((objectType << 2) + (objectFace & 3)).put((byte) 0);
		player.getSession().write(bldr2.toPacket());
		return this;
	}

	/**
	 * Add a object to a position.
	 * 
	 * @param objectType
	 *            The object type.
	 * @param objectFace
	 *            The facing of the object.
	 * @param position
	 *            The position of the object.
	 * @return The action sender instance, for chaining.
	 */
	public ActionSender sendCreateObject(int objectId, int objectType,
			int objectFace, Location position) {
		sendCoords(position);
		final PacketBuilder bldr = new PacketBuilder(236);
		bldr.putLEShortA(objectId).putByteC(0)
				.putByteS((byte) ((objectType << 2) + (objectFace & 3)));
		player.getSession().write(bldr.toPacket());
		return this;
	}

}
