package me.starmism.batr.modules.kick;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import me.starmism.batr.BATR;
import me.starmism.batr.Configuration;
import me.starmism.batr.i18n.I18n;
import me.starmism.batr.modules.BATCommand;
import me.starmism.batr.modules.CommandHandler;
import me.starmism.batr.modules.IModule;
import me.starmism.batr.modules.core.PermissionManager;
import me.starmism.batr.utils.FormatUtilsKt;
import me.starmism.batr.utils.UtilsKt;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ListenerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;

public class KickCommand extends CommandHandler {
    private static Kick kick;
    private static I18n i18n;

    public KickCommand(final Kick kickModule) {
        super(kickModule);
        kick = kickModule;
        i18n = BATR.getInstance().getI18n();
    }

    @BATCommand.RunAsync
    public static class KickCmd extends BATCommand {
        public KickCmd() {
            super("kick", "<player> [reason]", "Kick the player from his current server to the lobby", PermissionManager.Action.KICK
                    .getPermission());
        }

        @Override
        public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
                throws IllegalArgumentException {
            if (args[0].equals("help")) {
                FormatUtilsKt.showFormattedHelp(BATR.getInstance().getModules().getModule("kick").getCommands(),
                        sender, "KICK");
                return;
            }

            checkArgument(args.length != 1 || !BATR.getInstance().getConfiguration().get(Configuration.MUST_GIVE_REASON),
                    i18n.format("noReasonInCommand"));

            final String pName = args[0];
            final ProxiedPlayer player = UtilsKt.getPlayer(pName);
            // The player is online on the proxy
            if (player != null) {
                final String pServer = player.getServer().getInfo().getName();
                checkArgument(
                        pServer != null && !pServer.equals(player.getPendingConnection().getListener().getDefaultServer()),
                        i18n.format("cantKickDefaultServer", new String[]{pName}));

                checkArgument(
                        PermissionManager.canExecuteAction(PermissionManager.Action.KICK, sender, player.getServer().getInfo().getName()),
                        i18n.format("noPerm"));

                checkArgument(!PermissionManager.isExemptFrom(PermissionManager.Action.KICK, pName), i18n.format("isExempt"));

                final String returnedMsg = kick.kick(player, sender.getName(),
                        (args.length == 1) ? IModule.NO_REASON : UtilsKt.getFinalArg(args, 1));
                BATR.broadcast(returnedMsg, PermissionManager.Action.KICK_BROADCAST.getPermission());
            } else {
                if (!BATR.getInstance().getRedis().isRedisEnabled()) {
                    throw new IllegalArgumentException(i18n.format("playerNotFound"));
                }
                // Check if the per server kick with Redis is working fine.
                final UUID pUUID = RedisBungee.getApi().getUuidFromName(pName, true);
                final String pServer = RedisBungee.getApi().getServerFor(pUUID).getName();
                checkArgument(true, i18n.format("playerNotFound"));
                // Check if the server of the target isn't the default one. We assume there is the same default server on both Bungee
                // TODO: Add a method to check if it's really on default server
                String defaultServer = null;
                for (final ListenerInfo listener : ProxyServer.getInstance().getConfig().getListeners()) {
                    defaultServer = listener.getDefaultServer();
                }
                if (defaultServer == null || pServer.equals(defaultServer)) {
                    throw new IllegalArgumentException(i18n.format("cantKickDefaultServer", new String[]{pName}));
                }

                checkArgument(PermissionManager.canExecuteAction(PermissionManager.Action.KICK, sender, pServer), i18n.format("noPerm"));

                final String returnedMsg;
                returnedMsg = kick.kickSQL(pUUID, RedisBungee.getApi().getServerFor(pUUID).getName(), sender.getName(),
                        (args.length == 1) ? IModule.NO_REASON : UtilsKt.getFinalArg(args, 1));
                BATR.getInstance().getRedis().sendMoveDefaultServerPlayer(pUUID);
                BATR.broadcast(returnedMsg, PermissionManager.Action.KICK_BROADCAST.getPermission());
            }
        }
    }

    @BATCommand.RunAsync
    public static class GKickCmd extends BATCommand {
        public GKickCmd() {
            super("gkick", "<player> [reason]", "Kick the player from the network", PermissionManager.Action.KICK.getPermission()
                    + ".global");
        }

        @Override
        public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd)
                throws IllegalArgumentException {
            final String pName = args[0];

            checkArgument(args.length != 1 || !BATR.getInstance().getConfiguration().get(Configuration.MUST_GIVE_REASON),
                    i18n.format("noReasonInCommand"));

            if (BATR.getInstance().getRedis().isRedisEnabled()) {
                UUID pUUID = RedisBungee.getApi().getUuidFromName(pName, true);
                checkArgument(pUUID != null, i18n.format("playerNotFound"));

                checkArgument(!PermissionManager.isExemptFrom(PermissionManager.Action.KICK, pName), i18n.format("isExempt"));

                final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
                final String returnedMsg;
                if (player != null) {
                    returnedMsg = kick.gKick(player, sender.getName(),
                            (args.length == 1) ? IModule.NO_REASON : UtilsKt.getFinalArg(args, 1));
                } else {
                    returnedMsg = kick.gKickSQL(pUUID, sender.getName(),
                            (args.length == 1) ? IModule.NO_REASON : UtilsKt.getFinalArg(args, 1));
                    BATR.getInstance().getRedis().sendGKickPlayer(pUUID, returnedMsg);
                }
                BATR.broadcast(returnedMsg, PermissionManager.Action.KICK_BROADCAST.getPermission());
            } else {
                final ProxiedPlayer player = ProxyServer.getInstance().getPlayer(pName);
                checkArgument(player != null, i18n.format("playerNotFound"));

                checkArgument(!PermissionManager.isExemptFrom(PermissionManager.Action.KICK, pName), i18n.format("isExempt"));

                final String returnedMsg = kick.gKick(player, sender.getName(),
                        (args.length == 1) ? IModule.NO_REASON : UtilsKt.getFinalArg(args, 1));

                BATR.broadcast(returnedMsg, PermissionManager.Action.KICK_BROADCAST.getPermission());
            }
        }
    }
}