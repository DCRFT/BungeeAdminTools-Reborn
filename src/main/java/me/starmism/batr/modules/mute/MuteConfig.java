package me.starmism.batr.modules.mute;

import me.mattstudios.config.SettingsHolder;
import me.mattstudios.config.annotations.Comment;
import me.mattstudios.config.annotations.Description;
import me.mattstudios.config.annotations.Path;
import me.mattstudios.config.properties.Property;

import java.util.Map;
import java.util.Set;

import static java.util.Map.entry;

@Description({"BungeeAdminTools Reborn - Mute Module Configuration File", ""})
public class MuteConfig implements SettingsHolder {

    private MuteConfig() {}

    @Path("enabled")
    public static final Property<Boolean> ENABLED = Property.create(true);

    @Path("commands")
    public static final Property<Map<String, Boolean>> COMMANDS = Property.create(Boolean.class, Map.ofEntries(
            entry("gtempmute", true),
            entry("gunmute", true),
            entry("gunmuteip", true),
            entry("tempmute", true),
            entry("gtempmuteip", true),
            entry("muteip", true),
            entry("unmuteip", true),
            entry("mute", true),
            entry("unmute", true),
            entry("tempmuteip", true),
            entry("gmute", true),
            entry("gmuteip", true)
    ));

    @Comment("Forbidden commands when a player is mute")
    @Path("forbiddenCmds")
    public static final Property<Set<String>> FORBIDDEN_CMDS = Property.create(Set.of(
            ("msg"),
            ("tell"),
            ("whisper"),
            ("r"),
            ("reply"),
            ("rp")
    ));
}