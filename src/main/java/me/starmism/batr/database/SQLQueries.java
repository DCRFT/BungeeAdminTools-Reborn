package me.starmism.batr.database;

/**
 * This class contains almost all sql queries used by the plugin. Each subclass
 * contains queries handled by a module. Each subclass has another subclass
 * called "SQLite" which provides compatibility with SQLite.
 */
public class SQLQueries {

    public static class Ban {
        public final static String table = "BAT_ban";
        public final static String createTable = "CREATE TABLE IF NOT EXISTS `" + table + "` ("
                + "`ban_id` INTEGER PRIMARY KEY AUTO_INCREMENT," + "`UUID` varchar(100) NULL,"
                + "`ban_ip` varchar(50) NULL,"

                + "`ban_staff` varchar(30) NOT NULL," + "`ban_reason` varchar(100) NULL,"
                + "`ban_server` varchar(30) NOT NULL," + "`ban_begin` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,"
                + "`ban_end` timestamp NULL," + "`ban_state` bool NOT NULL default 1,"

                + "`ban_unbandate` timestamp NULL," + "`ban_unbanstaff` varchar(30) NULL,"
                + "`ban_unbanreason` varchar(100) NULL,"

                + "INDEX(UUID)," + "INDEX(ban_ip)" + ") ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1;";

        // Use to check if a player is ban on a ban_server
        // Parameter : player, player's ban_ip, (ban_server)
        public static final String isBan = "SELECT ban_id FROM `" + table + "` WHERE ban_state = 1 AND UUID = ?;";
        public static final String isBanServer = "SELECT ban_id FROM `" + table + "` WHERE ban_state = 1 AND UUID = ? "
                + "AND ban_server = ?;";

        public static final String isBanIP = "SELECT ban_id FROM `" + table
                + "` WHERE ban_state = 1 AND ban_ip = ? AND UUID IS NULL;";
        public static final String isBanServerIP = "SELECT ban_id FROM `" + table
                + "` WHERE ban_state = 1 AND ban_ip = ? AND ban_server = ? AND UUID IS NULL;";

        public static final String createBan = "INSERT INTO `" + table
                + "`(UUID, ban_staff, ban_server, ban_end, ban_reason) VALUES (?, ?, ?, ?, ?);";

        public static final String createBanIP = "INSERT INTO `" + table
                + "`(ban_ip, ban_staff, ban_server, ban_end, ban_reason) VALUES (?, ?, ?, ?, ?);";

        public static final String unBan = "UPDATE `" + table
                + "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = NOW() "
                + "WHERE UUID = ? AND ban_state = 1;";
        public static final String unBanServer = "UPDATE `" + table
                + "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = NOW() "
                + "WHERE UUID = ? AND ban_server = ? AND ban_state = 1;";

        public static final String unBanIP = "UPDATE `" + table
                + "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = NOW()  "
                + "WHERE ban_ip = ? AND UUID IS NULL;";
        public static final String unBanIPServer = "UPDATE `" + table
                + "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = NOW()  "
                + "WHERE ban_ip = ? AND ban_server = ? AND UUID IS NULL;";

        public static final String getBan = "SELECT * FROM `"
                + table + "`" + " WHERE UUID = ? ORDER BY ban_state DESC, ban_end DESC;";
        public static final String getBanIP = "SELECT * FROM `"
                + table + "`" + " WHERE ban_ip = ? AND UUID IS NULL ORDER BY ban_state DESC, ban_end DESC;";

        public static final String getManagedBan = "SELECT * FROM `"
                + table + "`" + " WHERE ban_staff = ? OR ban_unbanstaff = ? ORDER BY ban_state DESC, ban_end DESC;";

        public static final String getBanMessage = "SELECT ban_reason, ban_end, ban_staff, ban_begin FROM `"
                + table + "` WHERE (UUID = ? OR ban_ip = ?) AND ban_state = 1 AND ban_server = ?;";

        public static final String updateExpiredBan = "UPDATE `" + table + "` SET ban_state = 0 "
                + "WHERE ban_state = 1 AND (ban_end != 0 AND ban_end < NOW());";

        public static class SQLite {
            // Ban related
            public final static String[] createTable = {
                    "CREATE TABLE IF NOT EXISTS `" + table + "` (" + "`ban_id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "`UUID` varchar(100) NULL," + "`ban_ip` varchar(50) NULL,"

                            + "`ban_staff` varchar(30) NOT NULL," + "`ban_reason` varchar(100) NULL,"
                            + "`ban_server` varchar(30) NOT NULL,"
                            + "`ban_begin` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL," + "`ban_end` timestamp NULL,"
                            + "`ban_state` bool NOT NULL default 1,"

                            + "`ban_unbandate` timestamp NULL," + "`ban_unbanstaff` varchar(30) NULL,"
                            + "`ban_unbanreason` varchar(100) NULL" + ");",
                    "CREATE INDEX IF NOT EXISTS `ban.uuid_index` ON " + table + " (`UUID`);",
                    "CREATE INDEX IF NOT EXISTS `ban.ip_index` ON " + table + " (`ban_ip`);"};

