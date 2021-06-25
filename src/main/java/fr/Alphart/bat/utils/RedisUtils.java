package fr.Alphart.bat.utils;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import fr.Alphart.bat.BAT;
import fr.Alphart.bat.modules.InvalidModuleException;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.UUID;

public class RedisUtils implements Listener {

    private static final String channel = "BungeeAdminTools";
    private static final String split = "######";
    private final boolean redis;

    public RedisUtils(final boolean enable) {
        if (enable) {
            if (BAT.getInstance().getProxy().getPluginManager().getPlugin("RedisBungee") != null && RedisBungee.getApi() != null) {
                BAT.getInstance().getLogger().info("Detected RedisBungee.  Enabling experimental RedisBungee support.  This currently only supports RedisBungee 0.3.3 or higher (but not 0.4).");
                BAT.getInstance().getProxy().getPluginManager()
                        .registerListener(BAT.getInstance(), this);
                RedisBungee.getApi().registerPubSubChannels(channel);
                redis = true;
            } else {
                redis = false;
            }
        } else {
            redis = false;
        }
    }

    @EventHandler
    public void onPubSubMessage(final PubSubMessageEvent e) {
        if (!e.getChannel().equals(channel)) return;

        String[] message = e.getMessage().split(split);

        if (message[0].equalsIgnoreCase(RedisBungee.getApi().getServerId()) || message.length < 3) return;

        String messageType = message[1];

		switch (messageType) {
			case "gkick" -> receiveGKickPlayer(message[2], message[3]);
			case "message" -> receiveMessagePlayer(message[2], message[3]);
			case "broadcast" -> receiveBroadcast(message[2], message[3]);
			case "muteupdate" -> receiveMuteUpdatePlayer(message[2], message[3]);
			case "movedefaultserver" -> receiveMoveDefaultServerPlayer(message[2]);
			default -> BAT.getInstance().getLogger().warning("Undeclared BungeeAdminTool redis message received: " + messageType);
		}
    }

    public Boolean isRedisEnabled() {
        return redis;
    }

    public void sendMessagePlayer(UUID pUUID, String message) {
        if (!redis) return;
        sendMessage("message", pUUID.toString() + split + message);
    }

    private void receiveMessagePlayer(String sUUID, String message) {
        ProxiedPlayer player = BAT.getInstance().getProxy().getPlayer(UUID.fromString(sUUID));
        if (player != null) {
            player.sendMessage(TextComponent.fromLegacyText(message));
        }
    }

    public void sendGKickPlayer(UUID pUUID, String reason) {
        if (!redis) return;
        sendMessage("gkick", pUUID.toString() + split + reason);
    }

    private void receiveGKickPlayer(String sUUID, String reason) {
        if (BAT.getInstance().getModules().isLoaded("ban") || BAT.getInstance().getModules().isLoaded("kick")) {
            ProxiedPlayer player = BAT.getInstance().getProxy().getPlayer(UUID.fromString(sUUID));
            if (player != null) {
                BAT.kick(player, reason);
            }
        } else {
            throw new IllegalStateException("Neither the ban nor the kick module are enabled. The gkick message can't be handled.");
        }
    }

    public void sendBroadcast(String permission, String broadcast) {
        if (!redis) return;
        sendMessage("broadcast", permission + split + broadcast);
    }

    private void receiveBroadcast(String permission, String broadcast) {
        BAT.noRedisBroadcast(broadcast, permission);
    }

    public void sendMuteUpdatePlayer(UUID pUUID, String server) {
        if (!redis) return;
        sendMessage("muteupdate", pUUID.toString() + split + server);
    }

    private void receiveMuteUpdatePlayer(String sUUID, String server) {
        if (BAT.getInstance().getModules().isLoaded("mute")) {
            ProxiedPlayer player = BAT.getInstance().getProxy().getPlayer(UUID.fromString(sUUID));
            if (player != null) {
                try {
                    BAT.getInstance().getModules().getMuteModule().updateMuteData(player.getName());
                } catch (InvalidModuleException ignored) {
                }
            }
        } else {
            throw new IllegalStateException("The mute module isn't enabled. The mute message can't be handled.");
        }
    }

    public void sendMoveDefaultServerPlayer(UUID pUUID) {
        if (!redis) return;
        sendMessage("movedefaultserver", pUUID.toString());
    }

    private void receiveMoveDefaultServerPlayer(String sUUID) {
        ProxiedPlayer player = BAT.getInstance().getProxy().getPlayer(UUID.fromString(sUUID));
        if (player != null) {
            player.connect(ProxyServer.getInstance().getServerInfo(
                    player.getPendingConnection().getListener().getDefaultServer()));
        }
    }

    void sendMessage(String messageType, String messageBody) {
        if (!redis) return;

        if (messageBody.trim().length() == 0) return;

        final String message = RedisBungee.getApi().getServerId() + split + messageType + split + messageBody;

        BAT.getInstance().getProxy().getScheduler().runAsync(BAT.getInstance(), () ->
				RedisBungee.getApi().sendChannelMessage(channel, message));
    }

    public void destroy() {
        if (!redis) return;
        RedisBungee.getApi().unregisterPubSubChannels("BungeeAdminTools");
        BAT.getInstance().getProxy().getPluginManager()
                .unregisterListener(this);
    }

}
