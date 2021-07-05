package me.starmism.batr.utils

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import me.starmism.batr.modules.core.Core
import net.md_5.bungee.api.ProxyServer
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

private val gson = Gson()
private val httpClient = HttpClient.newHttpClient()

    fun getUUID(pName: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI("https://api.mojang.com/users/profiles/minecraft/$pName")).build()
        val response = httpClient.send(request, BodyHandlers.ofString()).body()
        return gson.fromJson(response, MojangUUIDProfile::class.java).id
    }

    /**
     * Fetch a player's name history from **Mojang's server : high latency**
     *
     * @param pName The player's name to search for
     */
    fun getPlayerNameHistory(pName: String): List<String> {
        if (!ProxyServer.getInstance().config.isOnlineMode) {
            throw RuntimeException("Can't get player name history from an offline server!")
        }

        val request = HttpRequest.newBuilder()
            .uri(URI("https://api.mojang.com/user/profiles/${Core.getUUID(pName)}/names")).build()
        val response = httpClient.send(request, BodyHandlers.ofString()).body()
        return gson.fromJson<Set<Map<String, Any>>>(response, object : TypeToken<Set<Map<String, Any>>>() {}.type)
            .map { it["name"].toString() }
    }

    private data class MojangUUIDProfile(var id: String, var name: String)