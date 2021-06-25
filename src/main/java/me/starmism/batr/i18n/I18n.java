package me.starmism.batr.i18n;

import me.starmism.batr.BATR;
import me.starmism.batr.Configuration;
import me.starmism.batr.modules.IModule;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class I18n {
    private static final Map<String, String> argsReplacer = new HashMap<>() {
        @Serial
        private static final long serialVersionUID = 1L;

        @Override
        public String put(final String key, final String value) {
            return super.put(key, ChatColor.translateAlternateColorCodes('&', value));
        }
    };
    private ResourceBundle bundle;

    private I18n() {
        // Unzip the messages files and place them if they don't already exist.
        try (final InputStream translationsZip = BATR.getInstance().getResourceAsStream("translations.zip");
             final ZipInputStream zip = new ZipInputStream(Objects.requireNonNull(translationsZip, "translationsZip"))) {
            ZipEntry zipEntry;
            Files.createDirectories(BATR.getInstance().getDataFolder().toPath().resolve("lang"));

            while ((zipEntry = zip.getNextEntry()) != null) {
                final Path translationFile = BATR.getInstance().getDataFolder().toPath().resolve("lang").resolve(zipEntry.getName());
                if (Files.notExists(translationFile)) {
                    Files.copy(zip, translationFile);
                }
                zip.closeEntry();
            }
        } catch (IOException e) {
            BATR.getInstance().getLogger().severe("Could not output the default jar language files.");
            e.printStackTrace();
        }

        // Get the language file associated with the language setting in the config.
        final Locale locale = new Locale(BATR.getInstance().getConfiguration().get(Configuration.LANGUAGE));
        try {
            bundle = new PropertyResourceBundle(new FileReader(BATR.getInstance().getDataFolder()
                    .toPath().resolve("lang").resolve("messages_" + locale + ".properties").toFile()));
        } catch (final MissingResourceException | IOException e) {
            BATR.getInstance().getLogger()
                    .severe("The language file " + locale.toLanguageTag() + " was not found or is incorrect.");
            BATR.getInstance().onDisable();
        }

        argsReplacer.put(IModule.ANY_SERVER, bundle.getString("global"));
        argsReplacer.put(IModule.GLOBAL_SERVER, bundle.getString("global"));
        argsReplacer.put(IModule.NO_REASON, bundle.getString("noReason"));

    }

    private static I18n getInstance() {
        return I18nHolder.instance;
    }

    public static String getString(final String key) throws IllegalArgumentException {
        String message;
        try {
            message = getInstance().bundle.getString(key);
        } catch (final MissingResourceException e) {
            BATR.getInstance().getLogger().info("Incorrect translation key: " + key);
            throw new IllegalArgumentException("Incorrect translation key, please check the log.");
        }
        return message;
    }

    /**
     * Format a message with given object. Parse color
     */
    public static String format(final String message, final String[] formatObject) {
        try {
            final MessageFormat mf = new MessageFormat(getString(message));
            return ChatColor.translateAlternateColorCodes('&', mf.format(preprocessArgs(formatObject)));
        } catch (final IllegalArgumentException e) {
            return "";
        }
    }

    /**
     * Format a message with given object. Parse color
     */
    public static String format(final String message) {
        try {
            // Replace the quote as the message formatter does
            return ChatColor.translateAlternateColorCodes('&', getString(message).replace("''", "'"));
        } catch (final IllegalArgumentException e) {
            return "";
        }
    }

    /**
     * Same as {@link #format(String, String[])} except it adds a prefix
     */
    public static BaseComponent[] formatPrefix(final String message, final String[] formatObject) {
        try {
            final MessageFormat mf = new MessageFormat(getString(message));
            return BATR.convertStringToComponent(mf.format(preprocessArgs(formatObject)));
        } catch (final IllegalArgumentException e) {
            return TextComponent.fromLegacyText("");
        }
    }

    /**
     * Same as {@link #format(String, String[])} except it adds a prefix
     */
    public static BaseComponent[] formatPrefix(final String message) {
        try {
            // Replace the quote as the message formatter does
            return BATR.convertStringToComponent(getString(message).replace("''", "'"));
        } catch (final IllegalArgumentException e) {
            return TextComponent.fromLegacyText("");
        }
    }

    /**
     * Preprocess formatArgs to replace value contained in the map argsReplacer,
     * in order to have global instead of global for example
     *
     * @param formatArgs The message containing placeholders.
     * @return The message but with the global arguments replaced.
     */
    public static String[] preprocessArgs(final String[] formatArgs) {
        for (int i = 0; i < formatArgs.length; i++) {
            if (argsReplacer.containsKey(formatArgs[i])) {
                formatArgs[i] = argsReplacer.get(formatArgs[i]);
            }
        }
        return formatArgs;
    }

    public static void reload() {
        I18nHolder.reload();
    }

    private static class I18nHolder {
        private static I18n instance = new I18n();

        private static void reload() {
            instance = new I18n();
        }
    }
}