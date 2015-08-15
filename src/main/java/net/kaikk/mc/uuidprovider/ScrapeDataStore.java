package net.kaikk.mc.uuidprovider;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.UUID;

class ScrapeDataStore {
	public UUIDProvider instance;
	private String dbUrl;
	private String username;
	private String password;
	protected Connection db = null;
	
	private String dbTable;
	private String dbColUsers;
	private String dbColUUID;
	public String name;
	
	public long Count = 0;

	ScrapeDataStore(UUIDProvider instance, String ScrapeName, String url, String username, String password, String table, String users, String uuid) throws Exception {
		this.instance=instance;
		this.name = ScrapeName;
		this.dbUrl = url;
		this.username = username;
		this.password = password;
		this.dbTable = table;
		this.dbColUsers = users;
		this.dbColUUID = uuid;
		
		
		
		try {
			//load the java driver for mySQL
			Class.forName("com.mysql.jdbc.Driver");
		} catch(Exception e) {
			this.instance.getLogger().severe("Unable to load Java's mySQL database driver.  Check to make sure you've installed it properly.");
			throw e;
		}
		
		try {
			this.dbCheck();
		} catch(Exception e) {
			this.instance.getLogger().severe("Unable to connect to database.  Check your config file settings. Details: \n"+e.getMessage());
			throw e;
		}
	}
	
	public boolean scrape() throws Exception{
		// go ahead and scrape the database for any data we don't have.
		try {
			this.Count = 0;
			this.dbCheck();

			Statement statement = this.db.createStatement();

			// load cache data from database
			ResultSet results = statement.executeQuery("SELECT " + this.dbColUsers + "," + this.dbColUUID + " FROM " + this.dbTable);
			while(results.next()) {
				PlayerData playerData = new PlayerData(UUID.fromString(results.getString(this.dbColUUID)), results.getString(dbColUsers));
				if(!UUIDProvider.isCached(playerData.uuid)){
					UUIDProvider.ds.addData(playerData);
					UUIDProvider.cachedPlayersUUID.put(playerData.uuid, playerData);
					UUIDProvider.cachedPlayersName.put(playerData.name, playerData);
					Count++;
				}
			}

			this.instance.getLogger().info("Scraped "+Count+" players.");
			return true;
		} catch(Exception e) {
			this.instance.getLogger().severe("Unable to read database data. Details: \n"+e.getMessage());
			throw e;
		}
	}
	
	synchronized void dbCheck() throws SQLException {
		if(this.db == null || this.db.isClosed()) {
			Properties connectionProps = new Properties();
			connectionProps.put("user", this.username);
			connectionProps.put("password", this.password);
			
			this.db = DriverManager.getConnection(this.dbUrl, connectionProps); 
		}
	}
	
	synchronized void dbClose()  {
		try {
			if (!this.db.isClosed()) {
				this.db.close();
				this.db=null;
			}
		} catch (SQLException e) {
			
		}
	}
	
	public static UUID toUUID(byte[] bytes) {
	    if (bytes.length != 16) {
	        throw new IllegalArgumentException();
	    }
	    int i = 0;
	    long msl = 0;
	    for (; i < 8; i++) {
	        msl = (msl << 8) | (bytes[i] & 0xFF);
	    }
	    long lsl = 0;
	    for (; i < 16; i++) {
	        lsl = (lsl << 8) | (bytes[i] & 0xFF);
	    }
	    return new UUID(msl, lsl);
	}
	
	public static String UUIDtoHexString(UUID uuid) {
		if (uuid==null) return "0x0";
		return "0x"+org.apache.commons.lang.StringUtils.leftPad(Long.toHexString(uuid.getMostSignificantBits()), 16, "0")+org.apache.commons.lang.StringUtils.leftPad(Long.toHexString(uuid.getLeastSignificantBits()), 16, "0");
	}
	
	public static int epoch() {
		return (int) (System.currentTimeMillis()/1000);
	}
}
