package me.starmism.batr.i18n

import me.starmism.batr.BATR
import me.starmism.batr.Configuration
import me.starmism.batr.modules.IModule
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.TextComponent
import java.io.FileReader
import java.nio.file.Files
import java.text.MessageFormat
import java.util.*
import java.util.zip.ZipInputStream


class I18n {
    private lateinit var bundle: ResourceBundle

    private val argsReplacer: MutableMap<String, String> = object : HashMap<String, String>() {
        override fun put(key: String, value: String): String {
            return super.put(key, ChatColor.translateAlternateColorCodes('&', value)).toString()
        }
    }

    fun getString(key: String): String {
        return try {
            bundle.getString(key)
        } catch (ex: Exception) {
            ""
        }
    }

    /**
     * Format a message with given object. Parse color
     */
    fun format(message: String, formatObject: Array<String>): String {
        return try {
            val mf = MessageFormat(getString(message))
            ChatColor.translateAlternateColorCodes('&', mf.format(preprocessArgs(formatObject)))
        } catch (e: IllegalArgumentException) {
            ""
        }
    }

    /**
     * Format a message with given object. Parse color
     */
    fun format(message: String): String {
        return ChatColor.translateAlternateColorCodes('&', getString(message).replace("''", "'"))
    }

    /**
     * Same as [.format] except it adds a prefix
     */
    fun formatPrefix(message: String, formatObject: Array<String>): Array<BaseComponent> {
        return try {
            val mf = MessageFormat(getString(message))
            BATR.convertStringToComponent(mf.format(preprocessArgs(formatObject)))
        } catch (e: IllegalArgumentException) {
            TextComponent.fromLegacyText("")
        }
    }

/**
     * Same as [.format] except it adds a prefix
     */
    fun formatPrefix(message: String): Array<BaseComponent> {
        return try {
            // Replace the quote as the message formatter does
            BATR.convertStringToComponent(getString(message).replace("''", "'"))
        } catch (e: IllegalArgumentException) {
            TextComponent.fromLegacyText("")
        }
    }

    /**
     * Preprocess formatArgs to replace value contained in the map argsReplacer,
     * in order to have global instead of global for example
     *
     * @param formatArgs The message containing placeholders.
     * @return The message but with the global arguments replaced.
     */
    private fun preprocessArgs(formatArgs: Array<String>): Array<String> {
        formatArgs.forEachIndexed { i, message ->
            if (argsReplacer.containsKey(message)) {
                formatArgs[i] = argsReplacer[message].toString()
            }
        }
        return formatArgs
    }

    init {
        // Unzip the messages files and place them if they don't already exist.
        try {
            val langPath = BATR.getInstance().dataFolder.toPath().resolve("lang")
            Files.createDirectories(BATR.getInstance().dataFolder.toPath().resolve("lang"))

            BATR.getInstance().getResourceAsStream("translations.zip").use { translationsZip ->
                ZipInputStream(translationsZip).use{ zip ->
                    generateSequence { zip.nextEntry }
                        .map { langPath.resolve(it.name) }
                        .filter(Files::notExists)
                        .forEach { Files.copy(zip, it) }
                }
            }
        } catch (e: Exception) {
            BATR.getInstance().logger.severe("Could not output the default jar language files.")
            e.printStackTrace()
        }

        // Get the language file associated with the language setting in the config.
        val locale = Locale(BATR.getInstance().configuration.get(Configuration.LANGUAGE))
        try {
            bundle = PropertyResourceBundle(
                FileReader(
                    BATR.getInstance().dataFolder
                        .toPath().resolve("lang").resolve("messages_$locale.properties").toFile()
                )
            )
        } catch (ex: Exception) {
            BATR.getInstance().logger.severe("The language file ${locale.toLanguageTag()} was not found or is incorrect.")
            ex.printStackTrace()
            BATR.getInstance().onDisable()
        }
        argsReplacer[IModule.ANY_SERVER] = bundle.getString("global")
        argsReplacer[IModule.GLOBAL_SERVER] = bundle.getString("global")
        argsReplacer[IModule.NO_REASON] = bundle.getString("noReason")
    }
}