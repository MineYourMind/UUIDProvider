package net.kaikk.mc.uuidprovider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

class Config {
	final static String configFilePath = "plugins" + File.separator + "UUIDProvider" + File.separator + "config.yml";
	private File configFile;
	FileConfiguration config;
	
	String dbUrl;
	String dbUsername;
	String dbPassword;
	
	ArrayList<dbScrape> scrapeEntries = new ArrayList<dbScrape>();
	
	public class dbScrape{
		String ScrapeName;
		String dbUrl;
		String dbUsername;
		String dbPassword;
		String dbTable;
		String dbColUsername;
		String dbColUUID;
		
		public dbScrape(String source){
			String[] data = source.split(",");
			this.ScrapeName = data[0];
			this.dbUrl = data[1];
			this.dbUsername = data[2];
			this.dbPassword = data[3];
			this.dbTable = data[4];
			this.dbColUsername = data[5];
			this.dbColUUID = data[6];
			
		}
		
	}
	
	Config() {
		this.configFile = new File(configFilePath);
		this.config = YamlConfiguration.loadConfiguration(this.configFile);
		this.load();
	}
	
	void load() {
		this.dbUrl=config.getString("dbUrl", "jdbc:mysql://127.0.0.1/uuidprovider");
		this.dbUsername=config.getString("dbUsername", "uuidprovider");
		this.dbPassword=config.getString("dbPassword", "");
		String s = config.getString("dbScrapes");
		if(s != null){
			dbScrape dbs = new dbScrape(s);
			this.scrapeEntries.add(dbs);
		}
			
	
		
		this.save();
	}
	
	void save() {
		try {
			this.config.set("dbUrl", this.dbUrl);
			this.config.set("dbUsername", this.dbUsername);
			this.config.set("dbPassword", this.dbPassword);

			this.config.save(this.configFile);
		} catch (IOException e) {
			UUIDProvider.instance.getLogger().warning("Couldn't create or save config file.");
			e.printStackTrace();
		}
	}
}