            public static final String unBan = "UPDATE `" + table
                    + "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = datetime() "
                    + "WHERE UUID = ? AND ban_state = 1;";
            public static final String unBanIP = "UPDATE `" + table
                    + "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = datetime()  "
                    + "WHERE ban_ip = ? AND UUID IS NULL;";
            public static final String unBanIPServer = "UPDATE `" + table
                    + "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = datetime()  "
                    + "WHERE ban_ip = ? AND ban_server = ? AND UUID IS NULL;";
            public static final String unBanServer = "UPDATE `" + table
                    + "` SET ban_state = 0, ban_unbanreason = ?, ban_unbanstaff = ?, ban_unbandate = datetime() "
                    + "WHERE UUID = ? AND ban_server = ? AND ban_state = 1;";

            public static final String getBan = "SELECT *, "
                    + "strftime('%s',ban_begin), strftime('%s',ban_end), strftime('%s',ban_unbandate) "
                    + "FROM `" + table + "`" + " WHERE UUID = ? ORDER BY ban_state DESC, ban_end DESC;";
            public static final String getBanIP = "SELECT *, "
                    + "strftime('%s',ban_begin), strftime('%s',ban_end), strftime('%s',ban_unbandate) "
                    + "FROM `" + table + "`" + " WHERE ban_ip = ? AND UUID IS NULL ORDER BY ban_state DESC, ban_end DESC;";

            public static final String getBanMessage = "SELECT ban_reason, ban_staff, ban_end, strftime('%s',ban_begin) FROM `"
                    + table + "` WHERE (UUID = ? OR ban_ip = ?) AND ban_state = 1 AND ban_server = ?;";

            public static final String getManagedBan = "SELECT *, "
                    + "strftime('%s',ban_begin), strftime('%s',ban_end), strftime('%s',ban_unbandate) "
                    + "FROM `" + table + "`" + " WHERE ban_staff = ? OR ban_unbanstaff = ? ORDER BY ban_state DESC, ban_end DESC;";

            public static final String updateExpiredBan = "UPDATE `" + table + "` SET ban_state = 0 "
                    + "WHERE ban_state = 1 AND (ban_end != 0 AND (ban_end / 1000) < CAST(strftime('%s', 'now') as integer));";
        }
    }


    public static class Core {
        public static final String table = "BAT_players";

        public static final String createTable = "CREATE TABLE IF NOT EXISTS `" + table + "` ("
                + "`BAT_player` varchar(30) NOT NULL," + "`UUID` varchar(100) UNIQUE NOT NULL,"
                + "`lastip` varchar(50) NOT NULL," + "`firstlogin` timestamp NULL,"
                + "`lastlogin` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," + "INDEX(BAT_player)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
        public static final String updateIPUUID = "INSERT INTO `" + table + "` (BAT_player, lastip, firstlogin, UUID)"
                + " VALUES (?, ?, NOW(), ?) ON DUPLICATE KEY UPDATE lastip = ?, lastlogin = NOW(), BAT_player = ?;";

        public static final String getPlayerName = "SELECT BAT_player FROM `" + table + "` WHERE UUID = ?;";

        public static final String getIP = "SELECT lastip FROM `" + table + "` WHERE UUID = ?;";

        public static final String getUUID = "SELECT UUID FROM `" + table + "` WHERE BAT_player = ?;";

        public static final String getPlayerData = "SELECT lastip, firstlogin, lastlogin FROM `" + table
                + "` WHERE UUID = ?;";

        public static final String getIpUsers = "SELECT BAT_player FROM `" + table + "` WHERE lastip = ?";

        public static class SQLite {
            public static final String[] createTable = {
                    "CREATE TABLE IF NOT EXISTS `" + table + "` (" + "`BAT_player` varchar(30) NOT NULL,"
                            + "`UUID` varchar(100) UNIQUE NOT NULL," + "`lastip` varchar(50) NOT NULL,"
                            + "`firstlogin` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,"
                            + "`lastlogin` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL" + ");",
                    "CREATE INDEX IF NOT EXISTS `core.player_index` ON " + table + " (`BAT_player`);"};
            public static final String updateIPUUID = "INSERT OR REPLACE INTO `" + table
                    + "` (BAT_player, lastip, firstlogin, lastlogin, UUID)"
                    + " VALUES (?, ?, (SELECT firstlogin FROM `" + table + "` WHERE UUID = ?), DATETIME(), ?);";
            public static final String getPlayerData = "SELECT strftime('%s',firstlogin), strftime('%s',lastlogin), lastip FROM `"
                    + table + "` WHERE UUID = ?;";
        }
    }
}