package fr.Alphart.BAT.Modules;

import fr.Alphart.BAT.BAT;
import fr.Alphart.BAT.Modules.Comment.Comment;
import fr.Alphart.BAT.Modules.Mute.Mute;

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
                BAT.getInstance()
                        .getLogger()
                        .severe("An error happened during loading of " + module.getName() + " commands please report this:");
                e.printStackTrace();
            }
        }

        // Sort the commands list and remove unused command
        final List<String> enabledCmds;
        if (module instanceof ModuleConfiguration) {
            enabledCmds = sortCommandMap(module.getConfig().get(ModuleConfiguration.commands));
        } else if (module instanceof Mute.MuteConfig) {
            enabledCmds = sortCommandMap(module.getConfig().get(Mute.MuteConfig.commands));
        } else {
            enabledCmds = sortCommandMap(module.getConfig().get(Comment.CommentConfig.commands));
        }
        commands.removeIf(cmd -> !enabledCmds.contains(cmd.getName()));
    }

    private List<String> sortCommandMap(Map<String, Boolean> commands) {
        return commands.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).collect(Collectors.toList());
    }
}