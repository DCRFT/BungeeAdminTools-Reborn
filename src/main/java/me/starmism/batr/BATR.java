package me.starmism.batr;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import me.mattstudios.config.SettingsManager;
import me.starmism.batr.database.DataSourceHandler;
import me.starmism.batr.i18n.I18n;
import me.starmism.batr.modules.ModulesManager;
import me.starmism.batr.modules.core.Core;
import me.starmism.batr.utils.CallbackUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main class BungeeAdminTools
 *
 * @author Alphart
 */
public class BATR extends Plugin {
    private static BATR instance;
    private static DataSourceHandler dsHandler;
    private static String prefix;
    // This way we can check at runtime if the required BC build (or a higher one) is installed
    private final int requiredBCBuild = 1576;
    private SettingsManager config;
    private ModulesManager modules;
    private I18n i18n;

    public static BATR getInstance() {
        return instance;
    }

    public static BaseComponent[] convertStringToComponent(final String message) {
        return TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }

    /**
     * Send a broadcast message to everyone with the given perm <br>
     * Also broadcast through Redis if it's installed that's why this method <strong>should not be called
     * from a Redis call</strong> otherwise it will broadcast it again and again
     *
     * @param message
     * @param perm
     */
    public static void broadcast(final String message, final String perm) {
        noRedisBroadcast(message, perm);
    }

    public static void noRedisBroadcast(final String message, final String perm) {
        final BaseComponent[] bsMsg = convertStringToComponent(message);
        for (final ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
            if (p.hasPermission(perm) || p.hasPermission("bat.admin")) {
                p.sendMessage(bsMsg);
            }
            // If he has a grantall permission, he will have the broadcast on all the servers
            else {
                for (final String playerPerm : Core.getCommandSenderPermission(p)) {
                    if (playerPerm.startsWith("bat.grantall.")) {
                        p.sendMessage(bsMsg);
                        break;
                    }
                }
            }
        }
        getInstance().getLogger().info(ChatColor.translateAlternateColorCodes('&', message));
    }

    public static Connection getConnection() {
        return dsHandler.getConnection();
    }

