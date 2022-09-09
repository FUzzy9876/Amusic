package com.fuzzy.amusic.utils

import android.os.Environment
import android.util.Log
import com.fuzzy.amusic.MainApplication.getInstance
import com.fuzzy.amusic.database.Song
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ConfigParser {
    val context = getInstance()

    fun savePlaylist(playlist: List<Song>) {
        val playlistFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!.absolutePath + "/playlist.json")
        val playlistStr = Json.encodeToString(playlist)
        playlistFile.writeText(playlistStr)
        Log.i("savePlaylist", "complete")
    }

    fun loadPlaylist(): List<Song> {
        val playlistFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!.absolutePath + "/playlist.json")
        return if (playlistFile.isFile) {
            val playlistStr = playlistFile.readText()
            val playlist: List<Song> = Json.decodeFromString(playlistStr)
            Log.i("loadPlaylist", "$playlist")
            playlist
        } else {
            Log.i("loadPlaylist", "file doesn't exist")
            listOf()
        }
    }
}