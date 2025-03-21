package me.starmism.batr.modules;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import me.starmism.batr.BATR;
import me.starmism.batr.Configuration;
import me.starmism.batr.i18n.I18n;
import me.starmism.batr.modules.core.CommandQueue;
import me.starmism.batr.modules.core.Core;
import me.starmism.batr.modules.core.CoreCommand;
import me.starmism.batr.utils.UUIDNotFoundException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.MissingResourceException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BATCommand extends net.md_5.bungee.api.plugin.Command implements TabExecutor {
    private static final Pattern pattern = Pattern.compile("<.*?>");
    private final String name;
    private final String syntax;
    private final String description;
    private final String permission;
    private final I18n i18n;
    private boolean runAsync = false;
    private boolean coreCommand = false;

    private int minArgs = 0;

    /**
     * Constructor
     *
     * @param name        name of this command
     * @param description description of this command
     * @param permission  permission required to use this commands
     * @param aliases     aliases of this command (optional)
     */
    public BATCommand(final String name, final String syntax, final String description, final String permission,
                      final String... aliases) {
        super(name, null, aliases); // Use own permission system
        this.name = name;
        this.syntax = syntax;
        this.permission = permission;
        this.description = description;

        i18n = BATR.getInstance().getI18n();


        // Compute min args
        final Matcher matcher = pattern.matcher(syntax);
        while (matcher.find()) {
            minArgs++;
        }

        final RunAsync asyncAnnot = getClass().getAnnotation(RunAsync.class);
        if (asyncAnnot != null) {
            runAsync = true;
        }

        if (CoreCommand.class.equals(getClass().getEnclosingClass())) {
            coreCommand = true;
        }
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return Joiner.on(' ').join(name, syntax, description);
    }

    public String getSyntax() {
        return syntax;
    }

    /**
     * Get a nice coloured usage
     *
     * @return coloured usage
     */
    public String getFormatUsage() {
        return ChatColor.translateAlternateColorCodes('&', "&e" + name + " &6" + syntax + " &f-&B " + description);
    }

    public String getBATPermission() {
        return permission;
    }

    public void handleCommandException(final CommandSender sender, final Exception exception) {
        if (exception instanceof IllegalArgumentException) {
            if (exception.getMessage() == null) {
                if (coreCommand) {
                    // Just need to add the /bat if it's a core command
                    sender.sendMessage(i18n.formatPrefix("invalidArgsUsage", new String[]{"&e/bat " + getFormatUsage()}));
                } else {
                    sender.sendMessage(i18n.formatPrefix("invalidArgsUsage", new String[]{"&e/" + getFormatUsage()}));
                }
            } else if (i18n.format("noPerm").equals(exception.getMessage())) {
                sender.sendMessage(i18n.formatPrefix("noPerm"));
            } else {
                sender.sendMessage(i18n.formatPrefix("invalidArgs", new String[]{exception.getMessage()}));
            }
        } else if (exception instanceof UUIDNotFoundException) {
            sender.sendMessage(i18n.formatPrefix("invalidArgs", new String[]{i18n.format("cannotGetUUID", new String[]{((UUIDNotFoundException) exception).getInvolvedPlayer()})}));
        } else if (exception instanceof MissingResourceException) {
            sender.sendMessage(BATR.convertStringToComponent("&cAn error occurred with the translation. Key involved: &a" + ((MissingResourceException) exception).getKey()));
        } else {
            sender.sendMessage(BATR.convertStringToComponent("A command error happened! Please check the console."));
            BATR.getInstance().getLogger().severe("A command error happened! Please report this stacktrace :");
            exception.printStackTrace();
        }
    }

    @Override
    public void execute(final CommandSender sender, final String[] args) {
        // If the sender doesn't have the permission, we're gonna check if he has this permission with children permission
        // Example: in this plugin, if the sender has "bat.ban.server1", he also has "bat.ban"
        if (!(permission == null || sender.hasPermission(permission) || sender.hasPermission("bat.admin")
                || (sender.hasPermission("bat.grantall.global") && permission.endsWith("global")))) {
            boolean hasPerm = false;
            Collection<String> senderPerm = Core.getCommandSenderPermission(sender);
            for (final String perm : senderPerm) {
                // The grantall give access to all command (used when command is executed, but the plugin check in the command if the sender can execute this action)
                // except the /bat ... commands
                if (perm.toLowerCase().startsWith(permission)) {
                    hasPerm = true;
                    break;
                }
                // The global grantall perm has already been checked before
                if (!coreCommand && perm.toLowerCase().startsWith("bat.grantall") && !permission.endsWith("global")) {
                    // We're going to check if there is no perm to cancel (using -)
                    final String searchedPattern = "-" + permission;
                    boolean permFound = false;
                    for (final String perm2 : senderPerm) {
                        if (perm2.toLowerCase().startsWith(searchedPattern)) {
                            permFound = true;
                            break;
                        }
                    }
                    if (permFound) {
                        hasPerm = false;
                    } else {
                        hasPerm = true;
                        break;
                    }
                }
            }
            if (!hasPerm) {
                sender.sendMessage(i18n.formatPrefix("noPerm"));
                return;
            }
        }
        // Overrides command to confirm if /bat confirm is disabled
        final boolean confirmedCmd = !BATR.getInstance().getConfiguration().get(Configuration.CONFIRM_COMMAND) || CommandQueue.isExecutingQueueCommand(sender);
        try {
            Preconditions.checkArgument(args.length >= minArgs);
            if (runAsync) {
                ProxyServer.getInstance().getScheduler().runAsync(BATR.getInstance(), () -> {
                    try {
                        onCommand(sender, args, confirmedCmd);
                    } catch (final Exception exception) {
                        handleCommandException(sender, exception);
                    }
                });
            } else {
                onCommand(sender, args, confirmedCmd);
            }
        } catch (final Exception exception) {
            handleCommandException(sender, exception);
        }
        if (confirmedCmd) {
            CommandQueue.removeFromExecutingQueueCommand(sender);
        }
    }

    @Override
    public Iterable<String> onTabComplete(final CommandSender sender, final String[] args) {
        final List<String> result = new ArrayList<>();
        if (args.length == 0) {
            sender.sendMessage(BATR.convertStringToComponent("Add the first letter to autocomplete"));
            return result;
        }
        final String playerToCheck = args[args.length - 1];
        if (playerToCheck.length() > 0) {
                for (final ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                    if (player
                            .getName()
                            .substring(
                                    0,
                                    Math.min(playerToCheck.length(), player.getName().length())).equalsIgnoreCase(playerToCheck)) {
                        result.add(player.getName());
                    }
                }
        }
        return result;
    }

    public abstract void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
            throws IllegalArgumentException;

    public void mustConfirmCommand(final CommandSender sender, final String command, final String message) {
        final String cmdToConfirm = (BATR.getInstance().getConfiguration().get(Configuration.SIMPLE_ALIASES_COMMANDS).get("confirm"))
                ? "confirm" : "bat confirm";
        if (!CommandQueue.isExecutingQueueCommand(sender)) {
            if ("".equals(message)) {
                sender.sendMessage(i18n.formatPrefix("mustConfirm", new String[]{"", cmdToConfirm}));
            } else {
                sender.sendMessage(i18n.formatPrefix("mustConfirm", new String[]{"&e" + message, cmdToConfirm}));
            }
            CommandQueue.queueCommand(sender, command);
        }
    }

    public void setMinArgs(int minArgs) {
        this.minArgs = minArgs;
    }

    /* UtilsKt for command */

    /**
     * Use this annotation onCommand if the command need to be ran async
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface RunAsync {
    }

    /**
     * Use this annotation to disable a command
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Disable {
    }
}