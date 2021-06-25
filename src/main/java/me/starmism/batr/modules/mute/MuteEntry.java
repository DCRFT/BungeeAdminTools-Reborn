package me.starmism.batr.modules.mute;

import java.sql.Timestamp;

public record MuteEntry(String entity, String server, String reason,
						String staff, Timestamp beginDate, Timestamp endDate,
						Timestamp unmuteDate, String unmuteReason, String unmuteStaff,
						boolean active) {}
