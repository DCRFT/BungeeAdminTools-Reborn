package me.starmism.batr.modules.core;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import me.starmism.batr.BATR;
import me.starmism.batr.Configuration;
import me.starmism.batr.database.DataSourceHandler;
import me.starmism.batr.i18n.I18n;
import me.starmism.batr.modules.BATCommand;
import me.starmism.batr.modules.IModule;
import me.starmism.batr.modules.InvalidModuleException;
import me.starmism.batr.modules.ModulesManager;
import me.starmism.batr.modules.ban.BanEntry;
import me.starmism.batr.modules.comment.CommentEntry;
import me.starmism.batr.modules.core.PermissionManager.Action;
import me.starmism.batr.modules.core.importer.*;
import me.starmism.batr.modules.core.importer.Importer.ImportStatus;
import me.starmism.batr.modules.kick.KickEntry;
import me.starmism.batr.modules.mute.MuteEntry;
import me.starmism.batr.utils.CallbackUtils.ProgressCallback;
import me.starmism.batr.utils.UtilsKt;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;

import static com.google.common.base.Preconditions.checkArgument;
import static me.starmism.batr.utils.FormatUtilsKt.showFormattedHelp;

public class CoreCommand extends BATCommand {
    private final BaseComponent[] CREDIT;
    private final BaseComponent[] HELP_MSG;
    private final Map<List<String>, BATCommand> subCmd;
    private final I18n i18n;


    public CoreCommand(final Core coreModule) {
        super("bat", "", "", null);
        final Map<String, Boolean> simpleAliasesCommands = BATR.getInstance().getConfiguration().get(Configuration.SIMPLE_ALIASES_COMMANDS);
        subCmd = new HashMap<>();
        CREDIT = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes(
                '&', "------ &9Bungee&fAdmin&cTools &d&lREBORN&r - &aVersion {version}&e &f------" +
                        "\n       Originally by &aAlphart&f, now maintained by &aStarmism")
                .replace("{version}", BATR.getInstance().getDescription().getVersion()));

        // Dynamic commands load, commands are not configurable as with other modules
        final List<String> cmdsList = Lists.newArrayList();
        for (final Class<?> subClass : CoreCommand.this.getClass().getDeclaredClasses()) {
            try {
                if (subClass.getAnnotation(BATCommand.Disable.class) != null) {
                    continue;
                }
                final BATCommand command = (BATCommand) subClass.getConstructors()[0].newInstance();
                cmdsList.add(command.getName());
                final List<String> aliases = new ArrayList<>(Arrays.asList(command.getAliases()));
                aliases.add(command.getName());
                subCmd.put(aliases, command);
            } catch (final InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | SecurityException e) {
                BATR.getInstance()
                        .getLogger()
                        .severe("An error occurred during loading of CORE commands please report this :");
                e.printStackTrace();
            }
        }

        Collections.sort(cmdsList);
        // Add new commands if there are
        for (final String cmdName : cmdsList) {
            if (!simpleAliasesCommands.containsKey(cmdName)) {
                simpleAliasesCommands.put(cmdName, false);
            }
        }
        // Iterate through the commands map and remove the ones who don't exist (e.g because of an update)
        simpleAliasesCommands.entrySet().removeIf(cmdEntry -> !cmdsList.contains(cmdEntry.getKey()));
        BATR.getInstance().getConfiguration().save();
        // Register command either as subcommand or as simple alias
        for (final Iterator<Map.Entry<List<String>, BATCommand>> it = subCmd.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry<List<String>, BATCommand> cmdEntry = it.next();
            if (simpleAliasesCommands.get(cmdEntry.getValue().getName())) {
                coreModule.addCommand(cmdEntry.getValue());
                it.remove();
            }
            // Otherwise, do nothing just let the command in the subcommand map
        }

        i18n = BATR.getInstance().getI18n();

