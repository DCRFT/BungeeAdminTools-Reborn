package fr.Alphart.bat.modules.kick;

import me.mattstudios.config.SettingsHolder;
import me.mattstudios.config.annotations.Description;
import me.mattstudios.config.annotations.Path;
import me.mattstudios.config.properties.Property;

import java.util.Map;

import static java.util.Map.entry;

@Description({"BungeeAdminTools Reborn - Kick Module Configuration File", ""})
public class KickConfig implements SettingsHolder {

    private KickConfig() {}

    @Path("enabled")
    public static final Property<Boolean> ENABLED = Property.create(true);

    @Path("commands")
    public static final Property<Map<String, Boolean>> COMMANDS = Property.create(Boolean.class, Map.ofEntries(
            entry("gkick", true),
            entry("kick", false)
    ));
}
