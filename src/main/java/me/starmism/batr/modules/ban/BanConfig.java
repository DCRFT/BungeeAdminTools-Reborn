package me.starmism.batr.modules.ban;

import me.mattstudios.config.SettingsHolder;
import me.mattstudios.config.annotations.Description;
import me.mattstudios.config.annotations.Path;
import me.mattstudios.config.properties.Property;

import java.util.Map;

import static java.util.Map.entry;

@Description({"BungeeAdminTools Reborn - Ban Module Configuration File", ""})
public class BanConfig implements SettingsHolder {

    private BanConfig() {}

    @Path("enabled")
    public static final Property<Boolean> ENABLED = Property.create(true);

    @Path("commands")
    public static final Property<Map<String, Boolean>> COMMANDS = Property.create(Boolean.class, Map.ofEntries(
            entry("banip", true),
            entry("banlist", true),
            entry("gtempbanip", true),
            entry("gunban", true),
            entry("gtempban", true),
            entry("ban", true),
            entry("gunbanip", true),
            entry("unban", true),
            entry("tempbanip", true),
            entry("gban", true),
            entry("gbanip", true),
            entry("unbanip", true)
    ));
}