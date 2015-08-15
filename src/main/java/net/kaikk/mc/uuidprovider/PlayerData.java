package net.kaikk.mc.uuidprovider;

import java.util.UUID;

public class PlayerData {
	UUID uuid;
	String name;
	int lastCheck;
	
	PlayerData(UUID uuid, String name) {
		this(uuid, name, DataStore.epoch());
	}
	
	PlayerData(UUID uuid, String name, int lastCheck) {
		this.uuid = uuid;
		this.name = name;
		this.lastCheck = lastCheck;
	}
	
	boolean check() {
		if (this.name==null||this.uuid==null) {
			return (DataStore.epoch()-this.lastCheck < 10);
		}
		
		return (DataStore.epoch()-this.lastCheck < 3196800); // 37 days
	}
}
