package com.fuzzy.amusic.utils

import android.os.Environment
import android.util.JsonReader
import android.util.Log
import com.fuzzy.amusic.MainApplication
import com.fuzzy.amusic.MainApplication.getInstance
import com.fuzzy.amusic.database.Song
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.io.File

class ConfigParser {
    val context: MainApplication = getInstance()

    fun savePlaylist(playlist: List<Song>) {
        val playlistFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!.absolutePath + "/playlist.json")
        val playlistStr = Json.encodeToString(playlist)
        playlistFile.writeText(playlistStr)
        Log.i("ConfigParser / save playlist", "complete")
    }

    fun loadPlaylist(): List<Song> {
        val playlistFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!.absolutePath + "/playlist.json")
        return if (playlistFile.isFile) {
            val playlist: List<Song> = Json.decodeFromString(playlistFile.readText())
            Log.i("ConfigParser / load playlist", "success")
            playlist
        } else {
            Log.i("ConfigParser / load playlist", "file doesn't exist")
            listOf()
        }
    }

    fun saveConfig(config: JsonObject) {
        val configFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!.absolutePath + "/config.json")
        val configStr = Json.encodeToString(config)
        configFile.writeText(configStr)
        Log.i("ConfigParser / save config", "complete")
    }

    fun loadConfig(): JsonObject? {
        val configFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!.absolutePath + "/config.json")
        return if (configFile.isFile) {
            val config: JsonObject = Json.decodeFromString(configFile.readText())
            Log.i("ConfigParser / load playlist", "success")
            config
        } else {
            Log.i("ConfigParser / load config", "file doesn't exist")
            null
        }
    }
}