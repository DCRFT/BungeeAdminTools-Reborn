package fr.Alphart.bat.modules.comment;

import com.google.common.base.Joiner;
import fr.Alphart.bat.BAT;
import fr.Alphart.bat.i18n.I18n;
import fr.Alphart.bat.modules.BATCommand;
import fr.Alphart.bat.modules.BATCommand.RunAsync;
import fr.Alphart.bat.modules.CommandHandler;
import fr.Alphart.bat.modules.InvalidModuleException;
import fr.Alphart.bat.modules.comment.CommentEntry.Type;
import fr.Alphart.bat.modules.core.Core;
import fr.Alphart.bat.modules.core.PermissionManager;
import fr.Alphart.bat.modules.core.PermissionManager.Action;
import fr.Alphart.bat.utils.FormatUtils;
import fr.Alphart.bat.utils.Utils;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import static com.google.common.base.Preconditions.checkArgument;
import static fr.Alphart.bat.i18n.I18n.format;
import static fr.Alphart.bat.i18n.I18n.formatPrefix;

public class CommentCommand extends CommandHandler {
    private static Comment comment;

    protected CommentCommand(final Comment commentModule) {
        super(commentModule);
        comment = commentModule;
    }

    @RunAsync
    public static class AddCommentCmd extends BATCommand {
        public AddCommentCmd() {
            super("addcomment", "<entity> <reason>", "Write a comment about the player.", "bat.comment.create", "note");
            // We need this command to handle the /comment help
            setMinArgs(1);
        }

        @Override
        public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
            if (args[0].equals("help")) {
                try {
                    FormatUtils.showFormattedHelp(BAT.getInstance().getModules().getModule("comment").getCommands(),
                            sender, "COMMENT");
                } catch (final InvalidModuleException e) {
                    e.printStackTrace();
                }
                return;
            }
            if (args.length < 2) {
                throw new IllegalArgumentException();
            }
            if (!confirmedCmd && Core.getPlayerIP(args[0]).equals("0.0.0.0")) {
                mustConfirmCommand(sender, "bat " + getName() + " " + Joiner.on(' ').join(args),
                        format("operationUnknownPlayer", new String[]{args[0]}));
                return;
            }

            comment.insertComment(args[0], Utils.getFinalArg(args, 1), Type.NOTE, sender.getName());
            sender.sendMessage(formatPrefix("commentAdded"));
        }
    }

    @RunAsync
    public static class ClearCommentCmd extends BATCommand {
        public ClearCommentCmd() {
            super("clearcomment", "<entity> [commentID]", "Clear all the comments and warnings or the specified one of the player.", "bat.comment.clear");
        }

        @Override
        public void onCommand(final CommandSender sender, final String[] args, final boolean confirmedCmd) throws IllegalArgumentException {
            sender.sendMessage(BAT.convertStringToComponent(comment.clearComments(args[0], ((args.length == 2) ? Integer.parseInt(args[1]) : -1))));
        }
    }

    @RunAsync
    public static class WarnCmd extends BATCommand {
        public WarnCmd() {
            super("warn", "<player> <reason>", "Warn a player and add warning note on player's info text.", Action.WARN.getPermission());
        }

        @Override
        public void onCommand(CommandSender sender, String[] args, boolean confirmedCmd)
                throws IllegalArgumentException {
            final ProxiedPlayer target = ProxyServer.getInstance().getPlayer(args[0]);
            final String reason = Utils.getFinalArg(args, 1);
            if (target == null) {
                if (!confirmedCmd && Core.getPlayerIP(args[0]).equals("0.0.0.0")) {
                    mustConfirmCommand(sender, getName() + " " + Joiner.on(' ').join(args),
                            format("operationUnknownPlayer", new String[]{args[0]}));
                    return;
                }
            }

            if (sender instanceof ProxiedPlayer) {
                checkArgument(PermissionManager.canExecuteAction(Action.WARN, sender, ((ProxiedPlayer) sender).getServer().getInfo().getName()),
                        I18n.format("noPerm"));
            }
            comment.insertComment(args[0], reason, Type.WARNING, sender.getName());
            if (target != null) {
                target.sendMessage(I18n.formatPrefix("wasWarnedNotif", new String[]{reason}));
            }

            BAT.broadcast(format("warnBroadcast", new String[]{args[0], sender.getName(), reason}), Action.WARN_BROADCAST.getPermission());
		}
    }
}
