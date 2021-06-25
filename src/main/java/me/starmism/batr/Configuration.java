package me.starmism.batr;

import me.mattstudios.config.SettingsHolder;
import me.mattstudios.config.annotations.Comment;
import me.mattstudios.config.annotations.Description;
import me.mattstudios.config.annotations.Path;
import me.mattstudios.config.properties.Property;

import java.util.Map;

@Description({"Bungee Admin Tools Reborn - Primary Configuration File", ""})
public class Configuration implements SettingsHolder {

	private Configuration() {}

	@Path("language")
	public static final Property<String> LANGUAGE = Property.create("en");
	@Path("prefix")
	public static final Property<String> PREFIX = Property.create("&6[&4BAT&6]&e ");
	
    @Comment("Force players to give reason when they use /ban /unban /kick /mute /unmute etc.")
	@Path("mustGiveReason")
	public static final Property<Boolean> MUST_GIVE_REASON = Property.create(false);

	@Comment("Enable /bat confirm, to confirm risky commands such as targeting an unknown player")
	@Path("confirmCommand")
	public static final Property<Boolean> CONFIRM_COMMAND = Property.create(true);

	@Comment("Enable or disable simple aliases to bypass the /bat prefix for core commands")
	@Path("simpleAliasesCommands")
	public static final Property<Map<String, Boolean>> SIMPLE_ALIASES_COMMANDS = Property.create(Boolean.class, Map.of("default", false));

	@Comment("Make the date more readable. If the date correspond to today, tmw or yda, it will replace the date by the corresponding word")
	@Path("literalDate")
	public static final Property<Boolean> LITERAL_DATE = Property.create(true);

	@Comment("Enable BETA (experimental) Redis support, requires RedisBungee")
	@Path("redisSupport")
	public static final Property<Boolean> REDIS_SUPPORT = Property.create(false);

	@Comment("The debug mode enables verbose logging. All the logged message will be in the debug.log file in BATR folder")
	@Path("debugMode")
	public static Property<Boolean> DEBUG_MODE = Property.create(false);
	


	@Comment("Set to true to use MySQL. Otherwise SQLite will be used")
	@Path("mysql.enabled")
	public static final Property<Boolean> MYSQL_ENABLED = Property.create(true);

    @Path("mysql.user")
	public static final Property<String> MYSQL_USER = Property.create("user");

    @Path("mysql.password")
	public static final Property<String> MYSQL_PASSWORD = Property.create("password");

    @Path("mysql.database")
	public static final Property<String> MYSQL_DATABASE = Property.create("database");

    @Path("mysql.host")
	public static final Property<String> MYSQL_HOST = Property.create("localhost");

    @Path("mysql.port")
	public static final Property<String> MYSQL_PORT = Property.create("3306");


//	public Locale getLocale() {
//		if (language.length() != 2) {
//			BATR.getInstance().getLogger().severe("Incorrect language set ... The language was set to english.");
//			return new Locale("en");
//		}
//		return new Locale(language);
//	}
}
