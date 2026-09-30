package com.coxteamutilities;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.GameState;
import net.runelite.api.InstanceTemplates;
import net.runelite.api.Item;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.Notifier;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.events.UserJoin;
import net.runelite.client.party.events.UserPart;
import net.runelite.client.party.messages.UserSync;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "CoX Team Utilities",
	description = "Pick your Chambers of Xeric CM roles, get reminded of missing role items, track potions and claim drops with your party",
	tags = {"cox", "chambers", "xeric", "raids", "cm", "challenge mode", "party", "roles", "storage", "overload", "team"}
)
public class CoxTeamUtilitiesPlugin extends Plugin implements CoxTeamPanel.Actions
{
	// keep in sync with build.gradle
	public static final String VERSION = "1.0.0";

	/** The raid lobby on Mount Quidamortem. */
	private static final int LOBBY_REGION = 4919;
	/** The Great Olm's chamber. */
	private static final int OLM_REGION = 12889;
	/** Ticks outside before a raid counts as left, so a relog or a reload doesn't wipe the raid's state. */
	private static final int LEAVE_TICKS = 5;
	/** Least ticks between two status messages to the party. */
	private static final int SEND_INTERVAL = 5;

	private static final int[] POUCH_TYPES = {
		VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2, VarbitID.RUNE_POUCH_TYPE_3,
		VarbitID.RUNE_POUCH_TYPE_4, VarbitID.RUNE_POUCH_TYPE_5, VarbitID.RUNE_POUCH_TYPE_6
	};
	private static final int[] POUCH_QUANTITIES = {
		VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2, VarbitID.RUNE_POUCH_QUANTITY_3,
		VarbitID.RUNE_POUCH_QUANTITY_4, VarbitID.RUNE_POUCH_QUANTITY_5, VarbitID.RUNE_POUCH_QUANTITY_6
	};
	private static final int[] POUCHES = {
		ItemIds.RUNE_POUCH, ItemIds.RUNE_POUCH_L, ItemIds.DIVINE_RUNE_POUCH, ItemIds.DIVINE_RUNE_POUCH_L
	};

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private CoxTeamUtilitiesConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ItemManager itemManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MissingItemsOverlay overlay;

	@Inject
	private ChestOverlay chestOverlay;

	@Inject
	private ChestItemOverlay chestItemOverlay;

	@Inject
	private Gson gson;

	@Inject
	private PartyService party;

	@Inject
	private WSClient wsClient;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private Notifier notifier;

	private CoxTeamPanel panel;
	private NavigationButton navigationButton;

	private final DropPlan plan = new DropPlan();
	private volatile ChestBook chests = new ChestBook();
	/** Names of the item in each inventory slot, null for empty, for the chest plans. */
	/** Item name to quantity in the inventory. */
	private Map<String, Integer> inventoryItems = Collections.emptyMap();
	/** The inventory when the storage was opened, so a "put in, N" line knows how many went in. */
	private Map<String, Integer> openedWith = Collections.emptyMap();
	/** Key of the chest for the room the player is in, null outside a room with one. */
	private volatile String currentChest;
	/** Progress at the chest whose storage is open, for the overlays. */
	private volatile ChestProgress openChest;
	/** Rooms seen this raid: room slot to chest key, to tell the two farming rooms apart. */
	private final Map<String, String> roomKeys = new LinkedHashMap<>();
	private String lastRoomSlot;
	private final Map<Integer, String> itemNames = new HashMap<>();
	/** While on, left-clicking an item in a storage or the inventory adds it to the chest's lists. */
	private volatile boolean marking;
	/** The chest picked in the sidebar, marked from the inventory when no storage is open. */
	private volatile String selectedChest;
	private volatile Needs needs = Needs.defaults();
	private volatile Needs needsSolo = Needs.soloDefaults();
	/** Whether the sidebar shows and edits the solo doses outside a raid. */
	private volatile boolean needsTabSolo;

	/** Guards everything below that the Swing, client and party threads share. */
	private final Object lock = new Object();
	private final Set<Role> roles = EnumSet.noneOf(Role.class);
	private final ClaimBook claims = new ClaimBook();
	private final Map<Long, MemberStatus> members = new HashMap<>();
	private Map<Role, List<String>> missing = new EnumMap<>(Role.class);
	private Supplies inventory = Supplies.EMPTY;
	private Supplies privateStorage;
	private Supplies sharedStorage;

	// client thread only
	/** What's in the private storage, by item id, as last seen or worked out from deposits. */
	private final Map<Integer, Integer> privateItems = new HashMap<>();
	/** Item counts in the inventory at the last inventory change, to see what a deposit moved. */
	private final Map<Integer, Integer> lastInventory = new HashMap<>();
	/** The storage interface that is open (its group id), 0 for none. */
	private int openStorage;
	/** The storage interface that just closed, and the last tick a deposit still counts for it. */
	private int closedStorage;
	private int closedStorageUntil;
	private boolean inRaid;
	private boolean soloRaid;
	private boolean atOlm;
	private int ticksOutside;
	private int ticksSinceSend = SEND_INTERVAL;
	private CoxStatusMessage lastSent;