    /**
     * Kick a player from the proxy for a specified reason
     *
     * @param player
     * @param reason
     */
    public static void kick(final ProxiedPlayer player, final String reason) {
        if (reason == null || reason.equals("")) {
            player.disconnect(TextComponent.fromLegacyText("You have been kicked from the server."));
        } else {
            player.disconnect(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', reason)));
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        config = SettingsManager.from(Path.of(instance.getDataFolder().getPath(), "config.yml")).configurationData(Configuration.class).create();
        getLogger().setLevel(Level.INFO);
        // Init the I18n module
        i18n = new I18n();

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        if (!ProxyServer.getInstance().getName().equals("BungeeCord")) {
            getLogger().warning("BungeeCord version check disabled because a fork has been detected."
                    + " Make sure your fork is based on a BungeeCord build > #" + requiredBCBuild);
        } else if (getBCBuild() < requiredBCBuild && !new File(getDataFolder(), "skipversiontest").exists()) {
            getLogger().severe("Your BungeeCord build (#" + getBCBuild() + ") is not supported. Please use at least BungeeCord #" + requiredBCBuild);
            getLogger().severe("If you want to skip that test, create a file named 'skipversiontest' in BATR directory.");
            getLogger().severe("BATR is going to shutdown ...");
            return;
        }
        if (config.get(Configuration.DEBUG_MODE)) {
            try {
                final File debugFile = new File(getDataFolder(), "debug.log");
                if (debugFile.exists()) {
                    debugFile.delete();
                }
                // Write header into debug log
                Files.asCharSink(debugFile, Charsets.UTF_8).writeLines(Arrays.asList("BATR log debug file"
                                + " - If you have an error with BATR, you should post this file on BATR topic on spigotmc",
                        "Bungee build: " + ProxyServer.getInstance().getVersion(),
                        "BATR version: " + getDescription().getVersion(),
                        "Operating System: " + System.getProperty("os.name"),
                        "Timezone: " + TimeZone.getDefault().getID(),
                        "------------------------------------------------------------"));
                final FileHandler handler = new FileHandler(debugFile.getAbsolutePath(), true);
                handler.setFormatter(new Formatter() {
                    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

                    @Override
                    public String format(LogRecord record) {
                        String pattern = "time [level] message\n";
                        return pattern.replace("level", record.getLevel().getName())
                                .replace("message", record.getMessage())
                                .replace("[BungeeAdminTools]", "")
                                .replace("time", sdf.format(Calendar.getInstance().getTime()));
                    }
                });
                getLogger().addHandler(handler);
                getLogger().setLevel(Level.CONFIG);
                getLogger().info("The debug mode is now enabled! Logs are available in the debug.log file in the plugin directory.");
                getLogger().config("Debug mode enabled...");
                getLogger().setUseParentHandlers(false);
            } catch (final Exception e) {
                getLogger().log(Level.SEVERE, "An exception occurred during the initialization of debug logging file", e);
            }
        }
        prefix = config.get(Configuration.PREFIX);
        loadDB((dbState, throwable) -> {
            if (dbState) {
                getLogger().config("Connection to the database established");
                // Try enabling redis support.
                modules = new ModulesManager();
                modules.loadModules();
            } else {
                getLogger().severe("BATR is gonna shutdown because it can't connect to the database.");
                return;
            }
        });
    }

    public int getBCBuild() {
        final Pattern p = Pattern.compile(".*?:(.*?:){3}(\\d*)");
        final Matcher m = p.matcher(ProxyServer.getInstance().getVersion());
        int BCBuild;
        try {
            if (m.find()) {
                BCBuild = Integer.parseInt(m.group(2));
            } else {
                throw new NumberFormatException();
            }
        } catch (final NumberFormatException e) {
            // We can't determine BC build, just display a message, and set the build so it doesn't trigger the security
            getLogger().info("BC build can't be detected. If you encounter any problems, please report that message. Otherwise don't take into account");
            BCBuild = requiredBCBuild;
        }
        return BCBuild;
    }

    @Override
    public void onDisable() {
        modules.unloadModules();
        instance = null;
    }

    public void loadDB(final CallbackUtils.Callback<Boolean> dbState) {
        if (config.get(Configuration.MYSQL_ENABLED)) {
            getLogger().config("Starting connection to the mysql database ...");
            final String username = config.get(Configuration.MYSQL_USER);
            final String password = config.get(Configuration.MYSQL_PASSWORD);
            final String database = config.get(Configuration.MYSQL_DATABASE);
            final String port = config.get(Configuration.MYSQL_PORT);
            final String host = config.get(Configuration.MYSQL_HOST);

            // BoneCP can accept no database and we want to avoid that
            Preconditions.checkArgument(!"".equals(database), "You must set the database.");
            ProxyServer.getInstance().getScheduler().runAsync(this, () -> {
                try {
                    dsHandler = new DataSourceHandler(host, port, database, username, password);
                    final Connection c = dsHandler.getConnection();
                    if (c != null) {
                        c.close();
                        dbState.done(true, null);
                    }
                } catch (final SQLException e) {
                    getLogger().severe("The connection pool (database connection) wasn't able to be launched!");
                    dbState.done(false, null);
                }
            });
        }

        // If MySQL is disabled, we are gonna use SQLite
        // Before initialize the connection, we must download the sqlite driver
        // (if it isn't already in the lib folder) and load it
        else {
            getLogger().config("Starting connection to the sqlite database...");
            getLogger().warning("It is strongly *NOT RECOMMENDED* to use SQLite with BATR,"
                    + " as the SQLite implementation is less stable and much slower than the MySQL implementation.");
            if (loadSQLiteDriver()) {
                dsHandler = new DataSourceHandler();
                dbState.done(true, null);
            } else {
                dbState.done(false, null);
            }
        }
    }

    public boolean loadSQLiteDriver() {
        // Load the driver
        try {
            Class.forName("org.sqlite.JDBC");
            return true;
        } catch (final Throwable t) {
            getLogger().severe("The sqlite driver cannot be loaded. Please report this error: ");
            t.printStackTrace();
            return false;
        }
    }

    public void reloadI18n() {
        i18n = new I18n();
    }

    public ModulesManager getModules() {
        return modules;
    }

    public SettingsManager getConfiguration() {
        return config;
    }

    public DataSourceHandler getDsHandler() {
        return dsHandler;
    }

    public I18n getI18n() {
        return i18n;
    }

}