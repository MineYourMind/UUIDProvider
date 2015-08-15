package net.kaikk.mc.uuidprovider;

import java.sql.SQLException;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;


public class AsynchScrape implements Runnable {

	public ScrapeDataStore ds;

	public boolean failure = false;
	public boolean state = false;
	private BukkitScheduler bs;
	
	private void callHome(final String msg){
		BukkitRunnable r = new BukkitRunnable() {
			String m = msg;
			@Override
			public void run() {
				ds.instance.getLogger().info(m);
			}
		};

		bs.scheduleSyncDelayedTask(ds.instance, r, 0);

	}

	AsynchScrape(ScrapeDataStore db, BukkitScheduler sched){
		ds = db;
		bs = sched;		
	}
	

	@Override
	public void run() {
		this.callHome("ASYNCH DEBUG : Starting Asynch Database Scrape!");
		try{
			ds.dbCheck();
		} catch(SQLException e) {
			this.failure = true;
			this.callHome("ASYNCH DEBUG : Couldn't connect to scrape DB "+ds.name+".");
			return;
		}
		
		boolean result = false;
		
		try{
			result = ds.scrape();
		} catch(Exception e) {
			this.state = false;
			this.failure = true;
			result = false;
			this.callHome("ASYNCH DEBUG : Exception while scraping - " + e.getMessage());
		}
		
		if(this.failure || !result){
			//We'll try again later. might even succeed.
			this.callHome("ASYNCH DEBUG : We failed to scrape the DB.");
			this.state = false;
			this.failure = true;
			
		} else {
			//We succeeded.
			this.state = true;
		}
		
		if(state && !failure){
			this.callHome("ASYNCH DEBUG : Finished Database Asynch Scrape Successfully");
		} else {
			this.callHome("ASYNCH DEBUG : Finished Database Asynch Scrape With Failures. :<");
		}
	}
}
