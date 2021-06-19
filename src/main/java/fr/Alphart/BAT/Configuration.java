package fr.Alphart.BAT;

import me.mattstudios.config.SettingsHolder;
import me.mattstudios.config.annotations.Comment;
import me.mattstudios.config.annotations.Path;
import me.mattstudios.config.properties.Property;

import java.util.Map;

public class Configuration implements SettingsHolder {

	private Configuration() {}

	public static final Property<String> language = Property.create("en");
	public static final Property<String> prefix = Property.create("&6[&4BAT&6]&e ");
	
    @Comment("Force players to give reason when /ban /unban /kick /mute /unmute etc.")
	public static final Property<Boolean> mustGiveReason = Property.create(false);

	@Comment("Enable /bat confirm, to confirm command such as action on unknown player.")
	public static final Property<Boolean> confirmCommand = Property.create(true);

	@Comment("Enable or disable simple aliases to bypass the /bat prefix for core commands")
	public static final Property<Map<String, Boolean>> simpleAliasesCommands = Property.create(Boolean.class, Map.of("default", false));

	@Comment("Make the date more readable."
			+ "If the date correspond to today, tmw or yda, it will replace the date by the corresponding word")
	public static final Property<Boolean> litteralDate = Property.create(true);

	@Comment("Enable BETA (experimental) Redis support, requires RedisBungee")
	public static final Property<Boolean> redisSupport = Property.create(false);

	@Comment("The debug mode enables verbose logging. All the logged message will be in the debug.log file in BAT folder")
	public static Property<Boolean> debugMode = Property.create(false);
	


	@Comment("Set to true to use MySQL. Otherwise SQL Lite will be used")
	@Path(value = "mysql.enabled")
	public static final Property<Boolean> mysql_enabled = Property.create(true);

    @Path(value = "mysql.user")
	public static final Property<String> mysql_user = Property.create("user");

    @Path(value = "mysql.password")
	public static final Property<String> mysql_password = Property.create("password");

    @Path(value = "mysql.database")
	public static final Property<String> mysql_database = Property.create("database");

    @Path(value = "mysql.host")
	public static final Property<String> mysql_host = Property.create("localhost");

	@Comment("If you don't know it, just leave it like this (3306 = default mysql port)")
    @Path(value = "mysql.port")
	public static final Property<String> mysql_port = Property.create("3306");


//	public Locale getLocale() {
//		if (language.length() != 2) {
//			BAT.getInstance().getLogger().severe("Incorrect language set ... The language was set to english.");
//			return new Locale("en");
//		}
//		return new Locale(language);
//	}
}
