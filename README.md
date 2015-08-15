# UUIDProvider
A bukkit plugin that provides players UUID support for other plugins.
This plugin has been tested on Cauldron 1.6.4 and 1.7.10.

All my plugin's builds can be downloaded from http://kaikk.net/mc/#bukkit

##Configuration
You can use this plugin without any configuration, but I recommend a MySQL database in order to enable cache.
Edit plugins/UUIDProvider/config.yml and set your database account.
If you're running multiple servers, be sure to use the same MySQL database to improve performance!

If you have an external database that you would like to scrape existing UUID->Username mappings from, there is a new config section that looks like this :

`dbScrapes: {ScrapeName, dbUrl, dbUsername, dbPassword, dbTable, dbColUsername, dbColUUID}`

Where :
```
ScrapeName = The name you'll be using to reference this scrape.
dbUrl = the address, port, tablename of the database you'll be accessing
dbUsername/dbPassword = Login credentials
dbTable = The table that the plugin will look to, to grab the data from
dbColUsername = the name of the column in dbTable that stores minecraft Usernames
dbColUUID = the name of the column in dbTable that stores minecraft UUIDs
```

##Developers: how to use it

Add this plugin in your project's build path - Don't forget to also install this plugin on your servers!
String denotes a Minecraft Username.

- UUID/Username translation methods:
    - (UUID) UUIDProvider.get(OfflinePlayer) - OfflinePlayer -> UUID
    - (OfflinePlayer) UUIDProvider.get(UUID) - UUID -> OfflinePlayer
    - (UUID) UUIDProvider.retrieve(String) - Name -> UUID, no 1.7.5+ UUID resolution.
    - (String) UUIDProvider.retrieve(UUID) - UUID -> Name, no 1.7.5+ UUID resolution.
    - (UUID) UUIDProvider.uuidFetcher(String) - Name -> UUID, Skips cache, pulls from Mojang, caches result.
    - (String) UUIDProvider.nameFetcher(UUID) - UUID -> Name, Skips cache, pulls from Mojang, caches result.
    - (UUID) UUIDProvider.getCachedPlayer(String) - Name -> UUID, Skips Mojang, pulls from cache.
    - (String) UUIDProvider.getCachedPlayer(UUID) - UUID -> Name, Skips Mojang, pulls from cache.
- Cache status methods:
    - (Boolean) UUIDProvider.isCached(UUID)
    - (Boolean) UUIDProvider.isCached(String)
    - (Boolean) UUIDProvider.isCached(OfflinePlayer)
    - (Long) UUIDProvider.cacheCount()
- Cache manipulation methods:
    - (Boolean) UUIDProvider.cacheRemove(UUID)
    - (Boolean) UUIDProvider.cacheRemove(String)
    - (Boolean) UUIDProvider.cacheAdd(UUID, String)
    - (Boolean) UUIDProvider.cacheRemove(UUID)

Please report any issue! Suggestions are well accepted!

##Server Admins/Operators
Use /uuidprovider at the console or in game (You need operator to access).

- get (name|uuid) [UUID | Player Name]
    - This command will resolve a Username to a UUID, or a UUID to a username. If you are in-game and omit the optional parameter, it will return YOUR UUID or Username.
- reload
    - This command will reload the config files, as well as the memory cache from DB.
- scrape [Scrape name]
    - This command will attempt to asynchronously pull UUID mappings from an external database specified in the configs by [Scrape name]. If the Scrape name is omitted, it will check on the status of your last scrape. If you have no pending scrapes, it will list available scrapes.
- clearcache
    - This command will clear out the memory cache AND the DB cache. It requires a confirmation code to be entered before it actually performs the operation. ***This command will completely wipe all UUID/Usernames from storage! Use with extreme caution! Can only be used from Server Console! ***

##Support my life!
I'm currently unemployed and I'm studying at University (Computer Science).
I'll be unable to continue my studies soon because I need money.
If you like this plugin and you run it fine on your server, please <a href='http://kaikk.net/mc/#donate'>consider a donation</a>!
Thank you very much!