package me.starmism.batr.utils

import com.google.common.base.Joiner
import com.google.common.base.Preconditions
import me.starmism.batr.BATR
import me.starmism.batr.Configuration
import me.starmism.batr.modules.BATCommand
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent

/**
     * Get the duration between the given timestamp and the current one
     *
     * @param futureTimestamp in milliseconds which must be superior to the current
     * timestamp
     * @return readable duration
     */
    fun getDuration(futureTimestamp: Long): String {
        var seconds = ((futureTimestamp - System.currentTimeMillis()) / 1000).toInt() + 1
        Preconditions.checkArgument(
            seconds > 0,
            "The timestamp passed in parameter must be superior to the current timestamp !"
        )
        val item: MutableList<String?> = ArrayList()
        var months = 0
        while (seconds >= 2678400) {
            months++
            seconds -= 2678400
        }
        if (months > 0) {
            item.add("$months miesięcy")
        }
        var days = 0
        while (seconds >= 86400) {
            days++
            seconds -= 86400
        }
        if (days > 0) {
            item.add("$days dni")
        }
        var hours = 0
        while (seconds >= 3600) {
            hours++
            seconds -= 3600
        }
        if (hours > 0) {
            item.add("$hours h")
        }
        var mins = 0
        while (seconds >= 60) {
            mins++
            seconds -= 60
        }
        if (mins > 0) {
            item.add("$mins min")
        }
        if (seconds > 0) {
            item.add("$seconds s")
        }
        return Joiner.on(", ").join(item)
    }

    fun showFormattedHelp(cmds: List<BATCommand>, sender: CommandSender, helpName: String) {
        val msg: MutableList<Array<BaseComponent>> = ArrayList()
        val simpleAliasesCommands = BATR.getInstance().configuration.get(Configuration.SIMPLE_ALIASES_COMMANDS)

        msg.add(TextComponent.fromLegacyText(
            ChatColor.translateAlternateColorCodes('&',
                "&9 ---- &9Bungee&fDragon&cTools&9 - &6$helpName&9 - &fHELP &9---- ")
        ))

        for (cmd in cmds) {
            if (sender.hasPermission("bat.admin") || sender.hasPermission(cmd.batPermission)) {
                var message = if ("core".equals(helpName, ignoreCase = true)) {
                    if (simpleAliasesCommands[cmd.name]!!) {
                        " &f- &e/"
                    } else {
                        " &f- &e/bat "
                    }
                } else {
                    " &f- &e/"
                }
                message += cmd.formatUsage
                msg.add(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message)))
            }
        }

        for (tx in msg) {
            sender.sendMessage(*tx)
        }
        if (msg.size == 1) {
            sender.sendMessage(*BATR.convertStringToComponent("&c No command corresponding to your permission has been found"))
        }
    }

    fun formatNewLine(message: String): List<Array<BaseComponent>> {
        return message.split("\n".toRegex()).map { TextComponent.fromLegacyText(it) }
    }