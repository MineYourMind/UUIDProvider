package net.kaikk.mc.uuidprovider;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.UUID;

class DataStore {
	private UUIDProvider instance;
	private String dbUrl;
	private String username;
	private String password;
	protected Connection db = null;

	DataStore(UUIDProvider instance, String url, String username, String password) throws Exception {
		this.instance=instance;
		this.dbUrl = url;
		this.username = username;
		this.password = password;
		
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
/**
 *This block seems to wreck the database cluster.
 *Please ensure that this database table exists :(
 *
		try {
			Statement statement = db.createStatement();

			// Creates tables on the database
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS uuidcache ("
					+ "  uuid binary(16) NOT NULL,"
					+ "  name varchar(64) NOT NULL,"
					+ "  lastcheck int(11) NOT NULL,"
					+ "  PRIMARY KEY (uuid),"
					+ "  KEY name (name));");
		} catch(Exception e) {
			this.instance.getLogger().severe("Unable to create the necessary database tables. Details: \n"+e.getMessage());
			
			throw e;
		}
**/		
		try {
			this.dbCheck();
			
			Statement statement = this.db.createStatement();
			
			// delete cache data older than 37 days
			statement.executeUpdate("DELETE FROM uuidcache WHERE lastcheck < "+(epoch()-3196800));
			
			// load cache data from database
			ResultSet results = statement.executeQuery("SELECT * FROM uuidcache");
			while(results.next()) {
				PlayerData playerData = new PlayerData(toUUID(results.getBytes(1)), results.getString(2), results.getInt(3));
				UUIDProvider.cachedPlayersUUID.put(playerData.uuid, playerData);
				UUIDProvider.cachedPlayersName.put(playerData.name, playerData);
			}
			
			this.instance.getLogger().info("Cached "+UUIDProvider.cachedPlayersUUID.size()+" players.");
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
	
	synchronized void addData(PlayerData playerData) {
		try {
			this.dbCheck();
			
			Statement statement = this.db.createStatement();
			statement.executeUpdate("INSERT INTO uuidcache VALUES("+UUIDtoHexString(playerData.uuid)+", \""+playerData.name+"\", "+playerData.lastCheck+") ON DUPLICATE KEY UPDATE name=\""+playerData.name+"\", lastcheck="+playerData.lastCheck);

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	synchronized PlayerData getPlayerData(String name) {
		try {
			this.dbCheck();
			
			Statement statement = this.db.createStatement();
			ResultSet results = statement.executeQuery("SELECT * FROM uuidcache WHERE name=\""+name+"\" AND lastcheck>"+(epoch()-3196800)+" LIMIT 1");
			
			if(results.next()) {
				PlayerData playerData = new PlayerData(toUUID(results.getBytes(1)), results.getString(2), results.getInt(3));
				// cache result
				UUIDProvider.cachedPlayersUUID.put(playerData.uuid, playerData);
				UUIDProvider.cachedPlayersName.put(playerData.name, playerData);
				return playerData;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	synchronized PlayerData getPlayerData(UUID uuid) {
		try {
			this.dbCheck();
			
			Statement statement = this.db.createStatement();
			ResultSet results = statement.executeQuery("SELECT * FROM uuidcache WHERE uuid="+UUIDtoHexString(uuid)+" AND lastcheck>"+(epoch()-3196800));
			
			if(results.next()) {
				PlayerData playerData = new PlayerData(toUUID(results.getBytes(1)), results.getString(2), results.getInt(3));
				// cache result
				UUIDProvider.cachedPlayersUUID.put(playerData.uuid, playerData);
				UUIDProvider.cachedPlayersName.put(playerData.name, playerData);
				return playerData;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	synchronized void clearCache() {
		try {
			this.dbCheck();
			
			Statement statement = this.db.createStatement();
			statement.executeUpdate("TRUNCATE uuidcache");
		} catch (SQLException e) {
			e.printStackTrace();
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
