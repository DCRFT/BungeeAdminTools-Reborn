package me.starmism.batr.modules.kick;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import me.mattstudios.config.SettingsManager;
import me.starmism.batr.BATR;
import me.starmism.batr.database.DataSourceHandler;
import me.starmism.batr.database.SQLQueries;
import me.starmism.batr.i18n.I18n;
import me.starmism.batr.modules.BATCommand;
import me.starmism.batr.modules.IModule;
import me.starmism.batr.modules.core.Core;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Kick implements IModule {
	private final SettingsManager config;
    private KickCommand commandHandler;
    private final I18n i18n;

    public Kick() {
        config = SettingsManager.from(Path.of(BATR.getInstance().getDataFolder().getPath(), "kick.yml"))
				.configurationData(KickConfig.class).create();
        i18n = BATR.getInstance().getI18n();
    }

    @Override
    public List<BATCommand> getCommands() {
        return commandHandler.getCmds();
    }

    @Override
    public String getMainCommand() {
        return "kick";
    }

    @Override
    public String getName() {
		return "kick";
    }

    @Override
    public SettingsManager getConfig() {
        return config;
    }

    @Override
    public boolean isEnabled() {
        return config.get(KickConfig.ENABLED);
    }

    @Override
    public boolean load() {
        // Init table
        Statement statement = null;
        try (Connection conn = BATR.getConnection()) {
            statement = conn.createStatement();
            if (DataSourceHandler.isSQLite()) {
                for (final String query : SQLQueries.Kick.SQLite.createTable) {
                    statement.executeUpdate(query);
                }
            } else {
                statement.executeUpdate(SQLQueries.Kick.createTable);
            }
            statement.close();
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement);
        }

        // Register commands
        commandHandler = new KickCommand(this);
        commandHandler.loadCmds();

        return true;
    }

    @Override
    public boolean unload() {
        return false;
    }

    /**
     * Kick a player and tp him to the default server
     */
    public String kick(final ProxiedPlayer player, final String staff, final String reason) {
        player.connect(ProxyServer.getInstance().getServerInfo(
                player.getPendingConnection().getListener().getDefaultServer()));
        player.sendMessage(TextComponent.fromLegacyText(i18n.format("wasKickedNotif", new String[]{reason})));
        return kickSQL(player.getUniqueId(), player.getServer().getInfo().getName(), staff, reason);
    }

    public String kickSQL(final UUID pUUID, final String server, final String staff, final String reason) {
        PreparedStatement statement = null;
        try (Connection conn = BATR.getConnection()) {
            if (DataSourceHandler.isSQLite()) {
                statement = conn.prepareStatement(SQLQueries.Kick.SQLite.kickPlayer);
            } else {
                statement = conn.prepareStatement(SQLQueries.Kick.kickPlayer);
            }
            statement.setString(1, pUUID.toString().replace("-", ""));
            statement.setString(2, staff);
            statement.setString(3, reason);
            statement.setString(4, server);
            statement.executeUpdate();
            statement.close();

            return i18n.format("kickBroadcast", new String[]{Core.getPlayerName(pUUID.toString().replace("-", "")), staff, server, reason});
        } catch (final SQLException e) {
            return DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement);
        }
    }

    /**
     * Kick a player from the network
     *
     * @param player
     * @param reason
     */
    public String gKick(final ProxiedPlayer player, final String staff, final String reason) {
        final String message = gKickSQL(player.getUniqueId(), staff, reason);
        player.disconnect(TextComponent.fromLegacyText(i18n.format("wasKickedNotif", new String[]{reason})));
        return message;
    }

    public String gKickSQL(final UUID pUUID, final String staff, final String reason) {
        PreparedStatement statement = null;
        try (Connection conn = BATR.getConnection()) {
            if (DataSourceHandler.isSQLite()) {
                statement = conn.prepareStatement(SQLQueries.Kick.SQLite.kickPlayer);
            } else {
                statement = conn.prepareStatement(SQLQueries.Kick.kickPlayer);
            }
            statement.setString(1, pUUID.toString().replace("-", ""));
            statement.setString(2, staff);
            statement.setString(3, reason);
            statement.setString(4, GLOBAL_SERVER);
            statement.executeUpdate();
            statement.close();

            if (BATR.getInstance().getRedis().isRedisEnabled()) {
                return i18n.format("gKickBroadcast", new String[]{RedisBungee.getApi().getNameFromUuid(pUUID), staff, reason});
            } else {
                return i18n.format("gKickBroadcast", new String[]{BATR.getInstance().getProxy().getPlayer(pUUID).getName(), staff, reason});
            }
        } catch (final SQLException e) {
            return DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement);
        }
    }

    /**
     * Get all kick data of a player <br>
     * <b>Should be runned async to optimize performance</b>
     *
     * @param pName Player's name
     * @return List of KickEntry of the player
     */
    public List<KickEntry> getKickData(final String pName) {
        final List<KickEntry> kickList = new ArrayList<>();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try (Connection conn = BATR.getConnection()) {
            statement = conn.prepareStatement(DataSourceHandler.isSQLite()
                    ? SQLQueries.Kick.SQLite.getKick
                    : SQLQueries.Kick.getKick);
            statement.setString(1, Core.getUUID(pName));
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                final String server = resultSet.getString("kick_server");
                String reason = resultSet.getString("kick_reason");
                if (reason == null) {
                    reason = NO_REASON;
                }
                final String staff = resultSet.getString("kick_staff");
                final Timestamp date;
                if (DataSourceHandler.isSQLite()) {
                    date = new Timestamp(resultSet.getLong("strftime('%s',kick_date)") * 1000);
                } else {
                    date = resultSet.getTimestamp("kick_date");
                }
                kickList.add(new KickEntry(pName, server, reason, staff, date));
            }
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement, resultSet);
        }
        return kickList;
    }

    public List<KickEntry> getManagedKick(final String staff) {
        final List<KickEntry> kickList = new ArrayList<>();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try (Connection conn = BATR.getConnection()) {
            statement = conn.prepareStatement(DataSourceHandler.isSQLite()
                    ? SQLQueries.Kick.SQLite.getManagedKick
                    : SQLQueries.Kick.getManagedKick);
            statement.setString(1, staff);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                final String server = resultSet.getString("kick_server");
                String reason = resultSet.getString("kick_reason");
                if (reason == null) {
                    reason = NO_REASON;
                }
                String pName = Core.getPlayerName(resultSet.getString("UUID"));
                if (pName == null) {
                    pName = "UUID:" + resultSet.getString("UUID");
                }
                final Timestamp date;
                if (DataSourceHandler.isSQLite()) {
                    date = new Timestamp(resultSet.getLong("strftime('%s',kick_date)") * 1000);
                } else {
                    date = resultSet.getTimestamp("kick_date");
                }
                kickList.add(new KickEntry(pName, server, reason, staff, date));
            }
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement, resultSet);
        }
        return kickList;
    }
}