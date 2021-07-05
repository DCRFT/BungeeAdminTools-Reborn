package me.starmism.batr.modules.core;

import com.google.common.base.Charsets;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import me.mattstudios.config.SettingsManager;
import me.starmism.batr.BATR;
import me.starmism.batr.Configuration;
import me.starmism.batr.database.DataSourceHandler;
import me.starmism.batr.database.SQLQueries;
import me.starmism.batr.modules.BATCommand;
import me.starmism.batr.modules.IModule;
import me.starmism.batr.utils.EnhancedDateFormat;
import me.starmism.batr.utils.MojangAPIProviderKt;
import me.starmism.batr.utils.UUIDNotFoundException;
import me.starmism.batr.utils.UtilsKt;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Core implements IModule, Listener {
	private List<BATCommand> cmds;
    public static EnhancedDateFormat defaultDF = new EnhancedDateFormat(false);
    private static final LoadingCache<String, String> uuidCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build(
                    new CacheLoader<>() {
                        public String load(final String pName) throws UUIDNotFoundException {
                            final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
                            if (player != null) {
                                // Note: if it's an offline server, the UUID will be generated using
                                // this
                                // function java.util.UUID.nameUUIDFromBytes, however it's an
                                // premium or cracked account
                                // Online server : bungee handle great the UUID
                                return player.getUniqueId().toString().replaceAll("-", "");
                            }

                            PreparedStatement statement = null;
                            ResultSet resultSet = null;
                            String UUID = "";
                            // Try to get the UUID from the BATR db
                            try (Connection conn = BATR.getConnection()) {
                                statement = conn.prepareStatement(SQLQueries.Core.getUUID);
                                statement.setString(1, pName);
                                resultSet = statement.executeQuery();
                                if (resultSet.next()) {
                                    UUID = resultSet.getString("UUID");
                                }
                            } catch (final SQLException e) {
                                DataSourceHandler.handleException(e);
                            } finally {
                                DataSourceHandler.close(statement, resultSet);
                            }

                            // If online server, retrieve the UUID from the mojang server
                            if (UUID.isEmpty() && ProxyServer.getInstance().getConfig().isOnlineMode()) {
                                UUID = MojangAPIProviderKt.getUUID(pName);
                                if (UUID == null) {
                                    throw new UUIDNotFoundException(pName);
                                }
                            }
                            // If offline server, generate the UUID
                            else if (UUID.isEmpty()) {
                                UUID = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + pName).getBytes(Charsets.UTF_8)).toString().replaceAll("-", "");
                            }

                            return UUID;
                        }
                    });

    /**
     * Get the UUID of the specified player
     *
     * @param pName
     * @return String which is the UUID
     * @throws UUIDNotFoundException
     */
    public static String getUUID(final String pName) {
        try {
            return uuidCache.get(pName);
        } catch (final Exception e) {
            try {
                return getUUIDfromString(pName).toString().replaceAll("-", "");
            } catch (IllegalArgumentException exception) {
                if (e.getCause() instanceof UUIDNotFoundException) {
                    throw (UUIDNotFoundException) e.getCause();
                }
            }
        }
        return null;
    }

    /**
     * Convert an string uuid into an UUID object
     *
     * @param strUUID
     * @return UUID
     */
    public static UUID getUUIDfromString(final String strUUID) {
        final String dashesUUID = strUUID.replaceFirst(
                "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)", "$1-$2-$3-$4-$5");
        return UUID.fromString(dashesUUID);
    }

    /**
     * Get the player name from a UUID using the BATR database
     *
     * @param UUID
     * @return player name with this UUID or "unknowName"
     */
    public static String getPlayerName(final String UUID) {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try (Connection conn = BATR.getConnection()) {
            statement = conn.prepareStatement(SQLQueries.Core.getPlayerName);
            statement.setString(1, UUID);
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("BAT_player");
            }
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement, resultSet);
        }
        return null;
    }

    public static String getPlayerIP(final String pName) {
        if (BATR.getInstance().getRedis().isRedisEnabled()) {
            try {
                final UUID pUUID = RedisBungee.getApi().getUuidFromName(pName, true);
                if (pUUID != null && RedisBungee.getApi().isPlayerOnline(pUUID))
                    return RedisBungee.getApi().getPlayerIp(pUUID).getHostAddress();
            } catch (Exception exp) {
                exp.printStackTrace();
            }
        } else {
            final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
            if (player != null) return UtilsKt.getPlayerIP(player);
        }

        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try (Connection conn = BATR.getConnection()) {
            statement = conn.prepareStatement(SQLQueries.Core.getIP);
            statement.setString(1, getUUID(pName));
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("lastip");
            }
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement, resultSet);
        }
        return "0.0.0.0";
    }

    /**
     * Get the command sender permission list using bungee api
     *
     * @param sender
     * @return permission in a collection of strings
     */
    public static Collection<String> getCommandSenderPermission(final CommandSender sender) {
        return sender.getPermissions();
    }

    @Override
    public String getName() {
		return "core";
    }

    @Override
    public SettingsManager getConfig() {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean load() {
        // Init players table
        Statement statement = null;
        try (Connection conn = BATR.getConnection()) {
            statement = conn.createStatement();
            if (DataSourceHandler.isSQLite()) {
                for (final String coreQuery : SQLQueries.Core.SQLite.createTable) {
                    statement.executeUpdate(coreQuery);
                }
            } else {
                statement.executeUpdate(SQLQueries.Core.createTable);
            }
            statement.close();
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement);
        }

        // Register commands
        cmds = new ArrayList<>();
        cmds.add(new CoreCommand(this)); // Most of the job is done in the constructor of CoreCommand

        // Update the date format (if translation has been changed)
        defaultDF = new EnhancedDateFormat(BATR.getInstance().getConfiguration().get(Configuration.LITERAL_DATE));

        return true;
    }

    @Override
    public boolean unload() {

        return true;
    }

    @Override
    public List<BATCommand> getCommands() {
        return cmds;
    }

    @Override
    public String getMainCommand() {
        return "bat";
    }

    public void addCommand(final BATCommand cmd) {
        cmds.add(cmd);
    }

    /**
     * Update the IP and UUID of a player in the database
     *
     * @param player
     */
    public void updatePlayerIPandUUID(final ProxiedPlayer player) {
        PreparedStatement statement = null;
        try (Connection conn = BATR.getConnection()) {
            final String ip = UtilsKt.getPlayerIP(player);
            final String UUID = player.getUniqueId().toString().replaceAll("-", "");
            statement = (DataSourceHandler.isSQLite()) ? conn.prepareStatement(SQLQueries.Core.SQLite.updateIPUUID)
                    : conn.prepareStatement(SQLQueries.Core.updateIPUUID);
            statement.setString(1, player.getName());
            statement.setString(2, ip);
            statement.setString(3, UUID);
            statement.setString(4, (DataSourceHandler.isSQLite()) ? UUID : ip);
            if (!DataSourceHandler.isSQLite()) {
                statement.setString(5, player.getName());
            }
            statement.executeUpdate();
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement);
        }

    }

    // Event listener
    @EventHandler
    public void onPlayerJoin(final PostLoginEvent ev) {
        BATR.getInstance().getProxy().getScheduler().runAsync(BATR.getInstance(), () -> updatePlayerIPandUUID(ev.getPlayer()));
    }

    @EventHandler
    public void onPlayerLeft(final PlayerDisconnectEvent ev) {
        CommandQueue.clearQueuedCommand(ev.getPlayer());
    }
}