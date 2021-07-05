package me.starmism.batr.utils

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZonedDateTime
import java.util.*
import java.util.regex.Pattern

private val gson = Gson()
    private val sb = StringBuilder()
    private val timePattern = Pattern.compile(
        "(?:([0-9]+)\\s*y[a-z]*[,\\s]*)?"
                + "(?:([0-9]+)\\s*mo[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*w[a-z]*[,\\s]*)?"
                + "(?:([0-9]+)\\s*d[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*h[a-z]*[,\\s]*)?" + "(?:([0-9]+)\\s*m[a-z]*[,\\s]*)?"
                + "(?:([0-9]+)\\s*(?:s[a-z]*)?)?", Pattern.CASE_INSENSITIVE
    ) //#y#mo#w#d#h#m#s
    private val ipPattern =
        Pattern.compile("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)")

    /**
     * Get the timestamp corresponding to the current date + this duration
     *
     * @param durationStr
     * @return timestamp in millis
     * @throws IllegalArgumentException
     */
    @Throws(IllegalArgumentException::class)
    fun parseDuration(durationStr: String?): Long {
        val m = timePattern.matcher(durationStr)
        var years = 0
        var months = 0
        var weeks = 0
        var days = 0
        var hours = 0
        var minutes = 0
        var seconds = 0
        var found = false
        while (m.find()) {
            if (m.group() == null || m.group().isEmpty()) {
                continue
            }
            for (i in 0 until m.groupCount()) {
                if (m.group(i) != null && !m.group(i).isEmpty()) {
                    found = true
                    break
                }
            }
            if (found) {
                if (m.group(1) != null && !m.group(1).isEmpty()) {
                    years = m.group(1).toInt()
                }
                if (m.group(2) != null && !m.group(2).isEmpty()) {
                    months = m.group(2).toInt()
                }
                if (m.group(3) != null && !m.group(3).isEmpty()) {
                    weeks = m.group(3).toInt()
                }
                if (m.group(4) != null && !m.group(4).isEmpty()) {
                    days = m.group(4).toInt()
                }
                if (m.group(5) != null && !m.group(5).isEmpty()) {
                    hours = m.group(5).toInt()
                }
                if (m.group(6) != null && !m.group(6).isEmpty()) {
                    minutes = m.group(6).toInt()
                }
                if (m.group(7) != null && !m.group(7).isEmpty()) {
                    seconds = m.group(7).toInt()
                }
                break
            }
        }
        require(found) { ChatColor.RED.toString() + "Invalid duration !" }
        var time = ZonedDateTime.now()
        if (years > 0) {
            time = time.plusYears(years.toLong())
        }
        if (months > 0) {
            time = time.plusMonths(months.toLong())
        }
        if (weeks > 0) {
            time = time.plusWeeks(weeks.toLong())
        }
        if (days > 0) {
            time = time.plusDays(days.toLong())
        }
        if (hours > 0) {
            time = time.plusHours(hours.toLong())
        }
        if (minutes > 0) {
            time = time.plusMinutes(minutes.toLong())
        }
        if (seconds > 0) {
            time = time.plusSeconds(seconds.toLong())
        }
        return time.toInstant().toEpochMilli()
    }

    /**
     * Get the final args from start
     *
     * @param args
     * @param start
     * @return finalArg from start
     */
    fun getFinalArg(args: Array<String?>, start: Int): String {
        for (i in start until args.size) {
            if (i != start) {
                sb.append(" ")
            }
            sb.append(args[i])
        }
        val msg = sb.toString()
        sb.setLength(0)
        return msg
    }

    /**
     * Check if a server with his name exist
     */
    fun isServer(serverName: String): Boolean {
        // We need to loop through and test the server name because the map is case insensitive
        return ProxyServer.getInstance().servers.containsKey(serverName)
    }

    fun getPlayerIP(player: ProxiedPlayer): String {
        return player.address.address.hostAddress
    }

    fun validIP(ip: String): Boolean {
        return ipPattern.matcher(ip).matches()
    }

    /**
     * Little extra for the ip lookup : get server location using freegeoip api
     */
    fun getIpDetails(ip: String): String {
        if (!validIP(ip)) {
            throw RuntimeException("$ip is not an valid ip!")
        }

        val url = URL("http://ip-api.com/json/$ip?fields=country,countryCode,city")
        with (url.openConnection() as HttpURLConnection) {
            inputStream.bufferedReader().use { response ->
                val attributes: Map<String, String> = gson.fromJson(
                    response.lineSequence().joinToString("\n"),
                    object : TypeToken<Map<String, String>>() {}.type
                )

                return """&7City: &f${attributes["city"]?.ifEmpty { "Unknown" }}&7
                    | Country: &f${attributes["country"]?.ifEmpty { "Unknown" }}&e
                    | (&f${attributes["countryCode"]?.ifEmpty { "Unknown" }}&e)""".trimMargin()
            }
        }
    }

    /**
     * Finds a player based on an input string
     *
     * @param str - either a player name, or UUID (in standard string format, with - separation)
     * @return a ProxiedPlayer with the above name or UUID. or Null if one couldn't be found.
     */
    fun getPlayer(str: String): ProxiedPlayer? {
        val out = ProxyServer.getInstance().getPlayer(str)
        if (out != null) {
            return out
        }

        //Input string is not a known player name, try it as a UUID.
        return try {
            val uuid = UUID.fromString(str)
            ProxyServer.getInstance().getPlayer(uuid)
        } catch (ex: IllegalArgumentException) {
            //Not a valid UUID - can't find player
            null
        }
    }
