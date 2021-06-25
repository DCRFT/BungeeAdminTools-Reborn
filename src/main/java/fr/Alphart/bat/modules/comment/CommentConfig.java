package fr.Alphart.bat.modules.comment;

import me.mattstudios.config.SettingsHolder;
import me.mattstudios.config.annotations.Description;
import me.mattstudios.config.annotations.Path;
import me.mattstudios.config.properties.Property;

import java.util.Map;

import static java.util.Map.entry;

@Description({"BungeeAdminTools Reborn - Comment Module Configuration File", ""})
public class CommentConfig implements SettingsHolder {

    private CommentConfig() {}

    @Path("enabled")
    public static final Property<Boolean> ENABLED = Property.create(true);

    @Path("commands")
    public static final Property<Map<String, Boolean>> COMMANDS = Property.create(Boolean.class, Map.ofEntries(
            entry("warn", true),
            entry("clearcomment", true),
            entry("comment", true)
    ));

    @me.mattstudios.config.annotations.Comment({"Triggers list",
            "Trigger name:",
            "  pattern: reason which must be provided to trigger this",
            "  commands: list of commands that should be executed when it triggers, you can use {player} variable",
            "  triggerNumber: the number at which this triggers"})
    @Path("triggers")
    public static final Property<Map<String, Trigger>> TRIGGERS = Property.create(Trigger.class, Map.of("example", new Trigger()));
}