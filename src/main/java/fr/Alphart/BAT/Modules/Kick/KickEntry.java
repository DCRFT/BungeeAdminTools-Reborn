package fr.Alphart.BAT.Modules.Kick;

import java.sql.Timestamp;

public record KickEntry(String entity, String server, String reason,
						String staff, Timestamp date) {}