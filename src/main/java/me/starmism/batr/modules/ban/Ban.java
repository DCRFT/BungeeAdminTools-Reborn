package me.starmism.batr.modules.ban;

import com.google.common.base.Charsets;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import me.mattstudios.config.SettingsManager;
import me.starmism.batr.BATR;
import me.starmism.batr.database.DataSourceHandler;
import me.starmism.batr.database.SQLQueries;
import me.starmism.batr.i18n.I18n;
import me.starmism.batr.modules.BATCommand;
import me.starmism.batr.modules.IModule;
import me.starmism.batr.modules.core.Core;
import me.starmism.batr.utils.FormatUtilsKt;
import me.starmism.batr.utils.UUIDNotFoundException;
import me.starmism.batr.utils.UtilsKt;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;

import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Ban implements IModule, Listener {
	private final SettingsManager config;
    private ScheduledTask task;
    private BanCommand commandHandler;
    private I18n i18n;

    public Ban() {
        config = SettingsManager.from(Path.of(BATR.getInstance().getDataFolder().getPath(), "ban.yml"))
				.configurationData(BanConfig.class).create();
        i18n = BATR.getInstance().getI18n();
    }

    @Override
    public List<BATCommand> getCommands() {
        return commandHandler.getCmds();
    }

    @Override
    public String getName() {
		return "ban";
    }

    @Override
    public String getMainCommand() {
        return "ban";
    }

    @Override
    public SettingsManager getConfig() {
        return config;
    }

    @Override
    public boolean isEnabled() {
        return config.get(BanConfig.ENABLED);
    }

    @Override
    public boolean load() {
        // Init table
        try (Connection conn = BATR.getConnection()) {
            final Statement statement = conn.createStatement();
            if (DataSourceHandler.isSQLite()) {
                for (final String query : SQLQueries.Ban.SQLite.createTable) {
                    statement.executeUpdate(query);
                }
            } else {
                statement.executeUpdate(SQLQueries.Ban.createTable);
            }
            statement.close();
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        }

        // Register commands
        commandHandler = new BanCommand(this);
        commandHandler.loadCmds();

        // Launch tempban task
        final BanExpirationTask banExpirationTask = new BanExpirationTask(this);
        task = ProxyServer.getInstance().getScheduler().schedule(BATR.getInstance(), banExpirationTask, 0, 10, TimeUnit.SECONDS);

        // Check if the online players are banned (if the module has been reloaded)
        for (final ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
            final List<String> serversToCheck = player.getServer() != null
                    ? Arrays.asList(player.getServer().getInfo().getName(), GLOBAL_SERVER)
                    : Collections.singletonList(GLOBAL_SERVER);
            for (final String server : serversToCheck) {
                if (isBan(player, server)) {
                    if (server.equals(player.getPendingConnection().getListener().getServerPriority().get(0)) || server.equals(GLOBAL_SERVER)) {
                        player.disconnect(getBanMessage(player.getPendingConnection(), server));
                        continue;
                    }
                    player.sendMessage(getBanMessage(player.getPendingConnection(), server));
                    player.connect(ProxyServer.getInstance().getServerInfo(player.getPendingConnection().getListener().getServerPriority().get(0)));
                }
            }
        }

        return true;
    }

    @Override
    public boolean unload() {
        task.cancel();
        return true;
    }

    public BaseComponent[] getBanMessage(final PendingConnection pConn, final String server) {
        String reason = "";
        Timestamp expiration = null;
        Timestamp begin = null;
        String staff = null;

        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try (Connection conn = BATR.getConnection()) {
            statement = conn.prepareStatement(DataSourceHandler.isSQLite()
                    ? SQLQueries.Ban.SQLite.getBanMessage
                    : SQLQueries.Ban.getBanMessage);
            try {
                final UUID pUUID;
                if (pConn.getUniqueId() != null) {
                    pUUID = pConn.getUniqueId();
                } else {
                    pUUID = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + pConn.getName()).getBytes(Charsets.UTF_8));
                }
                statement.setString(1, pUUID.toString().replace("-", ""));
                statement.setString(2, pConn.getAddress().getAddress().getHostAddress());
                statement.setString(3, server);
            } catch (final UUIDNotFoundException e) {
                BATR.getInstance().getLogger().severe("Error during retrieving of the UUID of " + pConn.getName() + ". Please report this error :");
                e.printStackTrace();
            }
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                if (DataSourceHandler.isSQLite()) {
                    begin = new Timestamp(resultSet.getLong("strftime('%s',ban_begin)") * 1000);
                    String endStr = resultSet.getString("ban_end"); // SQLite see this row as null but it doesn't seem to make the same with ban message though it's almost the same code ...
                    expiration = (endStr == null) ? null : new Timestamp(Long.parseLong(endStr));
                } else {
                    begin = resultSet.getTimestamp("ban_begin");
                    expiration = resultSet.getTimestamp("ban_end");
                }
                reason = (resultSet.getString("ban_reason") != null) ? resultSet.getString("ban_reason") : IModule.NO_REASON;
                staff = resultSet.getString("ban_staff");
            } else {
                throw new SQLException("No active ban found.");
            }
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement, resultSet);
        }
        if (expiration != null) {
            return TextComponent.fromLegacyText(i18n.format("isBannedTemp",
                    new String[]{reason, (expiration.getTime() < System.currentTimeMillis()) ? "a few moments" : FormatUtilsKt.getDuration(expiration.getTime()),
                            Core.defaultDF.format(begin), staff}));
        } else {
            return TextComponent.fromLegacyText(i18n.format("isBanned", new String[]{reason, Core.defaultDF.format(begin), staff}));
        }
    }

    /**
     * Check if both ip and name of this player are banned
     *
     * @param player
     * @param server
     * @return true if name or ip is banned
     */
    public boolean isBan(final ProxiedPlayer player, final String server) {
        final String ip = Core.getPlayerIP(player.getName());
		return isBan(player.getName(), server) || isBan(ip, server);
	}

    /**
     * Check if this entity (player or ip) is banned
     *
     * @param bannedEntity | can be an ip or a player name
     * @param server       | if server equals to (any) check if the player is ban on a
     *                     server
     * @return
     */
    public boolean isBan(final String bannedEntity, final String server) {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try (Connection conn = BATR.getConnection()) {
            // If this is an ip which may be banned
            if (UtilsKt.validIP(bannedEntity)) {
				statement = conn.prepareStatement((ANY_SERVER.equals(server)) ? SQLQueries.Ban.isBanIP
                        : SQLQueries.Ban.isBanServerIP);
                statement.setString(1, bannedEntity);
			}
            // If this is a player which may be banned
            else {
				final String uuid = Core.getUUID(bannedEntity);
                statement = conn.prepareStatement((ANY_SERVER.equals(server)) ? SQLQueries.Ban.isBan
                        : SQLQueries.Ban.isBanServer);
                statement.setString(1, uuid);
			}
			if (!ANY_SERVER.equals(server)) {
				statement.setString(2, server);
			}
			resultSet = statement.executeQuery();

            // If there are a result
            if (resultSet.next()) {
                return true;
            }

        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement, resultSet);
        }
        return false;
    }

    /**
     * Ban this entity (player or ip) <br>
     *
     * @param bannedEntity        | can be an ip or a player name
     * @param server              ; set to "(global)", to global ban
     * @param staff
     * @param expirationTimestamp ; set to 0 for ban def
     * @param reason              | optional
     * @return
     */
    public String ban(String bannedEntity, final String server, final String staff,
                      final long expirationTimestamp, final String reason) {
        try (Connection conn = BATR.getConnection()) {
            // If the bannedEntity is an ip
            if (UtilsKt.validIP(bannedEntity)) {
                System.out.println("tescik");

				final PreparedStatement statement = conn.prepareStatement(SQLQueries.Ban.createBanIP);
                statement.setString(1, bannedEntity);
                statement.setString(2, staff);
                statement.setString(3, server);
                statement.setTimestamp(4, (expirationTimestamp > 0) ? new Timestamp(expirationTimestamp) : null);
                statement.setString(5, (NO_REASON.equals(reason)) ? null : reason);
                statement.executeUpdate();
                statement.close();

                for (final ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                    if (UtilsKt.getPlayerIP(player).equals(bannedEntity) && (GLOBAL_SERVER.equals(server) || server.equalsIgnoreCase(player.getServer().getInfo().getName()))) {
                        BATR.kick(player, i18n.format("wasBannedNotif", new String[]{reason}));
                    }
                }

                bannedEntity = "Ukryte IP";
			}

            // Otherwise it's a player
            else {
                final String sUUID = Core.getUUID(bannedEntity);
                final ProxiedPlayer player = UtilsKt.getPlayer(bannedEntity);
                final PreparedStatement statement = conn.prepareStatement(SQLQueries.Ban.createBan);
                statement.setString(1, sUUID);
                statement.setString(2, staff);
                statement.setString(3, server);
                statement.setTimestamp(4, (expirationTimestamp > 0) ? new Timestamp(expirationTimestamp) : null);
                statement.setString(5, (NO_REASON.equals(reason)) ? null : reason);
                statement.executeUpdate();
                statement.close();

                // Kick player if he's online and on the server where he's
                // banned
                if (player != null
                        && (server.equals(GLOBAL_SERVER) || player.getServer().getInfo().getName().equalsIgnoreCase(server))) {
                    BATR.kick(player, i18n.format("wasBannedNotif", new String[]{reason}));
                }

			}
			if (expirationTimestamp > 0) {
				return i18n.format("banTempBroadcast", new String[]{bannedEntity, FormatUtilsKt.getDuration(expirationTimestamp),
						staff, server, reason});
			} else {
				return i18n.format("banBroadcast", new String[]{bannedEntity, staff, server, reason});
			}
		} catch (final SQLException e) {
            return DataSourceHandler.handleException(e);
        }
    }

    /**
     * Ban the ip of an online player
     *
     * @param server              ; set to "(global)", to global ban
     * @param staff
     * @param expirationTimestamp ; set to 0 for ban def
     * @param reason              | optional
     * @param player
     */
    public String banIP(final ProxiedPlayer player, final String server, final String staff,
                        final long expirationTimestamp, final String reason) {
        ban(UtilsKt.getPlayerIP(player), server, staff, expirationTimestamp, reason);
        return i18n.format("banBroadcast", new String[]{"Ukryte IP", staff, server, reason});
    }


    /**
     * Unban an entity (player or ip)
     *
     * @param bannedEntity | can be an ip or a player name
     * @param server       | if equals to (any), unban from all servers | if equals to
     *                     (global), remove global ban
     * @param staff
     * @param reason
     */
    public String unBan(String bannedEntity, final String server, final String staff, final String reason) {
        PreparedStatement statement = null;
        try (Connection conn = BATR.getConnection()) {
            // If the bannedEntity is an ip
            if (UtilsKt.validIP(bannedEntity)) {
                if (ANY_SERVER.equals(server)) {
                    statement = (DataSourceHandler.isSQLite()) ? conn.prepareStatement(SQLQueries.Ban.SQLite.unBanIP)
                            : conn.prepareStatement(SQLQueries.Ban.unBanIP);
                    statement.setString(1, reason);
                    statement.setString(2, staff);
                    statement.setString(3, bannedEntity);
                } else {
                    statement = (DataSourceHandler.isSQLite()) ? conn
                            .prepareStatement(SQLQueries.Ban.SQLite.unBanIPServer) : conn
                            .prepareStatement(SQLQueries.Ban.unBanIPServer);
                    statement.setString(1, reason);
                    statement.setString(2, staff);
                    statement.setString(3, bannedEntity);
                    statement.setString(4, server);
                }
                statement.executeUpdate();
                bannedEntity = "Ukryte IP";

            }

            // Otherwise it's a player
            else {
                final String UUID = Core.getUUID(bannedEntity);
                if (ANY_SERVER.equals(server)) {
                    statement = (DataSourceHandler.isSQLite()) ? conn.prepareStatement(SQLQueries.Ban.SQLite.unBan)
                            : conn.prepareStatement(SQLQueries.Ban.unBan);
                    statement.setString(1, reason);
                    statement.setString(2, staff);
                    statement.setString(3, UUID);
                } else {
                    statement = (DataSourceHandler.isSQLite()) ? conn
                            .prepareStatement(SQLQueries.Ban.SQLite.unBanServer) : conn
                            .prepareStatement(SQLQueries.Ban.unBanServer);
                    statement.setString(1, reason);
                    statement.setString(2, staff);
                    statement.setString(3, UUID);
                    statement.setString(4, server);
                }
                statement.executeUpdate();

            }
            return i18n.format("unbanBroadcast", new String[]{bannedEntity, staff, server, reason});
        } catch (final SQLException e) {
            return DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement);
        }

    }

    /**
     * Unban the ip of this entity
     *
     * @param entity
     * @param server | if equals to (any), unban from all servers | if equals to
     *               (global), remove global ban
     * @param staff
     * @param reason | optional
     */
    public String unBanIP(final String entity, final String server, final String staff, final String reason) {
        if (UtilsKt.validIP(entity)) {
            return unBan(entity, server, staff, reason);
        } else {
            unBan(Core.getPlayerIP(entity), server, staff, reason);
            return i18n.format("unbanBroadcast", new String[]{entity + "'s IP", staff, server, reason});
        }
    }

    /**
     * Get all ban data of an entity <br>
     * <b>Should be runned async to optimize performance</b>
     *
     * @param entity
     * @return List of BanEntry of the player
     */
    public List<BanEntry> getBanData(final String entity) {
        final List<BanEntry> banList = new ArrayList<>();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try (Connection conn = BATR.getConnection()) {
            // If the entity is an ip
            if (UtilsKt.validIP(entity)) {
                statement = conn.prepareStatement((DataSourceHandler.isSQLite())
                        ? SQLQueries.Ban.SQLite.getBanIP
                        : SQLQueries.Ban.getBanIP);
                statement.setString(1, entity);
			}
            // Otherwise if it's a player
            else {
                statement = conn.prepareStatement((DataSourceHandler.isSQLite())
                        ? SQLQueries.Ban.SQLite.getBan
                        : SQLQueries.Ban.getBan);
                statement.setString(1, Core.getUUID(entity));
			}
			resultSet = statement.executeQuery();

			while (resultSet.next()) {
                final Timestamp beginDate;
                final Timestamp endDate;
                final Timestamp unbanDate;
                if (DataSourceHandler.isSQLite()) {
                    beginDate = new Timestamp(resultSet.getLong("strftime('%s',ban_begin)") * 1000);
                    String endStr = resultSet.getString("ban_end");
                    endDate = (endStr == null) ? null : new Timestamp(Long.parseLong(endStr));
                    long unbanLong = resultSet.getLong("strftime('%s',ban_unbandate)") * 1000;
                    unbanDate = (unbanLong == 0) ? null : new Timestamp(unbanLong);
                } else {
                    beginDate = resultSet.getTimestamp("ban_begin");
                    endDate = resultSet.getTimestamp("ban_end");
                    unbanDate = resultSet.getTimestamp("ban_unbandate");
                }


                // Make it compatible with sqlite (date: get an int with the sfrt and then construct a tiemstamp)
                final String server = resultSet.getString("ban_server");
                String reason = resultSet.getString("ban_reason");
                if (reason == null) {
                    reason = NO_REASON;
                }
                final String staff = resultSet.getString("ban_staff");
                final boolean active = (resultSet.getBoolean("ban_state"));
                String unbanReason = resultSet.getString("ban_unbanreason");
                if (unbanReason == null) {
                    unbanReason = NO_REASON;
                }
                final String unbanStaff = resultSet.getString("ban_unbanstaff");
                banList.add(new BanEntry(entity, server, reason, staff, beginDate, endDate, unbanDate, unbanReason, unbanStaff, active));
            }
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement, resultSet);
        }
        return banList;
    }

    public List<BanEntry> getManagedBan(final String staff) {
        final List<BanEntry> banList = new ArrayList<>();
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try (Connection conn = BATR.getConnection()) {
            statement = conn.prepareStatement((DataSourceHandler.isSQLite())
                    ? SQLQueries.Ban.SQLite.getManagedBan
                    : SQLQueries.Ban.getManagedBan);
            statement.setString(1, staff);
            statement.setString(2, staff);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                final Timestamp beginDate;
                final Timestamp endDate;
                final Timestamp unbanDate;
                if (DataSourceHandler.isSQLite()) {
                    beginDate = new Timestamp(resultSet.getLong("strftime('%s',ban_begin)") * 1000);
                    String endStr = resultSet.getString("ban_end");
                    endDate = (endStr == null) ? null : new Timestamp(Long.parseLong(endStr));
                    long unbanLong = resultSet.getLong("strftime('%s',ban_unbandate)") * 1000;
                    unbanDate = (unbanLong == 0) ? null : new Timestamp(unbanLong);
                } else {
                    beginDate = resultSet.getTimestamp("ban_begin");
                    endDate = resultSet.getTimestamp("ban_end");
                    unbanDate = resultSet.getTimestamp("ban_unbandate");
                }


                // Make it compatible with sqlite (date: get an int with the sfrt and then construct a tiemstamp)
                final String server = resultSet.getString("ban_server");
                String reason = resultSet.getString("ban_reason");
                if (reason == null) {
                    reason = NO_REASON;
                }
                String entity = (resultSet.getString("ban_ip") != null)
                        ? resultSet.getString("ban_ip")
                        : Core.getPlayerName(resultSet.getString("UUID"));
                // If the UUID search failed
                if (entity == null) {
                    entity = "UUID:" + resultSet.getString("UUID");
                }
                final boolean active = (resultSet.getBoolean("ban_state"));
                String unbanReason = resultSet.getString("ban_unbanreason");
                if (unbanReason == null) {
                    unbanReason = NO_REASON;
                }
                final String unbanStaff = resultSet.getString("ban_unbanstaff");
                banList.add(new BanEntry(entity, server, reason, staff, beginDate, endDate, unbanDate, unbanReason, unbanStaff, active));
            }
        } catch (final SQLException e) {
            DataSourceHandler.handleException(e);
        } finally {
            DataSourceHandler.close(statement, resultSet);
        }
        return banList;
    }

    @EventHandler
    public void onServerConnect(final ServerConnectEvent e) {
        final ProxiedPlayer player = e.getPlayer();
        final String target = e.getTarget().getName();

        if (isBan(player, target)) {
            if (target.equals(player.getPendingConnection().getListener().getServerPriority().get(0))) {
                // If it's player's join server kick him
                if (e.getPlayer().getServer() == null) {
                    e.setCancelled(true);
                    // Need to delay for avoiding the "bit cannot be cast to fm exception" and to annoy the banned player :p
                    ProxyServer.getInstance().getScheduler().schedule(BATR.getInstance(), () ->
							e.getPlayer().disconnect(getBanMessage(player.getPendingConnection(), target)), 500, TimeUnit.MILLISECONDS);
                } else {
                    e.setCancelled(true);
                    e.getPlayer().sendMessage(getBanMessage(player.getPendingConnection(), target));
                }
                return;
            }
            player.sendMessage(getBanMessage(player.getPendingConnection(), target));
            if (player.getServer() == null) {
                player.connect(ProxyServer.getInstance().getServerInfo(
                        player.getPendingConnection().getListener().getServerPriority().get(0)));
            }
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerLogin(final LoginEvent ev) {
        ev.registerIntent(BATR.getInstance());
        BATR.getInstance().getProxy().getScheduler().runAsync(BATR.getInstance(), () -> {
			boolean isBanPlayer = false;

			PreparedStatement statement = null;
			ResultSet resultSet = null;
			UUID uuid;
			try (Connection conn = BATR.getConnection()) {
				statement = conn.prepareStatement("SELECT ban_id FROM `BAT_ban` WHERE ban_state = 1 AND UUID = ? AND ban_server = '" + GLOBAL_SERVER + "';");
				// If this is an online mode server, the uuid will be already set
				if (ev.getConnection().getUniqueId() != null) {
					uuid = ev.getConnection().getUniqueId();
				}
				// Otherwise it's an offline mode server, so we're gonna generate the UUID using player name (hashing)
				else {
					uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + ev.getConnection().getName()).getBytes(Charsets.UTF_8));
				}
				statement.setString(1, uuid.toString().replaceAll("-", ""));

				resultSet = statement.executeQuery();
				if (resultSet.next()) {
					isBanPlayer = true;
				}
			} catch (SQLException e) {
				DataSourceHandler.handleException(e);
			} finally {
				DataSourceHandler.close(statement, resultSet);
			}

			if ((isBanPlayer) || (isBan(ev.getConnection().getAddress().getAddress().getHostAddress(), GLOBAL_SERVER))) {
				BaseComponent[] bM = getBanMessage(ev.getConnection(), GLOBAL_SERVER);
				ev.setCancelReason(TextComponent.toLegacyText(bM));
				ev.setCancelled(true);
			}
			ev.completeIntent(BATR.getInstance());
		});
    }
}