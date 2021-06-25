package me.starmism.batr.modules;

import me.starmism.batr.BATR;
import me.starmism.batr.modules.ban.BanConfig;
import me.starmism.batr.modules.comment.CommentConfig;
import me.starmism.batr.modules.kick.KickConfig;
import me.starmism.batr.modules.mute.MuteConfig;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class CommandHandler {
    private final IModule module;
    private final List<BATCommand> commands;

    protected CommandHandler(final IModule module) {
        this.module = module;
        commands = new ArrayList<>();
    }

    public List<BATCommand> getCmds() {
        return commands;
    }

    public void loadCmds() {
        // Get all commands and put them in a list
        final List<String> cmdName = new ArrayList<>();
        for (final Class<?> subClass : getClass().getDeclaredClasses()) {
            try {
                if (subClass.getAnnotation(BATCommand.Disable.class) != null) {
                    continue;
                }
                final BATCommand command = (BATCommand) subClass.getConstructors()[0].newInstance();
                commands.add(command);
                cmdName.add(command.getName());
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | SecurityException e) {
                BATR.getInstance()
                        .getLogger()
                        .severe("An error happened during loading of " + module.getName() + " commands please report this:");
                e.printStackTrace();
            }
        }

        // Sort the commands list and remove unused command
        final List<String> enabledCmds;
        if (module instanceof BanConfig) {
            enabledCmds = sortCommandMap(module.getConfig().get(BanConfig.COMMANDS));
        } else if (module instanceof MuteConfig) {
            enabledCmds = sortCommandMap(module.getConfig().get(MuteConfig.COMMANDS));
        } else if (module instanceof CommentConfig) {
            enabledCmds = sortCommandMap(module.getConfig().get(CommentConfig.COMMANDS));
        } else {
            enabledCmds = sortCommandMap(module.getConfig().get(KickConfig.COMMANDS));
        }
        commands.removeIf(cmd -> !enabledCmds.contains(cmd.getName()));
    }

    private List<String> sortCommandMap(Map<String, Boolean> commands) {
        return commands.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toList());
    }
}