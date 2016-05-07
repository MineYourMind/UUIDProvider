package net.kaikk.mc.uuidprovider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.kaikk.mc.uuidprovider.Config.dbScrape;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class UUIDProvider extends JavaPlugin {
	public final EventListener eventListener = new EventListener();
	static UUIDProvider instance;
	static ConcurrentHashMap<String, PlayerData> cachedPlayersName = new ConcurrentHashMap<String, PlayerData>();
	static ConcurrentHashMap<UUID, PlayerData> cachedPlayersUUID = new ConcurrentHashMap<UUID, PlayerData>();

	static Method getUniqueId;
	static Method getPlayerByUUID;

	/** for /st remove <name> confirmation. */
	private HashMap<String, String> clearConfirm = new HashMap<String, String>();

	static Config config;
	static DataStore ds;
	// Storage for our various databases to scrape from.
	static HashMap<String, ScrapeDataStore> scrapeDS = new HashMap<String, ScrapeDataStore>();

	// When a scrape is triggered, we track it here.
	static HashMap<String, AsynchScrape> pendingScrapes = new HashMap<String, AsynchScrape>();

	public void onEnable() {
		instance = this;

		try {
			getUniqueId = OfflinePlayer.class.getMethod("getUniqueId");
			getPlayerByUUID = Server.class.getMethod("getOfflinePlayer", UUID.class);
			this.getLogger().info("Bukkit 1.7.5+ UUID support found.");
		} catch (Exception e) {
			getUniqueId = null;
			getPlayerByUUID = null;

			this.getLogger().info("No Bukkit 1.7.5+ UUID support found.");
		}

		config = new Config();

		try {
			ds = new DataStore(this, config.dbUrl, config.dbUsername, config.dbPassword);
		} catch (Exception e1) {
			this.getLogger()
			.warning(
					"UUIDProvider won't use a MySQL database! This may affect performance!");
			ds = null;
		}

		if (ds == null) {
			this.getLogger().severe("Not using db!");
		} else {
			this.getLogger().info("Attached to db!");
		}

		// Set up the databases for the scrape DB.
		for (dbScrape d : config.scrapeEntries) {
			try {
				ScrapeDataStore sds = new ScrapeDataStore(this, d.ScrapeName,
						d.dbUrl, d.dbUsername, d.dbPassword, d.dbTable,
						d.dbColUsername, d.dbColUUID);
				scrapeDS.put(d.ScrapeName, sds);
			} catch (Exception e) {
				this.getLogger().severe(
						"Error creating scrape datastore : " + e.getMessage());
				e.printStackTrace();
			}

		}

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this.eventListener, this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		if (!sender.isOp() && getServer().getConsoleSender() != sender) {
			sender.sendMessage("§2[UUID§aProvider§2] You don't have permission to do this!");
			return false;
		}

		if (cmd.getName().equalsIgnoreCase("uuidprovider")) {
			if (args.length == 0) {
				sender.sendMessage("§2[UUID§aProvider§2] Usage: /uuidprovider (get|reload|clearcache|scrape)");
				return false;
			}
			if(args[0].equalsIgnoreCase("cacheadd")){
				if(!sender.isOp()){
					sender.sendMessage("§2[UUID§aProvider§2] You must be an Operator to use this command!");
					return false;
				}
				if (args.length == 1) {
					sender.sendMessage("§2[UUID§aProvider§2] Usage: /uuidprovider cacheadd (name) (UUID)");
					return false;
				}

				if (args.length != 3) {
					sender.sendMessage("§2[UUID§aProvider§2] You must include a Name and a UUID as arguments to this command!");
					return false;
				}				
				
				UUID uuid = UUID.fromString(args[2]);
				
				if(uuid == null){
					sender.sendMessage("§2[UUID§aProvider§2] Could not convert the supplied UUID string to a java UUID!");
					return false;
				}
				
				PlayerData pd = new PlayerData(uuid, args[1]);
				ds.addData(pd);
				
				sender.sendMessage("§2[UUID§aProvider§2] Added Player '"+pd.name+"' to the UUID Cache with UUID '"+pd.uuid.toString()+"'");
				
			}
			if (args[0].equalsIgnoreCase("scrape")) {
				if (getServer().getConsoleSender() == sender) {
					if (args.length == 1) {

						if (pendingScrapes.containsKey(sender.getName())) {
							AsynchScrape as = pendingScrapes.get(sender
									.getName());
							if (as != null && as.ds != null) {
								if (as.state == true && as.failure == false) {
									sender.sendMessage("§2[UUID§aProvider§2] Scrape finished successfully - Scraped "
											+ as.ds.Count + " player records!");
									pendingScrapes.remove(sender.getName());
								} else if (as.state == false
										&& as.failure == false) {
									sender.sendMessage("§2[UUID§aProvider§2] Scrape is still in progress - Please be patient!");
								} else {
									sender.sendMessage("§2[UUID§aProvider§2] Scrape seems to have failed! I Hope you see a useful error message in the console spam! :(");
									pendingScrapes.remove(sender.getName());
								}
							} else {
								pendingScrapes.remove(sender.getName());
							}
						}

						sender.sendMessage("§2[UUID§aProvider§2] Usage: /uuidprovider scrape [scrapename]");
						String l = "§2[UUID§aProvider§2] Available scrapes : ";
						for (String n : scrapeDS.keySet()) {
							l += n + " ";
						}
						sender.sendMessage(l);
						return false;

					} else if (args.length == 2) {
						String thescrape = "";
						for (String n : scrapeDS.keySet()) {
							if (args[1].equalsIgnoreCase(n)) {
								thescrape = n;
							}
						}

						if (thescrape != "") {
							if (scrapeDS.get(thescrape) != null) {
								AsynchScrape as = new AsynchScrape(
										scrapeDS.get(thescrape),
										Bukkit.getScheduler());
								as.failure = false;
								as.state = false;
								Bukkit.getScheduler().runTaskAsynchronously(
										this, as);
								pendingScrapes.put(sender.getName(), as);
							} else {
								sender.sendMessage("§2[UUID§aProvider§2] Failed to retreive scrape?!");
								return false;
							}
						} else {
							// didn't find the scrape the player wanted.
							sender.sendMessage("§2[UUID§aProvider§2] Usage: /uuidprovider scrape [scrapename]");
							String l = "§2[UUID§aProvider§2] Available scrapes : ";
							for (String n : scrapeDS.keySet()) {
								l += n + " ";
							}
							sender.sendMessage(l);
							return false;
						}

					}
				} else {
					sender.sendMessage("§2[UUID§aProvider§2] Sorry, you can only do this from the server console!");
				}
			}

			if (args[0].equalsIgnoreCase("get")) {

				if (args.length == 1) {
					sender.sendMessage("§2[UUID§aProvider§2] Usage: /uuidprovider get (name|uuid) [player]");
					return false;
				}

				if (args[1].equalsIgnoreCase("uuid")) {
					String name = "";
					if (args.length == 2) {
						if (!(sender instanceof Player)) {
							sender.sendMessage("§2[UUID§aProvider§2] You're the console - Please provide a user name to look up!");
							return false;
						}

						name = sender.getName();
					} else {
						name = args[2];
					}

					UUID uuid = retrieve(name);

					sender.sendMessage("§2[UUID§aProvider§2] " + name + "'s UUID is "
							+ (uuid != null ? uuid.toString() : "null"));
					return true;

				}

				if (args[1].equalsIgnoreCase("name")) {
					if (args.length == 2) {
						sender.sendMessage("§2[UUID§aProvider§2] Usage: /uuidprovider get name (player's uuid)");
						return false;
					}
					UUID uuid;
					try {
						uuid = UUID.fromString(args[2]);
					} catch (Throwable t) {
						sender.sendMessage("§2[UUID§aProvider§2] Invalid UUID : "
								+ t.getMessage());
						return false;
					}

					String name = retrieve(uuid);
					if (name == null) {
						sender.sendMessage("§2[UUID§aProvider§2] Couldn't find the name for "
								+ args[2]);
						return false;
					}

					sender.sendMessage("§2[UUID§aProvider§2] " + args[2]
							+ " is " + name + "'s UUID.");
					return true;
				}
			}

			if (args[0].equalsIgnoreCase("reload")) {
				cachedPlayersName.clear();
				cachedPlayersUUID.clear();
				scrapeDS.clear();
				this.onEnable();
				sender.sendMessage("§2[UUID§aProvider§2] Reloaded configs and cache!");
				return true;
			}

			if (args[0].equalsIgnoreCase("confirm")) {
				if (getServer().getConsoleSender() == sender) {

					// If there isn't an arg[1] at all
					if (args.length != 2) {
						sender.sendMessage("§2[UUID§aProvider§2] §cPlease enter your confirmation code!");
						return true;
					}
					// if there is (?), but its nothing
					if (args[1].length() == 0) {
						sender.sendMessage("§2[UUID§aProvider§2] §cPlease enter your confirmation code!");
						return true;
					}
					// is this a valid confirm code?
					if (clearConfirm.containsKey(args[1])
							&& clearConfirm.get(args[1]) == sender.getName()) {
						cacheClear();
						sender.sendMessage("§2[UUID§aProvider§2] §cCache has been cleared.");

					} else {
						sender.sendMessage("§2[UUID§aProvider§2] §cCouldn't find that confirmation code! Please check the sequence and try again!");
					}

				} else {
					sender.sendMessage("§2[UUID§aProvider§2] §cYou can only execute this command from server console. Logging attempt.§r");
					this.getLogger()
					.warning(
							"Player "
									+ sender.getName()
									+ " attempted to clear the UUIDProvider cache from in-game!");
				}
			}

			if (args[0].equalsIgnoreCase("clearcache")) {
				
				if (getServer().getConsoleSender() == sender) {
					String conf = this.generateRandomNumbers(4);
					// put them into the confirm tracking
					this.clearConfirm.put(conf, sender.getName());
					sender.sendMessage("§2[UUID§aProvider§2] §aPlease enter the command §e/uuidprovider confirm "
							+ conf
							+ "§a to confirm deletion of ALL CACHED DATA.");
					sender.sendMessage("§2[UUID§aProvider§2] §cPlease be advised this is a §4PERMANENT§c operation, and cannot be undone.");
					sender.sendMessage("§2[UUID§aProvider§2] §cThis operation will empty the current cache, and clear out the database!");
					return true;
				} else {
					sender.sendMessage("§2[UUID§aProvider§2] §cYou can only execute this command from server console. Logging attempt.§r");
					this.getLogger().warning("Player " + sender.getName() + " attempted to clear the UUIDProvider cache from in-game!");
				}
			}
		}
		return true;
	}

	// /////////////////////////////////////////////////////////////////////////////////
	// Cache Query Methods

	/**
	 * get the player UUID for this player
	 * 
	 * @return player's UUID, null if it couldn't have found
	 */
	public static UUID get(OfflinePlayer player) {
		if (player == null) {
			return null;
		}

		// Bukkit 1.7.5+
		if (getUniqueId != null) {
			try {
				UUID bukkitUUID = (UUID) getUniqueId.invoke(player);
				if (bukkitUUID != null && player.getName() != null
						&& !player.getName().isEmpty()) {
					if (!cachedPlayersUUID.containsKey(bukkitUUID)) {
						cacheAdd(bukkitUUID, player.getName());
					}
					return bukkitUUID;
				}
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}
		}

		String name = player.getName();
		if (name != null) {
			return retrieve(name);
		}
		return null;
	}

	/**
	 * get the OfflinePlayer for this UUID
	 * 
	 * @return OfflinePlayer, null if not found
	 */
	public static OfflinePlayer get(UUID uuid) {
		if (uuid == null) {
			return null;
		}

		// Bukkit 1.7.5+
		if (getPlayerByUUID != null) {
			try {
				OfflinePlayer player = (OfflinePlayer) getPlayerByUUID.invoke(
						instance.getServer(), uuid);

				if (player != null && player.getName() != null
						&& !player.getName().isEmpty()) {
					if (!cachedPlayersUUID.containsKey(uuid)) {
						cacheAdd(uuid, player.getName());
					}
					return player;
				}
			} catch (IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}
		}

		String name = retrieve(uuid);
		if (name == null) {
			return null;
		}

		return instance.getServer().getOfflinePlayer(name);
	}

	/**
	 * This gets the player name without checking the server data
	 * 
	 * @return String player's name for this uuid, null if not found
	 */
	public static String retrieve(UUID uuid) {
		if (uuid == null) {
			return null;
		}

		// Cache
		String name = getCachedPlayer(uuid);
		if (name != null) {
			return name;
		}

		// NameFetcher
		return nameFetcher(uuid);
	}

	/**
	 * This gets the player's uuid without checking the server data
	 * 
	 * @return UUID player's uuid for this name, null if not found
	 */
	public static UUID retrieve(String name) {
		// Cache
		UUID uuid = getCachedPlayer(name);
		if (uuid != null) {
			return uuid;
		}

		// UUIDFetcher
		return uuidFetcher(name);
	}

	/**
	 * This gets the player name from the Mojang's server (slowest method)
	 * 
	 * @return String player's name for this uuid, null if not found
	 */
	public static String nameFetcher(UUID uuid) {
		NameFetcher namefetcher = new NameFetcher(Arrays.asList(uuid));
		try {
			String name = null;
			Map<UUID, String> UUIDToNameMap = namefetcher.call();
			for (String nameRow : UUIDToNameMap.values()) {
				name = nameRow;
			}

			instance.getLogger().info(
					(name != null ? name : "null") + " <-> " + uuid.toString());

			// cache result
			cacheAdd(uuid, name);

			return name;
		} catch (Exception e) {
			instance.getLogger().severe(e.getMessage());
			//@MrWisski : Removed due to HTTP 429 (Too many requests) error.
			//e.printStackTrace();
			return null;
		}
	}

	/**
	 * This gets the player's uuid from the Mojang's server (slowest method)
	 * 
	 * @return UUID player's uuid for this name, null if not found
	 */
	public static UUID uuidFetcher(String name) {
		UUIDFetcher uuidfetcher = new UUIDFetcher(Arrays.asList(name));
		try {
			Map<String, UUID> nameToUUIDMap = uuidfetcher.call();
			UUID uuid = null;
			for (UUID uuidRow : nameToUUIDMap.values()) {
				uuid = uuidRow;
			}

			instance.getLogger().info(
					name + " <-> " + (uuid != null ? uuid.toString() : "null"));

			// cache result
			cacheAdd(uuid, name);

			return uuid;
		} catch (Exception e) {
			instance.getLogger().severe(e.getMessage());
			//@MrWisski : Removed due to HTTP 429 (Too many requests) error.
			//e.printStackTrace();
			return null;
		}
	}

	/**
	 * This gets the player's name if in cache (MySQL database included), Skips
	 * Mojang.
	 * 
	 * @return String player name for this uuid, null if not found
	 */
	public static String getCachedPlayer(UUID uuid) {
		PlayerData playerData = cachedPlayersUUID.get(uuid);
		if (playerData != null && playerData.check()) {
			return playerData.name;
		}

		// Database cache
		if (ds != null) {
			playerData = ds.getPlayerData(uuid);
			if (playerData != null) {
				return playerData.name;
			}
		}

		return null;
	}

	/**
	 * This gets the player's uuid if in cache (MySQL database included)
	 * 
	 * @return String player uuid for this name, null if not found
	 */
	public static UUID getCachedPlayer(String name) {
		PlayerData playerData = cachedPlayersName.get(name);
		if (playerData != null && playerData.check()) {
			return playerData.uuid;
		}

		// Database cache
		if (ds != null) {
			playerData = ds.getPlayerData(name);
			if (playerData != null && playerData.uuid != null) {
				return playerData.uuid;
			}
		}

		return null;
	}

	// ///////////////////////////////////////////////////////////////////////////////
	// Cache Manipulation Methods

	/**
	 * Adds a player to the memory cache, and the database.
	 * 
	 * @param uuid
	 *            - UUID of Player
	 * @param name
	 *            - Name that the UUID belongs to
	 * @return true on success/already in cache, false otherwise
	 */
	public static boolean cacheAdd(UUID uuid, String name) {
		PlayerData oldPlayerData, playerData = new PlayerData(uuid, name);
		// Check if this record exists EXACTLY as recieved in the cache.
		if (isCached(playerData)) {
			return true;
		}

		// Ok, either not found, or UUID not connected to this Playername
		// Check for a name change!
		oldPlayerData = cachedPlayersUUID.put(uuid, playerData);
		if (oldPlayerData != null && oldPlayerData.name != null) {
			// this player changed name... remove old name from cache
			cachedPlayersName.remove(oldPlayerData.name); 
		}
		// We don't care about what the old name was, so just ignore it.
		cachedPlayersName.put(name, playerData);

		if (ds != null) {
			ds.addData(playerData);
		}
		return playerData != null;
	}

	/**
	 * Public function to remove a player from the memory cache.
	 * 
	 * @return true on success, false on failure/invalid data/not in cache.
	 */
	public static boolean cacheRemove(String name) {
		UUID uuid = cachedPlayersUUID.get(name).uuid;
		if (uuid != null)
			return cRemove(name, uuid);
		else
			return false;
	}

	/**
	 * Public function to remove a player from the memory cache.
	 * 
	 * @return true on success, false on failure/invalid data/not in cache.
	 */
	public static boolean cacheRemove(UUID uuid) {
		String name = cachedPlayersName.get(uuid).name;

		if (name != null)
			return cRemove(name, uuid);
		else
			return false;
	}

	/**
	 * Will completely empty out the memory cache, AS WELL AS THE DATASTORE.
	 * 
	 * I REPEAT : THIS WILL WIPE OUT THE DATABASE. Use with caution :D
	 */
	private static void cacheClear() {

		ds.clearCache();
		cachedPlayersName.clear();
		cachedPlayersUUID.clear();
	}

	///////////////////////////////////////////////////////////////////////////////////
	// Cache status methods

	public static int cacheCount() {
		if (cachedPlayersUUID != null) {
			return cachedPlayersUUID.size();
		} else {
			return 0;
		}
	}

	/**
	 * boolean isCached(String)
	 * 
	 * @param p
	 *            - an player name to check for against the player name cache.
	 * @return true if player was found, false if not.
	 */
	public static boolean isCached(String username) {
		return cachedPlayersName.containsKey(username);
	}

	/**
	 * boolean isCached(OfflinePlayer)
	 * 
	 * @param uuid
	 *            - a UUID to check for against the player uuid cache.
	 * @return true if player was found, false if not.
	 */
	static public boolean isCached(UUID uuid) {
		return cachedPlayersUUID.containsKey(uuid);
	}

	/**
	 * boolean isCached(PlayerData)
	 * 
	 * @param p
	 *            - a PlayerData record to check for against the player UUID
	 *            cache.
	 * @return true if player was found with identical data, false if not found,
	 *         or data mismatch.
	 */
	static private boolean isCached(PlayerData p) {
		if (p == null || p.name == null || p.uuid == null) {
			return false;
		}
		PlayerData pdn = cachedPlayersUUID.get(p.uuid);
		PlayerData pdu = cachedPlayersName.get(p.name);

		if (pdn != null && pdu != null && p.name == pdn.name
				&& p.uuid == pdu.uuid) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * boolean isCached(OfflinePlayer)
	 * 
	 * @param p
	 *            - an offline player to check for against the player name
	 *            cache.
	 * @return true if player was found, false if not.
	 */
	static public boolean isCached(OfflinePlayer p) {
		return cachedPlayersName.containsKey(p.getName());
	}

	// //////////////////////////////////////////////////////////////////////////////////
	// Helper stuff

	/**
	 * Generates a string of random numbers, suitable for a confirmation string
	 * IE, len of 3 might return a string like "184"
	 */
	private String generateRandomNumbers(int len) {
		String ret = "";

		for (int x = 0; x < len; x++) {
			int n = (int) (Math.random() * 10);
			ret += Integer.toString(n);
		}

		return ret;
	}

	private static boolean cRemove(String name, UUID uuid) {

		PlayerData playerData = new PlayerData(uuid, name);

		cachedPlayersName.remove(playerData.name);
		cachedPlayersUUID.remove(playerData.uuid);

		return true;
	}
}
