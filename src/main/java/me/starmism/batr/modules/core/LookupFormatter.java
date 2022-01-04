package me.starmism.batr.modules.core;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import me.starmism.batr.BATR;
import me.starmism.batr.i18n.I18n;
import me.starmism.batr.modules.InvalidModuleException;
import me.starmism.batr.modules.ModulesManager;
import me.starmism.batr.modules.ban.BanEntry;
import me.starmism.batr.utils.FormatUtilsKt;
import me.starmism.batr.utils.MojangAPIProviderKt;
import me.starmism.batr.utils.UtilsKt;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.*;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LookupFormatter {
    private static final int entriesPerPage = 15;
    private final ModulesManager modules;
    private final String lookupHeader;
    private final String lookupFooter;
    private final I18n i18n;

    public LookupFormatter() {
        i18n = BATR.getInstance().getI18n();
        lookupHeader = i18n.format("perModuleLookupHeader");
        lookupFooter = i18n.format("perModuleLookupFooter");
        modules = BATR.getInstance().getModules();
    }

    public List<BaseComponent[]> getSummaryLookupPlayer(final String pName, final boolean displayIP) {
        // Gather players data related to each modules
        final EntityEntry pDetails = new EntityEntry(pName);

        if (!pDetails.exist()) {
            final List<BaseComponent[]> returnedMsg = new ArrayList<>();
            returnedMsg.add(i18n.formatPrefix("playerNotFound"));
            return returnedMsg;
        }

        final EntityEntry ipDetails = new EntityEntry(Core.getPlayerIP(pName));
        boolean isBan = false;
        boolean isBanIP = false;
        int bansNumber = 0;
        final List<String> banServers = Lists.newArrayList();
        final List<String> banIPServers = Lists.newArrayList();
        boolean isMute = false;
        boolean isMuteIP = false;
        int mutesNumber = 0;
        final List<String> muteServers = Lists.newArrayList();
        final List<String> muteIPServers = Lists.newArrayList();
        int kicksNumber = 0;
        // Compute player's state (as well as his ip) concerning ban and mute
        for (final BanEntry banEntry : pDetails.getBans()) {
            if (banEntry.active()) {
                isBan = true;
                banServers.add(banEntry.server());
            }
        }
        for (final BanEntry banEntry : ipDetails.getBans()) {
            if (banEntry.active()) {
                isBanIP = true;
                banIPServers.add(banEntry.server());
            }
        }

        bansNumber = pDetails.getBans().size() + ipDetails.getBans().size();

        // Load the lookup pattern
        final String lookupPattern = i18n.format("playerLookup");

        // Initialize all the strings to prepare the big replace
        String connection_state;
            if (ProxyServer.getInstance().getPlayer(pName) != null) {
                connection_state = i18n.format("connectionStateOnline")
                        .replace("{server}", ProxyServer.getInstance().getPlayer(pName).getServer().getInfo().getName());
            } else {
                connection_state = i18n.format("connectionStateOffline");
            }


        final String joinChar = "&f, &3";
        final String ban_servers = !banServers.isEmpty()
                ? Joiner.on(joinChar).join(banServers).toLowerCase()
                : i18n.format("none");
        final String banip_servers = !banIPServers.isEmpty()
                ? Joiner.on(joinChar).join(banIPServers).toLowerCase()
                : i18n.format("none");

        final String first_login = pDetails.getFirstLogin() != EntityEntry.noDateFound
                ? Core.defaultDF.format(new Date(pDetails.getFirstLogin().getTime()))
                : i18n.format("unknownDate");
        final String last_login = pDetails.getLastLogin() != EntityEntry.noDateFound
                ? Core.defaultDF.format(new Date(pDetails.getLastLogin().getTime()))
                : i18n.format("unknownDate");
        final String last_ip = !"0.0.0.0".equals(pDetails.getLastIP())
                ? ((displayIP) ? pDetails.getLastIP() : i18n.format("hiddenIp"))
                : i18n.format("unknownIp");

        final String ip_users = !ipDetails.getUsers().isEmpty()
                ? Joiner.on(joinChar).join(ipDetails.getUsers())
                : i18n.format("none");

        String name_history_list;
        // Create a function for that or something better than a big chunk of code inside the lookup
        if (ProxyServer.getInstance().getConfig().isOnlineMode()) {
            try {
                name_history_list = Joiner.on("&e, &a").join(MojangAPIProviderKt.getPlayerNameHistory(pName));
            } catch (final RuntimeException e) {
                name_history_list = "unable to fetch player's name history. Check the logs";
                BATR.getInstance().getLogger().severe("An error occurred while fetching " + pName + "'s name history from Mojang servers."
                        + "Please report this: ");
                e.printStackTrace();
            }
        } else {
            name_history_list = "offline server";
        }

        StringBuilder last_comments = new StringBuilder();
        // We need to parse the number of last comments from the lookup pattern
        final Pattern lastCommentsPattern = Pattern.compile("(?:.|\n)*?\\{last_comments:(\\d*)\\}(?:.|\n)*?");
        final Matcher matcher = lastCommentsPattern.matcher(lookupPattern);
        try {
            if (!matcher.matches()) {
                throw new NumberFormatException();
            }
            int nLastComments = Integer.parseInt(matcher.group(1));
            if (nLastComments < 1) {
                throw new NumberFormatException();
            }
            int i = 0;
            if (last_comments.length() == 0) {
                last_comments = new StringBuilder(i18n.format("none\n"));
            }
        } catch (final NumberFormatException e) {
            last_comments = new StringBuilder("Unable to parse the number of last_comments");
        }

        return FormatUtilsKt.formatNewLine(ChatColor.translateAlternateColorCodes('&',
                lookupPattern
                        .replace("{connection_state}", connection_state)
                        .replace("{ban_servers}", ban_servers)
                        .replace("{banip_servers}", banip_servers)
                        .replace("{first_login}", first_login)
                        .replace("{last_login}", last_login)
                        .replace("{last_ip}", last_ip)
                        .replace("{ip_users}", ip_users)
                        .replace("{bans_number}", String.valueOf(bansNumber))
                        .replace("{name_history_list}", name_history_list)
                        .replaceAll("\\{last_comments:\\d\\}", last_comments.toString())
                        .replace("{player}", pName)
                        .replace("{uuid}", Core.getUUID(pName))
                        // '¤' is used as a space character, so we replace it with space and display correctly the escaped one
                        .replace("¤", " ")
                        .replace("\\¤", "¤")
        ));
    }

    public List<BaseComponent[]> getSummaryLookupIP(final String ip) {
        final EntityEntry ipDetails = new EntityEntry(ip);
        if (!ipDetails.exist()) {
            final List<BaseComponent[]> returnedMsg = new ArrayList<>();
            returnedMsg.add(i18n.formatPrefix("unknownIp"));
            return returnedMsg;
        }
        boolean isBan = false;
        int bansNumber = 0;
        final List<String> banServers = new ArrayList<>();
        boolean isMute = false;
        int mutesNumber = 0;
        final List<String> muteServers = new ArrayList<>();
        if (!ipDetails.getBans().isEmpty()) {
            for (final BanEntry banEntry : ipDetails.getBans()) {
                if (banEntry.active()) {
                    isBan = true;
                    banServers.add(banEntry.server());
                }
            }
            bansNumber = ipDetails.getBans().size();
        }

        // Initialize all strings
        final String joinChar = "&f, &3";
        final String ip_users = !ipDetails.getUsers().isEmpty()
                ? Joiner.on(joinChar).join(ipDetails.getUsers())
                : i18n.format("none");
        final String ban_servers = !banServers.isEmpty()
                ? Joiner.on(joinChar).join(banServers).toLowerCase()
                : i18n.format("none");
        final String mute_servers = !muteServers.isEmpty()
                ? Joiner.on(joinChar).join(muteServers).toLowerCase()
                : i18n.format("none");

        String replacedString = i18n.format("ipLookup")
                .replace("{ban_servers}", ban_servers).replace("{mute_servers}", mute_servers)
                .replace("{bans_number}", String.valueOf(bansNumber)).replace("{mutes_number}", String.valueOf(mutesNumber))
                .replace("{ip}", ip).replace("{ip_users}", ip_users)
                // '¤' is used as a space character, so we replace it with space and display correctly the escaped one
                .replace("¤", " ").replace("\\¤", "¤");

        if (replacedString.contains("{ip_location}")) {
            String ipLocation;
            try {
                ipLocation = UtilsKt.getIpDetails(ip);
            } catch (final Exception e) {
                BATR.getInstance().getLogger().log(Level.SEVERE,
                        "Error while fetching ip location from the API. Please report this :", e);
                ipLocation = "unresolvable ip location. Check your logs";
            }
            replacedString = replacedString.replace("{ip_location}", ipLocation);
        }

        return FormatUtilsKt.formatNewLine(ChatColor.translateAlternateColorCodes('&',
                replacedString));
    }

    public List<BaseComponent[]> getSummaryStaffLookup(final String staff, final boolean displayID) {
        int bans_number = 0;
        int unbans_number = 0;
        int mutes_number = 0;
        int unmutes_number = 0;
        int kicks_number = 0;
        int comments_number = 0;
        int warnings_number = 0;
        if (modules.isLoaded("ban")) {
            for (final BanEntry ban : modules.getBanModule().getManagedBan(staff)) {
                if (staff.equalsIgnoreCase(ban.staff())) {
                    bans_number++;
                }
                if (staff.equalsIgnoreCase(ban.unbanStaff())) {
                    unbans_number++;
                }
            }
        }

        return FormatUtilsKt.formatNewLine(ChatColor.translateAlternateColorCodes('&',
                i18n.format("staffLookup")
                        .replace("{bans_number}", String.valueOf(bans_number)).replace("{unbans_number}", String.valueOf(unbans_number))
                        .replace("{staff}", staff).replace("{uuid}", Core.getUUID(staff))
                        .replace("¤", " ").replace("\\¤", "¤")
        ));
    }

    public List<BaseComponent[]> formatBanLookup(final String entity, final List<BanEntry> bans,
                                                 int page, final boolean staffLookup) throws InvalidModuleException {
        final StringBuilder msg = new StringBuilder();

        int totalPages = (int) Math.ceil((double) bans.size() / entriesPerPage);
        if (bans.size() > entriesPerPage) {
            if (page > totalPages) {
                page = totalPages;
            }
            int beginIndex = (page - 1) * entriesPerPage;
            int endIndex = Math.min(beginIndex + entriesPerPage, bans.size());
            for (int i = bans.size() - 1; i > 0; i--) {
                if (i >= beginIndex && i < endIndex) {
                    continue;
                }
                bans.remove(i);
            }
        }
        msg.append(lookupHeader.replace("{entity}", entity).replace("{module}", "Ban")
                .replace("{page}", page + "/" + totalPages));

        boolean isBan = false;
        for (final BanEntry banEntry : bans) {
            if (banEntry.active()) {
                isBan = true;
                break;
            }
        }

        // We begin with active ban
        if (isBan) {
            msg.append("&6&lActive bans: &f------------------------");
            final Iterator<BanEntry> it = bans.iterator();
            while (it.hasNext()) {
                final BanEntry ban = it.next();
                if (!ban.active()) {
                    break;
                }
                final String begin = Core.defaultDF.format(ban.beginDate());
                final String server = ban.server();
                final String reason = ban.reason();
                final String end;
                if (ban.endDate() == null) {
                    end = "Permanent Ban";
                } else {
                    end = Core.defaultDF.format(ban.endDate());
                }

                msg.append("\n");
                if (staffLookup) {
                    msg.append(i18n.format("activeStaffBanLookupRow",
                            new String[]{ban.entity(), begin, server, reason, end}));
                } else {
                    msg.append(i18n.format("activeBanLookupRow",
                            new String[]{begin, server, reason, ban.staff(), end}));
                }
                it.remove();
            }
        }

        if (!bans.isEmpty()) {
            msg.append("\n&7&lArchive bans: &f------------------------");
            for (final BanEntry ban : bans) {
                final String begin = Core.defaultDF.format(ban.beginDate());
                final String server = ban.server();
                final String reason = ban.reason();

                final String endDate;
                if (ban.endDate() == null) {
                    endDate = Core.defaultDF.format(ban.unbanDate());
                } else {
                    endDate = Core.defaultDF.format(ban.endDate());
                }
                final String unbanReason = ban.unbanReason();
                String unbanStaff = ban.unbanStaff();
                if (unbanStaff == null) {
                    unbanStaff = "Tempban";
                }

                msg.append("\n");
                if (staffLookup) {
                    msg.append(i18n.format("archiveStaffBanLookupRow",
                            new String[]{ban.entity(), begin, server, reason, endDate, unbanReason, unbanStaff}));
                } else {
                    msg.append(i18n.format("archiveBanLookupRow",
                            new String[]{begin, server, reason, ban.staff(), endDate, unbanReason, unbanStaff}));
                }
            }
        }

        msg.append(lookupFooter.replace("{entity}", entity).replace("{module}", "Ban")
                .replace("{page}", page + "/" + totalPages));

        return FormatUtilsKt.formatNewLine(ChatColor.translateAlternateColorCodes('&', msg.toString()));
    }
}