	private volatile boolean loadoutDirty;
	private volatile boolean resendStatus;
	private volatile boolean sendPlan;
	private volatile boolean showOverlay;
	private volatile boolean iron;

	@Provides
	CoxTeamUtilitiesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CoxTeamUtilitiesConfig.class);
	}

	@Override
	protected void startUp()
	{
		synchronized (lock)
		{
			roles.addAll(Role.parse(split(config.roles())));
		}
		plan.merge(split(config.plan()));
		needs = Needs.parse(config.needs(), Needs.defaults());
		needsSolo = Needs.parse(config.needsSolo(), Needs.soloDefaults());
		needsTabSolo = config.needsTabSolo();

		panel = new CoxTeamPanel(this, (label, itemId) -> itemManager.getImage(itemId).addTo(label));
		navigationButton = NavigationButton.builder()
			.tooltip("CoX Team Utilities")
			.icon(icon())
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		overlayManager.add(overlay);
		overlayManager.add(chestOverlay);
		overlayManager.add(chestItemOverlay);
		chests = ChestBook.parse(config.chests(), gson);

		wsClient.registerMessage(CoxStatusMessage.class);
		wsClient.registerMessage(CoxPlanMessage.class);

		loadoutDirty = true;
		resendStatus = true;
		sendPlan = true;
		refresh();
	}

	@Override
	protected void shutDown()
	{
		wsClient.unregisterMessage(CoxStatusMessage.class);
		wsClient.unregisterMessage(CoxPlanMessage.class);
		overlayManager.remove(overlay);
		overlayManager.remove(chestOverlay);
		overlayManager.remove(chestItemOverlay);
		roomKeys.clear();
		lastRoomSlot = null;
		currentChest = null;
		openChest = null;
		itemNames.clear();
		marking = false;
		clientToolbar.removeNavigation(navigationButton);
		overlay.setLines(Collections.emptyList());
		panel = null;
		navigationButton = null;

		synchronized (lock)
		{
			roles.clear();
			claims.clear();
			members.clear();
			missing = new EnumMap<>(Role.class);
			inventory = Supplies.EMPTY;
			privateStorage = null;
			sharedStorage = null;
		}
		plan.clear();
		privateItems.clear();
		lastInventory.clear();
		openStorage = 0;
		inRaid = false;
		lastSent = null;
	}

	private static BufferedImage icon()
	{
		BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		// three heads over a vial
		g.setColor(new Color(220, 138, 0));
		g.fillOval(2, 3, 6, 6);
		g.fillOval(16, 3, 6, 6);
		g.setColor(new Color(255, 190, 70));
		g.fillOval(8, 1, 8, 8);
		g.setColor(new Color(120, 200, 230));
		g.fillRoundRect(7, 12, 10, 11, 4, 4);
		g.setColor(new Color(60, 120, 160));
		g.fillRect(9, 10, 6, 3);
		g.dispose();
		return image;
	}

	private static List<String> split(String csv)
	{
		if (csv == null || csv.isEmpty())
		{
			return Collections.emptyList();
		}
		return Arrays.asList(csv.split(","));
	}

	// ---- game state ----

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		switch (event.getContainerId())
		{
			case InventoryID.INV:
				inventoryChanged(event.getItemContainer().getItems());
				loadoutDirty = true;
				break;
			case InventoryID.WORN:
				loadoutDirty = true;
				break;
			case InventoryID.RAIDS_PRIVATESTORAGE:
				if (inRaid)
				{
					privateItems.clear();
					for (Item item : event.getItemContainer().getItems())
					{
						if (item.getId() > 0)
						{
							privateItems.merge(item.getId(), item.getQuantity(), Integer::sum);
						}
					}
					publishPrivateStorage();
					updateOpenChest();
				}
				break;
			case InventoryID.RAIDS_SHAREDSTORAGE:
				if (inRaid)
				{
					Supplies stored = count(event.getItemContainer().getItems());
					synchronized (lock)
					{
						sharedStorage = stored;
					}
					loadoutDirty = true;
					updateOpenChest();
				}
				break;
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.RAIDS_STORAGE_PRIVATE || event.getGroupId() == InterfaceID.RAIDS_STORAGE_SHARED)
		{
			openStorage = event.getGroupId();
			synchronized (lock)
			{
				openedWith = inventoryItems;
			}
			String key = currentChest;
			if (key != null && chests.get(key) == null && chests.getOrCreate(key, chestName(key)) != null)
			{
				saveChests();
			}
			updateOpenChest();
			refresh();
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == openStorage)
		{
			// a deposit made as the interface closes still shows up in the inventory this tick or the next
			closedStorage = openStorage;
			closedStorageUntil = client.getTickCount() + 1;
			openStorage = 0;
			openChest = null;
		}
	}

	/** Puts "Mark" on top of an item's menu while marking, so a left click adds it to the chest's list. */
	@Subscribe
	public void onPostMenuSort(PostMenuSort event)
	{
		if (!marking)
		{
			return;
		}
		MenuEntry[] entries = client.getMenu().getMenuEntries();
		for (MenuEntry entry : entries)
		{
			int itemId = entry.getItemId();
			if (itemId <= 0 || entry.getType() == MenuAction.RUNELITE)
			{
				continue;
			}
			int group = entry.getParam1() >> 16;
			boolean deposit;
			String key;
			if (group == InterfaceID.RAIDS_STORAGE_PRIVATE || group == InterfaceID.RAIDS_STORAGE_SHARED)
			{
				deposit = false;
				key = currentChest;
			}
			else if (group == InterfaceID.RAIDS_STORAGE_SIDE)
			{
				deposit = true;
				key = currentChest;
			}
			else if (group == InterfaceID.INVENTORY && openStorage == 0)
			{
				deposit = true;
				key = selectedChest;
			}
			else
			{
				continue;
			}
			if (key == null)
			{
				return;
			}
			String target = entry.getTarget();
			String list = deposit ? "put in" : "take out";
			client.getMenu().createMenuEntry(-1)
				.setOption("Unmark " + list)
				.setTarget(target)
				.setType(MenuAction.RUNELITE)
				.onClick(e -> markItem(key, deposit, itemId, -1));
			client.getMenu().createMenuEntry(-1)
				.setOption("Mark " + list)
				.setTarget(target)
				.setType(MenuAction.RUNELITE)
				.onClick(e -> markItem(key, deposit, itemId, 1));
			return;
		}
	}

	private void markItem(String key, boolean deposit, int itemId, int delta)
	{
		ChestPlan plan = chests.getOrCreate(key, chestName(key));
		if (plan != null && ChestPlan.mark(deposit ? plan.getDeposit() : plan.getWithdraw(), itemName(itemId), delta))
		{
			saveChests();
			updateOpenChest();
			refresh();
		}
	}

	/**
	 * The game only sends a storage's contents while its interface is open, so a deposit made as it
	 * closes never arrives. What left the inventory went into the storage, so it's added here.
	 */
	private void inventoryChanged(Item[] items)
	{
		Map<Integer, Integer> now = new HashMap<>();
		for (Item item : items)
		{
			if (item.getId() > 0)
			{
				now.merge(item.getId(), item.getQuantity(), Integer::sum);
			}
		}
		int storage = openStorage != 0 ? openStorage : client.getTickCount() <= closedStorageUntil ? closedStorage : 0;
		if (storage != 0 && inRaid && !lastInventory.isEmpty())
		{
			Map<Integer, Integer> moved = new HashMap<>();
			for (Map.Entry<Integer, Integer> e : lastInventory.entrySet())
			{
				int delta = e.getValue() - now.getOrDefault(e.getKey(), 0);
				if (delta != 0)
				{
					moved.put(e.getKey(), delta);
				}
			}
			for (Map.Entry<Integer, Integer> e : now.entrySet())
			{
				if (!lastInventory.containsKey(e.getKey()))
				{
					moved.put(e.getKey(), -e.getValue());
				}
			}
			if (!moved.isEmpty())
			{
				storageChanged(storage, moved);
			}
		}
		lastInventory.clear();
		lastInventory.putAll(now);
	}

	/** @param moved item id to the count that went into the storage, negative for what came out */
	private void storageChanged(int storage, Map<Integer, Integer> moved)
	{
		if (storage == InterfaceID.RAIDS_STORAGE_PRIVATE)
		{
			for (Map.Entry<Integer, Integer> e : moved.entrySet())
			{
				int quantity = privateItems.getOrDefault(e.getKey(), 0) + e.getValue();
				if (quantity > 0)
				{
					privateItems.put(e.getKey(), quantity);
				}
				else
				{
					privateItems.remove(e.getKey());
				}
			}
			publishPrivateStorage();
		}
		else
		{
			Supplies shared;
			synchronized (lock)
			{
				shared = sharedStorage;
			}
			if (shared == null)
			{
				return;
			}
			int[] doses = shared.toArray();
			for (Map.Entry<Integer, Integer> e : moved.entrySet())
			{
				Potion potion = Potion.of(e.getKey());
				if (potion != null)
				{
					doses[potion.ordinal()] += potion.doses(e.getKey()) * e.getValue();
				}
			}
			Supplies changed = Supplies.of(doses);
			synchronized (lock)
			{
				sharedStorage = changed;
			}
			loadoutDirty = true;
		}
	}

	private void publishPrivateStorage()
	{
		Supplies stored = count(privateItems);
		synchronized (lock)
		{
			privateStorage = stored;
		}
		loadoutDirty = true;
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int id = event.getVarbitId();
		if (id == VarbitID.SPELLBOOK)
		{
			loadoutDirty = true;
			return;
		}
		for (int i = 0; i < POUCH_TYPES.length; i++)
		{
			if (id == POUCH_TYPES[i] || id == POUCH_QUANTITIES[i])
			{
				loadoutDirty = true;
				return;
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		boolean changed = false;
		boolean entered = false;

		if (client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) == 1)
		{
			ticksOutside = 0;
			if (!inRaid)
			{
				inRaid = true;
				entered = true;
				// what the client still holds is from an earlier raid until the storage is opened
				loadoutDirty = true;
			}
			boolean soloNow = client.getVarbitValue(VarbitID.RAIDS_CLIENT_PARTYSIZE) <= 1;
			if (soloNow != soloRaid)
			{
				soloRaid = soloNow;
				changed = true;
			}
			trackRoom();
			boolean olmNow = region() == OLM_REGION;
			if (olmNow && !atOlm && config.olmReminder())
			{
				remindAtOlm();
			}
			atOlm = olmNow;
		}
		else if (inRaid && ++ticksOutside >= LEAVE_TICKS)
		{
			inRaid = false;
			leftRaid();
			changed = true;
		}

		boolean ironNow = client.getVarbitValue(VarbitID.IRONMAN) != 0;
		if (ironNow != iron)
		{
			iron = ironNow;
			changed = true;
		}

		boolean visible = overlayVisible();
		if (visible != showOverlay)
		{
			showOverlay = visible;
			changed = true;
		}

		if (loadoutDirty)
		{
			loadoutDirty = false;
			recompute();
			changed = true;
		}
		if (entered)
		{
			remindOnEntry();
		}
		if (changed)
		{
			refresh();
		}
		sendToParty();
	}

	private boolean overlayVisible()
	{
		if (!config.overlay() || !remindersOn())
		{
			return false;
		}
		if (inRaid)
		{
			return config.overlayDuringRaid() || client.getVarbitValue(VarbitID.RAIDS_CLIENT_PROGRESS) == 0;
		}
		return region() == LOBBY_REGION;
	}

	/** Role reminders are for teams unless asked for in solos too. Party size is 0 or 1 when alone. */
	private boolean remindersOn()
	{
		return config.remindSolo() || client.getVarbitValue(VarbitID.RAIDS_CLIENT_PARTYSIZE) > 1;
	}

	/**
	 * Works out which chest the player is at. A room is its template (RAIDS_FARMING, RAIDS_ICE_DEMON...)
	 * and the two rooms of the same template are told apart by which came first this raid, which
	 * is fixed in Challenge Mode. Room slots are 32x32 tile squares of the instance; walking within
	 * a room that straddles two squares keeps the same key.
	 */
	private void trackRoom()
	{
		Player player = client.getLocalPlayer();
		WorldView view = client.getTopLevelWorldView();
		if (player == null || view == null || !view.isInstance())
		{
			currentChest = null;
			return;
		}
		LocalPoint local = player.getLocalLocation();
		int[][][] chunks = view.getInstanceTemplateChunks();
		int plane = view.getPlane();
		int chunkX = local.getSceneX() / 8;
		int chunkY = local.getSceneY() / 8;
		if (chunks == null || plane >= chunks.length || chunkX < 0 || chunkX >= chunks[plane].length
			|| chunkY < 0 || chunkY >= chunks[plane][chunkX].length)
		{
			currentChest = null;
			return;
		}
		InstanceTemplates template = InstanceTemplates.findMatch(chunks[plane][chunkX][chunkY]);
		if (template == null || !template.name().startsWith("RAIDS_") || template == InstanceTemplates.RAIDS_LOBBY)
		{
			currentChest = null;
			return;
		}
		WorldPoint world = player.getWorldLocation();
		int slotX = Math.floorDiv(world.getX(), 32);
		int slotY = Math.floorDiv(world.getY(), 32);
		String slot = template.name() + ":" + slotX + ":" + slotY + ":" + world.getPlane();
		String key = roomKeys.get(slot);
		if (key == null)
		{
			// the same room type next door is the same room across a square's edge
			if (lastRoomSlot != null && lastRoomSlot.startsWith(template.name() + ":"))
			{
				String[] parts = lastRoomSlot.split(":");
				if (Math.abs(Integer.parseInt(parts[1]) - slotX) <= 1 && Math.abs(Integer.parseInt(parts[2]) - slotY) <= 1)
				{
					key = roomKeys.get(lastRoomSlot);
				}
			}
			if (key == null)
			{
				int n = 1;
				for (String seen : new HashSet<>(roomKeys.values()))
				{
					if (seen.startsWith(template.name() + "#"))
					{
						n++;
					}
				}
				key = template.name() + "#" + n;
			}
			roomKeys.put(slot, key);
		}
		lastRoomSlot = slot;
		if (!key.equals(currentChest))
		{
			currentChest = key;
			refresh();
		}
	}

	/** "Farming 2", "Ice Demon" from a key like RAIDS_FARMING#2. */
	static String chestName(String key)
	{
		String[] parts = key.substring("RAIDS_".length()).split("#");
		String[] words = parts[0].toLowerCase(Locale.ROOT).split("_");
		StringBuilder name = new StringBuilder();
		for (String word : words)
		{
			name.append(name.length() == 0 ? "" : " ").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		if (name.toString().endsWith("2"))
		{
			name.setLength(name.length() - 1);
		}
		String number = parts.length > 1 ? parts[1] : "1";
		boolean several = key.startsWith("RAIDS_FARMING") || key.startsWith("RAIDS_SCAVENGERS") || !number.equals("1");
		return several ? name + " " + number : name.toString();
	}

	private void saveChests()
	{
		configManager.setConfiguration(CoxTeamUtilitiesConfig.GROUP, CoxTeamUtilitiesConfig.KEY_CHESTS, chests.encode(gson));
	}

	/** Recomputes the progress the overlays show, on the client thread. */
	private void updateOpenChest()
	{
		ChestPlan plan = openStorage == 0 ? null : chests.get(currentChest);
		if (plan == null)
		{
			openChest = null;
			return;
		}
		Map<String, Integer> items;
		synchronized (lock)
		{
			items = inventoryItems;
		}
		ItemContainer storage = client.getItemContainer(openStorage == InterfaceID.RAIDS_STORAGE_SHARED
			? InventoryID.RAIDS_SHAREDSTORAGE : InventoryID.RAIDS_PRIVATESTORAGE);
		openChest = new ChestProgress(plan, items, openedWith, tally(storage));
	}

	/** Item name to quantity for a container, empty for one the client hasn't seen. */
	private Map<String, Integer> tally(ItemContainer container)
	{
		if (container == null)
		{
			return Collections.emptyMap();
		}
		Map<String, Integer> items = new LinkedHashMap<>();
		for (Item item : container.getItems())
		{
			if (item.getId() > 0)
			{
				items.merge(itemName(item.getId()), item.getQuantity(), Integer::sum);
			}
		}
		return items;
	}

	/** Item name from the cache, filled on the client thread. */
	String itemName(int itemId)
	{
		String name = itemNames.get(itemId);
		if (name == null)
		{
			name = itemManager.getItemComposition(itemId).getName();
			itemNames.put(itemId, name);
		}
		return name;
	}

	ChestProgress getOpenChest()
	{
		return config.chestOverlay() || config.chestGlow() ? openChest : null;
	}

	/** Region of the local player, the template's region inside an instance, -1 when not logged in. */
	private int region()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return -1;
		}
		return WorldPoint.fromLocalInstance(client, player.getLocalLocation()).getRegionID();
	}

	/** Whether the numbers should be the solo ones right now. */
	private boolean solo()
	{
		return inRaid ? soloRaid : needsTabSolo;
	}

	/** The solo doses for the solo tab, or a solo raid, when they are kept apart. */
	private Needs needsFor(boolean solo)
	{
		return solo && config.separateSoloNeeds() ? needsSolo : needs;
	}

	private void remindAtOlm()
	{
		String shortfalls = snapshot().shortfalls();
		if (!shortfalls.isEmpty())
		{
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage("Short for Olm: " + shortfalls)
				.build());
		}
	}

	private void leftRaid()
	{
		soloRaid = false;
		atOlm = false;
		privateItems.clear();
		roomKeys.clear();
		lastRoomSlot = null;
		currentChest = null;
		openChest = null;
		synchronized (lock)
		{
			privateStorage = null;
			sharedStorage = null;
			claims.clear();
		}
		if (plan.reset(true))
		{
			savePlan();
		}
		loadoutDirty = true;
	}

	private static Supplies count(Item[] items)
	{
		int[] ids = new int[items.length];
		int[] quantities = new int[items.length];
		for (int i = 0; i < items.length; i++)
		{
			ids[i] = items[i].getId();
			quantities[i] = items[i].getQuantity();
		}
		return Supplies.count(ids, quantities);
	}

	private static Supplies count(Map<Integer, Integer> items)
	{
		int[] ids = new int[items.size()];
		int[] quantities = new int[items.size()];
		int i = 0;
		for (Map.Entry<Integer, Integer> e : items.entrySet())
		{
			ids[i] = e.getKey();
			quantities[i++] = e.getValue();
		}
		return Supplies.count(ids, quantities);
	}

	private static void add(Loadout loadout, Item[] items)
	{
		for (Item item : items)
		{
			loadout.add(item.getId(), item.getQuantity());
		}
	}

	private void recompute()
	{
		Loadout loadout = new Loadout().spellbook(client.getVarbitValue(VarbitID.SPELLBOOK));
		ItemContainer carried = client.getItemContainer(InventoryID.INV);
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		Supplies carriedSupplies = Supplies.EMPTY;
		Map<String, Integer> items = tally(carried);
		if (carried != null)
		{
			add(loadout, carried.getItems());
			carriedSupplies = count(carried.getItems());
		}
		if (worn != null)
		{
			add(loadout, worn.getItems());
		}
		for (Map.Entry<Integer, Integer> e : privateItems.entrySet())
		{
			loadout.add(e.getKey(), e.getValue());
		}
		if (loadout.quantityOfAny(POUCHES) > 0)
		{
			addPouch(loadout);
		}

		synchronized (lock)
		{
			inventory = carriedSupplies;
			inventoryItems = items;
			missing = missingFor(roles, loadout);
		}
		updateOpenChest();
	}

	private void addPouch(Loadout loadout)
	{
		EnumComposition runes = client.getEnum(EnumID.RUNEPOUCH_RUNE);
		for (int i = 0; i < POUCH_TYPES.length; i++)
		{
			int type = client.getVarbitValue(POUCH_TYPES[i]);
			int quantity = client.getVarbitValue(POUCH_QUANTITIES[i]);
			if (type > 0 && quantity > 0)
			{
				loadout.add(runes.getIntValue(type), quantity);
			}
		}
	}

	static Map<Role, List<String>> missingFor(Set<Role> roles, Loadout loadout)
	{
		Map<Role, List<String>> missing = new EnumMap<>(Role.class);
		for (Role role : roles)
		{
			List<String> lacking = role.missing(loadout);
			if (!lacking.isEmpty())
			{
				missing.put(role, lacking);
			}
		}
		return missing;
	}

	/** "Zamorak godsword (Muttadile ZGS)" for everything missing. */
	private static List<String> describe(Map<Role, List<String>> missing)
	{
		List<String> lines = new ArrayList<>();
		for (Map.Entry<Role, List<String>> e : missing.entrySet())
		{
			lines.add(e.getKey().getDisplayName() + ": " + String.join(", ", e.getValue()));
		}
		return lines;
	}

	private void remindOnEntry()
	{
		List<MissingItemsOverlay.Line> lines = missingLines();
		if (lines.isEmpty() || !remindersOn())
		{
			return;
		}
		StringBuilder text = new StringBuilder("CoX missing - ");
		String who = null;
		for (MissingItemsOverlay.Line line : lines)
		{
			if (!line.who.equals(who))
			{
				text.append(who == null ? "" : "; ").append(line.who).append(' ');
				who = line.who;
			}
			else
			{
				text.append(", ");
			}
			text.append(line.what);
		}
		if (config.chatReminder())
		{
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(text.toString())
				.build());
		}
		notifier.notify(config.notification(), text.toString());
	}

	/** Who is missing what: you first, then the party if that's turned on. */
	private List<MissingItemsOverlay.Line> missingLines()
	{
		List<MissingItemsOverlay.Line> lines = new ArrayList<>();
		PartyMember local = party.getLocalMember();
		synchronized (lock)
		{
			for (String line : describe(missing))
			{
				lines.add(new MissingItemsOverlay.Line("You", line));
			}
			if (config.includeTeam() && party.isInParty())
			{
				for (PartyMember member : party.getMembers())
				{
					MemberStatus status = members.get(member.getMemberId());
					if (status == null || (local != null && local.getMemberId() == member.getMemberId()))
					{
						continue;
					}
					for (String line : status.getMissing())
					{
						lines.add(new MissingItemsOverlay.Line(name(member), line));
					}
				}
			}
		}
		return lines;
	}

	private static String name(PartyMember member)
	{
		String name = member.getDisplayName();
		return name == null || name.isEmpty() || "<unknown>".equals(name) ? "Unknown" : name;
	}

	// ---- party ----

	private void sendToParty()
	{
		ticksSinceSend++;
		if (!party.isInParty() || party.getLocalMember() == null)
		{
			lastSent = null;
			return;
		}
		if (sendPlan)
		{
			sendPlan = false;
			List<String> edits = plan.encode();
			if (!edits.isEmpty())
			{
				party.send(new CoxPlanMessage(edits));
			}
		}
		if (ticksSinceSend < SEND_INTERVAL)
		{
			return;
		}
		CoxStatusMessage status;
		synchronized (lock)
		{
			List<String> claimed = new ArrayList<>();
			for (Claim claim : claims.all())
			{
				claimed.add(claim.encode());
			}
			status = new CoxStatusMessage(Role.names(roles), describe(missing), claimed, inventory.toArray(),
				privateStorage == null ? null : privateStorage.toArray(),
				sharedStorage == null ? null : sharedStorage.toArray(), iron);
		}
		if (resendStatus || !status.sameContent(lastSent))
		{
			resendStatus = false;
			lastSent = status;
			ticksSinceSend = 0;
			party.send(status);
		}
	}

	@Subscribe
	public void onCoxStatusMessage(CoxStatusMessage message)
	{
		PartyMember local = party.getLocalMember();
		if (local != null && local.getMemberId() == message.getMemberId())
		{
			// the party echoes our own messages; we already know
			return;
		}
		synchronized (lock)
		{
			members.put(message.getMemberId(), MemberStatus.from(message));
			if (!iron)
			{
				claims.yieldTo(ironDoses());
			}
		}
		refresh();
	}

	@Subscribe
	public void onCoxPlanMessage(CoxPlanMessage message)
	{
		List<String> edits = message.getEdits();
		if (edits != null && edits.size() <= CmRoom.values().length * Potion.values().length && plan.merge(edits))
		{
			savePlan();
			dropClaimsPastCount();
			refresh();
		}
	}

	@Subscribe
	public void onUserJoin(UserJoin event)
	{
		resendStatus = true;
		sendPlan = true;
		refresh();
	}

	@Subscribe
	public void onUserSync(UserSync event)
	{
		resendStatus = true;
		sendPlan = true;
	}

	@Subscribe
	public void onUserPart(UserPart event)
	{
		synchronized (lock)
		{
			members.remove(event.getMemberId());
		}
		refresh();
	}

	@Subscribe
	public void onPartyChanged(PartyChanged event)
	{
		synchronized (lock)
		{
			members.clear();
		}
		resendStatus = true;
		sendPlan = true;
		refresh();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CoxTeamUtilitiesConfig.GROUP.equals(event.getGroup()))
		{
			refresh();
		}
	}

	// ---- sidebar actions, on the Swing thread ----

	@Override
	public void setRole(Role role, boolean selected)
	{
		String stored;
		synchronized (lock)
		{
			if (!(selected ? roles.add(role) : roles.remove(role)))
			{
				return;
			}
			stored = String.join(",", Role.names(roles));
		}
		configManager.setConfiguration(CoxTeamUtilitiesConfig.GROUP, CoxTeamUtilitiesConfig.KEY_ROLES, stored);
		rolesChanged();
	}

	@Override
	public void renameChest(String key, String name)
	{
		ChestPlan plan = chests.get(key);
		if (plan != null && !plan.getName().equals(name.trim()))
		{
			plan.setName(name);
			saveChests();
			refresh();
		}
	}

	@Override
	public void setChestOrdered(String key, boolean ordered)
	{
		ChestPlan plan = chests.get(key);
		if (plan != null && plan.isOrdered() != ordered)
		{
			plan.setOrdered(ordered);
			saveChests();
			clientThread.invokeLater(this::updateOpenChest);
			refresh();
		}
	}

	@Override
	public void setChestLines(String key, boolean deposit, String text)
	{
		ChestPlan plan = chests.get(key);
		if (plan == null)
		{
			return;
		}
		List<String> lines = ChestPlan.lines(text);
		List<String> target = deposit ? plan.getDeposit() : plan.getWithdraw();
		if (!target.equals(lines))
		{
			target.clear();
			target.addAll(lines);
			saveChests();
			clientThread.invokeLater(this::updateOpenChest);
			refresh();
		}
	}

	@Override
	public void setMarking(boolean on)
	{
		if (marking != on)
		{
			marking = on;
			refresh();
		}
	}

	@Override
	public void selectChest(String key)
	{
		selectedChest = key;
	}

	@Override
	public void deleteChest(String key)
	{
		if (chests.remove(key))
		{
			saveChests();
			clientThread.invokeLater(this::updateOpenChest);
			refresh();
		}
	}

	@Override
	public void finishRaid()
	{
		synchronized (lock)
		{
			roles.clear();
			claims.clear();
		}
		marking = false;
		configManager.setConfiguration(CoxTeamUtilitiesConfig.GROUP, CoxTeamUtilitiesConfig.KEY_ROLES, "");
		rolesChanged();
	}

	/** Re-checks the items for the roles on the next tick, or right away when no tick is coming. */
	private void rolesChanged()
	{
		loadoutDirty = true;
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			// no tick is coming to pick this up
			clientThread.invokeLater(() ->
			{
				loadoutDirty = false;
				recompute();
				refresh();
			});
		}
	}

	@Override
	public void setNeed(Potion potion, int doses)
	{
		Needs plan = needsFor(needsTabSolo);
		if (plan.set(potion, doses))
		{
			configManager.setConfiguration(CoxTeamUtilitiesConfig.GROUP,
				plan == needsSolo ? CoxTeamUtilitiesConfig.KEY_NEEDS_SOLO : CoxTeamUtilitiesConfig.KEY_NEEDS, plan.encode());
			refresh();
		}
	}

	@Override
	public void setNeedsTab(boolean solo)
	{
		if (needsTabSolo != solo)
		{
			needsTabSolo = solo;
			configManager.setConfiguration(CoxTeamUtilitiesConfig.GROUP, CoxTeamUtilitiesConfig.KEY_NEEDS_TAB_SOLO, solo);
			refresh();
		}
	}

	@Override
	public void setDropCount(CmRoom room, Potion potion, int count)
	{
		if (count >= 0 && plan.set(room, potion, count))
		{
			planEdited();
		}
	}

	@Override
	public void resetDropCounts()
	{
		if (plan.reset(false))
		{
			planEdited();
		}
	}

	@Override
	public void toggleClaim(Slot slot)
	{
		synchronized (lock)
		{
			PanelState.SlotView others = others(slot);
			if (!claims.toggle(slot, others.takenOnClick(), others.nextOrder()))
			{
				return;
			}
		}
		refresh();
	}

	@Override
	public void setClaimDoses(Slot slot, int doses)
	{
		synchronized (lock)
		{
			PanelState.SlotView others = others(slot);
			if (!claims.setDoses(slot, doses, others.availableToMe(), others.nextOrder()))
			{
				return;
			}
		}
		refresh();
	}

	@Override
	public boolean setSipRoom(Slot slot, CmRoom room, boolean sipThere)
	{
		synchronized (lock)
		{
			PanelState.SlotView others = others(slot);
			if (!claims.setSipRoom(slot, room, sipThere, others.availableToMe(), others.nextOrder()))
			{
				return false;
			}
		}
		refresh();
		return true;
	}

	/** Everyone else's share of the potion. Call with the lock held. */
	private PanelState.SlotView others(Slot slot)
	{
		PanelState.SlotView view = new PanelState.SlotView();
		view.slot = slot;
		view.viewerIron = iron;
		for (Map.Entry<Long, MemberStatus> e : members.entrySet())
		{
			for (Claim claim : e.getValue().getClaims())
			{
				if (claim.getSlot().equals(slot))
				{
					view.add(String.valueOf(e.getKey()), false, e.getValue().isIron(), claim);
				}
			}
		}
		return view;
	}

	/** Doses ironmen in the party have claimed, per potion. Call with the lock held. */
	private Map<Slot, Integer> ironDoses()
	{
		Map<Slot, Integer> doses = new HashMap<>();
		for (MemberStatus status : members.values())
		{
			if (status.isIron())
			{
				for (Claim claim : status.getClaims())
				{
					doses.merge(claim.getSlot(), claim.getDoses(), Integer::sum);
				}
			}
		}
		return doses;
	}

	private void planEdited()
	{
		savePlan();
		dropClaimsPastCount();
		sendPlan = true;
		refresh();
	}

	private void savePlan()
	{
		configManager.setConfiguration(CoxTeamUtilitiesConfig.GROUP, CoxTeamUtilitiesConfig.KEY_PLAN,
			String.join(",", plan.encode()));
	}

	private void dropClaimsPastCount()
	{
		synchronized (lock)
		{
			claims.dropPast(plan);
		}
	}

	// ---- drawing ----

	/** Pushes the current state to the sidebar and the overlay. Safe from any thread. */
	private void refresh()
	{
		CoxTeamPanel target = panel;
		if (target == null)
		{
			return;
		}
		overlay.setLines(showOverlay ? missingLines() : Collections.emptyList());
		PanelState state = snapshot();
		SwingUtilities.invokeLater(() -> target.update(state));
	}

	private PanelState snapshot()
	{
		PanelState state = new PanelState();
		state.inParty = party.isInParty();
		state.iron = iron;
		state.countShared = config.countShared();
		state.countSplit = config.countSplit();
		state.units = config.needUnits();
		state.chests = chests.copy(gson);
		state.currentChest = currentChest;
		state.marking = marking;
		state.openChest = openChest;
		synchronized (lock)
		{
			state.carriedItems = inventoryItems;
		}
		state.separateSoloNeeds = config.separateSoloNeeds();
		state.trackStamina = config.trackStamina();
		state.solo = solo();
		Needs needs = needsFor(state.solo);
		for (Potion potion : Potion.values())
		{
			state.need.put(potion, state.applies(potion) ? needs.get(potion) : 0);
		}

		PartyMember local = party.getLocalMember();
		List<PartyMember> partyMembers = state.inParty ? party.getMembers() : Collections.emptyList();

		synchronized (lock)
		{
			state.inventory = inventory;
			state.privateStorage = privateStorage;
			state.sharedStorage = sharedStorage;
			state.roles.addAll(roles);
			state.missing.putAll(missing);

			MemberStatus mine = new MemberStatus(EnumSet.copyOf(state.roles), describe(missing),
				claims.bySlot(), inventory, privateStorage, sharedStorage, iron);

			state.claimed = claims.doses();

			Map<Slot, PanelState.SlotView> shares = new HashMap<>();
			for (Claim claim : claims.all())
			{
				shares.computeIfAbsent(claim.getSlot(), s -> new PanelState.SlotView()).add("You", true, iron, claim);
			}
			for (PartyMember member : partyMembers)
			{
				PanelState.Member view = new PanelState.Member();
				view.name = name(member);
				view.self = local != null && local.getMemberId() == member.getMemberId();
				view.status = view.self ? mine : members.get(member.getMemberId());
				view.iron = view.status != null && view.status.isIron();
				state.team.add(view);
				if (!view.self && view.status != null)
				{
					for (Claim claim : view.status.getClaims())
					{
						shares.computeIfAbsent(claim.getSlot(), s -> new PanelState.SlotView())
							.add(view.name, false, view.iron, claim);
					}
				}
			}

			for (CmRoom room : CmRoom.values())
			{
				PanelState.RoomDrops drops = new PanelState.RoomDrops();
				drops.room = room;
				for (Potion potion : Potion.values())
				{
					int count = plan.count(room, potion);
					if (count == 0 && DropPlan.wikiCount(room, potion) == 0)
					{
						continue;
					}
					PanelState.PotionDrop drop = new PanelState.PotionDrop();
					drop.potion = potion;
					drop.count = count;
					drop.edited = plan.isEdited(room, potion);
					for (int i = 0; i < count; i++)
					{
						Slot slot = new Slot(room, potion, i);
						PanelState.SlotView view = shares.getOrDefault(slot, new PanelState.SlotView());
						view.slot = slot;
						view.viewerIron = iron;
						drop.slots.add(view);
					}
					drops.potions.add(drop);
				}
				if (DropPlan.hasDrops(room) || !drops.potions.isEmpty())
				{
					state.drops.add(drops);
				}
			}
		}
		return state;
	}
}
