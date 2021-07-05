package me.starmism.batr.modules

import me.starmism.batr.BATR
import me.starmism.batr.modules.ban.Ban
import me.starmism.batr.modules.comment.Comment
import me.starmism.batr.modules.core.Core
import me.starmism.batr.modules.kick.Kick
import me.starmism.batr.modules.mute.Mute
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.plugin.Listener
import java.util.logging.Logger

class ModulesManager {
    private val log: Logger = BATR.getInstance().logger
    private var helpMessage: String? = null
    private val modules: MutableMap<IModule, Int>
    private val modulesNames: MutableMap<String, IModule>
    private val cmdsModules: MutableMap<String, IModule>

    fun showHelp(sender: CommandSender) {
        if (helpMessage == null) {
            helpMessage += "&2---- &1Bungee&fAdmin&cTools&2 - HELP ----\n"
            for ((key, value) in modules) {
                helpMessage += if (value == IModule.ON_STATE) "- &B" else "- &MDisabled: "
                helpMessage += "/${key.name} help&2: Show the help relative to the ${key.name} module\n"
            }
            helpMessage += "&2-----------------------------------------"
            helpMessage = ChatColor.translateAlternateColorCodes('&', helpMessage)
        }
        sender.sendMessage(TextComponent(helpMessage))
    }

    fun loadModules() {
        // The core module MUST NOT be disabled.
        modules[Core()] = IModule.OFF_STATE
        modules[Ban()] = IModule.OFF_STATE
        modules[Mute()] = IModule.OFF_STATE
        modules[Kick()] = IModule.OFF_STATE
        modules[Comment()] = IModule.OFF_STATE

        for (module in modules.keys) {

            if (!module.isEnabled) {
                continue
            }

            if (module.load()) {
                modulesNames[module.name] = module
                modules[module] = IModule.ON_STATE

                if (module is Listener) {
                    ProxyServer.getInstance().pluginManager.registerListener(BATR.getInstance(), module)
                }
                for (cmd in module.commands) {
                    cmdsModules[cmd.name] = module
                    ProxyServer.getInstance().pluginManager.registerCommand(BATR.getInstance(), cmd)
                }
                module.config?.save()

            } else {
                log.severe("The ${module.name} module encountered an error while loading.")
            }
        }
    }

    fun unloadModules() {
        for (module in loadedModules) {
            module.unload()
            if (module is Listener) {
                ProxyServer.getInstance().pluginManager.unregisterListener(module)
            }
            modules[module] = IModule.OFF_STATE
        }
        ProxyServer.getInstance().pluginManager.unregisterCommands(BATR.getInstance())
        modules.clear()
        helpMessage = null
    }

    val loadedModules: Set<IModule>
        get() {
            return modules.filter { it.value == IModule.ON_STATE }.keys
        }

    fun isLoaded(name: String): Boolean {
        return getModule(name) != null
    }

    val core: Core?
        get() {
            return getModule("core") as Core?
        }

    val banModule: Ban?
        get() {
            return getModule("ban") as Ban?
        }

    val muteModule: Mute?
        get() {
            return getModule("mute") as Mute?
        }

    val kickModule: Kick?
        get() {
            return getModule("kick") as Kick?
        }

    val commentModule: Comment?
        get() {
            return getModule("comment") as Comment?
        }

    fun getModule(name: String): IModule? {
        val module = modulesNames[name]
        if (modules[module] == IModule.ON_STATE) {
            return module
        }
        return null
    }

    init {
        modules = LinkedHashMap()
        modulesNames = HashMap()
        cmdsModules = HashMap()
    }
}