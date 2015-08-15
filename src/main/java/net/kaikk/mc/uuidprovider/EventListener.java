package net.kaikk.mc.uuidprovider;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class EventListener implements Listener {
	@EventHandler(priority = EventPriority.LOWEST)
	void onAsyncPlayerPreLoginEvent(AsyncPlayerPreLoginEvent event) {
		if (UUIDProvider.getUniqueId==null) {
			UUID uuid = UUIDProvider.retrieve(event.getName());
			if (uuid==null) {
				event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Invalid UUID or Mojang's servers down. Try later.");
				UUIDProvider.instance.getLogger().warning("UUIDProvider couldn't retrieve UUID for "+event.getName());
			} else {
				UUIDProvider.instance.getLogger().info(event.getName()+"'s UUID is "+uuid.toString());
			}
		}
	}
}
