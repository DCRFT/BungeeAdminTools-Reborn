package me.starmism.batr.modules.comment;

import com.google.common.base.Joiner;
import me.starmism.batr.BATR;
import me.starmism.batr.i18n.I18n;
import me.starmism.batr.modules.BATCommand;
import me.starmism.batr.modules.CommandHandler;
import me.starmism.batr.modules.comment.CommentEntry.Type;
import me.starmism.batr.modules.core.Core;
import me.starmism.batr.modules.core.PermissionManager;
import me.starmism.batr.utils.FormatUtilsKt;
import me.starmism.batr.utils.UtilsKt;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import static com.google.common.base.Preconditions.checkArgument;

public class CommentCommand extends CommandHandler {
    private static Comment comment;
    private static I18n i18n;

    protected CommentCommand(final Comment commentModule) {
        super(commentModule);
        comment = commentModule;
        i18n = BATR.getInstance().getI18n();
    }

    @BATCommand.RunAsync
    public static class AddCommentCmd extends BATCommand {
        public AddCommentCmd() {
            super("addcomment", "<entity> <reason>", "Write a comment about the player.", "bat.comment.create", "note");
            // We need this command to handle the /comment help
            setMinArgs(1);
        }

        @Override
        public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
            if (args[0].equals("help")) {
                FormatUtilsKt.showFormattedHelp(BATR.getInstance().getModules().getModule("comment").getCommands(),
                        sender, "COMMENT");
                return;
            }
            if (args.length < 2) {
                throw new IllegalArgumentException();
            }
            if (!confirmedCmd && Core.getPlayerIP(args[0]).equals("0.0.0.0")) {
                mustConfirmCommand(sender, "bat " + getName() + " " + Joiner.on(' ').join(args),
                        i18n.format("operationUnknownPlayer", new String[]{args[0]}));
                return;
            }

            comment.insertComment(args[0], UtilsKt.getFinalArg(args, 1), Type.NOTE, sender.getName());
            sender.sendMessage(i18n.formatPrefix("commentAdded"));
        }
    }

    @BATCommand.RunAsync
    public static class ClearCommentCmd extends BATCommand {
        public ClearCommentCmd() {
            super("clearcomment", "<entity> [commentID]", "Clear all the comments and warnings or the specified one of the player.", "bat.comment.clear");
        }

        @Override
        public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
            sender.sendMessage(BATR.convertStringToComponent(comment.clearComments(args[0], ((args.length == 2) ? Integer.parseInt(args[1]) : -1))));
        }
    }

    @BATCommand.RunAsync
    public static class WarnCmd extends BATCommand {
        public WarnCmd() {
            super("warn", "<player> <reason>", "Warn a player and add warning note on player's info text.", PermissionManager.Action.WARN.getPermission());
        }

        @Override
        public void onCommand(CommandSender sender, String[] args, boolean confirmedCmd)
                throws IllegalArgumentException {
            final ProxiedPlayer target = ProxyServer.getInstance().getPlayer(args[0]);
            final String reason = UtilsKt.getFinalArg(args, 1);
            if (target == null) {
                if (!confirmedCmd && Core.getPlayerIP(args[0]).equals("0.0.0.0")) {
                    mustConfirmCommand(sender, getName() + " " + Joiner.on(' ').join(args),
                            i18n.format("operationUnknownPlayer", new String[]{args[0]}));
                    return;
                }
            }

            if (sender instanceof ProxiedPlayer) {
                checkArgument(PermissionManager.canExecuteAction(PermissionManager.Action.WARN, sender, ((ProxiedPlayer) sender).getServer().getInfo().getName()),
                        i18n.format("noPerm"));
            }
            comment.insertComment(args[0], reason, Type.WARNING, sender.getName());
            if (target != null) {
                target.sendMessage(i18n.formatPrefix("wasWarnedNotif", new String[]{reason}));
            }

            BATR.broadcast(i18n.format("warnBroadcast", new String[]{args[0], sender.getName(), reason}), PermissionManager.Action.WARN_BROADCAST.getPermission());
		}
    }
}