        HELP_MSG = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
                "&eType &6" + ((simpleAliasesCommands.get("help")) ? "/help" : "/bat help") + "&e to get help"));
    }

    public List<BATCommand> getSubCmd() {
        return new ArrayList<>(subCmd.values());
    }

    // Route the core subcommand
    @Override
    public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
            throws IllegalArgumentException {
        if (args.length == 0 || subCmd.isEmpty()) {
            sender.sendMessage(CREDIT);
            sender.sendMessage(HELP_MSG);
        } else {
            BATCommand cmd = null;
            for (final Entry<List<String>, BATCommand> aliasesCommand : subCmd.entrySet()) {
                if (aliasesCommand.getKey().contains(args[0])) {
                    cmd = aliasesCommand.getValue();
                    break;
                }
            }

            if (cmd != null) {
                // Reorganize args (remove subcommand)
                final String[] cleanArgs = new String[args.length - 1];
                System.arraycopy(args, 1, cleanArgs, 0, args.length - 1);

                if (cmd.getBATPermission().isEmpty() || sender.hasPermission(cmd.getBATPermission()) || sender.hasPermission("bat.admin")) {
                    cmd.execute(sender, cleanArgs);
                } else {
                    sender.sendMessage(i18n.formatPrefix("noPerm"));
                }
            } else {
                sender.sendMessage(i18n.formatPrefix("invalidCommand"));
            }
        }
    }

    public static class HelpCmd extends BATCommand {
        public HelpCmd() {
            super("help", "", "Displays help for core BATR commands.", "bat.help");
        }

        @Override
        public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
                throws IllegalArgumentException {
            final List<BATCommand> cmdsList = new ArrayList<>();
            for (final BATCommand cmd : BATR.getInstance().getModules().getCore().getCommands()) {
                if (cmd instanceof CoreCommand) {
                    cmdsList.addAll(((CoreCommand) cmd).getSubCmd());
                } else {
                    cmdsList.add(cmd);
                }
            }
            showFormattedHelp(cmdsList, sender, "CORE");
        }
    }

    public static class ModulesCmd extends BATCommand {
        private final StringBuilder sb = new StringBuilder();

        public ModulesCmd() {
            super("modules", "", "Displays what modules are loaded and commands for those modules.", "bat.modules");
        }

        @Override
        public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
                throws IllegalArgumentException {
            sender.sendMessage(BATR.convertStringToComponent("The loaded modules are:&a"));
            for (final IModule module : BATR.getInstance().getModules().getLoadedModules()) {
                if (module instanceof Core) {
                    continue;
                }
                sb.setLength(0);
                sb.append("&f - &9");
                sb.append(module.getName());
                if (module.getMainCommand() == null) {
                    sb.append(" &f| &eNo main command");
                } else {
                    sb.append(" &f| &eMain command: &a/");
                    sb.append(module.getMainCommand());
                }
                sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&',
                        sb.toString())));
            }
            // It means that no module were loaded otherwise there would be
            // something remaining in the StringBuilder
            if (sb.length() == 0) {
                sender.sendMessage(BATR.convertStringToComponent("&cThere aren't any loaded modules!"));
            } else {
                sb.setLength(0); // Clean the sb
            }
        }
    }

    public static class ReloadCmd extends BATCommand {
        public ReloadCmd() {
            super("reload", "", "Reload the whole plugin", "bat.reload");
        }

        @Override
        public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
                throws IllegalArgumentException {
            sender.sendMessage(BATR.convertStringToComponent("Starting reload ..."));
            BATR.getInstance().getConfiguration().reload();
            BATR.getInstance().reloadI18n();
            BATR.getInstance().getModules().unloadModules();
            BATR.getInstance().getModules().loadModules();
            sender.sendMessage(BATR.convertStringToComponent("Reload successfully executed ..."));
        }
    }

    @RunAsync
    public static class LookupCmd extends BATCommand {
        private static LookupFormatter lookupFormatter;
        private final ModulesManager modules;
        private final I18n i18n;

        public LookupCmd() {
            super("lookup", "<player/ip> [module] [page]", "Displays a player or an ip related information (universal or per module).", Action.LOOKUP.getPermission());
            modules = BATR.getInstance().getModules();
            lookupFormatter = new LookupFormatter();
            i18n = BATR.getInstance().getI18n();
        }

        public static LookupFormatter getLookupFormatter() {
            return LookupCmd.lookupFormatter;
        }

        @Override
        public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
                throws IllegalArgumentException {
            final String entity = args[0];
            if (UtilsKt.validIP(entity)) {
                checkArgument(sender.hasPermission("bat.admin") || sender.hasPermission(Action.LOOKUP.getPermission() + ".ip"), i18n.format("noPerm"));
                if (args.length == 1) {
                    for (final BaseComponent[] msg : lookupFormatter.getSummaryLookupIP(entity)) {
                        sender.sendMessage(msg);
                    }
                }
            } else {
                if (args.length == 1) {
                    for (final BaseComponent[] msg : lookupFormatter.getSummaryLookupPlayer(entity, sender.hasPermission(Action.LOOKUP.getPermission() + ".displayip"))) {
                        sender.sendMessage(msg);
                    }
                }
            }
            if (args.length > 1) {
                int page = 1;
                if (args.length > 2) {
                    try {
                        page = Integer.parseInt(args[2]);
                        if (page <= 0) {
                            page = 1;
                        }
                    } catch (final NumberFormatException e) {
                        throw new IllegalArgumentException("Incorrect page number");
                    }
                }
                try {
                    final List<BaseComponent[]> message;
                    switch (args[1]) {
                        case "ban" -> {
                            final List<BanEntry> bans = modules.getBanModule().getBanData(entity);
                            if (!bans.isEmpty()) {
                                message = lookupFormatter.formatBanLookup(entity, bans, page, false);
                            } else {
                                message = new ArrayList<>();
                                message.add(BATR.convertStringToComponent((!UtilsKt.validIP(entity))
                                        ? "&eThe player &a" + entity + "&e has never been banned."
                                        : "&eThe IP &a" + entity + "&e has never been banned."));
                            }
                        }
                        case "mute" -> {
                            final List<MuteEntry> mutes = modules.getMuteModule().getMuteData(entity);
                            if (!mutes.isEmpty()) {
                                message = lookupFormatter.formatMuteLookup(entity, mutes, page, false);
                            } else {
                                message = new ArrayList<>();
                                message.add(BATR.convertStringToComponent((!UtilsKt.validIP(entity))
                                        ? "&eThe player &a" + entity + "&e has never been muted."
                                        : "&eThe IP &a" + entity + "&e has never been muted."));
                            }
                        }
                        case "kick" -> {
                            final List<KickEntry> kicks = modules.getKickModule().getKickData(entity);
                            if (!kicks.isEmpty()) {
                                message = lookupFormatter.formatKickLookup(entity, kicks, page, false);
                            } else {
                                message = new ArrayList<>();
                                message.add(BATR.convertStringToComponent((!UtilsKt.validIP(entity))
                                        ? "&eThe player &a" + entity + "&e has never been kicked."
                                        : "&eThe IP &a" + entity + "&e has never been kicked."));
                            }
                        }
                        case "comment" -> {
                            final List<CommentEntry> comments = modules.getCommentModule().getComments(entity);
                            if (!comments.isEmpty()) {
                                message = lookupFormatter.commentRowLookup(entity, comments, page, false);
                            } else {
                                message = new ArrayList<>();
                                message.add(BATR.convertStringToComponent((!UtilsKt.validIP(entity))
                                        ? "&eThe player &a" + entity + "&e has no comment about him."
                                        : "&eThe IP &a" + entity + "&e has no comment."));
                            }
                        }
                        case "ip" -> {
                            EntityEntry pDetails = new EntityEntry(entity);
                            String last_ip = pDetails.getLastIP();
                            message = lookupFormatter.getSummaryLookupIP(last_ip);
                        }
                        default -> throw new InvalidModuleException("Module not found or invalid");
                    }

                    for (final BaseComponent[] msg : message) {
                        sender.sendMessage(msg);
                    }
                } catch (final InvalidModuleException e) {
                    throw new IllegalArgumentException(e.getMessage());
                }
            }
        }

    }

    @RunAsync
    public static class StaffLookupCmd extends BATCommand {
        private final ModulesManager modules;

        public StaffLookupCmd() {
            super("stafflookup", "<staff> [module] [page]", "Displays a staff member history (universal or per module).", "bat.stafflookup");
            modules = BATR.getInstance().getModules();
        }

        @Override
        public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
            final String entity = args[0];
            if (args.length == 1) {
                for (final BaseComponent[] msg : LookupCmd.getLookupFormatter().getSummaryStaffLookup(entity,
                        sender.hasPermission(Action.LOOKUP.getPermission() + ".displayip"))) {
                    sender.sendMessage(msg);
                }
            }
            if (args.length > 1) {
                int page = 1;
                if (args.length > 2) {
                    try {
                        page = Integer.parseInt(args[2]);
                        if (page <= 0) {
                            page = 1;
                        }
                    } catch (final NumberFormatException e) {
                        throw new IllegalArgumentException("Incorrect page number");
                    }
                }
                try {
                    final List<BaseComponent[]> message;
                    switch (args[1]) {
                        case "ban" -> {
                            final List<BanEntry> bans = modules.getBanModule().getManagedBan(entity);
                            if (!bans.isEmpty()) {
                                message = LookupCmd.getLookupFormatter().formatBanLookup(entity, bans, page, true);
                            } else {
                                message = new ArrayList<>();
                                message.add(BATR.convertStringToComponent("&b" + entity + "&e has never performed any operation concerning ban."));
                            }
                        }
                        case "mute" -> {
                            final List<MuteEntry> mutes = modules.getMuteModule().getManagedMute(entity);
                            if (!mutes.isEmpty()) {
                                message = LookupCmd.getLookupFormatter().formatMuteLookup(entity, mutes, page, true);
                            } else {
                                message = new ArrayList<>();
                                message.add(BATR.convertStringToComponent("&b" + entity + "&e has never performed any operation concerning mute."));
                            }
                        }
                        case "kick" -> {
                            final List<KickEntry> kicks = modules.getKickModule().getManagedKick(entity);
                            if (!kicks.isEmpty()) {
                                message = LookupCmd.getLookupFormatter().formatKickLookup(entity, kicks, page, true);
                            } else {
                                message = new ArrayList<>();
                                message.add(BATR.convertStringToComponent("&b" + entity + "&e has never performed any operation concerning kick."));
                            }
                        }
                        case "comment" -> {
                            final List<CommentEntry> comments = modules.getCommentModule().getManagedComments(entity);
                            if (!comments.isEmpty()) {
                                message = LookupCmd.getLookupFormatter().commentRowLookup(entity, comments, page, true);
                            } else {
                                message = new ArrayList<>();
                                message.add(BATR.convertStringToComponent("&b" + entity + "&e has never performed any operation concerning comment."));
                            }
                        }
                        default -> throw new InvalidModuleException("Module not found or invalid");
                    }

                    for (final BaseComponent[] msg : message) {
                        sender.sendMessage(msg);
                    }
                } catch (final InvalidModuleException e) {
                    throw new IllegalArgumentException(e.getMessage());
                }
            }
        }
    }

    public static class ConfirmCmd extends BATCommand {
        private final I18n i18n;

        public ConfirmCmd() {
            super("confirm", "", "Confirm your queued command.", "");
            i18n = BATR.getInstance().getI18n();
        }

        @Override
        public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
                throws IllegalArgumentException {
            if (!CommandQueue.executeQueueCommand(sender)) {
                sender.sendMessage(i18n.formatPrefix("noQueuedCommand"));
            }
        }

    }

    @RunAsync
    public static class ImportCmd extends BATCommand {
        private final static Map<String, Importer> importers = new HashMap<>() {{
            put("bungeeSuiteBans", new BungeeSuiteImporter());
            put("geSuitBans", new GeSuiteImporter());
            put("MC-Previous1.7.8", new MinecraftPreUUIDImporter());
            put("BanHammer", new BanHammerImporter());
            put("BATSQLite", new SQLiteMigrater());
            put("MC-Post1.7.8", new MinecraftUUIDImporter());
        }};

        public ImportCmd() {
            super("import", "<" + Joiner.on('/').join(importers.keySet()) + ">", "Imports ban data from the specified source. Available sources: &a"
                    + Joiner.on("&e,&a").join(importers.keySet()), "bat.import");
        }

        @Override
        public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
                throws IllegalArgumentException {
            checkArgument(BATR.getInstance().getConfiguration().get(Configuration.MYSQL_ENABLED), "You must use MySQL in order to use the import function.");
            final String source = args[0];

            final Importer importer = importers.get(source);
            if (importer != null) {
                sender.sendMessage(BATR.convertStringToComponent("BATR will be disabled during the import ..."));
                BATR.getInstance().getModules().unloadModules();

                importer.startImport(new ProgressCallback<>() {
                    @Override
                    public void done(ImportStatus result, Throwable throwable) {
                        if (throwable != null) {
                            if (throwable instanceof RuntimeException) {
                                sender.sendMessage(BATR.convertStringToComponent("An error (" + throwable.getMessage()
                                        + ") has occurred during the import. Please check the logs"));
                            } else {
                                sender.sendMessage(BATR.convertStringToComponent("An error has occured during the import. Please check the logs"));
                                BATR.getInstance().getLogger().severe("An error has occurred during the import of data from " + source
                                        + ". Please report this :");
                            }
                            throwable.printStackTrace();
                        } else {
                            sender.sendMessage(BATR.convertStringToComponent("Congratulations, the migration is finished. &a"
                                    + result.getConvertedEntries() + " entries&e were converted successfully."));
                        }
                        BATR.getInstance().getModules().loadModules();
                        sender.sendMessage(BATR.convertStringToComponent("BATR is now re-enabled ..."));
                    }

                    @Override
                    public void onProgress(ImportStatus progressStatus) {
                        sender.sendMessage(BATR.convertStringToComponent("&a" + new DecimalFormat("0.00").format(progressStatus.getProgressionPercent())
                                + "%&e entries converted:&a" + (progressStatus.getRemainingEntries())
                                + "&e remaining entries on a total of &6" + progressStatus.getTotalEntries()));
                    }

                    @Override
                    public void onMinorError(String errorMessage) {
                        sender.sendMessage(BATR.convertStringToComponent(errorMessage));
                    }
                }, UtilsKt.getFinalArg(args, 1));
            } else {
                throw new IllegalArgumentException("The specified source is incorrect. Available sources: &a"
                        + Joiner.on("&e,&a").join(importers.keySet()));
            }
        }
    }

    public static class BackupCmd extends BATCommand {
        public BackupCmd() {
            super("backup", "", "Backup the BATR's data from the mysql database into a file", "bat.backup");
        }

        @Override
        public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
                throws IllegalArgumentException {
            if (DataSourceHandler.isSQLite()) {
                throw new IllegalArgumentException("You can't backup an SQLite database with this command. "
                        + "To save an SQLite database just copy and paste the file 'bat_database.db'.");
            }
            sender.sendMessage(BATR.convertStringToComponent("Starting backup of BATR datas ..."));
            BATR.getInstance().getDsHandler().generateMysqlBackup((result, throwable) -> sender.sendMessage(BATR.convertStringToComponent(result)));
        }
    }

    @RunAsync
    public static class MigrateCmd extends BATCommand {
        public MigrateCmd() {
            super("migrateToMysql", "", "Migrate from sqlite to mysql (one-way conversion)", "bat.import");
        }

        @Override
        public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
            boolean isImportSimpleAlias = BATR.getInstance().getConfiguration().get(Configuration.SIMPLE_ALIASES_COMMANDS).get("import");
            ProxyServer.getInstance().getPluginManager().dispatchCommand(sender,
                    ((!isImportSimpleAlias) ? "bat " : "") + "import BATSQLite");
        }
    }
}