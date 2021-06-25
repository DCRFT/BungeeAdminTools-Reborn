package me.starmism.batr.modules.ban;

import me.starmism.batr.BATR;
import me.starmism.batr.database.DataSourceHandler;
import me.starmism.batr.database.SQLQueries;
import me.starmism.batr.modules.IModule;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This task handle the tempban's state update.
 */
public record BanExpirationTask(Ban ban) implements Runnable {

	@Override
	public void run() {
		Statement statement = null;
		try (Connection conn = BATR.getConnection()) {
			statement = conn.createStatement();
			if (DataSourceHandler.isSQLite()) {
				statement.executeUpdate(SQLQueries.Ban.SQLite.updateExpiredBan);
			} else {
				statement.executeUpdate(SQLQueries.Ban.updateExpiredBan);
			}
			statement.close();
		} catch (final SQLException e) {
			DataSourceHandler.handleException(e);
		} finally {
			DataSourceHandler.close(statement);
		}

		// Check if the online players are banned (if modifications have been made from the WebInterface)
		for (final ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
			final List<String> serversToCheck;
			if (player.getServer() != null) {
				serversToCheck = Arrays.asList(player.getServer().getInfo().getName(), IModule.GLOBAL_SERVER);
			} else {
				serversToCheck = Collections.singletonList(IModule.GLOBAL_SERVER);
			}
			for (final String server : serversToCheck) {
				if (ban.isBan(player, server)) {
					if (server.equals(player.getPendingConnection().getListener().getDefaultServer()) || server.equals(IModule.GLOBAL_SERVER)) {
						player.disconnect(ban.getBanMessage(player.getPendingConnection(), server));
						continue;
					}
					player.sendMessage(ban.getBanMessage(player.getPendingConnection(), server));
					player.connect(ProxyServer.getInstance().getServerInfo(player.getPendingConnection().getListener().getDefaultServer()));
				}
			}
		}
	}
}