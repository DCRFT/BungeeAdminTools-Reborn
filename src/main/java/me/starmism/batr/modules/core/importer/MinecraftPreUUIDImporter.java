package me.starmism.batr.modules.core.importer;

import me.starmism.batr.BATR;
import me.starmism.batr.database.SQLQueries;
import me.starmism.batr.modules.IModule;
import me.starmism.batr.utils.CallbackUtils.ProgressCallback;
import me.starmism.batr.utils.UUIDNotFoundException;
import me.starmism.batr.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MinecraftPreUUIDImporter extends Importer {
    private static final DateFormat dfMc1v6 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");

    @Override
    protected void importData(ProgressCallback<ImportStatus> progressionCallback, final String... additionalsArgs) throws Exception {
        try (Connection conn = BATR.getConnection()) {
            // Check if either the banned-players.txt or the banned-ips.txt file exists
            if (!new File(BATR.getInstance().getDataFolder(), "banned-players.txt").exists()
                    && !new File(BATR.getInstance().getDataFolder(), "banned-ips.txt").exists()) {
                throw new IllegalArgumentException("You must put either banned-players.txt or banned-ips.txt file into BATR folder to "
                        + "import your datas.");
            }

            // Count the totalEntries which need to be converted
            int totalEntries = 0;
            final List<File> filesToConvert = new ArrayList<>();
            if (new File(BATR.getInstance().getDataFolder(), "banned-players.txt").exists()) {
                filesToConvert.add(new File(BATR.getInstance().getDataFolder(), "banned-players.txt"));
            }
            if (new File(BATR.getInstance().getDataFolder(), "banned-ips.txt").exists()) {
                filesToConvert.add(new File(BATR.getInstance().getDataFolder(), "banned-ips.txt"));
            }
            for (final File file : filesToConvert) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    while (br.readLine() != null) {
                        totalEntries++;
                    }
                    totalEntries -= 3; // Number of lines which are comment or blank in default mc ban file
                }
            }
            status = new ImportStatus(totalEntries);

            // Init the reader and some code
            final PreparedStatement insertBans = conn.prepareStatement("INSERT INTO `" + SQLQueries.Ban.table
                    + "`(UUID, ban_ip, ban_staff, ban_server, ban_begin, ban_end, ban_reason) VALUES (?, ?, ?, ?, ?, ?, ?);");
            BufferedReader brPlayer = null;
            if (new File(BATR.getInstance().getDataFolder(), "banned-players.txt").exists()) {
                brPlayer = new BufferedReader(new FileReader(new File(BATR.getInstance().getDataFolder(), "banned-players.txt")));
            }
            BufferedReader brIPs = null;
            if (new File(BATR.getInstance().getDataFolder(), "banned-ips.txt").exists()) {
                brIPs = new BufferedReader(new FileReader(new File(BATR.getInstance().getDataFolder(), "banned-ips.txt")));
            }

            // Process the import
            String line;
            conn.setAutoCommit(false);
            int uncommittedEntries = 0;
            while ((brPlayer != null && (line = brPlayer.readLine()) != null)
                    || (brIPs != null && (line = brIPs.readLine()) != null)) {
                try {
                    final Minecraft1v6_BanRecord banRecord = new Minecraft1v6_BanRecord(line);
                    insertBans.setString(1, banRecord.getUuid());
                    insertBans.setString(2, banRecord.getIp());
                    insertBans.setString(3, banRecord.getStaffBan());
                    insertBans.setString(4, IModule.GLOBAL_SERVER);
                    insertBans.setTimestamp(5, banRecord.getBeginBan());
                    insertBans.setTimestamp(6, banRecord.getExpirationBan());
                    insertBans.setString(7, banRecord.getReason());
                    insertBans.executeUpdate();
                    insertBans.clearParameters();
                    uncommittedEntries++;

                    if (banRecord.getUuid() != null) {
                        initPlayerRowInBatPlayer(conn, banRecord.getPName(), banRecord.getUuid());
                    }
                } catch (final RuntimeException e) {
                    if (!"commentline".equals(e.getMessage())) {
                        progressionCallback.onMinorError(e.getMessage());
                    }
                }
                if (uncommittedEntries % 100 == 0) {
                    conn.commit();
                    status.incrementConvertedEntries(uncommittedEntries);
                    uncommittedEntries = 0;
                    progressionCallback.onProgress(status);
                }
            }

            conn.commit();
            status.incrementConvertedEntries(uncommittedEntries);
            progressionCallback.done(status, null);
        } catch (final IOException e) {
            BATR.getInstance().getLogger().severe("An error related to files occurred during the import of Minecraft v1.6 ban records:");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private class Minecraft1v6_BanRecord {
        private final String reason;
        private final Timestamp beginBan;
        private final Timestamp expirationBan;
        private String uuid = null;
        private String pName = null;
        private String ip = null;
        private String staffBan;

        /**
         * Parse a line of banned-xx.txt file
         *
         * @param line
         */
        public Minecraft1v6_BanRecord(final String line) throws RuntimeException {
            if (line.startsWith("#") || line.isEmpty()) {
                throw new RuntimeException("commentline");
            }
            final String[] splittedLine = line.split("\\|");
            if (splittedLine.length != 5) {
                throw new RuntimeException("Invalid ban format. The import process will continue...");
            }
            if (Utils.validIP(splittedLine[0])) {
                ip = splittedLine[0];
            } else {
                try {
                    pName = splittedLine[0];
                    uuid = uuidCache.get(splittedLine[0]);
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof UUIDNotFoundException) {
                        throw new RuntimeException("The uuid of " + splittedLine[0] + " wasn't found. The import process will continue...");
                    }
                }
            }
            try {
                beginBan = new Timestamp(dfMc1v6.parse(splittedLine[1]).getTime());
                expirationBan = (splittedLine[3].equals("Forever")) ? null : new Timestamp(dfMc1v6.parse(splittedLine[3]).getTime());
            } catch (final ParseException e) {
                throw new RuntimeException("Invalid ban format. The import process will continue ...");
            }
            staffBan = (splittedLine[2].equals("(Unknown)")) ? "CONSOLE" : splittedLine[2];
            if (staffBan.length() > 20) {
                staffBan = staffBan.substring(0, 20);
            }
            reason = splittedLine[4];
        }

        public String getUuid() {
            return this.uuid;
        }

        public String getPName() {
            return this.pName;
        }

        public String getIp() {
            return this.ip;
        }

        public String getReason() {
            return this.reason;
        }

        public String getStaffBan() {
            return this.staffBan;
        }

        public Timestamp getBeginBan() {
            return this.beginBan;
        }

        public Timestamp getExpirationBan() {
            return this.expirationBan;
        }
    }
}