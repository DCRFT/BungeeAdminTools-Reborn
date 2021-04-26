package fr.Alphart.BAT.Utils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private static Gson gson = new Gson();
	private static StringBuilder sb = new StringBuilder();
	private static Pattern ipPattern = Pattern
			.compile("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)");
	private final static Pattern timePattern = Pattern.compile("(?:([0-9]+)\\s*y[a-z]*[,\\s]*)?"
			+ "(?:([0-9]+)\\s*mo[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*w[a-z]*[,\\s]*)?"
			+ "(?:([0-9]+)\\s*d[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*h[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*m[a-z]*[,\\s]*)?"
			+ "(?:([0-9]+)\\s*(?:s[a-z]*)?)?", Pattern.CASE_INSENSITIVE); //#y#mo#w#d#h#m#s

	/**
	 * Get the timestamp corresponding to the current date + this duration
	 *
	 * @param durationStr
	 * @return timestamp in millis
	 * @throws IllegalArgumentException
	 */
	public static long parseDuration(final String durationStr) throws IllegalArgumentException {
		final Matcher m = timePattern.matcher(durationStr);
		int years = 0;
		int months = 0;
		int weeks = 0;
		int days = 0;
		int hours = 0;
		int minutes = 0;
		int seconds = 0;
		boolean found = false;
		while (m.find()) {
			if (m.group() == null || m.group().isEmpty()) {
				continue;
			}
			for (int i = 0; i < m.groupCount(); i++) {
				if (m.group(i) != null && !m.group(i).isEmpty()) {
					found = true;
					break;
				}
			}
			if (found) {
				if (m.group(1) != null && !m.group(1).isEmpty()) {
					years = Integer.parseInt(m.group(1));
				}
				if (m.group(2) != null && !m.group(2).isEmpty()) {
					months = Integer.parseInt(m.group(2));
				}
				if (m.group(3) != null && !m.group(3).isEmpty()) {
					weeks = Integer.parseInt(m.group(3));
				}
				if (m.group(4) != null && !m.group(4).isEmpty()) {
					days = Integer.parseInt(m.group(4));
				}
				if (m.group(5) != null && !m.group(5).isEmpty()) {
					hours = Integer.parseInt(m.group(5));
				}
				if (m.group(6) != null && !m.group(6).isEmpty()) {
					minutes = Integer.parseInt(m.group(6));
				}
				if (m.group(7) != null && !m.group(7).isEmpty()) {
					seconds = Integer.parseInt(m.group(7));
				}
				break;
			}
		}
		if (!found) {
			throw new IllegalArgumentException(ChatColor.RED + "Invalid duration !");
		}
		ZonedDateTime time = ZonedDateTime.now();
		if (years > 0) {
			time = time.plusYears(years);
		}
		if (months > 0) {
			time = time.plusMonths(months);
		}
		if (weeks > 0) {
			time = time.plusWeeks(weeks);
		}
		if (days > 0) {
			time = time.plusDays(days);
		}
		if (hours > 0) {
			time = time.plusHours(hours);
		}
		if (minutes > 0) {
			time = time.plusMinutes(minutes);
		}
		if (seconds > 0) {
			time = time.plusSeconds(seconds);
		}
		return time.toInstant().toEpochMilli();
	}

	/**
	 * Get the final args from start
	 *
	 * @param args
	 * @param start
	 * @return finalArg from start
	 */
	public static String getFinalArg(final String[] args, final int start) {
		for (int i = start; i < args.length; i++) {
			if (i != start) {
				sb.append(" ");
			}
			sb.append(args[i]);
		}
		final String msg = sb.toString();
		sb.setLength(0);
		return msg;
	}

	/**
	 * Check if a server with his name exist
	 *
	 * @return
	 */
	public static boolean isServer(final String serverName) {
		// We need to loop through and test the server name because the map is case insensitive
		return ProxyServer.getInstance().getServers().containsKey(serverName);
	}

	public static String getPlayerIP(final ProxiedPlayer player) {
		return player.getAddress().getAddress().getHostAddress();
	}

	public static boolean validIP(final String ip) {
		return ipPattern.matcher(ip).matches();
	}

	/**
	 * Little extra for the ip lookup : get server location using freegeoip api
	 * @param ip
	 * @return
	 */
	public static String getIpDetails(final String ip){
		if(!validIP(ip)){
			throw new RuntimeException(ip + " is not an valid ip!");
		}
		// Fetch player's name history from Mojang servers
		BufferedReader reader = null;
		try{
			final URL geoApiURL = new URL("http://ip-api.com/json/" + ip + "?fields=country,countryCode,city");
			final URLConnection conn = geoApiURL.openConnection();
			conn.setConnectTimeout(5000);
			reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String content = "";
			String line;
			while((line = reader.readLine()) != null){
				content += line;
			}
			final Map<String, Object> attributes = gson.fromJson(content, new TypeToken<Map<String, Object>>() {}.getType());
			String city = !((String)attributes.get("city")).isEmpty()
					? (String)attributes.get("city")
					: "unknown";
			String country = !((String)attributes.get("country")).isEmpty()
					? (String)attributes.get("country")
					: "unknown";
			String country_code = !((String)attributes.get("countryCode")).isEmpty()
					? (String)attributes.get("countryCode")
					: "unknown";
			return "&7City: &f" + city + "&7 Country: &f" + country +
					" &e(&f" + country_code + "&e)";
		}catch(final IOException e){
			throw new RuntimeException(e);
		}finally{
			if(reader != null){
				try {
					reader.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	/**
	 * Finds a player based on an input string
	 * @param str - either a player name, or UUID (in standard string format, with - separation)
	 * @return a ProxiedPlayer with the above name or UUID. or Null if one couldn't be found.
	 */
	public static ProxiedPlayer getPlayer(String str) {
		ProxiedPlayer out = ProxyServer.getInstance().getPlayer(str);
		if (out==null) {
			//Input string is not a known player name, try it as a UUID.
			try {
				UUID uuid = UUID.fromString(str);
				out = ProxyServer.getInstance().getPlayer(uuid);
			} catch (IllegalArgumentException ex) {
				//Not a valid UUID - can't find player
				out = null;
			}
		}
		return out;
	}
}